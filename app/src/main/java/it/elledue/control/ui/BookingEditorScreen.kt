package it.elledue.control.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import it.elledue.control.data.PropertyWithOwner
import it.elledue.control.domain.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingEditorScreen(initial: BookingDraft, properties: List<PropertyWithOwner>, isNew: Boolean, busy: Boolean, error: String?, pending: PendingOverlap?, onBack: () -> Unit, onSave: (BookingDraft) -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val draftKey=if(isNew)"booking/new" else "booking/${initial.id}/edit"
    var draft by rememberPersistentDraft(draftKey,initial)
    var correction by rememberSaveable{mutableStateOf(false)}
    var correctionText by rememberPersistentDraft("$draftKey#correction", "")
    val focus=androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController=androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val scroll=rememberScrollState()
    LaunchedEffect(error){if(error!=null)scroll.animateScrollTo(0)}
    LaunchedEffect(correction){if(correction)scroll.scrollTo(0)}
    fun prepared()=if(isNew && draft.taxableGuestCount.isBlank()) draft.copy(taxableGuestCount=draft.guests) else draft
    fun save(){
        draft=prepared()
        focus.clearFocus();keyboardController?.hide()
        val financial=!isNew && listOf(draft.gross,draft.otaPercent,draft.actualOta,draft.managementPercent,draft.otaTreatment,draft.otaVatBasisPoints)!=listOf(initial.gross,initial.otaPercent,initial.actualOta,initial.managementPercent,initial.otaTreatment,initial.otaVatBasisPoints)
        if(financial && draft.correctionReason.isBlank()){correction=true;return}
        onSave(draft)
    }
    var discard by rememberSaveable { mutableStateOf(false) }
    val back = { if (draft != initial) discard = true else onBack() }
    val enabled = !busy && pending == null
    val selected = properties.firstOrNull { it.property.id == draft.propertyId }
    BackHandler { if (enabled) back() }
    Scaffold(topBar = { TopAppBar(title = { Text(if (isNew) "Nuova prenotazione" else "Modifica prenotazione") }, navigationIcon = { IconButton(onClick = { back() }, enabled = enabled) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Indietro") } }, actions = {
        TextButton(onClick = { save() }, enabled = enabled, modifier = Modifier.testTag("save-booking")) { Text(if (busy) "Salvataggio…" else "Salva") }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(scroll).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("booking-error"))
            if(correction)OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Text("Motiva la rettifica economica",style=MaterialTheme.typography.titleMedium,color=ElledueGold)
                Text("Gli importi precedenti resteranno nello storico. Il salvataggio richiede un motivo.")
                OutlinedTextField(correctionText,{correctionText=it},label={Text("Motivo")},singleLine=true,modifier=Modifier.fillMaxWidth().testTag("booking-correction-dialog"))
                Button(onClick={draft=draft.copy(correctionReason=correctionText);correction=false;onSave(draft)},enabled=correctionText.isNotBlank() && enabled){Text("Salva rettifica")}
                TextButton(onClick={correction=false}){Text("Continua a modificare")}
            }}
            DraftBanner(draftKey){draft=initial}
            Text("Soggiorno", style = MaterialTheme.typography.titleLarge)
            PropertySelector(properties.filter { !it.property.archived || it.property.id == initial.propertyId }, draft.propertyId, enabled) { id ->
                val item = properties.first { it.property.id == id }
                draft = if (isNew) draft.withPropertyDefaults(item) else draft.copy(propertyId = id,businessType=item.property.businessType,managementPercent=if(item.property.businessType=="OWNED")"0" else draft.managementPercent)
            }
            if (!isNew) Text("Le percentuali salvate restano invariate. Puoi rettificarle esplicitamente qui.", style = MaterialTheme.typography.bodySmall)
            BookingField("Nome ospite *", draft.firstName, enabled, tag = "booking-first-name") { draft = draft.copy(firstName = it) }
            BookingField("Cognome ospite *", draft.lastName, enabled, tag = "booking-last-name") { draft = draft.copy(lastName = it) }
            BookingField("Ospiti totali", draft.guests, enabled, KeyboardType.Number, "booking-guests") {
                val exempt=draft.guests.toIntOrNull()?.let{total->draft.taxableGuestCount.toIntOrNull()?.let{total-it}} ?: 0
                draft = draft.copy(guests = it,exemptGuestCount="",taxableGuestCount=it.toIntOrNull()?.let{total->(total-exempt).coerceAtLeast(0).toString()} ?: "")
            }
            val exempt=runCatching{draft.guests.toInt()-(draft.taxableGuestCount.ifBlank{if(isNew)draft.guests else ""}).toInt()}.getOrNull()?.toString() ?: ""
            BookingField("Ospiti esenti · soggetti dai ${selected?.property?.touristTaxMinAge ?: 16} anni compiuti",exempt,enabled,KeyboardType.Number,"booking-exempt-guests"){value->draft=draft.copy(exemptGuestCount=value,taxableGuestCount=runCatching{(draft.guests.toInt()-value.toInt()).toString()}.getOrDefault(""))}
            val taxPreview=runCatching{val subjects=draft.taxableGuestCount.ifBlank{if(isNew)draft.guests else ""}.toInt();val total=draft.guests.toInt();require(subjects in 0..total);val n=(BookingDates.parse(draft.checkOut).toEpochDay()-BookingDates.parse(draft.checkIn).toEpochDay());require(n>0);val p=selected!!.property;java.math.BigDecimal.valueOf(p.touristTaxNightCents).multiply(java.math.BigDecimal.valueOf(subjects.toLong())).multiply(java.math.BigDecimal.valueOf(minOf(n,p.touristTaxMaxNights?.toLong() ?: n))).longValueExact()}.getOrNull()
            Text(taxPreview?.let{"Tassa prevista ${Amounts.euro(it)} · nessun incasso automatico"} ?: "Previsione da completare: indica gli ospiti soggetti, senza inserire età individuali.",style=MaterialTheme.typography.bodySmall)
            if(draft.businessType=="MANAGED") {
                Text(if(draft.automaticTaxAdvance)"Tassa riconosciuta automaticamente nel saldo" else "Nessun anticipo automatico per la tassa",style=MaterialTheme.typography.bodySmall)
                Text("Riconoscimento convenzionale: nessun incasso o riversamento viene registrato.",style=MaterialTheme.typography.bodySmall)
                var exception by rememberSaveable{mutableStateOf(false)}
                TextButton(onClick={exception=!exception}){Text(if(exception)"Chiudi eccezione tassa" else "Eccezione tassa / anticipo")}
                if(exception) {
                Row {Checkbox(draft.automaticTaxAdvance,{draft=draft.copy(automaticTaxAdvance=it,taxAdvanceManual=true)},enabled=enabled);Text("Tassa riconosciuta automaticamente nel saldo",Modifier.weight(1f))}
                BookingField("Anticipo diverso (€) · vuoto = automatico",draft.taxAdvanceOverride,enabled,KeyboardType.Decimal,"booking-tax-advance"){draft=draft.copy(taxAdvanceOverride=it,taxAdvanceManual=true)}
                }
            }
            if(draft.businessType=="OWNED")Text("Immobile proprio: nessun compenso di gestione automatico.",color=ElledueGold)
            PortalSelector(draft.platform,enabled) { value ->
                draft=draft.copy(platform=value,otaTreatment=if(isNew && !draft.otaManual && selected!=null)BookingDraft.otaTreatmentDefault(selected,value) else draft.otaTreatment,otaPercent=if(isNew && !draft.otaManual && selected!=null) BookingDraft.otaDefault(selected,value) else draft.otaPercent)
            }
            if(isNew && draft.otaPercent.isBlank()) Text("Percentuale del portale non configurata. Inseriscila qui oppure nelle commissioni dell’immobile; puoi usare l’importo OTA effettivo.",color=ElledueGold)
            StayRangeField(draft.checkIn,draft.checkOut,enabled){start,end->draft=draft.copy(checkIn=start,checkOut=end)}
            val nights = runCatching { BookingDates.parse(draft.checkOut).toEpochDay() - BookingDates.parse(draft.checkIn).toEpochDay() }.getOrNull()
            if (nights != null && nights > 0) Text("$nights notti" + if (nights < 2) " · soggiorno breve rispetto al riferimento di 2 notti" else "", style = MaterialTheme.typography.labelMedium)
            Text("Orari", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BookingField("Arrivo previsto", draft.expectedArrival, enabled, modifier = Modifier.weight(1f)) { draft = draft.copy(expectedArrival = it) }
                BookingField("Partenza prevista", draft.expectedDeparture, enabled, modifier = Modifier.weight(1f)) { draft = draft.copy(expectedDeparture = it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BookingField("Arrivo effettivo", draft.actualArrival, enabled, modifier = Modifier.weight(1f)) { draft = draft.copy(actualArrival = it) }
                BookingField("Partenza effettiva", draft.actualDeparture, enabled, modifier = Modifier.weight(1f)) { draft = draft.copy(actualDeparture = it) }
            }
            Text("Usa HH:mm. Gli orari effettivi sono facoltativi.", style = MaterialTheme.typography.bodySmall)
            HorizontalDivider()
            Text("Economia della prenotazione", style = MaterialTheme.typography.titleLarge)
            Text(if(draft.businessType=="OWNED")"Ricavo dell’immobile proprio: nessun compenso di gestione automatico." else "Pagamento accreditato al proprietario. Elledue matura il compenso sul netto dopo OTA.", style = MaterialTheme.typography.bodyMedium)
            BookingField("Lordo (€) *", draft.gross, enabled, KeyboardType.Decimal, "booking-gross") { draft = draft.copy(gross = it) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BookingField("OTA (%)", draft.otaPercent, enabled, KeyboardType.Decimal, "booking-ota-percent", Modifier.weight(1f)) { draft = draft.copy(otaPercent = it,otaManual=true) }
                BookingField("Elledue (%)", draft.managementPercent, enabled && draft.businessType!="OWNED", KeyboardType.Decimal, "booking-management-percent", Modifier.weight(1f)) { draft = draft.copy(managementPercent = it) }
            }
            BookingField("OTA effettiva (€) · facoltativa", draft.actualOta, enabled, KeyboardType.Decimal, "booking-actual-ota") { draft = draft.copy(actualOta = it,otaTreatment=if(it.isNotBlank() && draft.actualOta.isBlank())"INCLUDED" else draft.otaTreatment,otaManual=true) }
            OtaTreatmentField(draft.otaTreatment,enabled){draft=draft.copy(otaTreatment=it,otaManual=true)}
            if(draft.otaTreatment=="BASE_PLUS_VAT")Text("IVA ${Amounts.editable(draft.otaVatBasisPoints.toLong())}% sulla sola commissione, non sul lordo.",style=MaterialTheme.typography.bodySmall)
            Text("Se compilata, anche con zero, la commissione OTA effettiva prevale sulla percentuale.", style = MaterialTheme.typography.bodySmall)
            val totals = runCatching { draft.totals() }
            if (totals.isSuccess) FinanceCard(Amounts.parseCents(draft.gross), totals.getOrThrow(), "Anteprima aggiornata", draft.actualOta.isNotBlank())
            else if (draft.gross.isNotBlank()) Text(totals.exceptionOrNull()?.message ?: "Controlla gli importi.", color = MaterialTheme.colorScheme.error)
            if(selected!=null) {
                val forecast=runCatching{val guests=draft.guests.toLong();require(guests>0);val p=selected.property;val sets=(guests+p.laundryGuestsPerSet-1)/p.laundryGuestsPerSet;OperationalMoney.cost("2",p.cleaningHourlyCents) to Math.multiplyExact(sets,p.laundrySetCents)}.getOrNull()
                forecast?.let{(clean,laundry)->Text(if(draft.businessType=="MANAGED")"Standard proposti · pulizie ${Amounts.euro(clean)} · lavanderia ${Amounts.euro(laundry)}" else "Lavanderia proposta · ${Amounts.euro(laundry)} · costo privato",style=MaterialTheme.typography.bodySmall,color=ElledueGold)}
            }
            if (!isNew) BookingField("Motivo rettifica economica", draft.correctionReason, enabled, tag = "booking-correction-reason") { draft = draft.copy(correctionReason = it) }
            BookingField("Note", draft.notes, enabled, multiline = true) { draft = draft.copy(notes = it) }
            Text("Stato", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BookingStatus.entries.filter { it != BookingStatus.CANCELLED || !isNew }.forEach { status ->
                    FilterChip(selected = draft.status == status.name, onClick = { draft = draft.copy(status = status.name) }, enabled = enabled, label = { Text(status.label) })
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Abbandonare le modifiche?") }, text = { Text("La bozza locale rimane disponibile in Home → Riprendi bozze.") }, confirmButton = { TextButton(onClick = onBack) { Text("Abbandona") } }, dismissButton = { TextButton(onClick = { discard = false }) { Text("Continua modifica") } })
    if (pending != null) AlertDialog(onDismissRequest = onDismiss, title = { Text("Sovrapposizione rilevata") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Le date confliggono con queste prenotazioni. Conferma soltanto dopo aver verificato l’incompatibilità:")
            pending.conflicts.forEach { Text("${it.guestFirstName} ${it.guestLastName} · ${BookingDates.display(it.checkInEpochDay)} → ${BookingDates.display(it.checkOutEpochDay)}") }
        }
    }, confirmButton = { TextButton(onClick = onConfirm) { Text("Conferma sovrapposizione") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Correggi date") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertySelector(properties: List<PropertyWithOwner>, selectedId: String?, enabled: Boolean = true, allOption: Boolean = false, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val name = properties.firstOrNull { it.property.id == selectedId }?.property?.name ?: if (allOption) "Tutti gli immobili" else "Seleziona appartamento"
    ExposedDropdownMenuBox(expanded && enabled, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(name, {}, readOnly = true, enabled = enabled, label = { Text("Immobile") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth().testTag("booking-property-selector"))
        ExposedDropdownMenu(expanded && enabled, onDismissRequest = { expanded = false }) {
            if (allOption) DropdownMenuItem(text = { Text("Tutti gli immobili") }, onClick = { expanded = false; onSelect("") })
            properties.forEach { item -> DropdownMenuItem(text = { Text(item.property.name + if (item.property.archived) " · archiviato" else "") }, onClick = { expanded = false; onSelect(item.property.id) }, modifier = Modifier.testTag("select-property-${item.property.id}")) }
        }
    }
}

@Composable
private fun BookingField(label: String, value: String, enabled: Boolean, keyboard: KeyboardType = KeyboardType.Text, tag: String = label, modifier: Modifier = Modifier, multiline: Boolean = false, change: (String) -> Unit) {
    OutlinedTextField(value, change, label = { Text(label) }, singleLine = !multiline, minLines = if (multiline) 3 else 1, enabled = enabled, keyboardOptions = KeyboardOptions(keyboardType = keyboard), modifier = modifier.fillMaxWidth().testTag(tag))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDateField(label: String, value: String, enabled: Boolean, tag: String, modifier: Modifier, change: (String) -> Unit) {
    var show by rememberSaveable { mutableStateOf(false) }
    // Editable text supplements the picker and supports quick entry and accessible keyboards.
    OutlinedTextField(value, change, label = { Text(label) }, singleLine = true, enabled = enabled, trailingIcon = { IconButton(onClick = { show = true }, enabled = enabled) { Icon(Icons.Outlined.CalendarMonth, "Seleziona $label") } }, modifier = modifier.testTag(tag))
    if (show) {
        val initial = runCatching { val date = BookingDates.parse(value); require(date.year in 1900..2100); BookingDates.pickerMillis(date) }.getOrNull()
        val picker = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(onDismissRequest = { show = false }, confirmButton = { TextButton(onClick = { picker.selectedDateMillis?.let { change(BookingDates.fromPicker(it).format(BookingDates.inputFormat)) }; show = false }, enabled = picker.selectedDateMillis != null) { Text("Conferma data") } }, dismissButton = { TextButton(onClick = { show = false }) { Text("Annulla") } }) { DatePicker(picker) }
    }
}

@Composable
fun FinanceCard(gross: Long, totals: BookingTotals, title: String = "Riepilogo economico", actualOta: Boolean = false) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            listOf("Importo prenotazione" to gross, (if (actualOta) "Costo OTA effettivo" else "Costo OTA complessivo") to totals.otaCents, "Netto dopo OTA" to totals.netCents).forEach { (label, cents) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(Amounts.euro(cents)) } }
            HorizontalDivider()
            Text("Compenso di gestione Elledue", style = MaterialTheme.typography.labelLarge)
            Text(Amounts.euro(totals.managementCents), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("booking-fee"))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun StayRangeField(start:String,end:String,enabled:Boolean,onChange:(String,String)->Unit) {
 var show by rememberSaveable{mutableStateOf(false)}
 val focus=androidx.compose.ui.platform.LocalFocusManager.current
 val keyboard=androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
  listOf("Check-in" to start,"Check-out" to end).forEachIndexed{i,(label,value)->
   Box(Modifier.weight(1f).heightIn(min=56.dp)) {
    OutlinedTextField(value,{},readOnly=true,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth())
    Box(Modifier.matchParentSize().clickable(enabled=enabled){focus.clearFocus();keyboard?.hide();show=true}.testTag(if(i==0)"booking-check-in" else "booking-check-out").semantics(mergeDescendants=true){contentDescription="Seleziona $label · $value";role=Role.Button})
   }
  }
 }
 if(show) {
  val picker=rememberDateRangePickerState(
   initialSelectedStartDateMillis=runCatching{BookingDates.pickerMillis(BookingDates.parse(start))}.getOrNull(),
   initialSelectedEndDateMillis=runCatching{BookingDates.pickerMillis(BookingDates.parse(end))}.getOrNull())
  val valid=picker.selectedStartDateMillis!=null && picker.selectedEndDateMillis!=null && picker.selectedEndDateMillis!!>picker.selectedStartDateMillis!!
  DatePickerDialog(onDismissRequest={show=false},confirmButton={TextButton(enabled=valid,onClick={onChange(BookingDates.fromPicker(picker.selectedStartDateMillis!!).format(BookingDates.inputFormat),BookingDates.fromPicker(picker.selectedEndDateMillis!!).format(BookingDates.inputFormat));show=false}){Text("Conferma soggiorno")}},dismissButton={TextButton(onClick={show=false}){Text("Annulla")}}) {
   Column(Modifier.heightIn(max=560.dp)) {
    if(valid)Text("${(picker.selectedEndDateMillis!!-picker.selectedStartDateMillis!!)/86400000} notti",Modifier.padding(16.dp))
    DateRangePicker(picker,modifier=Modifier.weight(1f).testTag("stay-range-picker"),title={Text("Check-in e check-out",Modifier.padding(16.dp))},showModeToggle=true)
   }
  }
 }
}
