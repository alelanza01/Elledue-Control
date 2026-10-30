package it.elledue.control.domain

import it.elledue.control.data.ReservationEntity

/** Half-day endpoints free the departure day and split a same-day turnover without overlap. */
data class StaySegment(val booking:ReservationEntity,val startHalfDay:Int,val endHalfDay:Int,val startsHere:Boolean,val endsHere:Boolean)
object CalendarStays {
    fun segments(weekStart:Long,monthStart:Long,monthEndExclusive:Long,records:List<ReservationEntity>):List<StaySegment> {
        val lower=maxOf(weekStart,monthStart);val upper=minOf(weekStart+7,monthEndExclusive)
        return records.filter{it.status!="CANCELLED" && it.checkInEpochDay<upper && it.checkOutEpochDay>=lower}.mapNotNull{b->
            val start=if(b.checkInEpochDay>=lower)((b.checkInEpochDay-weekStart)*2+1).toInt() else ((lower-weekStart)*2).toInt()
            val end=if(b.checkOutEpochDay<upper)((b.checkOutEpochDay-weekStart)*2+1).toInt() else ((upper-weekStart)*2).toInt()
            if(end<=start)null else StaySegment(b,start,end,b.checkInEpochDay in lower until upper,b.checkOutEpochDay in lower until upper)
        }.sortedWith(compareBy<StaySegment>{it.startHalfDay}.thenBy{it.booking.id})
    }
    fun lanes(segments:List<StaySegment>):List<List<StaySegment>> {
        val lanes=mutableListOf<MutableList<StaySegment>>()
        for(s in segments.sortedWith(compareBy<StaySegment>{it.startHalfDay}.thenBy{it.booking.id})) {
            val available=lanes.firstOrNull{it.last().endHalfDay<=s.startHalfDay}
            if(available==null)lanes.add(mutableListOf(s)) else available.add(s)
        }
        return lanes
    }
    fun turnovers(day:Long,records:List<ReservationEntity>)=records.filter{it.status!="CANCELLED" && it.checkInEpochDay==day}.map{it.propertyId}.toSet()
        .intersect(records.filter{it.status!="CANCELLED" && it.checkOutEpochDay==day}.map{it.propertyId}.toSet())
}
