package it.elledue.control.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import it.elledue.control.data.PropertyWithOwner
import it.elledue.control.domain.Amounts
import it.elledue.control.domain.PropertyDraft

@Composable
fun EmptyState(title: String, description: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(Icons.Outlined.Apartment, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(48.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null) Button(onClick = onAction) { Text(action) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertiesScreen(items: List<PropertyWithOwner>, onCreate: () -> Unit, onOpen: (String) -> Unit) {
    var archived by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = items.filter {
        it.property.archived == archived && (it.property.name.contains(query, true) || it.property.address.contains(query, true) || it.owner.name.contains(query, true))
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Immobili") }, actions = {
            FilledTonalIconButton(onClick = onCreate, modifier = Modifier.testTag("new-property")) { Icon(Icons.Outlined.Add, "Nuovo immobile") }
        }) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                OutlinedTextField(query, { query = it }, label = { Text("Cerca immobile o proprietario") }, singleLine = true, leadingIcon = { Icon(Icons.Outlined.Search, null) }, modifier = Modifier.fillMaxWidth())
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !archived, onClick = { archived = false }, label = { Text("Attivi (${items.count { !it.property.archived }})") })
                    FilterChip(selected = archived, onClick = { archived = true }, label = { Text("Archiviati (${items.count { it.property.archived }})") }, modifier = Modifier.testTag("archived-filter"))
                }
            }
            if (filtered.isEmpty()) item {
                EmptyState(if (archived) "Nessun immobile archiviato" else "La tua gestione parte da qui", if (query.isNotBlank()) "Nessun risultato per questa ricerca." else if (archived) "L’archivio conserva schede e storico degli immobili." else "Aggiungi il primo immobile e configura proprietario, tariffe e informazioni operative.")
            }
            items(filtered, key = { it.property.id }) { item ->
                PortfolioTile(item) { onOpen(item.property.id) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailScreen(item: PropertyWithOwner, busy: Boolean, error: String?, onBack: () -> Unit, onEdit: () -> Unit, onArchive: (Boolean) -> Unit, onPhoto: (String?) -> Unit = {}, onDelete: () -> Unit = {}) {
    var confirmArchive by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }; var delete by remember { mutableStateOf(false) }
    val p = item.property
    Scaffold(topBar = { TopAppBar(title = { Text("Scheda immobile") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Indietro") } }, actions = { TextButton(onClick = onEdit, enabled = !busy) { Text("Modifica") }; Box {
        IconButton(onClick = { menu = true }, modifier = Modifier.testTag("property-menu")) { Icon(Icons.Outlined.MoreVert, "Azioni immobile") }
        DropdownMenu(menu, { menu = false }) {
            DropdownMenuItem(text = { Text("Modifica") }, onClick = { menu = false; onEdit() })
            DropdownMenuItem(text = { Text(if (p.archived) "Riattiva" else "Archivia") }, onClick = { menu = false; if (p.archived) onArchive(false) else confirmArchive = true })
            DropdownMenuItem(text = { Text("Elimina se privo di storico") }, onClick = { menu = false; delete = true })
        }
    } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            PhotoControls(p.coverImageBase64, onPhoto = onPhoto)
            Text(p.name, style = MaterialTheme.typography.headlineLarge)
            Text(p.address, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (p.archived) SuggestionChip(onClick = {}, label = { Text("Archiviato · storico conservato") })
            DetailSection("Proprietario", listOf("Nome" to item.owner.name, "Telefono" to item.owner.phone, "Email" to item.owner.email))
            DetailSection("Operatività", listOf("Posti letto" to p.beds.toString(), "Ospiti massimi" to p.maxGuests.toString(), "Check-in" to p.checkInTime, "Check-out" to p.checkOutTime, "Recapiti utili" to p.usefulContacts, "Note" to p.operationalNotes))
            DetailSection("Commissioni e servizi", listOf("Gestione Elledue" to "${Amounts.editable(p.managementBasisPoints.toLong())}% sul netto dopo OTA", "OTA predefinita" to "${Amounts.editable(p.otaBasisPoints.toLong())}%", "Pulizie / ora" to Amounts.euro(p.cleaningHourlyCents), "Lavanderia / set" to Amounts.euro(p.laundrySetCents)) + item.platformCommissions.map { it.platform to "${Amounts.editable(it.basisPoints.toLong())}%" })
            DetailSection("Tassa di soggiorno", listOf("Persona / notte" to Amounts.euro(p.touristTaxNightCents), "Età minima" to "${p.touristTaxMinAge} anni", "Limite notti" to (p.touristTaxMaxNights?.toString() ?: "Nessun limite configurato"), "Esenzioni" to p.touristTaxExemptions))
            if (item.templates.isNotEmpty()) DetailSection("Istruzioni di accesso", item.templates.map { (if (it.language == "it") "Italiano" else "English") to it.body })
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { if (p.archived) onArchive(false) else confirmArchive = true }, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("archive-property")) {
                Text(if (p.archived) "Riattiva immobile" else "Archivia immobile")
            }
        }
    }
    if (delete) AlertDialog(onDismissRequest = { delete = false }, title = { Text("Eliminare definitivamente?") }, text = { Text("Consentito soltanto senza prenotazioni, spese, attività, movimenti o rendiconti. In presenza di storico l’operazione viene bloccata: usa Archivia.") }, confirmButton = { TextButton(onClick = { delete = false; onDelete() }) { Text("Conferma eliminazione") } }, dismissButton = { TextButton(onClick = { delete = false }) { Text("Annulla") } })
    if (confirmArchive) AlertDialog(
        onDismissRequest = { confirmArchive = false }, title = { Text("Archiviare ${p.name}?") },
        text = { Text("La scheda e tutti i dati collegati rimangono conservati. Puoi riattivare l’immobile dall’archivio.") },
        confirmButton = { TextButton(onClick = { confirmArchive = false; onArchive(true) }) { Text("Conferma archiviazione") } },
        dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("Annulla") } },
    )
}

@Composable
private fun DetailSection(title: String, rows: List<Pair<String, String>>) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            rows.filter { it.second.isNotBlank() }.forEach { (label, value) ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyEditorScreen(initial: PropertyDraft, isNew: Boolean, busy: Boolean, error: String?, onBack: () -> Unit, onSave: (PropertyDraft) -> Unit) {
    var draft by rememberSaveable(initial.id, stateSaver = PropertyDraftSaver) { mutableStateOf(initial) }
    var discard by rememberSaveable { mutableStateOf(false) }
    val back = { if (draft != initial) discard = true else onBack() }
    BackHandler { if (!busy) back() }
    Scaffold(topBar = {
        TopAppBar(title = { Text(if (isNew) "Nuovo immobile" else "Modifica immobile") }, navigationIcon = { IconButton(onClick = { back() }, enabled = !busy) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Indietro") } }, actions = {
            TextButton(onClick = { onSave(draft) }, enabled = !busy, modifier = Modifier.testTag("save-property")) { Text(if (busy) "Salvataggio…" else "Salva") }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("property-error"))
            Text("Identità dell’immobile", style = MaterialTheme.typography.titleLarge)
            EditorField("Nome immobile *", draft.name, busy = busy, tag = "property-name") { draft = draft.copy(name = it) }
            EditorField("Indirizzo *", draft.address, busy = busy, tag = "property-address") { draft = draft.copy(address = it) }
            EditorField("Proprietario *", draft.ownerName, busy = busy, tag = "property-owner") { draft = draft.copy(ownerName = it) }
            EditorGroup("Contatti e operatività") {
                EditorField("Telefono proprietario", draft.ownerPhone, busy, KeyboardType.Phone) { draft = draft.copy(ownerPhone = it) }
                EditorField("Email proprietario", draft.ownerEmail, busy, KeyboardType.Email) { draft = draft.copy(ownerEmail = it) }
                EditorField("Recapiti utili", draft.usefulContacts, busy, multiline = true) { draft = draft.copy(usefulContacts = it) }
                EditorField("Posti letto", draft.beds, busy, KeyboardType.Number) { draft = draft.copy(beds = it) }
                EditorField("Ospiti massimi", draft.maxGuests, busy, KeyboardType.Number) { draft = draft.copy(maxGuests = it) }
                EditorField("Check-in standard (HH:mm)", draft.checkInTime, busy) { draft = draft.copy(checkInTime = it) }
                EditorField("Check-out standard (HH:mm)", draft.checkOutTime, busy) { draft = draft.copy(checkOutTime = it) }
                EditorField("Note operative", draft.notes, busy, multiline = true) { draft = draft.copy(notes = it) }
            }
            EditorGroup("Commissioni e servizi") {
                Text("La commissione Elledue si applica al netto dopo le commissioni OTA.", style = MaterialTheme.typography.bodyMedium)
                EditorField("Commissione Elledue (%)", draft.managementPercent, busy, KeyboardType.Decimal) { draft = draft.copy(managementPercent = it) }
                EditorField("Commissione OTA predefinita (%)", draft.otaPercent, busy, KeyboardType.Decimal) { draft = draft.copy(otaPercent = it) }
                EditorField("Commissioni per portale (Portale=percentuale)", draft.platformRates, busy, multiline = true) { draft = draft.copy(platformRates = it) }
                EditorField("Pulizie (€/ora)", draft.cleaningRate, busy, KeyboardType.Decimal) { draft = draft.copy(cleaningRate = it) }
                EditorField("Lavanderia (€/set matrimoniale)", draft.laundryRate, busy, KeyboardType.Decimal) { draft = draft.copy(laundryRate = it) }
            }
            EditorGroup("Tassa di soggiorno") {
                Text("Verifica le impostazioni secondo la disciplina comunale applicabile.", style = MaterialTheme.typography.bodyMedium)
                EditorField("Tassa (€/persona/notte)", draft.taxRate, busy, KeyboardType.Decimal) { draft = draft.copy(taxRate = it) }
                EditorField("Età minima", draft.taxMinAge, busy, KeyboardType.Number) { draft = draft.copy(taxMinAge = it) }
                EditorField("Limite notti (vuoto = nessun limite)", draft.taxMaxNights, busy, KeyboardType.Number) { draft = draft.copy(taxMaxNights = it) }
                EditorField("Esenzioni e note", draft.taxExemptions, busy, multiline = true) { draft = draft.copy(taxExemptions = it) }
            }
            EditorGroup("Istruzioni personalizzate") {
                Text("Inserisci soltanto le istruzioni reali dell’immobile. I testi restano modificabili.", style = MaterialTheme.typography.bodyMedium)
                EditorField("Istruzioni di accesso · Italiano", draft.accessItalian, busy, multiline = true) { draft = draft.copy(accessItalian = it) }
                EditorField("Access instructions · English", draft.accessEnglish, busy, multiline = true) { draft = draft.copy(accessEnglish = it) }
            }
            Text("* Campi obbligatori", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
        }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Abbandonare le modifiche?") }, text = { Text("Le modifiche non salvate andranno perse.") }, confirmButton = { TextButton(onClick = onBack) { Text("Abbandona") } }, dismissButton = { TextButton(onClick = { discard = false }) { Text("Continua modifica") } })
}

@Composable
private fun EditorGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    OutlinedCard(colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, if (expanded) "Comprimi" else "Espandi")
        }
        androidx.compose.animation.AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
        }
    }
}

@Composable
private fun EditorField(label: String, value: String, busy: Boolean, keyboard: KeyboardType = KeyboardType.Text, tag: String = label, multiline: Boolean = false, change: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = change, label = { Text(label) }, singleLine = !multiline, minLines = if (multiline) 3 else 1, enabled = !busy, keyboardOptions = KeyboardOptions(keyboardType = keyboard), modifier = Modifier.fillMaxWidth().testTag(tag))
}
