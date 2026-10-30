package it.elledue.control
import it.elledue.control.domain.*
import org.junit.Test
import org.junit.Assert.*
import java.time.*
class BookingUxTest {
 private val day=LocalDate.of(2026,11,2).toEpochDay()
 private val now=LocalDate.of(2026,11,4).atTime(17,0).atZone(BookingDates.zone).toInstant()
 private fun b(id:String="b",start:Long=day,end:Long=day+5)=BookingDraft(id=id,propertyId="p",firstName="Nicolò",lastName="D’Amico",checkIn=BookingDates.display(start),checkOut=BookingDates.display(end),gross="100").toRecord(null,false)
 @Test fun searchFindsHistoricalGuestsIndependentOfCalendarAndIgnoresAccents(){val old=b(start=day-400,end=day-396);assertEquals(listOf(old),BookingSearch(query="d’amico nicoLO").apply(listOf(old),now))}
 @Test fun statesRespectActualTimesAndCancellationsAndExplicitCompleted(){val active=b();val past=b("past",day-10,day-8);val future=b("future",day+10,day+13);val cancelled=b("cancel").copy(status="CANCELLED");val done=future.copy(id="done",status="COMPLETED");val all=listOf(future,active,past,cancelled,done);assertEquals(listOf(active),BookingSearch(state="ACTIVE").apply(all,now));assertEquals(listOf(cancelled),BookingSearch(state="CANCELLED").apply(all,now));assertEquals(listOf(past,done),BookingSearch(state="PAST").apply(all,now));assertEquals(4,BookingSearch().apply(all,now).size)}
 @Test fun propertyRangeAndMonthAreExplicitIntersectingFiltersAndOrderIsStable(){val a=b("a");val cross=b("cross",day-3,day);val other=b("q").copy(propertyId="q");assertEquals(listOf(cross,a),BookingSearch(propertyId="p",start=day,endInclusive=day+1).apply(listOf(other,a,cross),now));assertEquals(listOf(a,cross),BookingSearch(propertyId="p",start=day,endInclusive=day+1,descending=true).apply(listOf(other,a,cross),now))}
 @Test fun turnoverUsesHalfDayEndpointsAndSameLaneWithoutHidingEitherGuest(){val before=b("before",day,day+3);val after=b("after",day+3,day+6);val seg=CalendarStays.segments(day,day,day+31,listOf(before,after));assertEquals(seg[0].endHalfDay,seg[1].startHalfDay);assertEquals(1,CalendarStays.lanes(seg).size);assertEquals(setOf("p"),CalendarStays.turnovers(day+3,listOf(before,after)))}
 @Test fun realOverlapsUseSeparateLanesAndCancelledNeverOccupies(){val seg=CalendarStays.segments(day,day,day+31,listOf(b("a"),b("bb"),b("cancel").copy(status="CANCELLED")));assertEquals(2,CalendarStays.lanes(seg).size);assertEquals(2,seg.size)}
 @Test fun longStaysContinueAcrossWeeksAndMonthBoundariesWithoutLostDeparture(){val stay=b(start=day-5,end=day+12);val first=CalendarStays.segments(day,day,day+31,listOf(stay)).single();assertEquals(0,first.startHalfDay);assertEquals(14,first.endHalfDay);assertFalse(first.startsHere);assertFalse(first.endsHere);val second=CalendarStays.segments(day+7,day,day+31,listOf(stay)).single();assertEquals(11,second.endHalfDay);assertTrue(second.endsHere);val departure=CalendarStays.segments(day+12,day+12,day+43,listOf(stay)).single();assertEquals(1,departure.endHalfDay)}
}
