package it.elledue.control

import android.content.Context
import it.elledue.control.data.*
import it.elledue.control.domain.*
import it.elledue.control.reports.*

internal suspend fun reportFixture(db: AppDatabase, context: Context): ReportSnapshot {
    val property = PropertyDraft(id = "synthetic-property", ownerId = "synthetic-owner", name = "Casa Dimostrativa", address = "Via Esempio 10", ownerName = "Proprietario Dimostrativo", maxGuests = "4")
    PropertyRepository(db).save(property)
    val booking = BookingDraft(id = "synthetic-booking", propertyId = property.id, firstName = "Ospite", lastName = "Dimostrativo", guests = "3", checkIn = "10/11/2026", checkOut = "14/11/2026", gross = "1000", actualOta = "195")
    ReservationRepository(db).save(booking)
    val accounting = AccountingRepository(db); accounting.initialize()
    listOf(ExpenseDraft(id = "synthetic-cleaning", propertyId = property.id, reservationId = booking.id, date = "14/11/2026", month = "2026-11", categoryId = "cleaning", quantity = "3", unit = "8"),
        ExpenseDraft(id = "synthetic-laundry", propertyId = property.id, date = "14/11/2026", month = "2026-11", categoryId = "laundry", quantity = "2", unit = "5"),
        ExpenseDraft(id = "synthetic-detergents", propertyId = property.id, date = "12/11/2026", month = "2026-11", categoryId = "detergents", amount = "5")).forEach { accounting.saveExpense(it) }
    accounting.saveTax(booking.id, TaxDraft("3", "0", "4"))
    val cash = MoneyMovementEntity("synthetic-collection", property.id, booking.id, "COLLECTION", 3000, BookingDates.parse("14/11/2026").toEpochDay(), "2026-11", "Riscossione verificata")
    accounting.post(cash); accounting.post(cash.copy(id = "synthetic-compensation", kind = "COMPENSATION", sourceId = cash.id, notes = "Anticipo riconosciuto"))
    val reports = ReportRepository(db, context); val id = reports.generate(property.id, "2026-11")
    reports.refresh(id, "Documento generato esclusivamente con dati sintetici. La tassa di soggiorno è esterna al lordo della prenotazione.")
    return reports.confirm(id, ReportCodec.hash(ReportCodec.encode(reports.snapshot(id))), false)
}
