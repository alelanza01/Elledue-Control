package it.elledue.control.data

import android.content.Context
import android.util.Base64
import androidx.room.withTransaction
import it.elledue.control.R
import it.elledue.control.domain.*
import it.elledue.control.reports.*
import java.util.UUID
import kotlinx.coroutines.flow.first

class ReportRepository(private val db: AppDatabase, private val context: Context) {
    private val dao = db.reportDao()
    val reports = dao.observe()
    suspend fun generate(property: String, month: String): String = db.withTransaction {
        Competence.month(month); val selected=requireNotNull(db.propertyDao().get(property)) { "Seleziona un immobile." }; require(selected.property.businessType!="OWNED") {"Gli immobili propri non hanno un rendiconto per proprietario esterno."}
        val versions = dao.period(property, month)
        versions.firstOrNull { it.status == "DRAFT" }?.let { refreshLocked(it); return@withTransaction it.id }
        require(versions.none { it.status == "CONFIRMED" }) { "Periodo già confermato: apri una nuova versione rettificativa." }
        val report = ReportEntity(UUID.randomUUID().toString(), property, month, (versions.maxOfOrNull { it.version } ?: 0) + 1, "DRAFT", null, null)
        dao.save(report); refreshLocked(report); report.id
    }
    suspend fun beginRevision(id: String, reason: String): String {
        val result=db.withTransaction {
        require(reason.isNotBlank()) { "Motiva la versione rettificativa." }
        val original = requireNotNull(dao.get(id)); require(original.status == "CONFIRMED")
        val versions = dao.period(original.propertyId, original.yearMonth)
        require(versions.filter { it.status == "CONFIRMED" }.maxOf { it.version } == original.version) { "Apri la rettifica della versione più recente." }
        versions.firstOrNull { it.status == "DRAFT" }?.let { return@withTransaction it.id }
        val report = ReportEntity(UUID.randomUUID().toString(), original.propertyId, original.yearMonth, versions.maxOf { it.version } + 1, "DRAFT", null, null, original.id, reason.trim(), original.notes)
        dao.save(report); dao.options(original.id).forEach{dao.option(it.copy(reportId=report.id))}; refreshLocked(report); report.id
        }
        DraftStore.get(context).clear("report/$id#reason")
        return result
    }
    suspend fun refresh(id: String, notes: String? = null): ReportSnapshot {
        val snapshot=db.withTransaction {
        val report = requireNotNull(dao.get(id)); require(report.status == "DRAFT") { "La versione confermata è immutabile." }
        refreshLocked(if (notes != null) report.copy(notes = notes.trim()) else report)
        }
        if(notes!=null)DraftStore.get(context).clear("report/$id#notes")
        return snapshot
    }
    private suspend fun refreshLocked(report: ReportEntity,overrideOptions:List<ReportBookingOption>?=null,persist:Boolean=true): ReportSnapshot {
        val property = requireNotNull(db.propertyDao().get(report.propertyId))
        val options=overrideOptions ?: dao.options(report.id)
        val excluded=options.filter{it.excluded}.map{it.reservationId}.toSet()
        val hidden=options.filter{it.hideDetails && !it.excluded}.map{it.reservationId}
        val allBookings = dao.bookings()
        val bookings = allBookings.filter { it.propertyId == report.propertyId && Competence.booking(it.checkInEpochDay) == report.yearMonth && it.id !in excluded }.sortedWith(compareBy({ it.checkInEpochDay }, { it.id }))
        val allCosts=dao.expenses()
        val projection=StandardCostProjection.forReport(db,report,bookings,property.property,allCosts)
        val costs = allCosts.filter { it.propertyId == report.propertyId && it.competenceMonth == report.yearMonth && !it.voided && it.reservationId !in excluded }.sortedWith(compareBy({ it.dateEpochDay }, { it.id }))
        val allMoney = db.accountingDao().movements().filter { it.propertyId == report.propertyId && it.reservationId !in excluded }
        val related = allMoney.filter { it.reservationId in bookings.map { b -> b.id } || it.competenceMonth == report.yearMonth }
        // Include source/reversal dependencies so active recognition is evaluated correctly, across months.
        val ids = related.map { it.id }.toMutableSet()
        var changed: Boolean
        do { val before = ids.size; allMoney.filter { it.id in ids || it.reversesId in ids || it.id in related.mapNotNull { m -> m.sourceId } }.forEach { ids.add(it.id); it.sourceId?.let(ids::add); it.reversesId?.let(ids::add) }; changed = before != ids.size } while (changed)
        val logo = context.assets.open("brand/elledue_logo_gold.png").use { Base64.encodeToString(it.readBytes(), Base64.NO_WRAP) }
        val organization = db.openHelper.readableDatabase.query("SELECT organizationName FROM app_settings WHERE id=1").use { if (it.moveToFirst()) it.getString(0) else "Elledue Management" }
        val snapshot = ReportSnapshot(id = report.id, month = report.yearMonth, version = report.version, previousId = report.previousId, revisionReason = report.revisionReason,
            issuedAt = report.createdAt, property = property.property, owner = property.owner, organization = organization, logoBase64 = logo, notes = report.notes,
            bookings = bookings, expenses = costs, taxes = dao.taxes().filter { t -> bookings.any { it.id == t.reservationId } }, movements = allMoney.filter { it.id in ids }.sortedBy { it.id },
            linkedBookings = allBookings.filter { b -> b.propertyId == report.propertyId && bookings.none { it.id == b.id } && (costs.any { it.reservationId == b.id } || allMoney.any { it.id in ids && it.reservationId == b.id }) }.sortedBy { it.id }, hiddenBookingIds=hidden, excludedBookings=allBookings.filter{it.id in excluded},bookingOptions=options,unconfirmedCosts=emptyList(),proposedExpenses=projection.proposed,relatedExpenses=projection.related,automationWarnings=projection.warnings)
        if(persist)dao.save(report.copy(snapshotJson = ReportCodec.encode(snapshot))); return snapshot
    }
    suspend fun previewBookingOption(reportId:String,bookingId:String,hideDetails:Boolean,excluded:Boolean):ReportSnapshot=db.withTransaction {
        val report=requireNotNull(dao.get(reportId));require(report.status=="DRAFT")
        val options=dao.options(reportId).filter{it.reservationId!=bookingId}+ReportBookingOption(reportId,bookingId,hideDetails,excluded)
        refreshLocked(report,options,false)
    }
    suspend fun setBookingOption(reportId:String,bookingId:String,hideDetails:Boolean,excluded:Boolean,previewHash:String?=null,reason:String="")=db.withTransaction {
        val report=requireNotNull(dao.get(reportId));require(report.status=="DRAFT"){"Apri prima una versione rettificativa: la versione confermata resta immutabile."}
        val booking=requireNotNull(db.reservationDao().get(bookingId))
        require(booking.propertyId==report.propertyId)
        require(booking.competenceMonth==report.yearMonth || dao.expenses().any{it.reservationId==bookingId && it.competenceMonth==report.yearMonth} || db.accountingDao().movements().any{it.reservationId==bookingId && it.competenceMonth==report.yearMonth})
        if(previewHash!=null)require(ReportCodec.hash(ReportCodec.encode(previewBookingOption(reportId,bookingId,hideDetails,excluded)))==previewHash){"I dati sono cambiati: verifica nuovamente l’anteprima."}
        val old=dao.options(reportId).firstOrNull{it.reservationId==bookingId}
        require(!excluded || reason.isNotBlank()){ "Motiva l’esclusione interna dal rendiconto proprietario." }
        val history=org.json.JSONArray(old?.auditJson ?: "[]").put(org.json.JSONObject().put("at",System.currentTimeMillis()).put("excluded",excluded).put("hideDetails",hideDetails).put("reason",reason.trim()))
        dao.option(ReportBookingOption(reportId,bookingId,hideDetails,excluded,reason.trim(),history.toString()));refreshLocked(report)
    }
    suspend fun refreshDrafts() {
        val ids=mutableListOf<String>();db.openHelper.readableDatabase.query("SELECT id FROM reports WHERE status='DRAFT'").use{c->while(c.moveToNext())ids.add(c.getString(0))}
        ids.forEach{id->runCatching{refresh(id)}}
    }
    suspend fun confirm(id: String, previewHash: String, acknowledgeWarnings: Boolean, approveStandards:Boolean=false): ReportSnapshot = db.withTransaction {
        val report = requireNotNull(dao.get(id)); require(report.status == "DRAFT") { "Rendiconto già confermato." }
        val snapshot = refreshLocked(report)
        require(ReportCodec.hash(ReportCodec.encode(snapshot)) == previewHash) { "I dati sorgente sono cambiati: aggiorna e controlla la bozza prima di confermare." }
        val issues = snapshot.issues(); require(issues.none { it.blocking }) { "Risolvi le criticità bloccanti." }
        require(issues.isEmpty() || acknowledgeWarnings) { "Verifica e riconosci esplicitamente gli avvisi." }
        require(snapshot.proposedExpenses.orEmpty().isEmpty() || approveStandards){"Approva in blocco i costi standard proposti prima di confermare."}
        var definitive=snapshot
        if(snapshot.proposedExpenses.orEmpty().isNotEmpty()) {
            val batch=StandardApproval(UUID.randomUUID().toString(),report.id,System.currentTimeMillis(),db.settingsDao().get()?.let{listOf(it.operatorFirstName,it.operatorLastName).filter{n->n.isNotBlank()}.joinToString(" ")}?.ifBlank{null} ?: "Operatore locale",com.google.gson.Gson().toJson(snapshot.proposedExpenses.orEmpty().map{it.originKey}),previewHash)
            db.accountingDao().seedCategories(AccountingDefaults.categories)
            db.openHelper.writableDatabase.execSQL("INSERT INTO standard_approvals VALUES (?,?,?,?,?,?)",arrayOf(batch.id,batch.reportId,batch.approvedAt,batch.actor,batch.sourcesJson,batch.previewHash))
            snapshot.proposedExpenses.orEmpty().forEach{proposal->
                require(dao.expenses().none{it.reservationId==proposal.reservationId && it.kind==proposal.kind}){"I costi sono cambiati: verifica nuovamente la bozza."}
                val approved=proposal.copy(recognition="STANDARD_APPROVED",approvalBatchId=batch.id,updatedAt=batch.approvedAt)
                db.accountingDao().saveExpense(approved)
                db.accountingDao().insertExpenseRevision(ExpenseRevisionEntity(UUID.randomUUID().toString(),approved.id,1,AccountingRepository.snapshot(approved),"Approvazione in blocco nel rendiconto ${report.yearMonth} · ${batch.actor}",batch.approvedAt))
            }
            definitive=refreshLocked(report).copy(approvalBatch=batch)
        }
        definitive.allocations().forEach { item ->
            val existing = dao.allocation(item.kind, item.sourceId)
            require(existing == null || existing.propertyId == item.propertyId && existing.yearMonth == item.yearMonth) { "Voce già attribuita a un altro periodo." }
            if (existing == null) dao.allocate(item)
        }
        val frozen = definitive.copy(draft = false, issuedAt = System.currentTimeMillis())
        dao.save(report.copy(status = "CONFIRMED", snapshotJson = ReportCodec.encode(frozen), confirmedAt = frozen.issuedAt))
        frozen
    }
    suspend fun snapshot(id:String):ReportSnapshot=db.withTransaction {
        val report=requireNotNull(dao.get(id));if(report.status=="DRAFT")refreshLocked(report) else ReportCodec.decode(requireNotNull(report.snapshotJson))
    }
    suspend fun unassigned(): List<ReservationEntity> {
        val assigned = dao.allocations().filter { it.kind == "BOOKING" }.map { it.sourceId }.toSet()
        return dao.bookings().filter { it.status != "CANCELLED" && it.id !in assigned }
    }
}
