package it.elledue.control.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import it.elledue.control.R
import it.elledue.control.data.*
import it.elledue.control.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.*
import java.util.UUID

fun copyGuestText(context:Context, text:String):Boolean = runCatching {
    require(text.isNotBlank())
    val clipboard=context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Elledue Control",text))
    true
}.getOrDefault(false)

val homeCovers=listOf(R.drawable.home_cover_1,R.drawable.home_cover_2,R.drawable.home_cover_3)
@Composable fun AppearanceScreen(settings:AppSettings?, model:SettingsViewModel, back:()->Unit) {
    val state by model.saveState.collectAsState()
    var selection by rememberPersistentDraft("appearance#selection",settings?.homeCover ?: 0)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        TextButton(onClick=back){Text("Indietro")};ConceptHeading("Aspetto","Copertina artistica della Home")
        Text("La scelta non modifica le fotografie degli immobili. Il tema Elledue rimane scuro per garantire il contrasto.")
        listOf("Terrazza Etna","Living mediterraneo","Terrazza all’ora blu").forEachIndexed { i,label ->
            Column(Modifier.fillMaxWidth().clickable {selection=i+1}.testTag("cover-${i+1}")) {
                Image(painterResource(homeCovers[i]),label,Modifier.fillMaxWidth().height(130.dp),contentScale=ContentScale.Crop)
                Row { RadioButton(selection==i+1,{selection=i+1});Text(label,Modifier.padding(12.dp)) }
            }
        }
        Row { RadioButton(selection==0,{selection=0});Text("Foto immobile preesistente, se presente",Modifier.padding(12.dp)) }
        Button(onClick={model.cover(selection)},enabled=state!=SaveState.Saving,modifier=Modifier.testTag("apply-cover")){Text("Applica copertina")}
        TextButton(onClick={selection=1;model.cover(1)}){Text("Ripristina Terrazza Etna")}
        if(state==SaveState.Saved) Text("Copertina salvata")
        if(state==SaveState.Failed) Text("Salvataggio non riuscito",color=MaterialTheme.colorScheme.error)
    }
}

@Composable fun ActivityEditor(repository:ActivityRepository, initial:ActivityEntity?, properties:List<PropertyWithOwner>, bookings:List<ReservationEntity>, back:()->Unit) {
    val draftKey=if(initial==null)"activity/new" else "activity/${initial.id}"
    val id by rememberPersistentDraft("$draftKey#id",initial?.id ?: UUID.randomUUID().toString())
    var title by rememberPersistentDraft("$draftKey#title",initial?.title ?: "");var notes by rememberPersistentDraft("$draftKey#notes",initial?.notes ?: "")
    val due=initial?.dueEpochMillis?.let {Instant.ofEpochMilli(it).atZone(ZoneId.of(initial.zoneId))}
    var date by rememberPersistentDraft("$draftKey#date",due?.toLocalDate()?.format(BookingDates.inputFormat) ?: "")
    var time by rememberPersistentDraft("$draftKey#time",due?.takeIf{initial?.hasTime!=false}?.toLocalTime()?.toString()?.take(5) ?: "")
    var property by rememberPersistentDraft("$draftKey#property",initial?.propertyId ?: "");var booking by rememberPersistentDraft("$draftKey#booking",initial?.reservationId ?: "")
    var completed by rememberPersistentDraft("$draftKey#completed",initial?.completed ?: false)
    var kind by rememberPersistentDraft("$draftKey#kind",initial?.kind ?: "GENERAL")
    var busy by remember {mutableStateOf(false)};var error by remember {mutableStateOf<String?>(null)};var deleting by remember {mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    fun save() {if(busy)return;busy=true;scope.launch {try {
        require(time.isBlank() || date.isNotBlank()) {"Inserisci una data prima dell’orario."}
        val stamp=if(date.isBlank()) null else {
            val local=LocalDateTime.of(BookingDates.parse(date),if(time.isBlank()) LocalTime.MIDNIGHT else LocalTime.parse(time))
            val zone=ZoneId.of(initial?.zoneId ?: "Europe/Rome")
            require(zone.rules.getValidOffsets(local).isNotEmpty()) {"Orario inesistente per il passaggio all’ora legale."}
            local.atZone(zone).toInstant().toEpochMilli()
        }
        repository.save(ActivityEntity(id,property.ifBlank {null},title,stamp,initial?.zoneId ?: "Europe/Rome",completed,booking.ifBlank {null},notes,kind=kind,hasTime=time.isNotBlank()));back()
    } catch(e:CancellationException){throw e} catch(e:Exception){error=e.message ?: "Salvataggio non riuscito"} finally {busy=false} } }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp).testTag("activity-editor"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        DraftBanner(draftKey){back()}
        TextButton(onClick=back,enabled=!busy){Text("Indietro")};Text(if(initial==null)"Nuova attività" else "Dettaglio e modifica attività",style=MaterialTheme.typography.headlineLarge)
        OutlinedTextField(title,{title=it},label={Text("Titolo *")},modifier=Modifier.fillMaxWidth().testTag("activity-title"),enabled=!busy)
        OutlinedTextField(notes,{notes=it},label={Text("Descrizione / note")},modifier=Modifier.fillMaxWidth().testTag("activity-notes"),enabled=!busy)
        ChoiceField("Tipologia attività",kind,listOf("GENERAL" to "Attività generale","CLEANING" to "Pulizia programmata","MAINTENANCE" to "Manutenzione","REGISTRATION" to "Registrazione ospiti"),!busy){kind=it}
        BookingDateField("Data facoltativa",date,!busy,"activity-date",Modifier.fillMaxWidth()){date=it}
        OutlinedTextField(time,{time=it},label={Text("Ora HH:mm · facoltativa")},modifier=Modifier.fillMaxWidth().testTag("activity-time"),enabled=!busy)
        Text("Senza data: attività non programmata. Con data e senza ora: attività della giornata. Fuso Europe/Rome.",style=MaterialTheme.typography.bodySmall)
        ConceptPropertySelector(properties,property,allOption=true,enabled=!busy){property=it;booking=""}
        var expand by remember {mutableStateOf(false)}
        OutlinedButton(onClick={expand=true},enabled=!busy){Text(bookings.firstOrNull {it.id==booking}?.let {"${it.guestFirstName} ${it.guestLastName}"} ?: "Prenotazione facoltativa")}
        DropdownMenu(expand,{expand=false}) {
            DropdownMenuItem(text={Text("Nessuna prenotazione")},onClick={booking="";expand=false})
            bookings.filter {property.isBlank() || it.propertyId==property}.forEach {b -> DropdownMenuItem(text={Text("${b.guestFirstName} ${b.guestLastName} · ${BookingDates.display(b.checkInEpochDay)}")},onClick={booking=b.id;property=b.propertyId;expand=false})}
        }
        Row {Checkbox(completed,{completed=it},enabled=!busy);Text(if(completed)"Completata (deseleziona per riaprire)" else "Da fare",Modifier.padding(12.dp))}
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        Button(onClick={save()},enabled=!busy && title.isNotBlank(),modifier=Modifier.testTag("save-activity")){Text("Salva attività")}
        if(initial!=null) TextButton(onClick={deleting=true},enabled=!busy){Text("Elimina attività")}
    }
    if(deleting) AlertDialog(onDismissRequest={deleting=false},title={Text("Eliminare l’attività?")},text={Text("Spese, pulizie e movimenti contabili rimarranno conservati.")},confirmButton={TextButton(onClick={if(!busy){busy=true;scope.launch {try{repository.delete(id);back()}catch(e:Exception){error="Eliminazione non riuscita"}finally{busy=false;deleting=false}}}},enabled=!busy){Text("Conferma eliminazione")}},dismissButton={TextButton(onClick={deleting=false}){Text("Annulla")}})
}

@Composable fun CopyCalendarScreen(records:List<ReservationEntity>,properties:List<PropertyWithOwner>, initialProperty:String, initialMonth:String, selected:Long, back:()->Unit) {
    var property by rememberSaveable {mutableStateOf(initialProperty)}
    val context=LocalContext.current;var feedback by remember {mutableStateOf("")}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).testTag("copy-calendar-screen"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        TextButton(onClick=back){Text("Indietro")};ConceptHeading("Copia calendario","Un messaggio indipendente per appartamento")
        ConceptPropertySelector(properties,property,allOption=true){property=it;feedback=""}
        Text("Tutti i soggiorni non cancellati con check-out da oggi in poi, compresi quelli in partenza oggi. Nessun limite futuro e nessun invio automatico.",style=MaterialTheme.typography.bodySmall)
        properties.filter {property.isBlank() || it.property.id==property}.forEach {p ->
            val rows=CalendarText.upcoming(p.property.id,BookingDates.today(),records)
            val text=CalendarText.render(p.property.name,rows)
            Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=ConceptCard),border=BorderStroke(1.dp,ConceptBorder)) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text(p.property.name,style=MaterialTheme.typography.titleLarge)
                Text(text.ifBlank {"Nessuna prenotazione attiva nel periodo."},modifier=Modifier.testTag("calendar-text-${p.property.id}"))
                Button(onClick={feedback=if(copyGuestText(context,text))"Messaggio copiato · ${p.property.name}" else "Copia non riuscita"},enabled=text.isNotBlank(),modifier=Modifier.testTag("copy-calendar-${p.property.id}")){Text("Copia messaggio")}
            }}
        }
        if(properties.isEmpty()) Text("Aggiungi un immobile per esportare il calendario.")
        if(feedback.isNotBlank()) Text(feedback,modifier=Modifier.testTag("copy-feedback"))
    }
}
