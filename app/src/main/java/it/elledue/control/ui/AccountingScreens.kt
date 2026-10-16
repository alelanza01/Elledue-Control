package it.elledue.control.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.elledue.control.data.*
import it.elledue.control.domain.*
import java.time.YearMonth
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountingHome(data: AccountingData, bookings: List<ReservationEntity>, properties: List<PropertyWithOwner>, model: AccountingViewModel,
                   onNew: (String) -> Unit, onExpense: (String) -> Unit, onTax: (String) -> Unit, onCategories: () -> Unit, onReports: () -> Unit = {}, initialTab:Int=0) {
    var month by rememberSaveable { mutableStateOf(YearMonth.from(BookingDates.today()).toString()) }
    var property by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    var kind by rememberSaveable { mutableStateOf("") }
    var showVoided by rememberSaveable { mutableStateOf(false) }
    var payment by rememberSaveable { mutableStateOf(false) }
    var reversalId by rememberSaveable { mutableStateOf<String?>(null) }
    val busy by model.busy.collectAsStateWithLifecycle(); val error by model.error.collectAsStateWithLifecycle(); val saved by model.saved.collectAsStateWithLifecycle()
    val summary = runCatching { operationalSummary(month, property, bookings, data.expenses, data.taxes, data.movements) }
    Scaffold(topBar = { TopAppBar(title = { Text("Contabilità operativa") }, actions = { IconButton(onClick = onCategories) { Icon(Icons.Outlined.Tune, "Categorie spese") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("accounting-scroll"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { PropertySelector(properties, property, allOption = true) { property = it } }
            item { Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = YearMonth.parse(month).minusMonths(1).toString() }) { Icon(Icons.Outlined.ChevronLeft, "Mese precedente contabilità") }
                Text(month, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).testTag("accounting-month"))
                IconButton(onClick = { month = YearMonth.parse(month).plusMonths(1).toString() }) { Icon(Icons.Outlined.ChevronRight, "Mese successivo contabilità") }
            } }
            item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Riepilogo", "Spese", "Tassa e anticipi").forEachIndexed { i, label -> FilterChip(tab == i, { tab = i }, label = { Text(label) }, modifier = Modifier.testTag("accounting-tab-$i")) }
            } }
            if (tab == 0) {
                item { Button(onClick = onReports, modifier = Modifier.fillMaxWidth().testTag("open-reports")) { Text("Rendiconti mensili") } }
                val currentBookings = bookings.filter { it.status != BookingStatus.CANCELLED.name && Competence.booking(it.checkInEpochDay) == month && (property.isEmpty() || it.propertyId == property) }
                val unconfigured = currentBookings.count { booking -> data.taxes.none { it.reservationId == booking.id } }
                val needsReview = data.taxes.count { tax -> tax.needsReview && currentBookings.any { it.id == tax.reservationId } }
                if (unconfigured > 0 || needsReview > 0) item {
                    OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Tasse da configurare: $unconfigured · da rivedere: $needsReview", style = MaterialTheme.typography.titleSmall)
                        Text("I totali fiscali includono soltanto le previsioni confermate; quelli da rivedere sono provvisori.", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { tab = 2 }) { Text("Verifica tasse dei soggiorni") }
                    } }
                }
                item {
                    summary.getOrNull()?.let { s ->
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(if (s.balance >= 0) "Saldo operativo da riconciliare" else "Credito da riconciliare", style = MaterialTheme.typography.labelLarge)
                                Text(Amounts.euro(if (s.balance == Long.MIN_VALUE) s.balance else kotlin.math.abs(s.balance)), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.testTag("operational-balance"))
                                Text("Non è un rendiconto confermato né un ordine di bonifico.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } ?: Text("Importi aggregati oltre il limite: seleziona un immobile.", color = MaterialTheme.colorScheme.error)
                }
                summary.getOrNull()?.let { s ->
                    item { AmountLine("Commissioni Elledue maturate", s.fees); AmountLine("Spese complessive", s.expenses); AmountLine("Spese anticipate da Elledue", s.advanced); AmountLine("Anticipate e rimborsabili", s.reimbursable); AmountLine("Pulizie", s.cleaning); AmountLine("Lavanderia", s.laundry) }
                    item { HorizontalDivider(); AmountLine("Tassa prevista", s.expectedTax); AmountLine("Tassa effettivamente riscossa", s.collectedTax); AmountLine("Tassa ancora da incassare", s.residualTax); AmountLine("Compensazioni riconosciute nel mese", s.compensated); AmountLine("Altri anticipi / pagamenti riconosciuti", s.payments) }
                }
                item { Text("Prenotazioni: interamente nel mese del check-in. Spese: mese assegnato. La tassa prevista/riscossa/residua riguarda questi soggiorni, anche se riscossa dopo; compensazioni e pagamenti seguono il mese del movimento.", style = MaterialTheme.typography.bodySmall) }
                item { Text("I portali pagano il proprietario. La tassa non è un ricavo Elledue né una spesa.", style = MaterialTheme.typography.bodyMedium) }
                item { OutlinedButton(onClick = { model.clear(); payment = true }, enabled = property.isNotEmpty(), modifier = Modifier.testTag("owner-payment")) { Text("Registra anticipo / pagamento proprietario") }; if (property.isEmpty()) Text("Seleziona un immobile per registrare un pagamento.", style = MaterialTheme.typography.bodySmall) }
                val ownerMovements = data.movements.filter { it.competenceMonth == month && (property.isEmpty() || it.propertyId == property) && (it.kind == "OWNER_PAYMENT" || it.reversesId != null && data.movements.any { original -> original.id == it.reversesId && original.kind == "OWNER_PAYMENT" }) }
                items(ownerMovements, key = { it.id }) { movement -> MovementRow(movement, data.movements, busy) { model.clear(); reversalId = movement.id } }
            } else if (tab == 1) {
                item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onNew("OTHER") }, enabled = properties.any { !it.property.archived }, modifier = Modifier.testTag("new-expense")) { Text("+ Spesa") }
                    OutlinedButton(onClick = { onNew("CLEANING") }, enabled = properties.any { !it.property.archived }) { Text("Pulizia") }
                    OutlinedButton(onClick = { onNew("LAUNDRY") }, enabled = properties.any { !it.property.archived }) { Text("Lavanderia") }
                } }
                item { ChoiceField("Tipo di spesa", kind, listOf("" to "Tutte", "CLEANING" to "Pulizie", "LAUNDRY" to "Lavanderia", "OTHER" to "Altre spese")) { kind = it } }
                item { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(showVoided, { showVoided = it }); Text("Mostra anche annullate") } }
                val filtered = data.expenses.filter { it.competenceMonth == month && (property.isEmpty() || it.propertyId == property) && (kind.isEmpty() || it.kind == kind) && (showVoided || !it.voided) }
                if (filtered.isEmpty()) item { Text("Nessuna spesa per questo periodo. Registra un acquisto o un intervento.") }
                items(filtered, key = { it.id }) { item ->
                    OutlinedCard(onClick = { onExpense(item.id) }, modifier = Modifier.fillMaxWidth().testTag("expense-row-${item.id}")) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(item.description, style = MaterialTheme.typography.titleMedium)
                            Text("${properties.firstOrNull { it.property.id == item.propertyId }?.property?.name ?: "Immobile"} · ${item.category} · ${BookingDates.display(item.dateEpochDay)}", style = MaterialTheme.typography.bodySmall)
                            Text(Amounts.euro(item.actualCents), style = MaterialTheme.typography.titleLarge)
                            Text(if (item.voided) "Annullata · storico conservato" else if (item.advancedByElledue && item.reimbursable) "Anticipata e rimborsabile" else "Non inclusa nei rimborsi Elledue", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            } else {
                item { Text("Previsione, incasso, compensazione e riversamento sono passaggi separati. Apri il soggiorno per registrare il movimento effettivo.",style=MaterialTheme.typography.bodySmall) }
                item { OutlinedButton(onClick={model.clear();payment=true},enabled=property.isNotEmpty()){Text("Altri pagamenti / anticipi del proprietario")}; if(property.isBlank()) Text("Seleziona prima un immobile.",style=MaterialTheme.typography.bodySmall) }
                val stays = bookings.filter { Competence.booking(it.checkInEpochDay) == month && (property.isEmpty() || it.propertyId == property) }
                if (stays.isEmpty()) item { Text("Nessuna prenotazione con check-in nel periodo.") }
                items(stays, key = { it.id }) { booking ->
                    val assessment = data.taxes.firstOrNull { it.reservationId == booking.id }
                    val collected = Ledger.total(data.movements.filter { it.reservationId == booking.id }, "COLLECTION")
                    OutlinedCard(onClick = { onTax(booking.id) }, modifier = Modifier.fillMaxWidth().testTag("tax-row-${booking.id}")) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${booking.guestFirstName} ${booking.guestLastName}", style = MaterialTheme.typography.titleMedium)
                            Text("${properties.firstOrNull { it.property.id == booking.propertyId }?.property?.name} · check-in ${BookingDates.display(booking.checkInEpochDay)}", style = MaterialTheme.typography.bodySmall)
                            Text(if (assessment?.needsReview == true) "Da rivedere: prenotazione modificata" else Ledger.taxStatus(assessment?.expectedCents, collected), color = MaterialTheme.colorScheme.secondary)
                            if (booking.status == BookingStatus.CANCELLED.name) Text("Prenotazione annullata · movimenti conservati")
                            Text("Riscossa ${Amounts.euro(collected)}", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            item {
                if (payment) MovementPanel("OWNER_PAYMENT", property, null, null, month, model) { payment = false }
    reversalId?.let { id -> data.movements.firstOrNull { it.id == id }?.let { MovementPanel("REVERSAL", it.propertyId, it.reservationId, it, month, model) { reversalId = null } } }
            }
            if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error) }
        }
    }
    LaunchedEffect(saved) { if (saved == "movement") { payment = false; reversalId = null; model.clear() } }
}

@Composable
fun AmountLine(label: String, amount: Long) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f)); Spacer(Modifier.width(12.dp)); Text(Amounts.euro(amount), style = MaterialTheme.typography.labelLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoiceField(label: String, selected: String, options: List<Pair<String, String>>, enabled: Boolean = true, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { if (enabled) expanded = !expanded }) {
        OutlinedTextField(options.firstOrNull { it.first == selected }?.second ?: "Seleziona", {}, readOnly = true, enabled = enabled, label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable).testTag(label))
        ExposedDropdownMenu(expanded, { expanded = false }) { options.forEach { (id, text) -> DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(id); expanded = false }) } }
    }
}

@Composable
fun AccountingField(label: String, value: String, tag: String = label, enabled: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, enabled = enabled, modifier = Modifier.fillMaxWidth().testTag(tag), singleLine = true)
}

@Composable
fun MovementRow(item: MoneyMovementEntity, all: List<MoneyMovementEntity>, busy: Boolean, onReverse: () -> Unit) {
    val reversed = all.any { it.reversesId == item.id }
    OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("${movementLabel(item.kind)} · ${Amounts.euro(item.amountCents)}", style = MaterialTheme.typography.titleSmall)
        Text("${BookingDates.display(item.dateEpochDay)} · competenza ${item.competenceMonth}", style = MaterialTheme.typography.bodySmall)
        if (item.sourceId != null) Text("Collegata alla riscossione del ${all.firstOrNull { it.id == item.sourceId }?.dateEpochDay?.let(BookingDates::display) ?: "data non disponibile"}", style = MaterialTheme.typography.bodySmall)
        if (item.notes.isNotBlank()) Text(item.notes, style = MaterialTheme.typography.bodySmall)
        if (reversed) Text("Stornato · originale conservato")
        else if (item.kind != "REVERSAL") TextButton(onClick = onReverse, enabled = !busy) { Text("Storna con motivazione") }
    } }
}
fun movementLabel(kind: String) = when (kind) { "COLLECTION" -> "Riscossione confermata"; "COMPENSATION" -> "Compensazione riconosciuta"; "REMITTANCE" -> "Riversamento registrato"; "OWNER_PAYMENT" -> "Anticipo / pagamento proprietario"; else -> "Storno" }

@Composable
fun MovementPanel(kind: String, propertyId: String, reservationId: String?, original: MoneyMovementEntity?, month: String, model: AccountingViewModel, dismiss: () -> Unit) {
    val draftKey="movement/new?property=$propertyId&booking=${reservationId ?: ""}&kind=$kind&source=${original?.id ?: ""}"
    val id by rememberPersistentDraft("$draftKey#id",UUID.randomUUID().toString())
    var amount by rememberPersistentDraft("$draftKey#amount",if (kind == "REVERSAL") original?.amountCents?.let(Amounts::editable) ?: "" else "")
    var date by rememberPersistentDraft("$draftKey#date",BookingDates.display(BookingDates.today().toEpochDay()))
    var competence by rememberPersistentDraft("$draftKey#competence",month)
    var notes by rememberPersistentDraft("$draftKey#notes","")
    var localError by remember { mutableStateOf<String?>(null) }
    val busy by model.busy.collectAsStateWithLifecycle(); val error by model.error.collectAsStateWithLifecycle()
    val bringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(id) { withFrameNanos { }; bringIntoView.bringIntoView() }
    OutlinedCard(Modifier.fillMaxWidth().bringIntoViewRequester(bringIntoView).testTag("movement-panel")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DraftBanner(draftKey){dismiss()}
            Text(movementLabel(kind), style = MaterialTheme.typography.titleMedium)
            Text(if (kind == "COLLECTION") "Conferma soltanto una somma materialmente incassata." else if (kind == "COMPENSATION") "Riconosci un anticipo al proprietario; questa operazione non registra un nuovo incasso." else if (kind == "REMITTANCE") "Registra un riversamento verificato, separato dalla compensazione." else "Il movimento rimarrà nello storico.", style = MaterialTheme.typography.bodySmall)
            AccountingField("Importo €", amount, "movement-amount", enabled = !busy && kind != "REVERSAL") { amount = it }
            BookingDateField("Data movimento", date, !busy, "movement-date", Modifier.fillMaxWidth()) { date = it }
            AccountingField("Competenza AAAA-MM", competence, "movement-month", !busy) { competence = it }
            AccountingField("Motivo / nota", notes, "movement-notes", !busy) { notes = it }
            (localError ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row { TextButton(enabled = !busy, onClick = {
                try {
                    val item = MoneyMovementEntity(id, propertyId, reservationId, kind, Amounts.parseCents(amount), BookingDates.parse(date).toEpochDay(), Competence.month(competence), notes.trim(),
                        sourceId = if (kind == "COMPENSATION" || kind == "REMITTANCE") original?.id else null, reversesId = if (kind == "REVERSAL") original?.id else null)
                    localError = null; model.post(item)
                } catch (e: Exception) { localError = e.message ?: "Controlla i dati." }
            }, modifier = Modifier.testTag("confirm-movement")) { Text("Conferma movimento") }; TextButton(onClick = dismiss, enabled = !busy) { Text("Annulla") } }
        }
    }
}
