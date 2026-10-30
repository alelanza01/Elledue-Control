package it.elledue.control.domain
import it.elledue.control.data.*
import java.time.*
data class DayEvent(val key:String,val time:String?,val title:String,val propertyId:String?,val route:String,val completed:Boolean,val kind:String)
object OperationalDay {
 fun events(day:Long,bookings:List<ReservationEntity>,activities:List<ActivityEntity>,issues:List<PropertyIssue>,now:Instant=Instant.now(),expenses:List<ExpenseEntity> = emptyList()):List<DayEvent> {
  val rows=mutableListOf<DayEvent>()
  bookings.filter{it.status!="CANCELLED"}.forEach{b->
   if(day>b.checkInEpochDay && day<b.checkOutEpochDay)rows.add(DayEvent("stay:${b.id}",null,"Soggiorno · ${b.guestFirstName} ${b.guestLastName}",b.propertyId,"booking/${b.id}",false,"STAY"))
   if(b.checkInEpochDay==day)rows.add(DayEvent("in:${b.id}",b.actualArrivalTime ?: b.expectedArrivalTime,"Check-in · ${b.guestFirstName} ${b.guestLastName}",b.propertyId,"booking/${b.id}",b.actualArrivalTime!=null,"ARRIVAL"))
   if(b.checkOutEpochDay==day)rows.add(DayEvent("out:${b.id}",b.actualDepartureTime ?: b.expectedDepartureTime,"Check-out · ${b.guestFirstName} ${b.guestLastName}",b.propertyId,"booking/${b.id}",b.actualDepartureTime!=null,"DEPARTURE"))
  }
  activities.forEach{a->a.dueEpochMillis?.let{Instant.ofEpochMilli(it).atZone(ZoneId.of(a.zoneId))}?.takeIf{it.toLocalDate().toEpochDay()==day}?.let{date->rows.add(DayEvent("task:${a.id}",if(a.hasTime)date.toLocalTime().toString() else null,a.title,a.propertyId,if(a.kind=="CLEANING" && a.reservationId!=null)expenses.firstOrNull{it.reservationId==a.reservationId && it.kind=="CLEANING" && !it.voided}?.let{"expense/${it.id}/edit"} ?: "expense/new/CLEANING?booking=${a.reservationId}" else "activity/${a.id}",a.completed,a.kind))}}
  issues.forEach{i->i.scheduledAt?.let{Instant.ofEpochMilli(it).atZone(BookingDates.zone)}?.takeIf{it.toLocalDate().toEpochDay()==day}?.let{date->rows.add(DayEvent("issue:${i.id}",if(i.hasTime)date.toLocalTime().toString() else null,"Intervento · ${i.title}",i.propertyId,"issue/${i.id}",!IssueRepository.open(i),"ISSUE"))}}
  return rows.sortedWith(compareBy<DayEvent>{it.time==null}.thenBy{it.time}.thenBy{it.key})
 }
}
