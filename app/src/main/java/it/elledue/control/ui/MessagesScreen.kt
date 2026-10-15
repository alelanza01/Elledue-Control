package it.elledue.control.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import it.elledue.control.data.*
import it.elledue.control.domain.GuestText
import it.elledue.control.domain.Ledger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.UUID

@Composable fun MessagesScreen(repository:MessageRepository, properties:List<PropertyWithOwner>,bookings:List<ReservationEntity>,bookingId:String?,accounting:AccountingData?,back:()->Unit) {
    val saved by repository.messages.collectAsState(initial=emptyList());val categories by repository.categories.collectAsState(initial=emptyList())
    val scope=rememberCoroutineScope();val context=LocalContext.current
    var error by remember {mutableStateOf<String?>(null)};var busy by remember {mutableStateOf(false)}
    LaunchedEffect(Unit){try{repository.initialize()}catch(e:Exception){error="Categorie non disponibili"}}
    val legacy=properties.flatMap {p->p.templates.groupBy {it.kind}.map { (kind,rows)->GuestMessageEntity("legacy-${p.property.id}-$kind",p.property.id,"other",kind,rows.firstOrNull {it.language.equals("it",true)}?.body ?: "",rows.firstOrNull {it.language.equals("en",true)}?.body ?: "") }}.filter {it.italian.isNotBlank() || it.english.isNotBlank()}
    val all=saved+legacy.filter {l->saved.none {it.id==l.id}}
    var selectedBooking by rememberSaveable {mutableStateOf(bookingId ?: "")};val booking=bookings.firstOrNull {it.id==selectedBooking}
    var filter by rememberSaveable {mutableStateOf(booking?.propertyId ?: "")};var archived by rememberSaveable {mutableStateOf(false)}
    var editId by rememberSaveable {mutableStateOf<String?>(null)};var previewId by rememberSaveable {mutableStateOf<String?>(null)};var language by rememberSaveable {mutableStateOf("IT")}
    var deleteId by remember {mutableStateOf<String?>(null)};var categoryEdit by rememberSaveable {mutableStateOf(false)}
    fun action(block:suspend ()->Unit){if(busy)return;busy=true;error=null;scope.launch{try{block()}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message ?: "Operazione non riuscita"}finally{busy=false}}}
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp).testTag("messages-screen"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        TextButton(onClick={if(editId!=null)editId=null else if(previewId!=null)previewId=null else back()}){Text("Indietro")}
        ConceptHeading("Messaggi ospiti","Italiano / English · copia manuale")
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        if(editId!=null) {
            val item=all.firstOrNull {it.id==editId}
            var title by rememberSaveable(editId){mutableStateOf(item?.title ?: "")};var italian by rememberSaveable(editId){mutableStateOf(item?.italian ?: "")};var english by rememberSaveable(editId){mutableStateOf(item?.english ?: "")}
            var property by rememberSaveable(editId){mutableStateOf(item?.propertyId ?: filter)};var category by rememberSaveable(editId){mutableStateOf(item?.categoryId ?: "pre")}
            OutlinedTextField(title,{title=it},label={Text("Titolo *")},modifier=Modifier.fillMaxWidth().testTag("message-title"))
            ConceptPropertySelector(properties,property,allOption=true){property=it};Text("Tutti gli immobili = modello globale",style=MaterialTheme.typography.bodySmall)
            var dropdown by remember {mutableStateOf(false)}
            OutlinedButton(onClick={dropdown=true}){Text(categories.firstOrNull {it.id==category}?.label ?: "Categoria")}
            DropdownMenu(dropdown,{dropdown=false}){categories.forEach {c->DropdownMenuItem(text={Text(c.label)},onClick={category=c.id;dropdown=false})}}
            OutlinedTextField(italian,{italian=it},label={Text("Testo italiano")},minLines=4,modifier=Modifier.fillMaxWidth().testTag("message-italian"))
            OutlinedTextField(english,{english=it},label={Text("English text")},minLines=4,modifier=Modifier.fillMaxWidth().testTag("message-english"))
            Text("Variabili: {nome_ospite}, {immobile}, {check_in}, {check_out}, {ora_check_in}, {ora_check_out}, {tassa_residua}. Non inserire codici o dettagli non verificati.",style=MaterialTheme.typography.bodySmall)
            Button(onClick={val entry=GuestMessageEntity(editId!!,property.ifBlank{null},category,title,italian,english,item?.archived ?: false);action{repository.save(entry);editId=null}},enabled=!busy,modifier=Modifier.testTag("save-message")){Text("Salva modello")}
        } else if(previewId!=null) {
            val item=all.firstOrNull {it.id==previewId}
            if(item!=null) {
                Text(item.title,style=MaterialTheme.typography.titleLarge)
                var dropdown by remember {mutableStateOf(false)}
                OutlinedButton(onClick={dropdown=true}){Text(booking?.let {"${it.guestFirstName} ${it.guestLastName}"} ?: "Seleziona prenotazione (facoltativa)")}
                DropdownMenu(dropdown,{dropdown=false}) {
                    DropdownMenuItem(text={Text("Senza prenotazione")},onClick={selectedBooking="";dropdown=false})
                    bookings.filter {item.propertyId==null || it.propertyId==item.propertyId}.forEach {b->DropdownMenuItem(text={Text("${b.guestFirstName} ${b.guestLastName}")},onClick={selectedBooking=b.id;dropdown=false})}
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("IT","EN").forEach{l->ConceptPill(language==l,{language=l},label={Text(if(l=="IT")"Italiano" else "English")})}}
                val assessment=accounting?.taxes?.firstOrNull {it.reservationId==booking?.id}
                val collected=accounting?.let {a->Ledger.active(a.movements).filter {it.kind=="COLLECTION" && it.reservationId==booking?.id}.fold(0L){v,m->Math.addExact(v,m.amountCents)}}
                val residual=assessment?.let {(it.expectedCents-(collected ?: 0)).coerceAtLeast(0)}
                val propertyName=properties.firstOrNull {it.property.id==(booking?.propertyId ?: item.propertyId)}?.property?.name
                val body=if(language=="IT")item.italian else item.english
                val resolved=GuestText.resolve(body,booking,propertyName,residual,language=="EN")
                Text(resolved.text.ifBlank {"Testo non disponibile in questa lingua."},modifier=Modifier.testTag("message-preview"))
                if(resolved.missing.isNotEmpty()) Text("Valori mancanti: ${resolved.missing.joinToString()}. Seleziona una prenotazione o completa i dati.",color=ElledueGold)
                var feedback by remember(previewId,language,selectedBooking){mutableStateOf("")}
                Button(onClick={feedback=if(copyGuestText(context,resolved.text))"Messaggio copiato" else "Copia non riuscita"},enabled=body.isNotBlank() && resolved.missing.isEmpty(),modifier=Modifier.testTag("copy-message")){Text("Copia messaggio")}
                if(feedback.isNotBlank())Text(feedback)
                Text("Nessun invio automatico. Verifica il testo prima di incollarlo.",style=MaterialTheme.typography.bodySmall)
            }
        } else {
            ConceptPropertySelector(properties,filter,allOption=true){filter=it}
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick={editId=UUID.randomUUID().toString()},modifier=Modifier.testTag("new-message")){Text("Nuovo modello")}
                OutlinedButton(onClick={categoryEdit=!categoryEdit}){Text("Categorie")}
                ConceptPill(archived,{archived=!archived},label={Text("Archiviati")})
            }
            if(categoryEdit) {
                var catId by rememberSaveable {mutableStateOf(UUID.randomUUID().toString())};var catLabel by rememberSaveable {mutableStateOf("")}
                OutlinedTextField(catLabel,{catLabel=it},label={Text("Nome categoria")},modifier=Modifier.fillMaxWidth())
                Button(onClick={action{repository.category(MessageCategoryEntity(catId,catLabel));catLabel="";catId=UUID.randomUUID().toString()}},enabled=!busy && catLabel.isNotBlank()){Text("Salva categoria")}
                categories.forEach {c->Row(Modifier.fillMaxWidth()) {TextButton(onClick={catId=c.id;catLabel=c.label},modifier=Modifier.weight(1f)){Text(c.label+" · modifica")};TextButton(onClick={deleteId="category:${c.id}"}){Text("Elimina")}} }
            }
            val shown=all.filter {it.archived==archived && (filter.isBlank() || it.propertyId==null || it.propertyId==filter)}
            if(shown.isEmpty())EmptyState("Le parole giuste, pronte da copiare","Crea il primo modello personalizzato in italiano o inglese.")
            shown.forEach {m->Card(Modifier.fillMaxWidth().testTag("message-${m.id}"),colors=CardDefaults.cardColors(containerColor=ConceptCard),border=BorderStroke(1.dp,ConceptBorder)){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(m.title,style=MaterialTheme.typography.titleLarge)
                Text((properties.firstOrNull{it.property.id==m.propertyId}?.property?.name ?: "Globale")+" · "+(categories.firstOrNull{it.id==m.categoryId}?.label ?: m.categoryId),style=MaterialTheme.typography.bodySmall)
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    TextButton(onClick={previewId=m.id;selectedBooking=bookingId?.takeIf {id->bookings.any{it.id==id&&(m.propertyId==null||it.propertyId==m.propertyId)}} ?: ""}){Text("Anteprima")}
                    TextButton(onClick={editId=m.id}){Text("Modifica")}
                    TextButton(onClick={action{repository.save(m.copy(id=UUID.randomUUID().toString(),title=m.title+" (copia)",archived=false))}}){Text("Duplica")}
                    TextButton(onClick={action{repository.save(m.copy(archived=!m.archived))}}){Text(if(m.archived)"Ripristina" else "Archivia")}
                    TextButton(onClick={deleteId=m.id}){Text("Elimina")}
                }
            }}}
        }
    }
    if(deleteId!=null) AlertDialog(onDismissRequest={deleteId=null},title={Text("Confermare eliminazione?")},text={Text("Il modello o la categoria saranno eliminati. I dati delle prenotazioni restano conservati.")},confirmButton={TextButton(onClick={val id=deleteId!!;action{if(id.startsWith("category:"))repository.deleteCategory(id.removePrefix("category:")) else {repository.delete(all.first{it.id==id})};deleteId=null}},enabled=!busy){Text("Conferma eliminazione")}},dismissButton={TextButton(onClick={deleteId=null}){Text("Annulla")}})
}
