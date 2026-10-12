package it.elledue.control.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.elledue.control.data.*
import it.elledue.control.domain.*
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun propertyColor(id: String): Color {
    val colors = listOf(Color(0xFF48658A), Color(0xFFAD8242), Color(0xFF536376), Color(0xFF7B8794))
    return colors[(id.hashCode().toLong().let { if (it < 0) -it else it } % colors.size).toInt()]
}

@Composable
fun BookingRow(item: ReservationEntity, propertyName: String, onOpen: () -> Unit) {
    val today = BookingDates.today().toEpochDay()
    val cancelled = item.status == BookingStatus.CANCELLED.name
    OutlinedCard(onClick = onOpen, modifier = Modifier.fillMaxWidth().testTag("booking-row-${item.id}"), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).background(propertyColor(item.propertyId), CircleShape))
                Text(propertyName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${item.guestFirstName} ${item.guestLastName}", style = MaterialTheme.typography.titleLarge)
            Text("${BookingDates.display(item.checkInEpochDay)} → ${BookingDates.display(item.checkOutEpochDay)}", style = MaterialTheme.typography.bodyMedium)
            Text("${item.guestCount} ospiti · ${item.platform} · ${BookingStatus.valueOf(item.status).label}", style = MaterialTheme.typography.bodySmall)
            if (!cancelled && item.status != BookingStatus.COMPLETED.name && today >= item.checkInEpochDay && today < item.checkOutEpochDay) Text("Soggiorno in corso", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge)
            if (item.overlapAcknowledged && !cancelled) Text("Sovrapposizione autorizzata al salvataggio", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Lordo ${Amounts.euro(item.grossCents)}", style = MaterialTheme.typography.bodyMedium)
                Text("Elledue ${Amounts.euro(item.managementCents)}", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationsScreen(records: List<ReservationEntity>, properties: List<PropertyWithOwner>, onNew: () -> Unit, onOpen: (String) -> Unit) {
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(BookingDates.today()).toString()) }
    var selectedDay by rememberSaveable { mutableLongStateOf(BookingDates.today().toEpochDay()) }
    var propertyFilter by rememberSaveable { mutableStateOf("") }
    var listMode by rememberSaveable { mutableStateOf(false) }
    var showCancelled by rememberSaveable { mutableStateOf(false) }
    val month = YearMonth.parse(monthText)
    val start = month.atDay(1).toEpochDay()
    val end = month.plusMonths(1).atDay(1).toEpochDay()
    val filtered = records.filter { (propertyFilter.isBlank() || it.propertyId == propertyFilter) && (showCancelled || it.status != BookingStatus.CANCELLED.name) }
    // Include departure markers on day 1 of the month, even for a stay that ended overnight.
    val inMonth = filtered.filter { it.checkInEpochDay < end && it.checkOutEpochDay >= start }.sortedBy { it.checkInEpochDay }
    val today = BookingDates.today().toEpochDay()
    Scaffold(topBar = { TopAppBar(title = { Text("Calendario") }, actions = { IconButton(onClick = onNew, enabled = properties.any { !it.property.archived }, modifier = Modifier.testTag("new-booking")) { Icon(Icons.Outlined.Add, "Nuova prenotazione") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("bookings-scroll"), contentPadding = PaddingValues(20.dp, 10.dp, 20.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { PropertySelector(properties, propertyFilter, allOption = true) { propertyFilter = it } }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!listMode, { listMode = false }, label = { Text("Calendario") }, modifier = Modifier.testTag("booking-calendar-mode"))
                FilterChip(listMode, { listMode = true }, label = { Text("Elenco") }, modifier = Modifier.testTag("booking-list-mode"))
                FilterChip(showCancelled, { showCancelled = !showCancelled }, label = { Text("Annullate") }, modifier = Modifier.testTag("booking-cancelled-filter"))
            } }
            item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { monthText = month.minusMonths(1).toString(); selectedDay = month.minusMonths(1).atDay(1).toEpochDay() }, modifier = Modifier.testTag("previous-month")) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Mese precedente") }
                Text(month.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALY)).replaceFirstChar { it.titlecase(Locale.ITALY) }, style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { monthText = month.plusMonths(1).toString(); selectedDay = month.plusMonths(1).atDay(1).toEpochDay() }, modifier = Modifier.testTag("next-month")) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Mese successivo") }
            } }
            item { TextButton(onClick = { monthText = YearMonth.from(BookingDates.today()).toString(); selectedDay = today }, modifier = Modifier.testTag("calendar-today")) { Text("Oggi · ${BookingDates.display(today)}") } }
            if (!listMode) {
                item { MonthGrid(month, selectedDay, today, inMonth.filter { it.status != BookingStatus.CANCELLED.name }) { selectedDay = it } }
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        properties.filter { property -> inMonth.any { it.propertyId == property.property.id } }.forEach { property ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.size(8.dp).background(propertyColor(property.property.id), CircleShape))
                                Text(property.property.name, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                item { Text("● soggiorno · A arrivo · P partenza", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                item { Text("${BookingDates.display(selectedDay)} · agenda", style = MaterialTheme.typography.titleMedium) }
                val daily = inMonth.filter { selectedDay >= it.checkInEpochDay && selectedDay <= it.checkOutEpochDay }
                if (daily.isEmpty()) item { Text("Nessun soggiorno, arrivo o partenza per il giorno selezionato.", style = MaterialTheme.typography.bodyMedium) }
                items(daily, key = { it.id }) { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (item.status == BookingStatus.CANCELLED.name) "Prenotazione annullata" else when (selectedDay) { item.checkInEpochDay -> "Arrivo · ${item.expectedArrivalTime}"; item.checkOutEpochDay -> "Partenza · ${item.expectedDepartureTime}"; else -> "Soggiorno" }, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                        BookingRow(item, properties.firstOrNull { it.property.id == item.propertyId }?.property?.name ?: "Immobile") { onOpen(item.id) }
                    }
                }
            } else {
                if (inMonth.isEmpty()) item { EmptyState("Un calendario pronto ad accoglierti", "Le prenotazioni del mese appariranno qui. Aggiungi una prenotazione per iniziare.") }
                items(inMonth, key = { it.id }) { item -> BookingRow(item, properties.firstOrNull { it.property.id == item.propertyId }?.property?.name ?: "Immobile") { onOpen(item.id) } }
            }
            if (properties.isEmpty()) item { Text("Crea prima un immobile nella sezione Immobili.", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun MonthGrid(month: YearMonth, selected: Long, today: Long, records: List<ReservationEntity>, select: (Long) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row { listOf("L", "M", "M", "G", "V", "S", "D").forEach { Text(it, modifier = Modifier.weight(1f).padding(8.dp), style = MaterialTheme.typography.labelMedium) } }
            val offset = month.atDay(1).dayOfWeek.value - 1
            val weeks = (offset + month.lengthOfMonth() + 6) / 7
            repeat(weeks) { week ->
                Row {
                    repeat(7) { weekday ->
                        val day = week * 7 + weekday - offset + 1
                        if (day !in 1..month.lengthOfMonth()) Spacer(Modifier.weight(1f).height(64.dp)) else {
                            val epoch = month.atDay(day).toEpochDay()
                            val bookings = records.filter { epoch >= it.checkInEpochDay && epoch <= it.checkOutEpochDay }
                            val arrivals = bookings.count { it.checkInEpochDay == epoch }
                            val departures = bookings.count { it.checkOutEpochDay == epoch }
                            val occupied = bookings.filter { epoch < it.checkOutEpochDay }
                            Column(Modifier.weight(1f).height(64.dp).background(if (epoch == selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent, MaterialTheme.shapes.small).clickable { select(epoch) }.testTag("calendar-day-$epoch").semantics { contentDescription = "${BookingDates.display(epoch)}, $arrivals arrivi, $departures partenze, ${occupied.size} soggiorni" }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(day.toString(), fontWeight = if (epoch == today || epoch == selected) FontWeight.Bold else FontWeight.Normal, color = if (epoch == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { occupied.map { it.propertyId }.distinct().take(3).forEach { Box(Modifier.width(8.dp).height(4.dp).background(propertyColor(it), CircleShape)) } }
                                if (arrivals > 0 || departures > 0) Text(listOfNotNull(if (arrivals > 0) "A$arrivals" else null, if (departures > 0) "P$departures" else null).joinToString(" "), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(item: ReservationEntity, propertyName: String, busy: Boolean, error: String?, onBack: () -> Unit, onEdit: () -> Unit, onCancel: () -> Unit, onTax: () -> Unit = {}, onExpense: (String) -> Unit = {}, onActivities: () -> Unit = {}, financialChanges: List<it.elledue.control.data.BookingFinancialChangeEntity> = emptyList()) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text("Scheda prenotazione") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Indietro") } }, actions = { TextButton(onClick = onEdit, enabled = !busy) { Text("Modifica") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("${item.guestFirstName} ${item.guestLastName}", style = MaterialTheme.typography.headlineLarge)
            Text(propertyName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            Text(BookingStatus.valueOf(item.status).label, modifier = Modifier.testTag("booking-status"))
            Text("${BookingDates.display(item.checkInEpochDay)} → ${BookingDates.display(item.checkOutEpochDay)} · ${item.checkOutEpochDay - item.checkInEpochDay} notti")
            Text("${item.guestCount} ospiti · ${item.platform}")
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Arrivo previsto: ${item.expectedArrivalTime}")
                    Text("Arrivo effettivo: ${item.actualArrivalTime ?: "Non registrato"}")
                    Text("Partenza prevista: ${item.expectedDepartureTime}")
                    Text("Partenza effettiva: ${item.actualDepartureTime ?: "Non registrata"}")
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onExpense("OTHER") }, modifier = Modifier.testTag("booking-add-expense")) { Text("Spesa") }
                OutlinedButton(onClick = { onExpense("CLEANING") }, modifier = Modifier.testTag("booking-add-cleaning")) { Text("Pulizia") }
                OutlinedButton(onClick = { onExpense("LAUNDRY") }) { Text("Lavanderia") }
                OutlinedButton(onClick = onActivities) { Text("Attività") }
            }
            FinanceCard(item.grossCents, BookingTotals(item.otaCents, item.netCents, item.managementCents), actualOta = item.actualOtaCents != null)
            Text("OTA ${Amounts.editable(item.appliedOtaBasisPoints.toLong())}% · Elledue ${Amounts.editable(item.appliedManagementBasisPoints.toLong())}% sul netto. Parametri storici conservati.", style = MaterialTheme.typography.bodySmall)
            financialChanges.filter { it.reservationId == item.id }.forEach { change ->
                val before = org.json.JSONObject(change.previousJson); val after = org.json.JSONObject(change.currentJson)
                Text("Rettifica: ${change.reason}\nOTA ${Amounts.euro(before.getLong("otaCents"))} → ${Amounts.euro(after.getLong("otaCents"))} · Elledue ${Amounts.euro(before.getLong("managementCents"))} → ${Amounts.euro(after.getLong("managementCents"))}", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onTax, modifier = Modifier.fillMaxWidth().testTag("open-booking-tax")) { Text("Tassa di soggiorno e movimenti") }
            Text("Il lordo è accreditato al proprietario. Il compenso maturato non indica un pagamento già ricevuto da Elledue.", style = MaterialTheme.typography.bodyMedium)
            if (item.overlapAcknowledged && item.status != BookingStatus.CANCELLED.name) Text("Sovrapposizione autorizzata al salvataggio.", color = MaterialTheme.colorScheme.error)
            if (item.notes.isNotBlank()) { Text("Note", style = MaterialTheme.typography.titleMedium); Text(item.notes) }
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            if (item.status != BookingStatus.CANCELLED.name) OutlinedButton(onClick = { confirm = true }, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("cancel-booking")) { Text("Annulla prenotazione") }
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Annullare la prenotazione?") }, text = { Text("Lo storico e gli importi restano conservati. Il soggiorno libera le date e non entra nei totali delle prenotazioni attive.") }, confirmButton = { TextButton(onClick = { confirm = false; onCancel() }) { Text("Conferma annullamento") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Indietro") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingEconomyScreen(records: List<ReservationEntity>) {
    val active = records.filter { it.status != BookingStatus.CANCELLED.name }
    fun total(values: List<Long>): String = NumberFormat.getCurrencyInstance(Locale.ITALY).format(values.fold(BigDecimal.ZERO) { sum, value -> sum + BigDecimal.valueOf(value, 2) })
    Scaffold(topBar = { TopAppBar(title = { Text("Economia") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Economia delle prenotazioni", style = MaterialTheme.typography.headlineMedium)
            Text("Tutte le prenotazioni non annullate, senza attribuzione ai rendiconti mensili.", style = MaterialTheme.typography.bodyMedium)
            listOf("Lordo accreditato ai proprietari" to active.map { it.grossCents }, "Commissioni OTA" to active.map { it.otaCents }, "Netto dopo OTA" to active.map { it.netCents }, "Compenso Elledue maturato" to active.map { it.managementCents }).forEach { (label, values) ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(label, style = MaterialTheme.typography.labelLarge); Text(total(values), style = MaterialTheme.typography.headlineMedium) } }
            }
            Text("Il compenso maturato non è un incasso. Spese, compensazioni, pagamenti e rendiconti saranno aggiunti nelle milestone successive.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
