package it.elledue.control.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.elledue.control.data.*
import it.elledue.control.domain.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxScreen(booking: ReservationEntity, property: PropertyEntity, data: AccountingData, model: AccountingViewModel, onBack: () -> Unit) {
    val assessment = data.taxes.firstOrNull { it.reservationId == booking.id }
    val initial = assessment?.let(TaxDraft::from) ?: TaxDraft(nights = minOf(booking.checkOutEpochDay - booking.checkInEpochDay, property.touristTaxMaxNights?.toLong() ?: Long.MAX_VALUE).toString())
    var draft by rememberSaveable(booking.id, assessment?.revision, stateSaver = TaxDraftSaver) { mutableStateOf(initial) }
    var exit by rememberSaveable { mutableStateOf(false) }
    var movementKind by rememberSaveable { mutableStateOf<String?>(null) }; var sourceId by rememberSaveable { mutableStateOf<String?>(null) }
    val busy by model.busy.collectAsStateWithLifecycle(); val error by model.error.collectAsStateWithLifecycle(); val saved by model.saved.collectAsStateWithLifecycle(); val history by model.taxHistory.collectAsStateWithLifecycle()
    val back = { if (!busy) { if (draft != initial) exit = true else onBack() } }
    BackHandler { back() }
    val money = data.movements.filter { it.reservationId == booking.id }; val active = Ledger.active(money)
    val collected = Ledger.total(money, "COLLECTION"); val compensated = Ledger.total(money, "COMPENSATION"); val remitted = Ledger.total(money, "REMITTANCE")
    val preview = runCatching { draft.record(booking, property, assessment) }.getOrNull()
    val canPost = assessment != null && !assessment.needsReview && booking.status != BookingStatus.CANCELLED.name && !busy
    LaunchedEffect(booking.id) { model.loadTaxHistory(booking.id) }
    Scaffold(topBar = { TopAppBar(title = { Text("Tassa di soggiorno") }, navigationIcon = { IconButton(onClick = back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Indietro tassa") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp).testTag("tax-screen"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("${booking.guestFirstName} ${booking.guestLastName}", style = MaterialTheme.typography.headlineMedium)
            Text("${property.name} · ${BookingDates.display(booking.checkInEpochDay)} → ${BookingDates.display(booking.checkOutEpochDay)}")
            Text("${booking.guestCount} ospiti totali · competenza ${booking.competenceMonth}", style = MaterialTheme.typography.labelLarge)
            if (assessment?.needsReview == true) Text("Prenotazione modificata: verifica e salva nuovamente i conteggi prima di registrare movimenti.", color = MaterialTheme.colorScheme.error)
            if (booking.status == BookingStatus.CANCELLED.name) Text("Prenotazione annullata: movimenti conservati, nuove riscossioni disabilitate.", color = MaterialTheme.colorScheme.error)
            Text("${Amounts.euro(assessment?.nightCents ?: property.touristTaxNightCents)} per persona/notte · dai ${assessment?.minAge ?: property.touristTaxMinAge} anni compiuti", style = MaterialTheme.typography.bodyMedium)
            Text("Limite notti: ${(if (assessment != null) assessment.maxNights else property.touristTaxMaxNights)?.toString() ?: "nessuno"}. Esenzioni: ${assessment?.exemptions?.ifBlank { "verificare disposizioni locali" } ?: property.touristTaxExemptions.ifBlank { "verificare disposizioni locali" }}", style = MaterialTheme.typography.bodySmall)
            Text("Le impostazioni dell’immobile sono fotografate alla prima conferma. Non inserire età individuali: conferma i conteggi.", style = MaterialTheme.typography.bodySmall)
            AccountingField("Ospiti soggetti", draft.taxable, "tax-subjects", !busy) { draft = draft.copy(taxable = it) }
            AccountingField("Ospiti esenti", draft.exempt, "tax-exempt", !busy) { draft = draft.copy(exempt = it) }
            AccountingField("Notti assoggettate", draft.nights, "tax-nights", !busy) { draft = draft.copy(nights = it) }
            preview?.let { AmountLine("Tassa calcolata", it.calculatedCents); AmountLine("Tassa prevista", it.expectedCents) }
            AccountingField("Tassa prevista rettificata € (facoltativa)", draft.actualExpected, "tax-override", !busy) { draft = draft.copy(actualExpected = it) }
            AccountingField("Motivo rettifica", draft.reason, "tax-reason", !busy) { draft = draft.copy(reason = it) }
            ChoiceField("Soggetto del riversamento", draft.responsible, listOf("UNDEFINED" to "Da definire", "OWNER" to "Proprietario", "ELLEDUE" to "Elledue", "OTHER" to "Altro soggetto"), !busy) { draft = draft.copy(responsible = it) }
            AccountingField("Obblighi / note riversamento", draft.remittanceNotes, enabled = !busy) { draft = draft.copy(remittanceNotes = it) }
            Button(onClick = { model.saveTax(booking.id, draft) }, enabled = !busy && (booking.status != BookingStatus.CANCELLED.name || assessment != null), modifier = Modifier.fillMaxWidth().testTag("save-tax")) { Text("Conferma conteggi e impostazione tassa") }
            HorizontalDivider()
            Text(if (booking.status == BookingStatus.CANCELLED.name) { if (collected > 0) "Annullata: riscossioni da riconciliare" else "Annullata: nessuna riscossione" } else Ledger.taxStatus(assessment?.expectedCents, collected), style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("tax-status"))
            assessment?.let { AmountLine("Prevista confermata", it.expectedCents); if (booking.status != BookingStatus.CANCELLED.name) AmountLine("Ancora da incassare", Ledger.residual(it.expectedCents, collected)); if (collected > it.expectedCents) Text("Eccedenza ${Amounts.euro(collected - it.expectedCents)}: riconcilia con uno storno motivato.", color = MaterialTheme.colorScheme.error) }
            AmountLine("Effettivamente riscossa", collected); AmountLine("Riconosciuta in compensazione", compensated); AmountLine("Riversamento registrato", remitted)
            Text("Riscossione e compensazione sono separate. Il check-out non registra incassi. Il riversamento segue gli obblighi configurati, senza presumere il soggetto responsabile.", style = MaterialTheme.typography.bodySmall)
            Button(onClick = { model.clear(); sourceId = null; movementKind = "COLLECTION" }, enabled = canPost && collected < (assessment?.expectedCents ?: 0), modifier = Modifier.fillMaxWidth().testTag("collect-tax")) { Text("Conferma riscossione, anche parziale") }
            active.filter { it.kind == "COLLECTION" }.forEach { source ->
                val compAvailable = source.amountCents - OperationalMoney.total(active.filter { it.kind == "COMPENSATION" && it.sourceId == source.id }.map { it.amountCents })
                val remitAvailable = source.amountCents - OperationalMoney.total(active.filter { it.kind == "REMITTANCE" && it.sourceId == source.id }.map { it.amountCents })
                OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Riscossione del ${BookingDates.display(source.dateEpochDay)} · ${Amounts.euro(source.amountCents)}", style = MaterialTheme.typography.titleSmall)
                    Text("Disponibile per compensazione ${Amounts.euro(compAvailable)}", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { model.clear(); sourceId = source.id; movementKind = "COMPENSATION" }, enabled = canPost && compAvailable > 0, modifier = Modifier.testTag("compensate-${source.id}")) { Text("Riconosci in compensazione") }
                    TextButton(onClick = { model.clear(); sourceId = source.id; movementKind = "REMITTANCE" }, enabled = canPost && remitAvailable > 0 && assessment?.remittanceResponsible != "UNDEFINED") { Text("Registra riversamento separato") }
                } }
            }
            Text("Storico movimenti", style = MaterialTheme.typography.titleLarge)
            if (money.isEmpty()) Text("Nessuna riscossione confermata.")
            money.reversed().forEach { item -> MovementRow(item, money, busy) { model.clear(); sourceId = item.id; movementKind = "REVERSAL" } }
            Text("Storico conteggi · ${history.size} ${if (history.size == 1) "conferma" else "conferme"}", style = MaterialTheme.typography.titleMedium)
            history.forEach { row -> val value = org.json.JSONObject(row.snapshotJson); Text("v${row.version} · prevista ${Amounts.euro(value.getLong("expectedCents"))} · ${value.getString("correctionReason")}", style = MaterialTheme.typography.bodySmall) }
            if (saved == "tax") Text("Conteggi salvati. Nessun incasso automatico.")
    movementKind?.let { kind -> MovementPanel(kind, property.id, booking.id, money.firstOrNull { it.id == sourceId }, Competence.booking(booking.checkInEpochDay), model) { movementKind = null; sourceId = null } }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
    LaunchedEffect(saved) { if (saved == "movement") { movementKind = null; sourceId = null; model.clear() } }
    if (exit) AlertDialog(onDismissRequest = { exit = false }, title = { Text("Uscire senza salvare i conteggi?") }, confirmButton = { TextButton(onClick = { exit = false; onBack() }) { Text("Esci") } }, dismissButton = { TextButton(onClick = { exit = false }) { Text("Continua") } })
}
