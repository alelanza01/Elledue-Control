package it.elledue.control.domain

import it.elledue.control.data.*

/** Conventional owner settlement only. Never creates a collection, cash or remittance. */
object TaxAdvance {
    fun remaining(booking:ReservationEntity,tax:TaxAssessmentEntity?,movements:List<MoneyMovementEntity>):Long {
        if(!booking.automaticTaxAdvance || booking.appliedBusinessType!="MANAGED" || booking.status=="CANCELLED")return 0
        val active=Ledger.active(movements).filter{it.reservationId==booking.id}
        val external=OperationalMoney.total(active.filter{it.kind=="COLLECTION" && it.collector!="ELLEDUE"}.map{it.amountCents})
        val target=booking.taxAdvanceOverrideCents ?: maxOf(0,(tax?.expectedCents ?: booking.expectedTaxCents ?: 0)-external)
        val recognized=OperationalMoney.total(active.filter{it.kind=="COMPENSATION"}.map{it.amountCents})
        return maxOf(0,target-recognized)
    }
}
