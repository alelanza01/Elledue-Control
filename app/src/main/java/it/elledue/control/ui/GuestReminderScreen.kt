package it.elledue.control.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import it.elledue.control.data.*
import it.elledue.control.domain.*
import it.elledue.control.notifications.ReminderStore

@Composable fun GuestReminderScreen(b:ReservationEntity,kind:String,property:PropertyWithOwner?,repository:MessageRepository,accounting:AccountingData?,store:ReminderStore,back:()->Unit) {
 val all by repository.messages.collectAsState(initial=emptyList())
 var language by rememberSaveable{mutableStateOf("IT")}
 val templates=all.filter{!it.archived && it.categoryId==kind && (it.propertyId==null || it.propertyId==b.propertyId)}.sortedBy{it.propertyId==null}
 var selected by rememberSaveable{mutableStateOf("")}
 val template=templates.firstOrNull{it.id==selected} ?: templates.firstOrNull()
 val collected=Ledger.total(accounting?.movements.orEmpty().filter{it.reservationId==b.id},"COLLECTION")
 val due=accounting?.taxes.orEmpty().firstOrNull{it.reservationId==b.id}?.expectedCents ?: b.expectedTaxCents
 val residual=due?.let{Ledger.residual(it,collected)}
 val raw=template?.let{if(language=="IT")it.italian else it.english} ?: ""
 val resolved=GuestText.resolve(raw,b,property?.property?.name,residual,language=="EN")
 val key="messages?booking=${b.id}&reminder=$kind&language=$language&template=${template?.id ?: "none"}"
 var text by rememberPersistentDraft("$key#text",resolved.text)
 val context=LocalContext.current;var feedback by remember{mutableStateOf("")}
 val reminderKey=(if(kind=="out")"departure:" else "arrival:")+b.id
 var handled by remember{mutableStateOf(store.handled(reminderKey))}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).testTag("guest-reminder-message"),verticalArrangement=Arrangement.spacedBy(12.dp)){
  TextButton(onClick=back){Text("Indietro")};ConceptHeading(if(kind=="out")"Messaggio check-out" else "Messaggio pre-arrivo","${b.guestFirstName} ${b.guestLastName} · ${property?.property?.name ?: ""}")
  Text("Check-out ${BookingDates.display(b.checkOutEpochDay)} · ${b.expectedDepartureTime}")
  if(residual!=null && residual>0)Text("Tassa ancora da riscuotere: ${Amounts.euro(residual)}",color=ElledueGold)
  else if(residual==0L)Text("Tassa interamente riscossa · nessun sollecito")
  ChoiceField("Lingua",language,listOf("IT" to "Italiano","EN" to "English")){language=it}
  if(templates.isNotEmpty())ChoiceField("Modello",template?.id ?: "",templates.map{it.id to it.title}){selected=it}
  else Text("Nessun modello pertinente configurato: scrivi il testo da copiare. Non vengono inventate istruzioni di accesso.")
  OutlinedTextField(text,{text=it},label={Text("Messaggio personalizzabile")},minLines=6,modifier=Modifier.fillMaxWidth().testTag("reminder-message-text"))
  if(resolved.missing.isNotEmpty())Text("Verifica i valori mancanti: ${resolved.missing.joinToString()}",color=ElledueGold)
  Button(onClick={feedback=if(copyGuestText(context,text))"Messaggio copiato · invio non confermato" else "Copia non riuscita"},enabled=text.isNotBlank()){Text("Copia messaggio")}
  OutlinedButton(onClick={context.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT,text),"Condividi messaggio"))},enabled=text.isNotBlank()){Text("Condividi…")}
  OutlinedButton(onClick={store.handle(reminderKey);handled=true}){Text(if(handled)"Gestito manualmente ✓" else "Segna come gestito")}
  Text("Copia e condivisione non confermano l’invio. Lo stato Gestito è sempre manuale.",style=MaterialTheme.typography.bodySmall)
  if(feedback.isNotBlank())Text(feedback)
 }
}
