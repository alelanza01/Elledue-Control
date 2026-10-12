package it.elledue.control.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
    Scaffold(topBar = { TopAppBar(title = { Text("Rendiconti") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Indietro rendiconti") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("Il mese, pronto da verificare", style = MaterialTheme.typography.headlineMedium) }
            item { PropertySelector(properties, property, enabled = !busy) { property = it } }
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
                 onBooking: (String) -> Unit, onExpense: (String) -> Unit, onTax: (String) -> Unit, onEconomy: () -> Unit, onExport: () -> Unit) {
    var tab by rememberSaveable(report.id) { mutableIntStateOf(0) }
    var notes by rememberSaveable(report.id) { mutableStateOf(report.notes) }
    var acknowledged by rememberSaveable(report.id, report.snapshotJson) { mutableStateOf(false) }
    var confirm by rememberSaveable { mutableStateOf(false) }; var revise by rememberSaveable { mutableStateOf(false) }; var reason by rememberSaveable { mutableStateOf("") }
    val busy by model.busy.collectAsStateWithLifecycle(); val error by model.error.collectAsStateWithLifecycle()
    val total = snapshot.summary(); val issues = snapshot.issues(); val draft = report.status == "DRAFT"
    Scaffold(topBar = { TopAppBar(title = { Text("${report.yearMonth} · v${report.version}") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Indietro documento") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("report-detail-scroll"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Column(Modifier.fillMaxWidth().background(ElledueNight, MaterialTheme.shapes.large).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(painterResource(R.drawable.elledue_logo_gold), "Logo ufficiale Elledue", modifier = Modifier.width(155.dp).height(28.dp))
                    Text(snapshot.property.name, color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.titleLarge)
                    Text("${snapshot.owner.name} · ${if (draft) "BOZZA" else "CONFERMATO"}", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelMedium)
                    Text(if (total.balance >= 0) "Bonifico ancora dovuto" else "Credito da riconciliare", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelLarge)
                    Text(if (total.balance < 0) Amounts.euroAbsolute(total.balance) else Amounts.euro(total.balance), color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.testTag("report-balance"))
                }
            }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Riepilogo", "Dettagli", "Verifica").forEachIndexed { i, label -> FilterChip(tab == i, { tab = i }, label = { Text(label) }, modifier = Modifier.testTag("report-tab-$i")) } } }
            if (tab == 0) {
                item { Column { AmountLine("Lordo (tassa esclusa)", snapshot.gross()); AmountLine("OTA", snapshot.ota()); AmountLine("Netto al proprietario", snapshot.net()); HorizontalDivider(); AmountLine("Compenso Elledue", total.fees); AmountLine("Spese anticipate rimborsabili", total.reimbursable); AmountLine("Compensazioni riconosciute", total.compensated); AmountLine("Altri anticipi / pagamenti", total.payments) } }
                item { Text("Le prenotazioni appartengono interamente al mese del check-in. La tassa è esterna al lordo; soltanto la compensazione riconosciuta riduce il saldo.", style = MaterialTheme.typography.bodySmall) }
            } else if (tab == 1) {
                item { Text("Prenotazioni · ${snapshot.bookings.count { it.status != "CANCELLED" }}", style = MaterialTheme.typography.titleMedium) }
                items(snapshot.bookings, key = { "booking-${it.id}" }) { b ->
                    ListItem(headlineContent = { Text("${b.guestFirstName} ${b.guestLastName}") }, supportingContent = { Text("${BookingDates.display(b.checkInEpochDay)} → ${BookingDates.display(b.checkOutEpochDay)} · ${b.guestCount} ospiti\nLordo ${Amounts.euro(b.grossCents)} · OTA ${Amounts.euro(b.otaCents)}\nElledue ${Amounts.euro(b.managementCents)}${if (b.status == "CANCELLED") " · annullata, esclusa dai compensi" else ""}") }, trailingContent = { if (draft) IconButton(onClick = { onBooking(b.id) }) { Icon(Icons.Outlined.Edit, "Modifica prenotazione") } })
                    if (draft) TextButton(onClick = { onTax(b.id) }) { Text("Verifica tassa e movimenti") }
                }
                if (snapshot.linkedBookings.orEmpty().isNotEmpty()) item {
                    Text("Soggiorni collegati di altri periodi", style = MaterialTheme.typography.titleMedium)
                    snapshot.linkedBookings.orEmpty().forEach { b -> Text("${b.guestFirstName} ${b.guestLastName} · ${BookingDates.display(b.checkInEpochDay)} → ${BookingDates.display(b.checkOutEpochDay)} · riferimento, senza compenso in questo periodo", style = MaterialTheme.typography.bodySmall) }
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
                if (draft && issues.any { !it.blocking }) item { Row { Checkbox(acknowledged, { acknowledged = it }, modifier = Modifier.testTag("ack-report-warnings")); Text("Ho verificato gli avvisi e accetto i dati indicati.", modifier = Modifier.weight(1f).padding(top = 10.dp)) } }
                item { AccountingField("Note del rendiconto", notes, "report-notes", draft && !busy) { notes = it } }
            }
            if (draft) {
                item { OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("Anteprima PDF della bozza") } }
                item { Button(onClick = { model.refresh(report.id, notes) }, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("refresh-report")) { Text("Aggiorna bozza dai dati sorgente") } }
                item { OutlinedButton(onClick = { tab = 2; confirm = true }, enabled = !busy && notes.trim() == report.notes && issues.none { it.blocking } && (issues.isEmpty() || acknowledged), modifier = Modifier.fillMaxWidth().testTag("confirm-report")) { Text("Conferma rendiconto") } }
            } else {
                item { Button(onClick = onExport, modifier = Modifier.fillMaxWidth().testTag("export-report-pdf")) { Text("Esporta / visualizza PDF confermato") } }
                item { OutlinedButton(onClick = { revise = true }, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("revise-report")) { Text("Crea versione rettificativa") } }
                if (revise) item { AccountingField("Motivo della rettifica", reason, "report-revision-reason", !busy) { reason = it }; TextButton(onClick = { model.revision(report.id, reason) }, enabled = !busy) { Text("Apri nuova bozza rettificativa") } }
                item { Text("Questa versione resta immutabile. Le versioni dello stesso periodo sostituiscono il documento corrente senza duplicare gli importi.", style = MaterialTheme.typography.bodySmall) }
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { if (!busy) confirm = false }, title = { Text("Confermare questa versione?") }, text = { Text("Verrà conservato uno snapshot immutabile. Il PDF non sarà inviato automaticamente.") }, confirmButton = { TextButton(enabled = !busy, onClick = { model.confirm(snapshot, acknowledged); confirm = false }) { Text("Conferma definitiva") } }, dismissButton = { TextButton(enabled = !busy, onClick = { confirm = false }) { Text("Torna alla verifica") } })
}
