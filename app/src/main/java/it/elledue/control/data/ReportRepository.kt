package it.elledue.control.data

import android.content.Context
import android.util.Base64
import androidx.room.withTransaction
import it.elledue.control.R
import it.elledue.control.domain.*
import it.elledue.control.reports.*
import java.util.UUID

class ReportRepository(private val db: AppDatabase, private val context: Context) {
    private val dao = db.reportDao()
    val reports = dao.observe()
    suspend fun generate(property: String, month: String): String = db.withTransaction {
        Competence.month(month); requireNotNull(db.propertyDao().get(property)) { "Seleziona un immobile." }
        val versions = dao.period(property, month)
        versions.firstOrNull { it.status == "DRAFT" }?.let { refreshLocked(it); return@withTransaction it.id }
        require(versions.none { it.status == "CONFIRMED" }) { "Periodo già confermato: apri una nuova versione rettificativa." }
        val report = ReportEntity(UUID.randomUUID().toString(), property, month, (versions.maxOfOrNull { it.version } ?: 0) + 1, "DRAFT", null, null)
        dao.save(report); refreshLocked(report); report.id
    }
    suspend fun beginRevision(id: String, reason: String): String = db.withTransaction {
        require(reason.isNotBlank()) { "Motiva la versione rettificativa." }
        val original = requireNotNull(dao.get(id)); require(original.status == "CONFIRMED")
        val versions = dao.period(original.propertyId, original.yearMonth)
        require(versions.filter { it.status == "CONFIRMED" }.maxOf { it.version } == original.version) { "Apri la rettifica della versione più recente." }
        versions.firstOrNull { it.status == "DRAFT" }?.let { return@withTransaction it.id }
        val report = ReportEntity(UUID.randomUUID().toString(), original.propertyId, original.yearMonth, versions.maxOf { it.version } + 1, "DRAFT", null, null, original.id, reason.trim(), original.notes)
        dao.save(report); refreshLocked(report); report.id
    }
    suspend fun refresh(id: String, notes: String? = null): ReportSnapshot = db.withTransaction {
        val report = requireNotNull(dao.get(id)); require(report.status == "DRAFT") { "La versione confermata è immutabile." }
        refreshLocked(if (notes != null) report.copy(notes = notes.trim()) else report)
    }
    private suspend fun refreshLocked(report: ReportEntity): ReportSnapshot {
        val property = requireNotNull(db.propertyDao().get(report.propertyId))
        val allBookings = dao.bookings()
        val bookings = allBookings.filter { it.propertyId == report.propertyId && Competence.booking(it.checkInEpochDay) == report.yearMonth }.sortedWith(compareBy({ it.checkInEpochDay }, { it.id }))
        val costs = dao.expenses().filter { it.propertyId == report.propertyId && it.competenceMonth == report.yearMonth && !it.voided }.sortedWith(compareBy({ it.dateEpochDay }, { it.id }))
        val allMoney = db.accountingDao().movements().filter { it.propertyId == report.propertyId }
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
            linkedBookings = allBookings.filter { b -> b.propertyId == report.propertyId && bookings.none { it.id == b.id } && (costs.any { it.reservationId == b.id } || allMoney.any { it.id in ids && it.reservationId == b.id }) }.sortedBy { it.id })
        dao.save(report.copy(snapshotJson = ReportCodec.encode(snapshot))); return snapshot
    }
    suspend fun confirm(id: String, previewHash: String, acknowledgeWarnings: Boolean): ReportSnapshot = db.withTransaction {
        val report = requireNotNull(dao.get(id)); require(report.status == "DRAFT") { "Rendiconto già confermato." }
        val snapshot = refreshLocked(report)
        require(ReportCodec.hash(ReportCodec.encode(snapshot)) == previewHash) { "I dati sorgente sono cambiati: aggiorna e controlla la bozza prima di confermare." }
        val issues = snapshot.issues(); require(issues.none { it.blocking }) { "Risolvi le criticità bloccanti." }
        require(issues.isEmpty() || acknowledgeWarnings) { "Verifica e riconosci esplicitamente gli avvisi." }
        snapshot.allocations().forEach { item ->
            val existing = dao.allocation(item.kind, item.sourceId)
            require(existing == null || existing.propertyId == item.propertyId && existing.yearMonth == item.yearMonth) { "Voce già attribuita a un altro periodo." }
            if (existing == null) dao.allocate(item)
        }
        val frozen = snapshot.copy(draft = false, issuedAt = System.currentTimeMillis())
        dao.save(report.copy(status = "CONFIRMED", snapshotJson = ReportCodec.encode(frozen), confirmedAt = frozen.issuedAt))
        frozen
    }
    suspend fun snapshot(id: String): ReportSnapshot = ReportCodec.decode(requireNotNull(dao.get(id)?.snapshotJson) { "Aggiorna la bozza." })
    suspend fun unassigned(): List<ReservationEntity> {
        val assigned = dao.allocations().filter { it.kind == "BOOKING" }.map { it.sourceId }.toSet()
        return dao.bookings().filter { it.status != "CANCELLED" && it.id !in assigned }
    }
}
