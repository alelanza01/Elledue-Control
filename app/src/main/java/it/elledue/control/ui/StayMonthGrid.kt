package it.elledue.control.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.elledue.control.data.*
import it.elledue.control.domain.*
import java.time.*

@Composable
fun StayMonthGrid(month:YearMonth,selected:Long,today:Long,records:List<ReservationEntity>,singleProperty:Boolean,activities:List<ActivityEntity>,labels:Map<String,String>,openBooking:(String)->Unit,select:(Long)->Unit) {
    val start=month.atDay(1).toEpochDay();val end=month.plusMonths(1).atDay(1).toEpochDay()
    val offset=month.atDay(1).dayOfWeek.value-1;val weeks=(offset+month.lengthOfMonth()+6)/7
    val multiple=labels.size>1 && !singleProperty
    val barHeight=maxOf(25f,19f*LocalDensity.current.fontScale).dp
    OutlinedCard(Modifier.fillMaxWidth().testTag("compact-month-grid")) {Column(Modifier.padding(6.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text(month.atDay(1).format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy",java.util.Locale.ITALIAN)).replaceFirstChar{it.titlecase()},style=MaterialTheme.typography.titleMedium)
        Text("Soggiorni · arrivo e partenza alle estremità",style=MaterialTheme.typography.labelSmall,color=ConceptMuted)
        if(!multiple && records.isNotEmpty())Text(labels[records.first().propertyId] ?: "Immobile",style=MaterialTheme.typography.labelMedium,color=ElledueGold)
        Row {listOf("Lun","Mar","Mer","Gio","Ven","Sab","Dom").forEach{Text(it,Modifier.weight(1f),style=MaterialTheme.typography.labelSmall,textAlign=androidx.compose.ui.text.style.TextAlign.Center,maxLines=1)}}
        repeat(weeks){w->
            val week=start-offset+w*7
            val segments=CalendarStays.segments(week,start,end,records)
            Column(Modifier.fillMaxWidth().testTag("stay-week-$w"),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                Row {repeat(7){d->
                    val day=week+d;val number=LocalDate.ofEpochDay(day).dayOfMonth
                    if(day !in start until end)Spacer(Modifier.weight(1f).height(32.dp)) else {
                        val ins=records.count{it.status!="CANCELLED" && it.checkInEpochDay==day};val outs=records.count{it.status!="CANCELLED" && it.checkOutEpochDay==day}
                        val occupied=records.count{it.status!="CANCELLED" && day>=it.checkInEpochDay && day<it.checkOutEpochDay}
                        val change=CalendarStays.turnovers(day,records)
                        Box(Modifier.weight(1f).height(32.dp).clip(MaterialTheme.shapes.small).background(if(day==selected)ElledueGold.copy(.24f) else if(change.isNotEmpty())ElledueGold.copy(.1f) else androidx.compose.ui.graphics.Color.Transparent).clickable{select(day)}.testTag("calendar-day-$day").semantics{contentDescription="${BookingDates.display(day)}, $ins arrivi, $outs partenze, $occupied soggiorni"+(if(change.isNotEmpty())", cambio ospiti: "+change.joinToString{labels[it] ?: "Immobile"} else "")+", apri giornata operativa"},contentAlignment=Alignment.Center){Text(number.toString(),color=if(day==today || day==selected)ElledueGold else MaterialTheme.colorScheme.onSurface,style=MaterialTheme.typography.labelLarge)}
                    }
                }}
                if(segments.isEmpty())Spacer(Modifier.height(16.dp))
                segments.groupBy{it.booking.propertyId}.toSortedMap(compareBy{labels[it] ?: it}).forEach{(property,stays)->
                    val color=propertyColor(property)
                    if(multiple)Text(labels[property] ?: "Immobile",style=MaterialTheme.typography.labelSmall,color=color,modifier=Modifier.testTag("week-property-$w-$property"))
                    CalendarStays.lanes(stays).forEach{lane->
                        BoxWithConstraints(Modifier.fillMaxWidth().height(barHeight)) {
                            val unit=maxWidth/14
                            lane.forEach{s->
                                val name="${s.booking.guestFirstName} ${s.booking.guestLastName}"
                                val description="${labels[property] ?: "Immobile"} · $name · arrivo ${BookingDates.display(s.booking.checkInEpochDay)} · partenza ${BookingDates.display(s.booking.checkOutEpochDay)}"
                                val shape=RoundedCornerShape(topStart=if(s.startsHere)8.dp else 0.dp,bottomStart=if(s.startsHere)8.dp else 0.dp,topEnd=if(s.endsHere)8.dp else 0.dp,bottomEnd=if(s.endsHere)8.dp else 0.dp)
                                Box(Modifier.offset(x=unit*s.startHalfDay).width(unit*(s.endHalfDay-s.startHalfDay)-1.dp).fillMaxHeight().clip(shape).background(color.copy(.23f)).border(1.dp,color.copy(.8f),shape).clickable{openBooking(s.booking.id)}.testTag("stay-bar-${s.booking.id}-$w").semantics{contentDescription=description}.padding(horizontal=4.dp),contentAlignment=Alignment.CenterStart){if(unit*(s.endHalfDay-s.startHalfDay)>=48.dp)Text(s.booking.guestLastName,style=MaterialTheme.typography.labelSmall,maxLines=1,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurface)}
                            }
                        }
                    }
                }
                val changes=(0..6).map{week+it}.filter{it in start until end && CalendarStays.turnovers(it,records).isNotEmpty()}
                changes.forEach{day->TextButton(onClick={select(day)},contentPadding=PaddingValues(horizontal=4.dp,vertical=0.dp),modifier=Modifier.heightIn(min=28.dp).testTag("turnover-$day")){Text("Cambio ospiti ${LocalDate.ofEpochDay(day).dayOfMonth} · "+CalendarStays.turnovers(day,records).joinToString{labels[it] ?: "Immobile"},style=MaterialTheme.typography.labelSmall)}}
                val tasks=activities.filter{a->a.dueEpochMillis?.let{Instant.ofEpochMilli(it).atZone(ZoneId.of(a.zoneId)).toLocalDate().toEpochDay() in maxOf(week,start) until minOf(week+7,end)}==true && !a.completed}
                if(tasks.isNotEmpty())Row(Modifier.horizontalScroll(rememberScrollState())){tasks.groupBy{Instant.ofEpochMilli(it.dueEpochMillis!!).atZone(ZoneId.of(it.zoneId)).toLocalDate().toEpochDay()}.toSortedMap().forEach{(day,items)->TextButton(onClick={select(day)},contentPadding=PaddingValues(horizontal=4.dp,vertical=0.dp)){Text("Attività ${LocalDate.ofEpochDay(day).dayOfMonth} (${items.size})",style=MaterialTheme.typography.labelSmall)}}}
            }
            if(w<weeks-1)HorizontalDivider(color=ConceptBorder)
        }
        if(records.none{it.status!="CANCELLED"})Text("Mese libero · nessun soggiorno",style=MaterialTheme.typography.bodySmall,color=ConceptMuted)
    }}
}
