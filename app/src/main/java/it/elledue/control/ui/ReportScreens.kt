package it.elledue.control.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.elledue.control.R
import it.elledue.control.data.*
import it.elledue.control.domain.*
import it.elledue.control.reports.*
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsHome(reports: List<ReportEntity>, properties: List<PropertyWithOwner>, model: ReportViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) {
    var property by rememberSaveable { mutableStateOf(properties.firstOrNull()?.property?.id ?: "") }
    var month by rememberSaveable { mutableStateOf(YearMonth.from(BookingDates.today()).toString()) }
    val busy by model.busy.collectAsStateWithLifecycle(); val error by model.error.collectAsStateWithLifecycle(); val saved by model.savedId.collectAsStateWithLifecycle(); val unassigned by model.unassigned.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { model.loadUnassigned() }
    LaunchedEffect(saved) { saved?.let { model.clear(); onOpen(it) } }
    Scaffold(topBar = { TopAppBar(title = { ConceptHeading("Rendiconti", "Report professionali per i proprietari") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Indietro rendiconti") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Row(Modifier.fillMaxWidth().background(ConceptCard,MaterialTheme.shapes.medium).padding(14.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.PeopleOutline,null,tint=ElledueGold,modifier=Modifier.size(28.dp))
                Column(Modifier.weight(1f)) { Text("Documenti per i proprietari",style=MaterialTheme.typography.titleMedium,color=ElledueGold);Text("Verifica la bozza, conferma e scegli il layout del PDF.",style=MaterialTheme.typography.bodySmall,color=ConceptMuted) }
            } }
            item { ConceptPropertySelector(properties, property, enabled = !busy) { property = it } }
            item { Row {
                IconButton(onClick = { month = YearMonth.parse(month).minusMonths(1).toString() }) { Icon(Icons.Outlined.ChevronLeft, "Mese precedente rendiconto") }
                Text(month, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).padding(top = 8.dp).testTag("report-period"))
                IconButton(onClick = { month = YearMonth.parse(month).plusMonths(1).toString() }) { Icon(Icons.Outlined.ChevronRight, "Mese successivo rendiconto") }
            } }
            item { Button(onClick = { model.generate(property, month) }, enabled = !busy && property.isNotBlank(), modifier = Modifier.fillMaxWidth().testTag("generate-report")) { Text("Genera / apri bozza") } }
            val missing = unassigned.filter { it.propertyId == property && it.competenceMonth == month }
            if (missing.isNotEmpty()) item { Text("${missing.size} prenotazioni non ancora attribuite nel periodo. La bozza include automaticamente tutte le prenotazioni registrate con check-in nel mese.", style = MaterialTheme.typography.bodyMedium) }
            item { Text("Documenti del periodo", style = MaterialTheme.typography.titleMedium) }
            val selected = reports.filter { it.propertyId == property && it.yearMonth == month }
            if (selected.isEmpty()) item { Text("Nessun documento. La generazione crea una bozza, mai un invio automatico.") }
            items(selected, key = { it.id }) { report ->
                OutlinedCard(onClick = { onOpen(report.id) }, modifier = Modifier.fillMaxWidth().testTag("report-${report.id}")) { Column(Modifier.padding(16.dp)) {
                    Text("${report.yearMonth} · versione ${report.version}", style = MaterialTheme.typography.titleMedium)
                    Text(if (report.status == "CONFIRMED") "Confermato · snapshot conservato" else "Bozza da verificare")
                    if (report.previousId != null) Text("Rettificativo · ${report.revisionReason}", style = MaterialTheme.typography.bodySmall)
                } }
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetail(report: ReportEntity, snapshot: ReportSnapshot, model: ReportViewModel, onBack: () -> Unit,
                 onBooking: (String) -> Unit, onExpense: (String) -> Unit, onTax: (String) -> Unit, onEconomy: () -> Unit, onStandard:(String)->Unit = {}) {
    var layout by rememberSaveable { mutableStateOf(ReportLayout.SUMMARY) }
    val exportPdf = rememberPdfExporter(snapshot, layout)
    var preview by remember { mutableStateOf(false) }
    var tab by rememberSaveable(report.id) { mutableIntStateOf(0) }
    var notes by rememberPersistentDraft("report/${report.id}#notes",report.notes)
    var acknowledged by rememberSaveable(report.id, report.snapshotJson) { mutableStateOf(false) }
    var confirm by rememberSaveable { mutableStateOf(false) }; var revise by rememberSaveable { mutableStateOf(false) }; var reason by rememberPersistentDraft("report/${report.id}#reason", "")
    val busy by model.busy.collectAsStateWithLifecycle(); val error by model.error.collectAsStateWithLifecycle()
    val total = snapshot.summary(); val issues = snapshot.issues(); val proposed=snapshot.proposedExpenses.orEmpty(); val draft = report.status == "DRAFT"
    Scaffold(topBar = { TopAppBar(title = { Text("${report.yearMonth} · v${report.version}") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Indietro documento") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("report-detail-scroll").semantics { stateDescription = report.status }, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Column(Modifier.fillMaxWidth().background(ConceptCard,MaterialTheme.shapes.medium).padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.elledue_logo_gold),"Logo ufficiale Elledue",modifier=Modifier.width(112.dp).height(20.dp))
                        StatusBadge(if(draft) "BOZZA" else "CONFERMATO",if(draft) ElledueGold else ArrivalColor)
                    }
                    Text(snapshot.property.name,style=MaterialTheme.typography.titleLarge)
                    Text("${snapshot.owner.name} · ${report.yearMonth} · v${report.version}",style=MaterialTheme.typography.bodySmall,color=ConceptMuted)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                        Text(if(total.balance>=0) "Bonifico ancora dovuto" else "Credito da riconciliare",style=MaterialTheme.typography.labelMedium,modifier=Modifier.weight(1f))
                        Text(if(total.balance<0) Amounts.euroAbsolute(total.balance) else Amounts.euro(total.balance),color=ElledueGold,style=MaterialTheme.typography.titleLarge,modifier=Modifier.testTag("report-balance"))
                    }
                }
            }
            if(draft && proposed.isNotEmpty())item {OutlinedCard(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text("Costi standard proposti · ${it.elledue.control.domain.Amounts.euro(it.elledue.control.domain.OperationalMoney.total(proposed.map{it.actualCents}))}",style=MaterialTheme.typography.titleMedium)
                Text("Già inclusi nella bozza. Alla conferma puoi approvarli tutti insieme; nessun pagamento è registrato automaticamente.",style=MaterialTheme.typography.bodySmall)
                proposed.forEach{e->TextButton(onClick={e.reservationId?.let(onStandard)}){Text("${e.category} · ${e.quantity} × ${e.unitCents?.let(it.elledue.control.domain.Amounts::euro)} · ${it.elledue.control.domain.Amounts.euro(e.actualCents)}")}}
            }}}
            if(snapshot.relatedExpenses.orEmpty().isNotEmpty())item{Text("Costi collegati in altro periodo · esclusi dai totali correnti",style=MaterialTheme.typography.titleMedium);snapshot.relatedExpenses.orEmpty().forEach{e->TextButton(onClick={onExpense(e.id)}){Text("${e.category} · ${e.competenceMonth} · ${it.elledue.control.domain.Amounts.euro(e.actualCents)}")}}}
            item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConceptPill(layout == ReportLayout.SUMMARY, { layout = ReportLayout.SUMMARY }, label = { Text("Sintetico") }, modifier = Modifier.testTag("pdf-summary"))
                ConceptPill(layout == ReportLayout.DETAILED, { layout = ReportLayout.DETAILED }, label = { Text("Dettagliato") }, modifier = Modifier.testTag("pdf-detailed"))
            } }
            item { PdfThumbnail(snapshot, layout) { preview = true } }
            item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick=exportPdf,shape=MaterialTheme.shapes.small,modifier=Modifier.weight(1f).testTag("export-current-layout")) { Icon(Icons.Outlined.PictureAsPdf,null,modifier=Modifier.size(16.dp));Spacer(Modifier.width(6.dp));Text(if(draft) "PDF della bozza" else "Scarica PDF") }
                OutlinedButton(onClick={preview=true},shape=MaterialTheme.shapes.small,modifier=Modifier.weight(1f)) { Text("Apri anteprima") }
            } }
            item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Riepilogo", "Dettagli", "Verifica").forEachIndexed { i, label -> ConceptPill(tab == i, { tab = i }, label = { Text(label) }, modifier = Modifier.testTag("report-tab-$i")) } } }
            if (tab == 0) {
                item { Column { AmountLine("Lordo (tassa esclusa)", snapshot.gross()); AmountLine("OTA", snapshot.ota()); AmountLine("Netto al proprietario", snapshot.net()); HorizontalDivider(); AmountLine("Compenso di gestione Elledue", total.fees); listOf("CLEANING" to "Pulizie rimborsabili","LAUNDRY" to "Lavanderia rimborsabile","OTHER" to "Altre spese rimborsabili").forEach{(kind,label)->AmountLine(label,it.elledue.control.domain.OperationalMoney.total(snapshot.expenses.filter{it.kind==kind && it.advancedByElledue && it.reimbursable && !it.voided}.map{it.actualCents}))}; AmountLine("Totale rimborsi dovuti", total.reimbursable); AmountLine("Compensazioni riconosciute", total.compensated); AmountLine("Altri anticipi / pagamenti", total.payments) } }
                item { Text("Le prenotazioni appartengono interamente al mese del check-in. La tassa è esterna al lordo; soltanto la compensazione riconosciuta riduce il saldo.", style = MaterialTheme.typography.bodySmall) }
            } else if (tab == 1) {
                item { Text("Prenotazioni · ${snapshot.bookings.count { it.status != "CANCELLED" }}", style = MaterialTheme.typography.titleMedium) }
                items(snapshot.bookings, key = { "booking-${it.id}" }) { b ->
                    ReportBookingControls(snapshot,b,model)
                    ListItem(headlineContent = { Text("${b.guestFirstName} ${b.guestLastName}") }, supportingContent = { Text("${BookingDates.display(b.checkInEpochDay)} → ${BookingDates.display(b.checkOutEpochDay)} · ${b.guestCount} ospiti\nLordo ${Amounts.euro(b.grossCents)} · OTA ${Amounts.euro(b.otaCents)}\nElledue ${Amounts.euro(b.managementCents)}${if (b.status == "CANCELLED") " · annullata, esclusa dai compensi" else ""}") }, trailingContent = { if (draft) IconButton(onClick = { onBooking(b.id) }) { Icon(Icons.Outlined.Edit, "Modifica prenotazione") } })
                    if (draft) TextButton(onClick = { onTax(b.id) }) { Text("Verifica tassa e movimenti") }
                }
                if(snapshot.excludedBookings.orEmpty().isNotEmpty())item{Text("Prenotazioni escluse dai calcoli",style=MaterialTheme.typography.titleMedium)}
                items(snapshot.excludedBookings.orEmpty(),key={"excluded-${it.id}"}){b->Text("${b.guestFirstName} ${b.guestLastName} · esclusa dal rendiconto, conservata nel calendario");ReportBookingControls(snapshot,b,model)}
                if (snapshot.linkedBookings.orEmpty().isNotEmpty()) item {
                    Text("Soggiorni collegati di altri periodi", style = MaterialTheme.typography.titleMedium)
                    snapshot.linkedBookings.orEmpty().forEach { b -> ReportBookingControls(snapshot,b,model);Text("${b.guestFirstName} ${b.guestLastName} · ${BookingDates.display(b.checkInEpochDay)} → ${BookingDates.display(b.checkOutEpochDay)} · riferimento, senza compenso in questo periodo", style = MaterialTheme.typography.bodySmall) }
                }
                item { Text("Spese · ${snapshot.expenses.size}", style = MaterialTheme.typography.titleMedium) }
                items(snapshot.expenses, key = { "expense-${it.id}" }) { e -> ListItem(headlineContent = { Text(e.description) }, supportingContent = { Text("${e.category} · ${BookingDates.display(e.dateEpochDay)}\n${e.quantity ?: ""} ${e.unitCents?.let { "× ${Amounts.euro(it)}" } ?: ""} · ${Amounts.euro(e.actualCents)}\n${if (e.advancedByElledue && e.reimbursable) "Rimborsabile" else "Esclusa dai rimborsi"}") }, trailingContent = { if (draft) IconButton(onClick = { onExpense(e.id) }) { Icon(Icons.Outlined.Edit, "Rettifica spesa") } }) }
                item { Text("Tassa e movimenti", style = MaterialTheme.typography.titleMedium); Column { AmountLine("Tassa prevista", total.expectedTax); AmountLine("Riscossa", total.collectedTax); AmountLine("Residua", total.residualTax) } }
                items(Ledger.active(snapshot.movements).filter { it.competenceMonth == snapshot.month && it.kind in listOf("COMPENSATION", "OWNER_PAYMENT", "REMITTANCE") }, key = { "movement-${it.id}" }) { m -> Text("${movementLabel(m.kind)} · ${BookingDates.display(m.dateEpochDay)} · ${Amounts.euro(m.amountCents)}\n${m.notes}", style = MaterialTheme.typography.bodyMedium) }
                if (draft) item { TextButton(onClick = onEconomy) { Text("Apri contabilità per correggere i movimenti") } }
            } else {
                item { Text("Controllo prima della conferma", style = MaterialTheme.typography.titleMedium); Text("La bozza raccoglie tutte le voci registrate pertinenti. Verifica eventuali prenotazioni non ancora inserite e confronta le OTA con il portale.") }
                items(issues) { issue -> Text(issue.message, color = if (issue.blocking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
                if (issues.isEmpty()) item { Text("Nessuna criticità rilevata nei dati registrati.") }
                if (draft && (issues.any { !it.blocking } || proposed.isNotEmpty())) item { Row { Checkbox(acknowledged, { acknowledged = it }, modifier = Modifier.testTag("ack-report-warnings")); Text(if(proposed.isNotEmpty())"Ho verificato gli avvisi e approvo in blocco i costi standard proposti nel saldo. Non attesto pagamenti." else "Ho verificato gli avvisi e accetto i dati indicati.", modifier = Modifier.weight(1f).padding(top = 10.dp)) } }
                item { AccountingField("Note del rendiconto", notes, "report-notes", draft && !busy) { notes = it } }
            }
            if (draft) {
                item { OutlinedButton(onClick = exportPdf, modifier = Modifier.fillMaxWidth()) { Text("Esporta PDF della bozza") } }
                item { Button(onClick = { model.refresh(report.id, notes) }, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("refresh-report")) { Text("Salva note · dati aggiornati automaticamente") } }
                item { OutlinedButton(onClick = { tab = 2; confirm = true }, enabled = !busy && notes.trim() == report.notes && issues.none { it.blocking } && ((issues.isEmpty() && proposed.isEmpty()) || acknowledged), modifier = Modifier.fillMaxWidth().testTag("confirm-report")) { Text("Conferma rendiconto") } }
            } else {
                item { Button(onClick = exportPdf, modifier = Modifier.fillMaxWidth().testTag("export-report-pdf")) { Text("Esporta / visualizza PDF confermato") } }
                item { OutlinedButton(onClick = { revise = true }, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("revise-report")) { Text("Crea versione rettificativa") } }
                if (revise) item { AccountingField("Motivo della rettifica", reason, "report-revision-reason", !busy) { reason = it }; TextButton(onClick = { model.revision(report.id, reason) }, enabled = !busy) { Text("Apri nuova bozza rettificativa") } }
                item { Text("Questa versione resta immutabile. Le versioni dello stesso periodo sostituiscono il documento corrente senza duplicare gli importi.", style = MaterialTheme.typography.bodySmall) }
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        }
    }
    if (preview) PdfPreview(snapshot, layout, { preview = false }, exportPdf) { layout = it }
    if (confirm) AlertDialog(onDismissRequest = { if (!busy) confirm = false }, title = { Text("Confermare questa versione?") }, text = { Text("Verrà conservato uno snapshot immutabile. Il PDF non sarà inviato automaticamente.") }, confirmButton = { TextButton(enabled = !busy, onClick = { model.confirm(snapshot, acknowledged); confirm = false }) { Text("Conferma definitiva") } }, dismissButton = { TextButton(enabled = !busy, onClick = { confirm = false }) { Text("Torna alla verifica") } })
}
