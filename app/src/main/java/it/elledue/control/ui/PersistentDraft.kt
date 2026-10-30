package it.elledue.control.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import it.elledue.control.data.DraftStore

@Composable inline fun <reified T:Any> rememberPersistentDraft(key:String,initial:T):MutableState<T> {
    val store=DraftStore.get(LocalContext.current)
    return remember(key) {
        val state=mutableStateOf(store.load(key,T::class.java) ?: initial)
        if(key.endsWith("#id") && state.value.toString().isNotBlank() && !store.contains(key))store.save(key,state.value)
        object:MutableState<T> {
            override var value:T
                get()=state.value
                set(v){state.value=v;if(v==initial)store.clear(key) else store.save(key,v)}
            override fun component1()=value
            override fun component2():(T)->Unit = {value=it}
        }
    }
}
@Composable fun DraftBanner(route:String,discard:()->Unit) {
    val store=DraftStore.get(LocalContext.current);val revision by store.revision.collectAsState();val error by store.error.collectAsState()
    if(revision>=0 && store.has(route)) Column(Modifier.fillMaxWidth().testTag("draft-banner")) {
        Text(error ?: "Modifiche locali non ancora salvate nel record",style=MaterialTheme.typography.bodySmall,color=if(error==null)ConceptMuted else MaterialTheme.colorScheme.error)
        TextButton(onClick={store.clear(route);discard();store.clear(route)},modifier=Modifier.testTag("discard-draft")){Text("Scarta modifiche locali")}
    }
}
@Composable fun LocalDraftExitDialog(save:()->Unit,keep:()->Unit,discard:()->Unit,dismiss:()->Unit) {
    AlertDialog(onDismissRequest=dismiss,title={Text("Come vuoi uscire dal modulo?")},text={Text("Puoi salvare nel record, conservare le modifiche solo come bozza locale oppure scartarle. Scartare non elimina il record originale.")},confirmButton={Column {
        TextButton(onClick=save,modifier=Modifier.testTag("exit-save")){Text("Salva modifica")}
        TextButton(onClick=keep,modifier=Modifier.testTag("exit-keep")){Text("Conserva come bozza")}
        TextButton(onClick=discard,modifier=Modifier.testTag("exit-discard")){Text("Scarta modifiche")}
    }},dismissButton={TextButton(onClick=dismiss){Text("Continua a modificare")}})
}
@Composable fun DraftsScreen(open:(String)->Unit,back:()->Unit) {
    val store=DraftStore.get(LocalContext.current);val version by store.revision.collectAsState()
    var all by remember{mutableStateOf(false)};var failure by remember{mutableStateOf<String?>(null)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).testTag("local-drafts")) {
        TextButton(onClick=back){Text("Indietro")};ConceptHeading("Modifiche locali","Moduli non ancora salvati nei record")
        Text("Questi appunti sono diversi dai rendiconti in bozza salvati nel database. Scartarli non elimina prenotazioni, spese, attività o documenti.",style=MaterialTheme.typography.bodySmall)
        val routes=remember(version){store.routes()}
        if(routes.isEmpty())Text("Nessuna modifica locale da recuperare.")
        if(routes.isNotEmpty())OutlinedButton(onClick={all=true},modifier=Modifier.testTag("discard-all-local-drafts")){Text("Scarta tutte le modifiche locali")}
        failure?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        routes.forEach{route->Row(Modifier.fillMaxWidth()) {
            TextButton(onClick={open(route)},modifier=Modifier.weight(1f).testTag("resume-draft-$route")){Text("Riprendi · "+when{route.startsWith("booking")->"Prenotazione";route.startsWith("property")->"Immobile";route.startsWith("activity")->"Attività";route.startsWith("expense")->"Spesa";route.startsWith("tax")->"Tassa";route.startsWith("messages")->"Messaggio locale";route.startsWith("movement")->"Registrazione locale";route.startsWith("report/")->"Note locali del rendiconto";route.startsWith("notifications")->"Preferenze non applicate";else->"Impostazioni"})}
            TextButton(onClick={store.clear(route)},modifier=Modifier.testTag("discard-local-$route")){Text("Scarta")}
        }}
    }
    if(all)AlertDialog(onDismissRequest={all=false},title={Text("Scartare tutte le modifiche locali?")},text={Text("I campi non salvati saranno rimossi. Record già salvati e rendiconti, comprese le bozze nel database, rimangono intatti.")},confirmButton={TextButton(onClick={runCatching{store.discardAll()}.onFailure{failure=it.message};all=false},modifier=Modifier.testTag("confirm-discard-all-local")){Text("Scarta tutte")}},dismissButton={TextButton(onClick={all=false}){Text("Annulla")}})
}
