package it.elledue.control.notifications

import it.elledue.control.data.*
import java.time.*

data class ReminderSettings(val enabled:Boolean=false,val arrivals:Boolean=true,val departures:Boolean=true,val tasks:Boolean=true,val cleaning:Boolean=true,val operations:Boolean=true,val tax:Boolean=true,val monthly:Boolean=true,val registrationDeadlineHours:Long=24,val dayBefore:Boolean=true,val reminderTime:String="18:00",val arrivalLeadHours:Long=24,val departureLeadHours:Long=24,val taskLeadMinutes:Long=60,val allDayTime:String="09:00",val quietEnabled:Boolean=false,val quietStart:String="22:00",val quietEnd:String="08:00")
data class Reminder(val key:String,val at:Long,val title:String,val text:String,val route:String,val version:String)
object ReminderPlanner {
    val zone:ZoneId=ZoneId.of("Europe/Rome")
    fun validate(s:ReminderSettings) {
        listOf(s.reminderTime,s.allDayTime,s.quietStart,s.quietEnd).forEach{LocalTime.parse(it)}
        require(s.registrationDeadlineHours in 1..24)
        require(s.arrivalLeadHours in 0..720 && s.departureLeadHours in 0..720 && s.taskLeadMinutes in 0..10080)
        require(!s.quietEnabled || s.quietStart!=s.quietEnd){"La fascia di silenzio deve avere un inizio e una fine distinti."}
    }
    fun quiet(time:ZonedDateTime,s:ReminderSettings):ZonedDateTime {
        if(!s.quietEnabled)return time
        val start=LocalTime.parse(s.quietStart);val end=LocalTime.parse(s.quietEnd);val local=time.toLocalTime()
        val silent=if(start<end)local>=start && local<end else local>=start || local<end
        if(!silent)return time
        val date=if(start>end && local>=start)time.toLocalDate().plusDays(1) else time.toLocalDate()
        return date.atTime(end).atZone(zone)
    }
    fun plan(properties:List<PropertyWithOwner>,bookings:List<ReservationEntity>,tasks:List<ActivityEntity>,s:ReminderSettings,now:Instant):List<Reminder> {
        validate(s);if(!s.enabled)return emptyList()
        val names=properties.filter{!it.property.archived}.associate{it.property.id to it.property.name}
        val jobs=mutableListOf<Reminder>()
        fun add(key:String,time:ZonedDateTime,title:String,text:String,route:String,version:String){
            val at=quiet(time.withZoneSameInstant(zone),s).toInstant()
            // A bounded lookback permits delivery of delayed inexact alarms, never ancient events.
            if(at.isBefore(now.minusSeconds(48*3600)))return
            jobs.add(Reminder(key,at.toEpochMilli(),title,text,route,"$version:${at.toEpochMilli()}:$text"))
        }
        bookings.filter{it.propertyId in names && it.status!="CANCELLED"}.forEach{b->
            val arrival=LocalDate.ofEpochDay(b.checkInEpochDay).atTime(runCatching{LocalTime.parse(b.expectedArrivalTime)}.getOrDefault(LocalTime.of(15,0))).atZone(zone)
            val departure=LocalDate.ofEpochDay(b.checkOutEpochDay).atTime(runCatching{LocalTime.parse(b.expectedDepartureTime)}.getOrDefault(LocalTime.of(10,0))).atZone(zone)
            val route="booking/${b.id}";val name=names[b.propertyId]!!;val revision=b.updatedAt.toString()
            if(b.status!="COMPLETED" && s.arrivals)add("arrival:${b.id}",if(s.dayBefore)arrival.toLocalDate().minusDays(1).atTime(LocalTime.parse(s.reminderTime)).atZone(zone) else arrival.minusHours(s.arrivalLeadHours),"Check-in imminente", "$name · verifica documenti, orario, accesso e preparazione programmata. L’immobile potrebbe essere ancora occupato.",route,revision)
            if(b.status!="COMPLETED" && s.departures)add("departure:${b.id}",if(s.dayBefore)departure.toLocalDate().minusDays(1).atTime(LocalTime.parse(s.reminderTime)).atZone(zone) else departure.minusHours(s.departureLeadHours),"Check-out imminente","$name · conferma l’orario di partenza e le istruzioni di check-out.",route,revision)
            if(b.status!="COMPLETED" && s.operations && b.actualArrivalTime!=null && tasks.none{it.reservationId==b.id && it.kind=="REGISTRATION" && it.completed})add("registration:${b.id}",arrival.toLocalDate().atTime(LocalTime.parse(b.actualArrivalTime)).atZone(zone).plusHours(s.registrationDeadlineHours),"Verifica registrazione ospiti","$name · controlla l’adempimento. La notifica non attesta una comunicazione effettuata.",route,revision)
            if(s.tax && (b.expectedTaxCents==null || b.collectedTaxCents<b.expectedTaxCents))add("tax:${b.id}",departure,"Verifica tassa di soggiorno","$name · conferma i conteggi e l’importo realmente incassato. Nessun incasso automatico.",route,revision)
        }
        tasks.filter{!it.completed && it.dueEpochMillis!=null && (it.propertyId==null || it.propertyId in names)}.forEach{t->
            if(t.kind=="CLEANING" && !s.cleaning || t.kind!="CLEANING" && !s.tasks)return@forEach
            val due=Instant.ofEpochMilli(t.dueEpochMillis!!).atZone(ZoneId.of(t.zoneId))
            val base=if(t.hasTime)due else due.toLocalDate().atTime(LocalTime.parse(s.allDayTime)).atZone(ZoneId.of(t.zoneId))
            add("task:${t.id}",base.minusMinutes(s.taskLeadMinutes),if(t.kind=="CLEANING")"Pulizia programmata" else "Attività programmata",t.title,"activity/${t.id}","${t.dueEpochMillis}:${t.title}:${t.propertyId}:${t.kind}:${t.hasTime}")
        }
        if(s.monthly && properties.any{!it.property.archived && it.property.businessType!="OWNED"}) {
            val month=YearMonth.from(now.atZone(zone))
            listOf(month,month.plusMonths(1)).forEach{m->add("month:$m",m.atEndOfMonth().atTime(LocalTime.parse(s.reminderTime)).atZone(zone),"Verifica rendiconti mensili","Prepara e controlla le bozze dei rendiconti del periodo.","reports",m.toString())}
        }
        return jobs.distinctBy{it.key}.sortedBy{it.at}
    }
}
