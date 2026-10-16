package it.elledue.control.ui

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
import it.elledue.control.data.*
import kotlinx.coroutines.*

@Composable fun DeletionScreen(repository:DeletionRepository,kind:String,id:String,back:()->Unit,done:()->Unit) {
    var plan by remember{mutableStateOf<DeletionPlan?>(null)};var receipt by remember{mutableStateOf<DeletionReceipt?>(null)}
    var text by remember{mutableStateOf("")};var docs by remember{mutableStateOf(false)};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)}
    val scope=rememberCoroutineScope();val context=LocalContext.current
    LaunchedEffect(kind,id){try{plan=repository.prepare(kind,id)}catch(e:Exception){error=e.message ?: "Anteprima non disponibile"}}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
        if(uri!=null && plan!=null)scope.launch{busy=true;try{receipt=repository.saveVerifiedBackup(plan!!,uri);error=null}catch(e:CancellationException){throw e}catch(e:Exception){error="Backup non verificato: ${e.message}"}finally{busy=false}}
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).testTag("deletion-screen"),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        TextButton(onClick=back,enabled=!busy){Text("Indietro")};ConceptHeading("Elimina definitivamente","Operazione irreversibile sul database corrente")
        plan?.let {p ->
            Text(p.label,style=MaterialTheme.typography.titleLarge)
            p.counts.forEach{(label,count)->Text("$label: $count")}
            if(p.reportIds.isNotEmpty()) {
                Text("Saranno eliminati i rendiconti completi dei periodi interessati, comprese le versioni confermate. Le altre prenotazioni e le spese indipendenti rimangono e tornano da rendicontare.",color=MaterialTheme.colorScheme.error)
                Row{Checkbox(docs,{docs=it},enabled=!busy);Text("Confermo l’eliminazione dei documenti e delle versioni coinvolte",Modifier.weight(1f).padding(top=10.dp))}
            }
            Text("Backup esterni e PDF già esportati o condivisi non possono essere cancellati dall’app. Le bozze locali dei record eliminati saranno rimosse.",style=MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick={picker.launch("Elledue-backup-prima-eliminazione.json")},enabled=!busy,modifier=Modifier.testTag("deletion-backup")){Text("1 · Salva e verifica backup preventivo")}
            receipt?.let{Text("Backup verificato e accessibile nel documento scelto.",color=ArrivalColor)}
            OutlinedTextField(text,{text=it},label={Text("Digita ELIMINA ${p.label}")},enabled=!busy,modifier=Modifier.fillMaxWidth().testTag("deletion-confirmation"))
            Button(onClick={if(!busy){busy=true;scope.launch{var committed=false;try {
                repository.delete(p,requireNotNull(receipt),text,docs)
                committed=true
                withContext(Dispatchers.Main.immediate){done()}
            }catch(e:CancellationException){throw e}catch(e:Exception){error=if(committed)"Eliminazione completata nel database. Torna all’elenco immobili." else e.message ?: "Eliminazione non riuscita. Dati conservati."}finally{busy=false}}}},enabled=!busy && receipt!=null && text=="ELIMINA ${p.label}" && (p.reportIds.isEmpty()||docs),colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.error),modifier=Modifier.testTag("delete-permanently")){Text("2 · Elimina definitivamente")}
        }
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth());error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
    }
}
