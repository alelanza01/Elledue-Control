package it.elledue.control.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import it.elledue.control.data.*
import kotlinx.coroutines.*

@Composable fun DeletionScreen(repository:DeletionRepository,kind:String,id:String,back:()->Unit,done:()->Unit) {
 var plan by remember{mutableStateOf<DeletionPlan?>(null)};var receipt by remember{mutableStateOf<DeletionReceipt?>(null)}
 var text by remember{mutableStateOf("")};var docs by remember{mutableStateOf(false)};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)}
 var protected by remember{mutableStateOf(false)};var loading by remember{mutableStateOf(true)}
 val scope=rememberCoroutineScope();val focus=LocalFocusManager.current;val keyboard=LocalSoftwareKeyboardController.current
 suspend fun prepare(){loading=true;receipt=null;try{plan=repository.prepare(kind,id);error=null;protected=false}catch(e:CancellationException){throw e}catch(e:Exception){plan=null;protected=e is ProtectedDeletionException;error=e.message ?: "Anteprima non disponibile"}finally{loading=false}}
 LaunchedEffect(kind,id){prepare()}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
  if(uri!=null && plan!=null)scope.launch{busy=true;receipt=null;try{receipt=repository.saveVerifiedBackup(plan!!,uri);error=null}catch(e:CancellationException){throw e}catch(e:Exception){error="Backup non verificato: ${e.message}"}finally{busy=false}}
 }
 Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp).testTag("deletion-screen"),verticalArrangement=Arrangement.spacedBy(14.dp)) {
  TextButton(onClick=back,enabled=!busy){Text("Indietro")};ConceptHeading("Elimina definitivamente","Controlla le voci e conserva prima un backup")
  if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
  error?.let{Text(it,color=MaterialTheme.colorScheme.error,modifier=Modifier.testTag("deletion-error"))}
  if(protected && kind=="property") {
   Text("L’archiviazione toglie l’immobile dalle viste operative e conserva prenotazioni, spese e documenti.")
   Button(enabled=!busy,onClick={busy=true;scope.launch{try{repository.archive(id);withContext(Dispatchers.Main){done()}}catch(e:Exception){error=e.message}finally{busy=false}}},modifier=Modifier.testTag("archive-protected-property")){Text("Archivia immobile")}
  }
  plan?.let{p->
   Text(p.label,style=MaterialTheme.typography.titleLarge)
   p.counts.forEach{(label,count)->Text("$label: $count")}
   if(p.reportIds.isNotEmpty()) {
    Text("Sono coinvolti documenti non confermati. Le versioni confermate sono protette e non possono essere eliminate.")
    Row{Checkbox(docs,{docs=it},enabled=!busy,modifier=Modifier.testTag("deletion-documents"));Text("Elimina anche i documenti in bozza indicati sopra",Modifier.weight(1f).padding(top=10.dp))}
   }
   Text("PDF e backup già esportati restano nei documenti scelti. Le bozze locali dei record eliminati saranno rimosse.",style=MaterialTheme.typography.bodySmall)
   OutlinedButton(onClick={focus.clearFocus();keyboard?.hide();picker.launch("Elledue-backup-prima-eliminazione.json")},enabled=!busy,modifier=Modifier.testTag("deletion-backup")){Text("1 · Salva e verifica backup preventivo")}
   Text(if(receipt==null)"Manca il backup preventivo verificato: completa prima il punto 1." else "Backup salvato, riletto e verificato.",color=if(receipt==null)ElledueGold else ArrivalColor,modifier=Modifier.testTag("deletion-backup-state"))
   OutlinedTextField(text,{text=it},label={Text("Digita ELIMINA")},singleLine=true,enabled=!busy,modifier=Modifier.fillMaxWidth().testTag("deletion-confirmation"))
   val correct=text.trim().equals("ELIMINA",ignoreCase=true)
   if(!correct)Text("Per attivare il comando scrivi solo ELIMINA, senza il nome.",modifier=Modifier.testTag("deletion-word-state"))
   if(p.reportIds.isNotEmpty() && !docs)Text("Conferma anche l’eliminazione dei documenti in bozza elencati.")
   Button(onClick={focus.clearFocus();keyboard?.hide();busy=true;scope.launch{var committed=false;try{
    repository.delete(p,requireNotNull(receipt),text,docs);committed=true;withContext(Dispatchers.Main){done()}
   }catch(e:CancellationException){throw e}catch(e:Exception){error=if(committed)"Eliminazione completata. Torna all’elenco immobili." else e.message ?: "Dati conservati: eliminazione non riuscita.";if(!committed)receipt=null}finally{busy=false}}},enabled=!busy && receipt!=null && correct && (p.reportIds.isEmpty() || docs),colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.error),modifier=Modifier.testTag("delete-permanently")){Text("2 · Elimina definitivamente")}
  }
  if(!loading)TextButton(onClick={scope.launch{prepare()}},enabled=!busy){Text("Aggiorna anteprima e ripeti il backup")}
  if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
 }
}
