package it.elledue.control.data

import androidx.room.withTransaction
import it.elledue.control.domain.*
import org.json.JSONObject
import java.util.UUID
import kotlinx.coroutines.flow.first

class AccountingRepository(private val db: AppDatabase,private val drafts:DraftStore?=null) {
    private val dao = db.accountingDao()
    val financialChanges = dao.observeFinancialChanges()
    val expenses = dao.observeExpenses(); val categories = dao.observeCategories()
    val assessments = dao.observeAssessments(); val movements = dao.observeMovements()
    suspend fun initialize() = dao.seedCategories(AccountingDefaults.categories)
    suspend fun saveCategory(id: String, label: String, kind: String = "OTHER", archived: Boolean = false) = db.withTransaction {
        require(label.isNotBlank()) { "Inserisci il nome della categoria." }
        require(kind in listOf("OTHER", "CLEANING", "LAUNDRY"))
        val old = dao.category(id)
        require(old == null || old.kind == kind) { "La tipologia di una categoria esistente non può cambiare." }
        dao.saveCategory(ExpenseCategoryEntity(id, label.trim(), kind, archived))
    }
    suspend fun saveExpense(draft: ExpenseDraft): String {
        val result = db.withTransaction {
            val old = dao.expense(draft.id)
            require((old?.revision ?: 0) == draft.revision) { "La spesa è cambiata: riapri la scheda prima di salvarla." }
            require(old?.accountingAllocationId == null) { "Spesa già rendicontata: occorre una rettifica del rendiconto." }
            require(old == null || !old.voided) { "La spesa è annullata." }
            val property = requireNotNull(db.propertyDao().get(draft.propertyId)) { "Seleziona un immobile presente nel database." }
            require(!property.property.archived || old?.propertyId == draft.propertyId) { "Non puoi creare spese su un immobile archiviato." }
            val category = requireNotNull(dao.category(draft.categoryId)) { "Seleziona una categoria." }
            require(!category.archived || old?.categoryId == category.id) { "La categoria è archiviata." }
            if (draft.reservationId.isNotBlank()) require(db.reservationDao().get(draft.reservationId)?.propertyId == draft.propertyId) { "La prenotazione deve appartenere allo stesso immobile." }
            require(old == null || draft.reason.isNotBlank()) { "Motiva la rettifica della spesa." }
            if(old==null && draft.reservationId.isNotBlank() && category.kind in listOf("CLEANING","LAUNDRY"))require(dao.observeExpenses().first().none{it.reservationId==draft.reservationId && it.kind==category.kind && !it.voided}){"Intervento già registrato: modifica la voce esistente, senza aggiungere una seconda spesa."}
            val preserveStandard=old?.recognition=="STANDARD_APPROVED" && !draft.confirmActual
            val item = draft.record(category).copy(recognition=if(preserveStandard)"STANDARD_APPROVED" else "ACTUAL",originKey=old?.originKey,approvalBatchId=old?.approvalBatchId,effectiveDateConfirmed=!preserveStandard)
            ReportGuard.allow(db, "EXPENSE", draft.id, item.propertyId, item.competenceMonth)
            // Preserve the original v1/v2 foundational row before its first rectification.
            if (old != null && dao.expenseHistory(old.id).isEmpty()) dao.insertExpenseRevision(ExpenseRevisionEntity(UUID.randomUUID().toString(), old.id, old.revision, snapshot(old), "Valore precedente alla rettifica", old.updatedAt))
            dao.saveExpense(item)
            dao.insertExpenseRevision(ExpenseRevisionEntity(UUID.randomUUID().toString(), item.id, item.revision, snapshot(item), draft.reason.ifBlank { "Inserimento" }, System.currentTimeMillis()))
            item.id
        }
        drafts?.routes()?.filter{it=="expense/${draft.id}/edit" || it.startsWith("expense/new/") && drafts.load(it,ExpenseDraft::class.java)?.id==draft.id}?.forEach(drafts::clear)
        return result
    }
    suspend fun voidExpense(id: String, reason: String) = db.withTransaction {
        require(reason.isNotBlank()) { "Motiva l’annullamento." }
        val old = requireNotNull(dao.expense(id)) { "Spesa non trovata." }
        require(old.accountingAllocationId == null) { "Spesa già rendicontata: occorre una rettifica." }
        ReportGuard.allow(db, "EXPENSE", id)
        if (!old.voided) {
            if (dao.expenseHistory(id).isEmpty()) dao.insertExpenseRevision(ExpenseRevisionEntity(UUID.randomUUID().toString(), id, old.revision, snapshot(old), "Valore precedente", old.updatedAt))
            val item = old.copy(voided = true, revision = old.revision + 1, updatedAt = System.currentTimeMillis())
            dao.saveExpense(item); dao.insertExpenseRevision(ExpenseRevisionEntity(UUID.randomUUID().toString(), id, item.revision, snapshot(item), reason.trim(), item.updatedAt))
        }
    }
    suspend fun expenseHistory(id: String) = dao.expenseHistory(id)
    suspend fun taxHistory(id: String) = dao.taxHistory(id)
    suspend fun saveTax(id: String, draft: TaxDraft):Unit {
        db.withTransaction {
            val booking = requireNotNull(db.reservationDao().get(id)) { "Prenotazione non trovata." }
            ReportGuard.allow(db, "BOOKING", id)
            val old = dao.assessment(id)
            require((old?.revision ?: 0) == draft.revision) { "I dati fiscali sono cambiati: riapri la scheda." }
            require(booking.accountingAllocationId == null) { "Prenotazione già rendicontata: occorre una rettifica." }
            require(booking.status != BookingStatus.CANCELLED.name || old != null) { "Prenotazione annullata." }
            val property = requireNotNull(db.propertyDao().get(booking.propertyId)).property
            val item = draft.record(booking, property, old)
            dao.saveAssessment(item)
            dao.insertTaxRevision(TaxAssessmentRevisionEntity(UUID.randomUUID().toString(), id, item.revision, snapshot(item), System.currentTimeMillis()))
            db.reservationDao().save(booking.copy(taxableGuests = item.taxableGuests, exemptGuests = item.exemptGuests, expectedTaxCents = item.expectedCents, updatedAt = maxOf(System.currentTimeMillis(), booking.updatedAt + 1)))
        }
        drafts?.clear("tax/$id")
    }
    suspend fun post(item: MoneyMovementEntity): String {
        val result = db.withTransaction {
            dao.movement(item.id)?.let { existing ->
                require(existing.copy(createdAt = item.createdAt) == item) { "Identificativo già usato per un movimento diverso." }
                return@withTransaction existing.id
            }
            require(item.collector in listOf("ELLEDUE","OWNER","PORTAL","OTHER"))
            require(item.amountCents > 0) { "Inserisci un importo maggiore di zero." }
            Competence.month(item.competenceMonth)
            java.time.LocalDate.ofEpochDay(item.dateEpochDay)
            requireNotNull(db.propertyDao().get(item.propertyId)) { "Immobile non trovato." }
            val all = dao.movements(); val active = Ledger.active(all)
            when (item.kind) {
                "OWNER_PAYMENT" -> {
                    require(db.propertyDao().get(item.propertyId)!!.property.businessType!="OWNED"){"Gli immobili propri non hanno un saldo con un proprietario esterno."}
                    require(item.reservationId == null && item.sourceId == null && item.reversesId == null) { "Collegamento pagamento non valido." }
                }
                "COLLECTION", "COMPENSATION", "REMITTANCE" -> {
                    val booking = requireNotNull(item.reservationId?.let { db.reservationDao().get(it) }) { "Seleziona una prenotazione." }
                    require(booking.propertyId == item.propertyId && booking.accountingAllocationId == null) { "Immobile incoerente o prenotazione già rendicontata." }
                    require(booking.status != BookingStatus.CANCELLED.name) { "Prenotazione annullata: puoi stornare i movimenti già registrati." }
                    val tax = requireNotNull(dao.assessment(booking.id)) { "Configura prima la tassa." }
                    require(!tax.needsReview) { "Rivedi la tassa dopo la modifica della prenotazione." }
                    require(item.reversesId == null)
                    if (item.kind == "COLLECTION") {
                        require(item.sourceId == null)
                        val collected = OperationalMoney.total(active.filter { it.reservationId == booking.id && it.kind == "COLLECTION" }.map { it.amountCents })
                        require(item.amountCents <= Ledger.residual(tax.expectedCents, collected)) { "L’importo supera la tassa residua. Rettifica prima la tassa se necessario." }
                    } else {
                        require(item.kind!="COMPENSATION" || booking.appliedBusinessType!="OWNED"){"Per gli immobili propri la tassa non è un anticipo a un proprietario esterno."}
                        val source = requireNotNull(active.firstOrNull { it.id == item.sourceId && it.kind == "COLLECTION" && it.reservationId == booking.id }) { "Seleziona una riscossione attiva della prenotazione." }
                        if(item.kind=="COMPENSATION") {
                            val versions=db.reportDao().period(item.propertyId,item.competenceMonth)
                            require(versions.none{it.status=="CONFIRMED"} || versions.any{it.status=="DRAFT" && it.previousId!=null}){"Apri una versione rettificativa per riconoscere anticipi nel periodo già confermato."}
                        }
                        require(item.kind!="COMPENSATION" || source.collector=="ELLEDUE"){"Solo una riscossione Elledue può essere compensata."}
                        val used = OperationalMoney.total(active.filter { it.kind == item.kind && it.sourceId == source.id }.map { it.amountCents })
                        require(item.amountCents <= source.amountCents - used) { "Importo già riconosciuto o superiore alla riscossione disponibile." }
                        if (item.kind == "REMITTANCE") require(tax.remittanceResponsible != "UNDEFINED") { "Configura separatamente il soggetto del riversamento." }
                    }
                }
                "REVERSAL" -> {
                    require(item.notes.isNotBlank()) { "Motiva lo storno." }
                    val original = requireNotNull(active.firstOrNull { it.id == item.reversesId }) { "Movimento già stornato o inesistente." }
                    ReportGuard.allow(db, "MOVEMENT", original.id)
                    require(item.amountCents == original.amountCents && item.propertyId == original.propertyId && item.reservationId == original.reservationId && item.sourceId == null) { "Storno incoerente con l’originale." }
                    require(active.none { it.sourceId == original.id }) { "Storna prima le compensazioni o i riversamenti collegati." }
                    original.reservationId?.let { require(db.reservationDao().get(it)?.accountingAllocationId == null) { "Movimento già rendicontato: occorre una rettifica." } }
                }
                else -> throw IllegalArgumentException("Tipo di movimento non valido.")
            }
            dao.insertMovement(item)
            item.reservationId?.let { id ->
                val booking = requireNotNull(db.reservationDao().get(id))
                val collected = Ledger.total((all + item).filter { it.reservationId == id }, "COLLECTION")
                db.reservationDao().save(booking.copy(collectedTaxCents = collected, updatedAt = maxOf(System.currentTimeMillis(), booking.updatedAt + 1)))
            }
            item.id
        }
        drafts?.routes()?.filter{it.startsWith("movement/new?") && drafts.load("$it#id",String::class.java)==item.id}?.forEach(drafts::clear)
        return result
    }
    suspend fun collectTax(bookingId:String,amountCents:Long,collector:String,compensate:Boolean,requestId:String,date:Long=BookingDates.today().toEpochDay()):Unit=db.withTransaction {
        require(collector in listOf("ELLEDUE","OWNER","PORTAL","OTHER"))
        require(!compensate || collector=="ELLEDUE"){"La compensazione richiede una riscossione Elledue."}
        val b=requireNotNull(db.reservationDao().get(bookingId))
        require(!compensate || b.appliedBusinessType=="MANAGED"){"Per gli immobili propri non esiste un saldo con un proprietario esterno."}
        val note="Riscossione confermata · $collector · compensazione ${if(compensate)"sì" else "no"}"
        val collection=MoneyMovementEntity("quick-tax:$requestId",b.propertyId,b.id,"COLLECTION",amountCents,date,b.competenceMonth,note,collector=collector)
        post(collection)
        if(compensate)post(collection.copy(id="quick-tax-comp:$requestId",kind="COMPENSATION",sourceId=collection.id))
    }
    companion object {
        internal fun snapshot(item: Any): String = JSONObject().apply {
            item.javaClass.declaredFields.filter { !java.lang.reflect.Modifier.isStatic(it.modifiers) }.forEach { field ->
                field.isAccessible = true; put(field.name, field.get(item) ?: JSONObject.NULL)
            }
        }.toString()
    }
}
