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
import it.elledue.control.ElledueApplication
import it.elledue.control.data.*
import it.elledue.control.domain.*
import kotlinx.coroutines.launch
import java.util.UUID

@Composable fun QuickTaxCollection(b:ReservationEntity,tax:TaxAssessmentEntity?,collected:Long,open:(String)->Unit) {
 var show by remember{mutableStateOf(false)};var feedback by remember{mutableStateOf("")}
 val residual=tax?.let{Ledger.residual(it.expectedCents,collected)} ?: 0L
 Button(onClick={show=true},enabled=tax!=null && !tax.needsReview && residual>0 && b.status!="CANCELLED",modifier=Modifier.fillMaxWidth().testTag("quick-collect-tax")){Text("Conferma tassa incassata")}
 if(tax==null || tax.needsReview)TextButton(onClick={open("tax/${b.id}")}){Text("Verifica i dati della tassa")}
 if(feedback.isNotBlank())Text(feedback,color=ArrivalColor)
 if(show) {
  val key="booking/${b.id}#quick-tax"
  var amount by rememberPersistentDraft("$key-amount",Amounts.editable(residual))
  var actor by rememberPersistentDraft("$key-collector","ELLEDUE")
  var compensate by rememberPersistentDraft("$key-compensate",b.appliedBusinessType=="MANAGED")
  var id by rememberPersistentDraft("$key-id",UUID.randomUUID().toString())
  var actorMenu by remember{mutableStateOf(false)}
  var error by remember{mutableStateOf<String?>(null)};var busy by remember{mutableStateOf(false)}
  val app=LocalContext.current.applicationContext as ElledueApplication;val scope=rememberCoroutineScope()
  OutlinedCard(Modifier.fillMaxWidth().testTag("quick-tax-panel")) {
   Surface(shape=MaterialTheme.shapes.large) {Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   Text("Conferma riscossione reale",style=MaterialTheme.typography.titleLarge)
   Text("Residuo ${Amounts.euro(residual)} · puoi indicare una riscossione parziale.")
   AccountingField("Importo ricevuto €",amount,"quick-tax-amount",!busy){amount=it}
   val collectors=listOf("ELLEDUE" to "Elledue","OWNER" to "Proprietario","PORTAL" to "Portale","OTHER" to "Altro")
   Box {
    OutlinedButton(onClick={actorMenu=true},enabled=!busy){Text("Incassato da: "+collectors.first{it.first==actor}.second)}
    DropdownMenu(expanded=actorMenu,onDismissRequest={actorMenu=false}){collectors.forEach{(value,label)->DropdownMenuItem(text={Text(label)},onClick={actor=value;actorMenu=false})}}
   }
   if(actor=="ELLEDUE" && b.appliedBusinessType=="MANAGED")Row{Checkbox(compensate,{compensate=it});Text("Riconosci come anticipo nel saldo del proprietario",Modifier.weight(1f).padding(top=10.dp))}
   else Text("Nessuna compensazione con il proprietario.",style=MaterialTheme.typography.bodySmall)
   Text("Conferma solo denaro realmente ricevuto. Il riversamento resta separato.",style=MaterialTheme.typography.bodySmall)
   error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
  TextButton(onClick={show=false},enabled=!busy){Text("Annulla")}
  TextButton(enabled=!busy,onClick={busy=true;scope.launch{try{
   check(app.drafts.save("$key-id",id)){"Impossibile salvare la richiesta: nessun movimento registrato."}
   app.accountingRepository.collectTax(b.id,Amounts.parseCents(amount),actor,compensate && actor=="ELLEDUE" && b.appliedBusinessType=="MANAGED",id)
   listOf("amount","collector","compensate","id").forEach{app.drafts.clear("$key-$it")};feedback="Riscossione registrata";show=false
  }catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){error=e.message ?: "Operazione non riuscita; nessun movimento parziale applicato."}finally{busy=false}}},modifier=Modifier.testTag("confirm-quick-tax")){Text("Conferma incasso")}
  }
  }}
  }
 }
}
