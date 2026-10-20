package it.elledue.control.domain

import it.elledue.control.data.ReservationEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object CalendarText {
    fun upcoming(propertyId:String, today:LocalDate, records:List<ReservationEntity>) = records.filter {
        it.propertyId==propertyId && it.status!="CANCELLED" && it.checkOutEpochDay>=today.toEpochDay()
    }.sortedWith(compareBy<ReservationEntity>{it.checkInEpochDay}.thenBy{it.expectedArrivalTime}.thenBy{it.id})

    /** Inclusive requested dates; stay interval is half-open: departure-only boundary is excluded. */
    fun bookings(propertyId:String, from:LocalDate, through:LocalDate, records:List<ReservationEntity>):List<ReservationEntity> {
        require(!through.isBefore(from)) { "Il termine deve seguire l’inizio." }
        return records.filter { it.propertyId==propertyId && it.status!="CANCELLED" && it.checkInEpochDay<=through.toEpochDay() && it.checkOutEpochDay>from.toEpochDay() }
            .sortedWith(compareBy<ReservationEntity> { it.checkInEpochDay }.thenBy { it.expectedArrivalTime }.thenBy { it.id })
    }
    fun render(name:String, bookings:List<ReservationEntity>):String = if(bookings.isEmpty()) "" else name.uppercase(Locale.ITALIAN)+"\n\n"+bookings.joinToString("\n\n") { b ->
        val guest=listOf(b.guestFirstName,b.guestLastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Ospite non indicato" }
        "$guest (${b.guestCount} ${if(b.guestCount==1) "ospite" else "ospiti"})\nCheck-in ${dateTime(b.checkInEpochDay,b.expectedArrivalTime)}\nCheck-out ${dateTime(b.checkOutEpochDay,b.expectedDepartureTime)}"
    }
    private fun dateTime(day:Long,time:String)=LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))+if(time.isBlank()) " orario non indicato" else " ore ${time.replace(':','.')}"
}
data class ResolvedMessage(val text:String,val missing:Set<String>)
object GuestText {
    fun forResidual(body:String,residual:Long?):String = if(residual==0L)body.lineSequence().filterNot{it.contains("{tassa_residua}")}.joinToString("\n") else body

    private val variable=Regex("\\{\\{?([a-zA-Z_]+)\\}?\\}")
    fun resolve(body:String, booking:ReservationEntity?, property:String?, residual:Long?, english:Boolean=false):ResolvedMessage {
        val values=mapOf("nome_ospite" to booking?.guestFirstName,"immobile" to property,"nome_immobile" to property,
            "check_in" to booking?.let { BookingDates.display(it.checkInEpochDay) },"check_out" to booking?.let { BookingDates.display(it.checkOutEpochDay) },
            "orario" to booking?.expectedArrivalTime,"ora_check_in" to booking?.expectedArrivalTime,"ora_check_out" to booking?.expectedDepartureTime,
            "tassa_residua" to residual?.let(Amounts::euro))
        val missing=linkedSetOf<String>()
        val result=variable.replace(body) { match -> val key=match.groupValues[1]; values[key]?.takeIf { it.isNotBlank() } ?: run { missing.add(key); "[${if(english) "missing" else "manca"}: $key]" } }
        return ResolvedMessage(result,missing)
    }
}
