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
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseEditor(initial: ExpenseDraft, properties: List<PropertyWithOwner>, bookings: List<ReservationEntity>, categories: List<ExpenseCategoryEntity>, model: AccountingViewModel, draftRoute:String?=null, onBack: () -> Unit) {
    val draftKey=draftRoute ?: if(initial.revision==0)"expense/new/OTHER?booking=${initial.reservationId}" else "expense/${initial.id}/edit"
    var draft by rememberPersistentDraft(draftKey,initial)
    var details by rememberSaveable{mutableStateOf(initial.revision>0)}
    var exit by rememberSaveable { mutableStateOf(false) }
    val busy by model.busy.collectAsStateWithLifecycle(); val error by model.error.collectAsStateWithLifecycle()
    val editorScroll=rememberScrollState()
    LaunchedEffect(error){if(error!=null)editorScroll.animateScrollTo(0)}
    val back = { if (!busy) { if (draft != initial) exit = true else onBack() } }
    BackHandler { back() }
    val category = categories.firstOrNull { it.id == draft.categoryId }
    val calculated = runCatching { if (draft.quantity.isNotBlank() && draft.unit.isNotBlank()) OperationalMoney.cost(draft.quantity, Amounts.parseCents(draft.unit)) else null }.getOrNull()
    fun selectCategory(id: String): ExpenseDraft {
        val c = categories.firstOrNull { it.id == id }; val p = properties.firstOrNull { it.property.id == draft.propertyId }?.property
        return draft.copy(categoryId = id, quantity = if(id=="transport" && p?.habitualTransportCents!=null)"1" else if (c?.kind == "OTHER") "" else draft.quantity.ifBlank { "1" }, unit = when (c?.kind) { "CLEANING" -> Amounts.editable(p?.cleaningHourlyCents ?: 800); "LAUNDRY" -> Amounts.editable(p?.laundrySetCents ?: 500); else -> if(id=="transport")p?.habitualTransportCents?.let(Amounts::editable) ?: "" else "" })
    }
    Scaffold(topBar = { TopAppBar(title = { Text(if (initial.revision == 0) "Nuova spesa" else "Rettifica spesa") }, navigationIcon = { IconButton(onClick = back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Indietro spesa") } }, actions = { TextButton(onClick = { model.saveExpense(draft) }, enabled = !busy, modifier = Modifier.testTag("save-expense")) { Text("Salva") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(editorScroll).padding(20.dp).testTag("expense-editor"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            DraftBanner(draftKey){draft=initial}
            error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
            PropertySelector(properties.filter { !it.property.archived || it.property.id == initial.propertyId }, draft.propertyId, enabled = !busy) { id ->
                val p = properties.first { it.property.id == id }.property
                draft = draft.copy(propertyId = id, reservationId = "", reimbursable=if(p.businessType=="OWNED")false else draft.reimbursable, advanced=if(p.businessType=="OWNED")false else draft.advanced, unit = if (initial.revision == 0) when (category?.kind) { "CLEANING" -> Amounts.editable(p.cleaningHourlyCents); "LAUNDRY" -> Amounts.editable(p.laundrySetCents); else -> draft.unit } else draft.unit)
            }
            ChoiceField("Categoria", draft.categoryId, categories.filter { !it.archived || it.id == initial.categoryId }.map { it.id to it.label }, !busy) { draft = selectCategory(it) }
            if (category?.kind != "OTHER") {
                AccountingField(if (category?.kind == "CLEANING") "Ore lavorate" else "Numero set", draft.quantity, "expense-quantity", !busy) { draft = draft.copy(quantity = it) }
                AccountingField(if (category?.kind == "CLEANING") "Tariffa oraria €" else "Tariffa per set €", draft.unit, "expense-unit", !busy) { draft = draft.copy(unit = it) }
                calculated?.let { Text("Costo calcolato ${Amounts.euro(it)}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("expense-calculated")) }
                if (details && category?.kind == "LAUNDRY") AccountingField("Definizione set", draft.setDefinition, enabled = !busy) { draft = draft.copy(setDefinition = it) }
            }
            AccountingField(if (category?.kind == "OTHER") "Importo €" else "Importo effettivo € (facoltativo)", draft.amount, "expense-amount", !busy) { draft = draft.copy(amount = it) }
            if (category?.kind != "OTHER") Text("Se inserito, l’importo effettivo prevale sul calcolo. L’intervento è una sola spesa.", style = MaterialTheme.typography.bodySmall)
            AccountingField("Descrizione (facoltativa)", draft.description, "expense-description", !busy) { draft = draft.copy(description = it) }
            if(initial.standardApproved)Row{Checkbox(draft.confirmActual,{draft=draft.copy(confirmActual=it)},enabled=!busy);Text("Confermo il consuntivo dell’intervento",Modifier.weight(1f).padding(top=12.dp))}
            BookingDateField(if(initial.standardApproved && !draft.confirmActual)"Intervento previsto" else "Data effettiva", draft.date, !busy, "expense-date", Modifier.fillMaxWidth()) { value ->
                val defaultMonth = runCatching { Competence.booking(BookingDates.parse(draft.date).toEpochDay()) }.getOrNull()
                draft = draft.copy(date = value, month = if (draft.month == defaultMonth) runCatching { Competence.booking(BookingDates.parse(value).toEpochDay()) }.getOrDefault(draft.month) else draft.month)
            }
            ChoiceField("Prenotazione collegata", draft.reservationId, listOf("" to "Nessuna") + bookings.filter { it.propertyId == draft.propertyId }.map { it.id to "${it.guestFirstName} ${it.guestLastName} · ${BookingDates.display(it.checkInEpochDay)}" }, !busy) { draft = draft.copy(reservationId = it) }
            ChoiceField("Chi ha pagato",if(draft.advanced)"ELLEDUE" else "OWNER",listOf("ELLEDUE" to "Elledue","OWNER" to "Proprietario / altro"),!busy){draft=draft.copy(advanced=it=="ELLEDUE")}
            TextButton(onClick={details=!details},modifier=Modifier.testTag("expense-more-details")){Text(if(details)"Nascondi dettagli" else "Altri dettagli")}
            if(details) {
            AccountingField("Competenza AAAA-MM", draft.month, "expense-month", !busy) { draft = draft.copy(month = it) }

            Row { Checkbox(draft.advanced, { draft = draft.copy(advanced = it) }, enabled = !busy); Text("Anticipata da Elledue", modifier = Modifier.padding(top = 12.dp)) }
            Row { Checkbox(draft.reimbursable, { draft = draft.copy(reimbursable = it) }, enabled = !busy && (initial.revision>0 || properties.firstOrNull{it.property.id==draft.propertyId}?.property?.businessType!="OWNED")); Text("Rimborsabile dal proprietario", modifier = Modifier.padding(top = 12.dp)) }
            if (category?.kind == "OTHER") {
                AccountingField("Quantità (facoltativa)", draft.quantity, "expense-quantity", !busy) { draft = draft.copy(quantity = it) }
                AccountingField("Tariffa unitaria € (facoltativa)", draft.unit, "expense-unit", !busy) { draft = draft.copy(unit = it) }
            }
            AccountingField("Note", draft.notes, enabled = !busy) { draft = draft.copy(notes = it) }
            if (initial.revision > 0) AccountingField("Motivo della rettifica", draft.reason, "expense-reason", !busy) { draft = draft.copy(reason = it) }
            }
            if (busy) CircularProgressIndicator()
        }
    }
    if (exit) AlertDialog(onDismissRequest = { exit = false }, title = { Text("Uscire senza salvare?") }, text = { Text("La bozza locale rimane disponibile in Home → Riprendi bozze.") }, confirmButton = { TextButton(onClick = { exit = false; onBack() }) { Text("Esci") } }, dismissButton = { TextButton(onClick = { exit = false }) { Text("Continua") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetail(item: ExpenseEntity, propertyName: String, model: AccountingViewModel, onBack: () -> Unit, onEdit: () -> Unit, bookingName: String? = null) {
    val busy by model.busy.collectAsStateWithLifecycle(); val error by model.error.collectAsStateWithLifecycle(); val history by model.expenseHistory.collectAsStateWithLifecycle()
    var void by rememberSaveable { mutableStateOf(false) }; var reason by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(item.id) { model.loadExpenseHistory(item.id) }
    Scaffold(topBar = { TopAppBar(title = { Text("Scheda spesa") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Indietro spesa") } }, actions = { if (!item.voided) TextButton(onClick = onEdit, enabled = !busy, modifier = Modifier.testTag("edit-expense")) { Text("Rettifica") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(item.description, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("expense-detail"))
            Text("$propertyName · ${item.category}")
            Text("${if(item.effectiveDateConfirmed)"Data effettiva" else "Intervento previsto"} ${BookingDates.display(item.dateEpochDay)} · competenza ${item.competenceMonth}")
            if(item.recognition=="STANDARD_APPROVED")Text("Standard approvato nel saldo · pagamento non attestato",color=ElledueGold)
            Text(Amounts.euro(item.actualCents), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.testTag("expense-actual"))
            if (item.quantity != null) Text("${item.quantity} × ${Amounts.euro(item.unitCents ?: 0)} · calcolato ${Amounts.euro(item.calculatedCents ?: 0)}")
            if (item.setDefinition.isNotBlank()) Text(item.setDefinition)
            Text("Anticipata: ${if (item.advancedByElledue) "sì" else "no"} · rimborsabile: ${if (item.reimbursable) "sì" else "no"}")
            if (item.reservationId != null) Text("Prenotazione: ${bookingName ?: "collegamento conservato"}")
            if (item.notes.isNotBlank()) Text(item.notes)
            if (item.voided) Text("Spesa annullata: esclusa dai totali, storico conservato.", color = MaterialTheme.colorScheme.error)
            else OutlinedButton(onClick = { reason = ""; void = true }, enabled = !busy, modifier = Modifier.testTag("void-expense")) { Text("Annulla spesa con motivazione") }
            if (void) OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Annullare la spesa?", style = MaterialTheme.typography.titleMedium)
                AccountingField("Motivo", reason, "void-reason", !busy) { reason = it }
                Row { TextButton(onClick = { model.voidExpense(item.id, reason) }, enabled = !busy) { Text("Conferma annullamento") }; TextButton(onClick = { void = false }, enabled = !busy) { Text("Indietro") } }
            } }
            Text("Storico rettifiche · versione ${item.revision}", style = MaterialTheme.typography.titleMedium)
            history.forEach { row -> Text("v${row.version} · ${row.reason}\n${Amounts.euro(org.json.JSONObject(row.snapshotJson).getLong("actualCents"))}", style = MaterialTheme.typography.bodySmall) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
    LaunchedEffect(item.voided) { if (item.voided) void = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(categories: List<ExpenseCategoryEntity>, model: AccountingViewModel, onBack: () -> Unit) {
    val contextForDrafts=androidx.compose.ui.platform.LocalContext.current
    var editId by rememberPersistentDraft("categories#id", ""); var label by rememberPersistentDraft("categories#label", ""); var kind by rememberPersistentDraft("categories#kind", "OTHER")
    val error by model.error.collectAsStateWithLifecycle(); val busy by model.busy.collectAsStateWithLifecycle(); val saved by model.saved.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text("Categorie spese") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Indietro categorie") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { model.clear(); editId = UUID.randomUUID().toString(); label = ""; kind = "OTHER" }, modifier = Modifier.testTag("new-category")) { Text("Nuova categoria") }
            if (editId.isNotEmpty()) OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DraftBanner("categories"){editId="";label="";kind="OTHER"}
                Text("Categoria", style = MaterialTheme.typography.titleMedium)
                AccountingField("Nome categoria", label, "category-name", !busy) { label = it }
                ChoiceField("Tipologia", kind, listOf("OTHER" to "Altra spesa", "CLEANING" to "Pulizia", "LAUNDRY" to "Lavanderia"), !busy && categories.none { it.id == editId }) { kind = it }
                Row { TextButton(onClick = { model.saveCategory(editId, label, kind, categories.firstOrNull { it.id == editId }?.archived ?: false) }, enabled = !busy) { Text("Salva categoria") }; TextButton(onClick = { editId = "" }, enabled = !busy) { Text("Annulla") } }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } }
            categories.forEach { item -> OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) {
                Text(item.label + if (item.archived) " · archiviata" else "", style = MaterialTheme.typography.titleSmall)
                Row { TextButton(onClick = { model.clear(); editId = item.id; label = item.label; kind = item.kind }, enabled = !busy) { Text("Modifica") }; TextButton(onClick = { model.saveCategory(item.id, item.label, item.kind, !item.archived) }, enabled = !busy) { Text(if (item.archived) "Riattiva" else "Archivia") } }
            } } }
            Text("La modifica del nome non riscrive le descrizioni storiche delle spese. Archiviare conserva i collegamenti.", style = MaterialTheme.typography.bodySmall)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
    LaunchedEffect(saved) { if (saved == "category") { editId = ""; it.elledue.control.data.DraftStore.get(contextForDrafts).clear("categories"); model.clear() } }
}
