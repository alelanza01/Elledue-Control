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
                set(v){state.value=v;store.save(key,v)}
            override fun component1()=value
            override fun component2():(T)->Unit = {value=it}
        }
    }
}
@Composable fun DraftBanner(route:String,discard:()->Unit) {
    val store=DraftStore.get(LocalContext.current);val revision by store.revision.collectAsState();val error by store.error.collectAsState()
    if(revision>=0 && store.has(route)) Column(Modifier.fillMaxWidth().testTag("draft-banner")) {
        Text(error ?: "Bozza locale salvata · puoi riprendere dopo un’interruzione",style=MaterialTheme.typography.bodySmall,color=if(error==null)ConceptMuted else MaterialTheme.colorScheme.error)
        TextButton(onClick={store.clear(route);discard();store.clear(route)},modifier=Modifier.testTag("discard-draft")){Text("Scarta bozza")}
    }
}
@Composable fun DraftsScreen(open:(String)->Unit,back:()->Unit) {
    val store=DraftStore.get(LocalContext.current);val version by store.revision.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        TextButton(onClick=back){Text("Indietro")};ConceptHeading("Le tue bozze","Solo moduli in compilazione, nessun movimento definitivo")
        val routes=remember(version){store.routes()}
        if(routes.isEmpty())Text("Nessuna bozza da recuperare.")
        routes.forEach{route->Row(Modifier.fillMaxWidth()) {
            TextButton(onClick={open(route)},modifier=Modifier.weight(1f).testTag("resume-draft-$route")){Text("Riprendi bozza · "+when{route.startsWith("booking")->"Prenotazione";route.startsWith("property")->"Immobile";route.startsWith("activity")->"Attività";route.startsWith("expense")->"Spesa";route.startsWith("tax")->"Tassa";route.startsWith("messages")->"Modello ospiti";route.startsWith("movement")->"Movimento";route.startsWith("report/")->"Rendiconto";route.startsWith("notifications")->"Promemoria";else->"Impostazioni"})}
            TextButton(onClick={store.clear(route)}){Text("Scarta")}
        }}
    }
}
