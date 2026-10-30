package it.elledue.control.domain

import it.elledue.control.data.ReservationEntity
import java.time.YearMonth

data class PortfolioMetrics(val gross: Long, val fees: Long, val bookings: Int, val nights: Int, val calendarOccupiedDays: Int, val calendarDays: Int,val stayNights:Int=nights) {
    val averageStay get() = if (bookings == 0) java.math.BigDecimal.ZERO else java.math.BigDecimal(stayNights).divide(java.math.BigDecimal(bookings), 1, java.math.RoundingMode.HALF_UP)
    val calendarPercent get() = java.math.BigDecimal(calendarOccupiedDays * 100).divide(java.math.BigDecimal(calendarDays), 1, java.math.RoundingMode.HALF_UP)
}
object PortfolioAnalytics {
    fun calculate(month: YearMonth, propertyId: String, source: List<ReservationEntity>): PortfolioMetrics {
        val active = source.filter { it.propertyId == propertyId && it.status != "CANCELLED" }
        val start = month.atDay(1).toEpochDay(); val end = month.plusMonths(1).atDay(1).toEpochDay()
        val competent = active.filter { it.checkInEpochDay in start until end }
        val occupied = (start until end).count { day -> active.any { day >= it.checkInEpochDay && day < it.checkOutEpochDay } }
        return PortfolioMetrics(OperationalMoney.total(competent.map { it.grossCents }), OperationalMoney.total(competent.map { it.managementCents }), competent.size, occupied, occupied, month.lengthOfMonth(),competent.sumOf{(it.checkOutEpochDay-it.checkInEpochDay).toInt()})
    }
}
