package it.elledue.control.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import it.elledue.control.ElledueApplication
import it.elledue.control.MainActivity
import it.elledue.control.notifications.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter

@Composable fun NotificationSettings(back:()->Unit) {
    val context=LocalContext.current;val app=context.applicationContext as ElledueApplication
    val saved by app.reminderStore.state.collectAsState();var value by rememberPersistentDraft("notifications#settings",saved)
    var error by remember{mutableStateOf<String?>(null)};var result by remember{mutableStateOf<String?>(null)}
    val maintenanceError by app.maintenanceError.collectAsState()
    val scope=rememberCoroutineScope();var refresh by remember{mutableIntStateOf(0)}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){refresh++;scope.launch{app.reminders.reconcile(force=true)}}
    val lifecycle=androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycle){val observer=androidx.lifecycle.LifecycleEventObserver{_,event->if(event==androidx.lifecycle.Lifecycle.Event.ON_RESUME){refresh++;scope.launch{app.reminders.reconcile(force=true)}}};lifecycle.lifecycle.addObserver(observer);onDispose{lifecycle.lifecycle.removeObserver(observer)}}
    val allowed=remember(refresh){app.reminders.allowed()}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).testTag("notification-settings"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        TextButton(onClick=back){Text("Indietro")};ConceptHeading("Notifiche Android","Promemoria locali · Europe/Rome")
        Text(if(allowed)"Permesso Android concesso" else "Notifiche bloccate da Android",color=if(allowed)ArrivalColor else ElledueGold)
        OutlinedButton(onClick={if(Build.VERSION.SDK_INT>=33 && !allowed)permission.launch(Manifest.permission.POST_NOTIFICATIONS) else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName))}){Text("Permesso e canale Android")}
        ToggleSetting("Attiva promemoria",value.enabled){value=value.copy(enabled=it)}
        ToggleSetting("Arrivi",value.arrivals){value=value.copy(arrivals=it)}
        ToggleSetting("Partenze",value.departures){value=value.copy(departures=it)}
        ToggleSetting("Attività programmate",value.tasks){value=value.copy(tasks=it)}
        ToggleSetting("Pulizie programmate",value.cleaning){value=value.copy(cleaning=it)}
        ToggleSetting("Verifica registrazione ospiti",value.operations){value=value.copy(operations=it)}
        ChoiceField("Termine verifica registrazione",value.registrationDeadlineHours.toString(),listOf(1,4,12,24).map{it.toString() to "Entro $it ore dall’arrivo effettivo"}){value=value.copy(registrationDeadlineHours=it.toLong())}
        ToggleSetting("Tassa da verificare al check-out",value.tax){value=value.copy(tax=it)}
        ToggleSetting("Rendiconti a fine mese",value.monthly){value=value.copy(monthly=it)}
        ToggleSetting("Arrivi/partenze: giorno precedente",value.dayBefore){value=value.copy(dayBefore=it)}
        TimeSetting("Orario promemoria / fine mese (HH:mm)",value.reminderTime){value=value.copy(reminderTime=it)}
        if(!value.dayBefore){
            ChoiceField("Anticipo arrivi",value.arrivalLeadHours.toString(),listOf(0,1,2,6,12,24,48).map{it.toString() to "$it ore"}){value=value.copy(arrivalLeadHours=it.toLong())}
            ChoiceField("Anticipo partenze",value.departureLeadHours.toString(),listOf(0,1,2,6,12,24,48).map{it.toString() to "$it ore"}){value=value.copy(departureLeadHours=it.toLong())}
        }
        ChoiceField("Anticipo attività",value.taskLeadMinutes.toString(),listOf(0,15,30,60,120,1440).map{it.toString() to "$it minuti"}){value=value.copy(taskLeadMinutes=it.toLong())}
        TimeSetting("Attività senza ora · orario (HH:mm)",value.allDayTime){value=value.copy(allDayTime=it)}
        ToggleSetting("Fascia di silenzio",value.quietEnabled){value=value.copy(quietEnabled=it)}
        if(value.quietEnabled){TimeSetting("Silenzio dalle (HH:mm)",value.quietStart){value=value.copy(quietStart=it)};TimeSetting("Silenzio fino alle (HH:mm)",value.quietEnd){value=value.copy(quietEnd=it)}}
        Text("Le notifiche nella fascia di silenzio slittano alla sua fine. Gli allarmi sono inesatti e possono essere ritardati da Android, risparmio energetico o produttore. Dopo un arresto forzato riapri l’app. Nessun allarme esatto richiesto.",style=MaterialTheme.typography.bodySmall)
        maintenanceError?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        error?.let{Text(it,color=MaterialTheme.colorScheme.error)};result?.let{Text(it)}
        Button(onClick={scope.launch{runCatching{app.reminderStore.save(value);app.drafts.clear("notifications");app.reminders.reconcile(force=true)}.onSuccess{result="Impostazioni salvate e promemoria aggiornati";error=null;refresh++}.onFailure{error=it.message ?: "Salvataggio non riuscito"}}},modifier=Modifier.fillMaxWidth().testTag("save-notifications")){Text("Salva promemoria")}
        OutlinedButton(onClick={runCatching{app.reminders.test()}.onFailure{error=it.message}},enabled=allowed){Text("Invia notifica di prova")}
        DraftBanner("notifications"){value=saved}
        Text("Prossimi promemoria",style=MaterialTheme.typography.titleLarge)
        val jobs=remember(refresh,saved){app.reminderStore.scheduled().values.sortedBy{it.at}}
        if(jobs.isEmpty())Text("Nessun promemoria futuro programmato. Verifica attivazione, permessi e date.")
        jobs.take(50).forEach{job->Text(job.title+" · "+DateTimeFormatter.ofPattern("dd/MM HH:mm").format(Instant.ofEpochMilli(job.at).atZone(ReminderPlanner.zone)),style=MaterialTheme.typography.bodySmall)}
    }
}
@Composable private fun ToggleSetting(label:String,selected:Boolean,change:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Text(label,Modifier.weight(1f));Switch(selected,change)}}

@Composable fun AccessSettings(activity:MainActivity,back:()->Unit) {
    val app=activity.application as ElledueApplication;val settings by app.appLock.settings.collectAsState()
    var value by remember{mutableStateOf(settings)};var message by remember{mutableStateOf<String?>(null)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).testTag("access-settings"),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        TextButton(onClick=back){Text("Indietro")};ConceptHeading("Accesso protetto","Biometria e credenziale del dispositivo")
        Text("${if(settings.enabled)"Protezione attiva" else "Protezione disattivata"}. Il codice del telefono resta gestito da Android.")
        ToggleSetting("Proteggi l’accesso",value.enabled){value=value.copy(enabled=it)}
        ToggleSetting("Preferisci credenziale del dispositivo",value.deviceCredentialPreferred){value=value.copy(deviceCredentialPreferred=it)}
        ChoiceField("Blocca dopo inattività fuori dall’app",value.timeoutSeconds.toString(),listOf("0" to "Subito","30" to "30 secondi","60" to "1 minuto","300" to "5 minuti","900" to "15 minuti")){value=value.copy(timeoutSeconds=it.toLong())}
        Text("Il dispositivo deve avere un blocco schermo sicuro. Per cambiare o disattivare la protezione conferma prima la tua identità. Se la biometria fallisce usa la credenziale Android; non cancellare i dati dell’app. Per recuperare la credenziale segui la procedura del produttore.",style=MaterialTheme.typography.bodySmall)
        message?.let{Text(it)}
        Button(onClick={activity.authenticate(value.deviceCredentialPreferred,{app.appLock.configureAuthenticated(value);message="Protezione aggiornata"},{message=it})},modifier=Modifier.fillMaxWidth().testTag("save-access")){Text("Conferma e salva protezione")}
        if(settings.enabled)OutlinedButton(onClick={app.appLock.lock()}){Text("Blocca adesso")}
        Text("Il blocco protegge l’accesso nell’app. Database e backup esportati non sono cifrati da questa funzione. Custodisci i backup in una posizione privata.",style=MaterialTheme.typography.bodySmall)
    }
}
@Composable fun LockedScreen(activity:MainActivity) {
    val app=activity.application as ElledueApplication;var error by remember{mutableStateOf<String?>(null)}
    Column(Modifier.fillMaxSize().padding(32.dp).testTag("app-locked"),verticalArrangement=Arrangement.Center) {
        androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(it.elledue.control.R.drawable.elledue_logo_gold),"Elledue",Modifier.fillMaxWidth().height(60.dp))
        Spacer(Modifier.height(32.dp));Text("Il tuo spazio, protetto",style=MaterialTheme.typography.headlineLarge)
        Text("Autenticati per accedere ai dati della gestione.",Modifier.padding(vertical=16.dp))
        Button(onClick={activity.authenticate(app.appLock.settings.value.deviceCredentialPreferred,{app.appLock.grant()},{error=it})},modifier=Modifier.fillMaxWidth()){Text("Sblocca Elledue Control")}
        OutlinedButton(onClick={activity.authenticate(true,{app.appLock.grant()},{error=it})},modifier=Modifier.fillMaxWidth()){Text("Usa credenziale del dispositivo")}
        error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        Text("Nessun dato viene eliminato dopo un tentativo fallito.",style=MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun TimeSetting(label:String,value:String,change:(String)->Unit){OutlinedTextField(value,change,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth())}
