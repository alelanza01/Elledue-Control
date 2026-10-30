package it.elledue.control.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.elledue.control.data.*
import it.elledue.control.domain.*
import java.time.*
import kotlinx.coroutines.launch

@Composable fun IssuesScreen(issues:List<PropertyIssue>,properties:List<PropertyWithOwner>,open:(String)->Unit,back:()->Unit) {
 var property by rememberSaveable{mutableStateOf("")};var status by rememberSaveable{mutableStateOf("")};var priority by rememberSaveable{mutableStateOf("")}
 var onlyOpen by rememberSaveable{mutableStateOf(true)};var date by rememberSaveable{mutableStateOf("")}
 val shown=issues.filter{(property.isBlank()||it.propertyId==property)&&(status.isBlank()||it.status==status)&&(priority.isBlank()||it.priority==priority)&&(!onlyOpen||IssueRepository.open(it))&&(date.isBlank()||runCatching{BookingDates.parse(date).toEpochDay()==it.reportedDay}.getOrDefault(false))}
 LazyColumn(Modifier.fillMaxSize().testTag("issues-screen"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  item{TextButton(onClick=back){Text("Indietro")};ConceptHeading("Manutenzioni","Segnalazioni, incarichi e risoluzioni")}
  item{Button(onClick={open("issue-create")}){Text("Segnala manutenzione")}}
  item{PropertySelector(properties,property,allOption=true){property=it}}
  item{ChoiceField("Stato",status,listOf("" to "Tutti gli stati")+IssueRepository.statuses.toList()){status=it}}
  item{ChoiceField("Priorità",priority,listOf("" to "Tutte le priorità")+IssueRepository.priorities.toList()){priority=it}}
  item{Row{Checkbox(onlyOpen,{onlyOpen=it});Text("Solo manutenzioni aperte",Modifier.padding(top=12.dp))}}
  item{AccountingField("Data segnalazione · gg/mm/aaaa (facoltativa)",date){date=it}}
  if(shown.isEmpty())item{EmptyState("Nessuna manutenzione da mostrare","Le segnalazioni restano qui anche quando un soggiorno termina.")}
  items(shown,key={it.id}){i->OutlinedCard(onClick={open("issue/${i.id}")},modifier=Modifier.fillMaxWidth().testTag("issue-row-${i.id}")){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
   Text(i.title,style=MaterialTheme.typography.titleMedium);Text(properties.firstOrNull{it.property.id==i.propertyId}?.property?.name ?: "Storico")
   Text("${IssueRepository.priorities[i.priority]} · ${IssueRepository.statuses[i.status]}",color=if(i.priority in listOf("HIGH","URGENT"))DepartureColor else ElledueGold)
   i.scheduledAt?.let{Text("Intervento ${Instant.ofEpochMilli(it).atZone(BookingDates.zone).format(java.time.format.DateTimeFormatter.ofPattern(if(i.hasTime)"dd/MM/yyyy HH:mm" else "dd/MM/yyyy"))}")}
  }}}
 }
}

@Composable fun IssueEditor(initial:PropertyIssue,properties:List<PropertyWithOwner>,bookings:List<ReservationEntity>,expenses:List<ExpenseEntity>,repository:IssueRepository,back:()->Unit,actionScope:kotlinx.coroutines.CoroutineScope?=null,saved:(String)->Unit) {
 val key=if(initial.updatedAt==0L)"issue/new" else "issue/${initial.id}"
 var draft by rememberPersistentDraft(key,initial)
 var scheduledDay by rememberPersistentDraft("$key#day",initial.scheduledAt?.let{BookingDates.display(Instant.ofEpochMilli(it).atZone(BookingDates.zone).toLocalDate().toEpochDay())} ?: "")
 var scheduledTime by rememberPersistentDraft("$key#time",initial.scheduledAt?.takeIf{initial.hasTime}?.let{Instant.ofEpochMilli(it).atZone(BookingDates.zone).toLocalTime().toString()} ?: "")
 val localScope=rememberCoroutineScope();val scope=actionScope ?: localScope;val context=androidx.compose.ui.platform.LocalContext.current
 var error by remember{mutableStateOf<String?>(null)};var busy by remember{mutableStateOf(false)}
 var details by rememberSaveable{mutableStateOf(false)}
 var history by remember(initial.id,initial.updatedAt){mutableStateOf<List<IssueHistory>>(emptyList())}
 LaunchedEffect(initial.id,initial.updatedAt){history=repository.history(initial.id)}
 Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp).testTag("issue-editor"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  TextButton(onClick=back){Text("Indietro")};ConceptHeading(if(initial.updatedAt==0L)"Nuova manutenzione" else "Scheda manutenzione","Storico conservato anche dopo la chiusura del soggiorno")
  DraftBanner(key){draft=initial}
  PropertySelector(properties.filter{!it.property.archived || it.property.id==initial.propertyId},draft.propertyId){draft=draft.copy(propertyId=it,reservationId=null,expenseId=null)}
  AccountingField("Titolo *",draft.title,"issue-title"){draft=draft.copy(title=it)}
  AccountingField("Descrizione",draft.description){draft=draft.copy(description=it)}
  TextButton(onClick={details=!details},modifier=Modifier.testTag("maintenance-details")){Text(if(details)"Chiudi programmazione e dettagli" else "Programmazione e dettagli")}
  if(details) {
  AccountingField("Categoria",draft.category){draft=draft.copy(category=it)}
  BookingDateField("Segnalato il",BookingDates.display(draft.reportedDay),true,"issue-reported",Modifier.fillMaxWidth()){runCatching{BookingDates.parse(it).toEpochDay()}.getOrNull()?.let{day->draft=draft.copy(reportedDay=day)}}
  ChoiceField("Priorità",draft.priority,IssueRepository.priorities.toList()){draft=draft.copy(priority=it)}
  }
  ChoiceField("Stato",draft.status,IssueRepository.statuses.toList()){draft=draft.copy(status=it)}
  draft.resolvedAt?.let{Text("Risolto il ${Instant.ofEpochMilli(it).atZone(BookingDates.zone).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}")}
  ChoiceField("Soggiorno collegato",draft.reservationId ?: "",listOf("" to "Nessuno")+bookings.filter{it.propertyId==draft.propertyId}.map{it.id to "${it.guestFirstName} ${it.guestLastName} · ${BookingDates.display(it.checkInEpochDay)}"}){draft=draft.copy(reservationId=it.ifBlank{null})}
  if(details) {
  AccountingField("Intervento previsto · gg/mm/aaaa (facoltativo)",scheduledDay){scheduledDay=it}
  AccountingField("Orario intervento · HH:mm (facoltativo)",scheduledTime){scheduledTime=it}
  AccountingField("Persona / fornitore incaricato",draft.assignee){draft=draft.copy(assignee=it)}
  AccountingField("Note operative",draft.notes){draft=draft.copy(notes=it)}
  }
  ChoiceField("Spesa già registrata",draft.expenseId ?: "",listOf("" to "Nessuna spesa")+expenses.filter{it.propertyId==draft.propertyId && !it.voided}.map{it.id to "${it.description} · ${Amounts.euro(it.actualCents)}"}){draft=draft.copy(expenseId=it.ifBlank{null})}
  Text("Collegare una spesa non crea un secondo movimento. La programmazione di un intervento non registra costi.",style=MaterialTheme.typography.bodySmall)
  if(error!=null)Text(error!!,color=MaterialTheme.colorScheme.error)
  Button(enabled=!busy,onClick={busy=true;scope.launch{try{
   require(scheduledDay.isNotBlank() || scheduledTime.isBlank()){ "Indica prima la data dell’intervento." }
   val at=scheduledDay.takeIf{it.isNotBlank()}?.let{BookingDates.parse(it).atTime(if(scheduledTime.isBlank())LocalTime.MIDNIGHT else LocalTime.parse(scheduledTime)).atZone(BookingDates.zone).toInstant().toEpochMilli()}
   val id=repository.save(draft.copy(scheduledAt=at,hasTime=scheduledTime.isNotBlank()))
   DraftStore.get(context).clear(key);kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main){saved(id)}
  }catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){error=e.message ?: "Salvataggio non riuscito"}finally{busy=false}}},modifier=Modifier.testTag("save-issue")){Text("Salva manutenzione")}
  if(details && history.isNotEmpty())Text("Storico modifiche",style=MaterialTheme.typography.titleMedium)
  if(details)history.forEach{h->val item=com.google.gson.Gson().fromJson(h.snapshotJson,PropertyIssue::class.java);Text("${Instant.ofEpochMilli(h.recordedAt).atZone(BookingDates.zone).toLocalDate()} · ${IssueRepository.statuses[item.status]} · ${item.title}",style=MaterialTheme.typography.bodySmall)}
 }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun OperationalDayScreen(day:Long,properties:List<PropertyWithOwner>,bookings:List<ReservationEntity>,activities:List<ActivityEntity>,issues:List<PropertyIssue>,open:(String)->Unit,expenses:List<ExpenseEntity> = emptyList(),back:()->Unit) {
 val app=androidx.compose.ui.platform.LocalContext.current.applicationContext as it.elledue.control.ElledueApplication
 val store=app.reminderStore
 val ids=properties.map{it.property.id}.toSet()
 val guestJobs=(store.scheduled().values+store.history()).distinctBy{it.key}.filter{job->
    job.route.startsWith("guest-message/") && Instant.ofEpochMilli(job.at).atZone(BookingDates.zone).toLocalDate().toEpochDay()==day && bookings.any{it.id==job.route.split('/').getOrNull(1) && it.propertyId in ids && it.status!="CANCELLED"}
 }
 val guestEvents=guestJobs.map{job->val b=bookings.first{it.id==job.route.split('/')[1]};DayEvent("guest:${job.key}",Instant.ofEpochMilli(job.at).atZone(BookingDates.zone).toLocalTime().toString(),job.title+" · messaggio ospite",b.propertyId,job.route,store.handled(job.key),"GUEST_MESSAGE")}
 val scopedBookings=bookings.filter{it.propertyId in ids}
 val rows=(OperationalDay.events(day,scopedBookings,activities.filter{it.propertyId==null || it.propertyId in ids},issues.filter{it.propertyId in ids},expenses=expenses)+guestEvents).sortedWith(compareBy<DayEvent>{it.time==null}.thenBy{it.time}.thenBy{it.key})
 val names=properties.associate{it.property.id to it.property.name}
 val change=scopedBookings.filter{it.status!="CANCELLED" && it.checkInEpochDay==day}.map{it.propertyId}.toSet().intersect(scopedBookings.filter{it.status!="CANCELLED" && it.checkOutEpochDay==day}.map{it.propertyId}.toSet())
 LazyColumn(Modifier.fillMaxSize().testTag("operational-day"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  item{TextButton(onClick=back){Text("Calendario")};ConceptHeading("Giornata operativa",LocalDate.ofEpochDay(day).format(java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM yyyy",java.util.Locale.ITALIAN)))}
  item{FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("${rows.count{it.kind=="ARRIVAL"}} check-in","${rows.count{it.kind=="DEPARTURE"}} check-out","${rows.count{it.kind !in listOf("ARRIVAL","DEPARTURE","ISSUE","STAY")}} attività","${rows.count{it.kind=="ISSUE"}} interventi").forEach{SuggestionChip(onClick={},label={Text(it)})}}}
  if(change.isNotEmpty())item{Text("Cambio ospite: ${change.joinToString{names[it] ?: "Immobile"}}",color=ElledueGold)}
  if(rows.isEmpty())item{EmptyState("Una giornata libera","Nessun evento programmato per questa data.")}
  val groups=listOf("Arrivi" to rows.filter{it.kind=="ARRIVAL"},"Partenze" to rows.filter{it.kind=="DEPARTURE"},"Soggiorni attivi" to rows.filter{it.kind=="STAY"},"Pulizie" to rows.filter{it.kind=="CLEANING"},"Attività e manutenzioni" to rows.filter{it.kind !in listOf("ARRIVAL","DEPARTURE","STAY","CLEANING")})
  groups.filter{it.second.isNotEmpty()}.forEach{(title,events)->
   item{Text(title,style=MaterialTheme.typography.titleLarge)}
   items(events,key={it.key}){e->OutlinedCard(onClick={open(e.route)},modifier=Modifier.fillMaxWidth().testTag("day-${e.key}")){
    Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){
     Text(e.time ?: if(e.kind=="STAY")"In casa" else "Giornata",style=MaterialTheme.typography.labelLarge,color=ElledueGold)
     Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){
      Text(e.title,style=MaterialTheme.typography.titleMedium);Text(names[e.propertyId] ?: "Gestione generale")
      if(e.route.startsWith("booking/"))bookings.firstOrNull{it.id==e.route.substringAfter("booking/")}?.let{Text("${it.guestCount} ospiti · ${BookingDates.display(it.checkInEpochDay)} – ${BookingDates.display(it.checkOutEpochDay)}",style=MaterialTheme.typography.bodySmall)}
      if(e.kind!="STAY")Text(if(e.completed)"Completato" else "Da verificare / aperto",color=ConceptMuted,style=MaterialTheme.typography.bodySmall)
     }
    }
   }}
  }

 }
}
