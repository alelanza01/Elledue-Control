package it.elledue.control.domain

import it.elledue.control.data.ReservationEntity
import java.text.Normalizer
import java.time.Instant

/** Search is independent of calendar state; period is an explicit optional overlap filter. */
data class BookingSearch(val query:String="",val propertyId:String="",val state:String="ALL",val start:Long?=null,val endInclusive:Long?=null,val descending:Boolean=false) {
    fun apply(records:List<ReservationEntity>,now:Instant=Instant.now()):List<ReservationEntity> {
        require(start==null || endInclusive==null || endInclusive>=start){"La fine del periodo precede l’inizio."}
        val words=normalize(query).split(Regex("\\s+")).filter{it.isNotBlank()}
        val selected=records.filter{b->
            val current=state(b,now)
            (propertyId.isBlank() || b.propertyId==propertyId) && (if(state=="ALL")current!="CANCELLED" else current==state) &&
                words.all{it in normalize("${b.guestFirstName} ${b.guestLastName}")} &&
                (start==null || b.checkOutEpochDay>=start) && (endInclusive==null || b.checkInEpochDay<=endInclusive)
        }
        val order=compareBy<ReservationEntity>{it.checkInEpochDay}.thenBy{it.id}
        return selected.sortedWith(if(descending)order.reversed() else order)
    }
    companion object {
        val states=listOf("ALL" to "Tutte","UPCOMING" to "Prossime","ACTIVE" to "In corso","PAST" to "Passate","CANCELLED" to "Annullate")
        fun state(b:ReservationEntity,now:Instant=Instant.now())=when {
            b.status=="CANCELLED"->"CANCELLED"
            b.status=="COMPLETED"->"PAST"
            else->when(StayStatus.label(b,now)){"Conclusa"->"PAST";"In corso"->"ACTIVE";else->"UPCOMING"}
        }
        fun normalize(value:String)=Normalizer.normalize(value,Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"").lowercase(java.util.Locale.ROOT).replace('’','\'').trim()
    }
}
