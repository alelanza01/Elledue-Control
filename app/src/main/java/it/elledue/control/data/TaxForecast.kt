package it.elledue.control.data

import it.elledue.control.domain.*
import java.util.UUID

object TaxForecast {
    /** Only provisional, uncollected and unallocated forecasts may be recalculated automatically. */
    suspend fun update(db:AppDatabase,booking:ReservationEntity,property:PropertyEntity) {
        val old=db.accountingDao().assessment(booking.id)
        val posted=db.accountingDao().movements().any{it.reservationId==booking.id}
        val allocated=db.reportDao().allocation("BOOKING",booking.id)!=null
        if(old!=null && (old.consolidated || posted || allocated)) {
            db.accountingDao().markTaxForReview(booking.id);return
        }
        val subjects=booking.taxableGuests
        if(subjects==null){if(old!=null)db.accountingDao().markTaxForReview(booking.id);return}
        val nights=minOf(booking.checkOutEpochDay-booking.checkInEpochDay,property.touristTaxMaxNights?.toLong() ?: Long.MAX_VALUE)
        val forecast=TaxDraft(subjects.toString(),(booking.guestCount-subjects).toString(),nights.toString(),revision=old?.revision ?: 0)
            .record(booking,property,null).copy(consolidated=false)
        db.accountingDao().saveAssessment(forecast)
        db.accountingDao().insertTaxRevision(TaxAssessmentRevisionEntity(UUID.randomUUID().toString(),booking.id,forecast.revision,AccountingRepository.snapshot(forecast),System.currentTimeMillis()))
        db.reservationDao().save(booking.copy(expectedTaxCents=forecast.expectedCents,exemptGuests=forecast.exemptGuests))
    }
}
