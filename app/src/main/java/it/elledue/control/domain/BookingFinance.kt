package it.elledue.control.domain

/** Payments go to the owner; this result describes Elledue's earned fee, not cash receipts. */
data class BookingTotals(val otaCents: Long, val netCents: Long, val managementCents: Long)

object BookingFinance {
    fun calculate(gross: Long, otaBasisPoints: Int, actualOta: Long?, managementBasisPoints: Int): BookingTotals {
        require(gross >= 0) { "Il lordo non può essere negativo." }
        require(otaBasisPoints in 0..10_000 && managementBasisPoints in 0..10_000) { "Le percentuali devono essere tra 0 e 100." }
        val ota = actualOta ?: Amounts.percentage(gross, otaBasisPoints)
        require(ota in 0..gross) { "La commissione OTA effettiva deve essere tra zero e il lordo." }
        val net = gross - ota
        return BookingTotals(ota, net, Amounts.percentage(net, managementBasisPoints))
    }
}
