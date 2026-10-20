package it.elledue.control.domain
import it.elledue.control.data.ReservationEntity
import java.time.*
object StayStatus {
 fun label(b:ReservationEntity,now:Instant=Instant.now()):String {
  if(b.status=="CANCELLED")return "Cancellata"
  fun time(day:Long,t:String)=LocalDate.ofEpochDay(day).atTime(LocalTime.parse(t)).atZone(BookingDates.zone).toInstant()
  val end=time(b.checkOutEpochDay,b.actualDepartureTime ?: b.expectedDepartureTime)
  val start=time(b.checkInEpochDay,b.actualArrivalTime ?: b.expectedArrivalTime)
  return when {now>=end->"Conclusa";now>=start->"In corso";else->"In arrivo"}
 }
}
