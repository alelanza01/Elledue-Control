package it.elledue.control.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.sp
import it.elledue.control.data.*
import it.elledue.control.domain.*
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

fun propertyColor(id: String): Color {
    val colors = listOf(Color(0xFF5D8DFF), Color(0xFFDBBF8B), Color(0xFF4CB7B0), Color(0xFF8A73D6), Color(0xFFD96B6B), Color(0xFF3DB58A), Color(0xFFB780AD), Color(0xFF7EA5C2))
    return colors[(id.hashCode().toLong().let { if (it < 0) -it else it } % colors.size).toInt()]
}

@Composable
fun BookingRow(item: ReservationEntity, propertyName: String, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(ConceptCard).border(1.dp,ConceptBorder,MaterialTheme.shapes.medium).clickable(onClick=onOpen).testTag("booking-row-${item.id}").padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        Box(Modifier.width(3.dp).height(52.dp).background(propertyColor(item.propertyId),CircleShape))
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
            Text(propertyName,style=MaterialTheme.typography.titleMedium)
            Text("${item.guestFirstName} ${item.guestLastName}",style=MaterialTheme.typography.bodyMedium)
            Text("${BookingDates.display(item.checkInEpochDay)} → ${BookingDates.display(item.checkOutEpochDay)} · ${item.guestCount} ospiti",style=MaterialTheme.typography.bodySmall,color=ConceptMuted)
            if(item.status=="CANCELLED") Text("Annullata",color=DepartureColor,style=MaterialTheme.typography.labelSmall)
        }
        androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.AutoMirrored.Outlined.KeyboardArrowRight,null,tint=ElledueGold,modifier=Modifier.size(20.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationsScreen(records: List<ReservationEntity>, properties: List<PropertyWithOwner>, onNew: () -> Unit, onOpen: (String) -> Unit, activities:List<ActivityEntity> = emptyList(), onActivity:(String)->Unit = {}, onCopy:(String,String,Long)->Unit = {_,_,_->},onDay:(Long)->Unit = {},issues:List<PropertyIssue> = emptyList()) {
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
    Scaffold(topBar = { TopAppBar(title = { ConceptHeading("Calendario", "Prenotazioni e operatività") }, actions = { IconButton(onClick = onNew, enabled = properties.any { !it.property.archived }, modifier = Modifier.testTag("new-booking")) { Icon(Icons.Outlined.Add, "Nuova prenotazione") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("bookings-scroll"), contentPadding = PaddingValues(16.dp, 6.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            item { ConceptPropertySelector(properties, propertyFilter, allOption = true) { propertyFilter = it } }
            item { OutlinedButton(onClick={onCopy(propertyFilter,monthText,selectedDay)},modifier=Modifier.testTag("open-copy-calendar")){Text("Copia calendario") } }
            item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConceptPill(!listMode, { listMode = false }, label = { Text("Mese") }, modifier = Modifier.testTag("booking-calendar-mode"))
                ConceptPill(listMode, { listMode = true }, label = { Text("Agenda") }, modifier = Modifier.testTag("booking-list-mode"))
                ConceptPill(showCancelled, { showCancelled = !showCancelled }, label = { Text("Mostra annullate") }, modifier = Modifier.testTag("booking-cancelled-filter"))
            } }
            item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { monthText = month.minusMonths(1).toString(); selectedDay = month.minusMonths(1).atDay(1).toEpochDay() }, modifier = Modifier.testTag("previous-month")) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Mese precedente") }
                Text(month.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALY)).replaceFirstChar { it.titlecase(Locale.ITALY) }, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { monthText = month.plusMonths(1).toString(); selectedDay = month.plusMonths(1).atDay(1).toEpochDay() }, modifier = Modifier.testTag("next-month")) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Mese successivo") }
            } }
            item { TextButton(onClick = { monthText = YearMonth.from(BookingDates.today()).toString(); selectedDay = today }, modifier = Modifier.testTag("calendar-today").heightIn(min=32.dp)) { Text("Oggi · ${BookingDates.display(today)}",style=MaterialTheme.typography.labelSmall) } }
            if (!listMode) {
                item { MonthGrid(month, selectedDay, today, inMonth.filter { it.status != BookingStatus.CANCELLED.name }, propertyFilter.isNotBlank(), (activities+issues.mapNotNull{i->i.scheduledAt?.let{ActivityEntity("issue:${i.id}",i.propertyId,i.title,it,completed=!IssueRepository.open(i),kind="MAINTENANCE",hasTime=i.hasTime)}}).filter {propertyFilter.isBlank() || it.propertyId==propertyFilter},properties.associate{it.property.id to it.property.name}) { selectedDay = it;onDay(it) } }
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
                item { Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("Check-in" to ArrivalColor,"Check-out" to DepartureColor,"Soggiorno" to StayColor,"Pulizie" to ElledueGold,"Manutenzione" to TaskColor).forEach { (label,color) -> Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) { Box(Modifier.size(6.dp).background(color,CircleShape));Text(label,style=MaterialTheme.typography.labelSmall,color=ConceptMuted) } } } }
                item { Text("${BookingDates.display(selectedDay)} · Giornata operativa", style = MaterialTheme.typography.titleMedium) }
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
            val tasks=activities.filter {task -> task.dueEpochMillis?.let { val day=Instant.ofEpochMilli(it).atZone(ZoneId.of(task.zoneId)).toLocalDate().toEpochDay(); day>=start && day<end && (listMode || day==selectedDay) }==true && (propertyFilter.isBlank() || task.propertyId==propertyFilter) }
            if(tasks.isNotEmpty()) item{Text("Attività programmate",style=MaterialTheme.typography.titleMedium)}
            items(tasks,key={"task-${it.id}"}) {task -> TextButton(onClick={onActivity(task.id)},modifier=Modifier.testTag("calendar-task-${task.id}")){Text((if(task.completed) "✓ " else "○ ")+task.title+" · "+Instant.ofEpochMilli(task.dueEpochMillis!!).atZone(ZoneId.of(task.zoneId)).toLocalDate().format(BookingDates.inputFormat))} }
            if (properties.isEmpty()) item { Text("Crea prima un immobile nella sezione Immobili.", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun MonthGrid(month:YearMonth,selected:Long,today:Long,records:List<ReservationEntity>,singleProperty:Boolean,activities:List<ActivityEntity>,labels:Map<String,String>,select:(Long)->Unit) {
 val first=month.atDay(1).minusDays((month.atDay(1).dayOfWeek.value-1).toLong()).toEpochDay()
 val weeks=((month.atDay(1).dayOfWeek.value-1)+month.lengthOfMonth()+6)/7
 OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  Row{listOf("Lun","Mar","Mer","Gio","Ven","Sab","Dom").forEach{Text(it,Modifier.weight(1f),style=MaterialTheme.typography.labelSmall,color=ConceptMuted)}}
  repeat(weeks){w->
   val start=first+w*7;val end=start+7
   Column(verticalArrangement=Arrangement.spacedBy(5.dp)){
    Row{repeat(7){offset->val day=start+offset;val date=LocalDate.ofEpochDay(day)
     Box(Modifier.weight(1f).heightIn(min=48.dp).clip(MaterialTheme.shapes.small).background(if(day==selected)ElledueGold else Color.Transparent).clickable{select(day)}.testTag("calendar-day-$day").semantics{contentDescription=BookingDates.display(day)+", ${records.count{it.checkInEpochDay==day}} arrivi, ${records.count{it.checkOutEpochDay==day}} partenze, ${records.count{day>=it.checkInEpochDay && day<it.checkOutEpochDay}} soggiorni, apri giornata operativa"},contentAlignment=Alignment.Center){Text(date.dayOfMonth.toString(),color=if(day==selected)ElledueNight else if(date.month!=month.month)ConceptMuted.copy(.45f) else if(day==today)ElledueGold else MaterialTheme.colorScheme.onSurface)}
    }}
    val stays=records.filter{it.checkInEpochDay<end && it.checkOutEpochDay>=start}.sortedWith(compareBy<ReservationEntity>{it.propertyId}.thenBy{it.checkInEpochDay})
    stays.take(5).forEach{b->
     val from=maxOf(start,b.checkInEpochDay);val through=minOf(end-1,b.checkOutEpochDay);val width=(through-from+1).toInt()
     Row(Modifier.fillMaxWidth()){
      if(from>start)Spacer(Modifier.weight((from-start).toFloat()))
      Column(Modifier.weight(width.toFloat()).clip(MaterialTheme.shapes.small).background(StayColor.copy(.16f)).clickable{select(from)}.padding(horizontal=5.dp,vertical=5.dp),verticalArrangement=Arrangement.spacedBy(3.dp)){
       Row(horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(6.dp).background(propertyColor(b.propertyId),CircleShape));Text(labels[b.propertyId] ?: "Immobile",style=MaterialTheme.typography.labelSmall,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)}
       Text((if(b.checkInEpochDay in start until end)"↘ ${b.expectedArrivalTime} · " else "Soggiorno · ")+b.guestLastName+(if(b.checkOutEpochDay in start until end)" · ↗ ${b.expectedDepartureTime}" else ""),color=if(b.checkInEpochDay in start until end)ArrivalColor else if(b.checkOutEpochDay in start until end)DepartureColor else StayColor,style=MaterialTheme.typography.labelSmall,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
      }
      if(through<end-1)Spacer(Modifier.weight((end-1-through).toFloat()))
     }
    }
    if(stays.size>5)Text("+${stays.size-5} soggiorni · tocca una data per tutti i dettagli",style=MaterialTheme.typography.labelSmall)
    val tasks=activities.filter{a->a.dueEpochMillis?.let{Instant.ofEpochMilli(it).atZone(ZoneId.of(a.zoneId)).toLocalDate().toEpochDay() in start until end}==true}
    tasks.forEach{a->TextButton(onClick={select(Instant.ofEpochMilli(a.dueEpochMillis!!).atZone(ZoneId.of(a.zoneId)).toLocalDate().toEpochDay())},modifier=Modifier.heightIn(min=48.dp)){Text((if(a.kind=="CLEANING")"Pulizia" else if(a.kind=="MAINTENANCE")"Intervento" else "Attività")+" · "+a.title,style=MaterialTheme.typography.labelMedium,color=if(a.kind=="CLEANING")ElledueGold else TaskColor)}}
    HorizontalDivider(color=ConceptBorder)
   }
  }
 }}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(item: ReservationEntity, propertyName: String, busy: Boolean, error: String?, onBack: () -> Unit, onEdit: () -> Unit, onCancel: () -> Unit, onDelete:()->Unit = {}, onTax: () -> Unit = {}, onExpense: (String) -> Unit = {}, onActivities: () -> Unit = {}, onMessages: () -> Unit = {}, financialChanges: List<it.elledue.control.data.BookingFinancialChangeEntity> = emptyList(), operationalContent:@Composable ()->Unit = {}) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text("Riepilogo soggiorno") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Indietro") } }, actions = { TextButton(onClick = onEdit, enabled = !busy) { Text("Modifica") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("${item.guestFirstName} ${item.guestLastName}", style = MaterialTheme.typography.headlineLarge)
            Text(propertyName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            Text(StayStatus.label(item), modifier = Modifier.testTag("booking-status"))
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
                OutlinedButton(onClick = onMessages,modifier=Modifier.testTag("booking-messages")){Text("Messaggi IT/EN")}
            }
            operationalContent()
            FinanceCard(item.grossCents, BookingTotals(item.otaCents, item.netCents, item.managementCents), actualOta = item.actualOtaCents != null)
            Text("OTA ${Amounts.editable(item.appliedOtaBasisPoints.toLong())}% · Elledue ${Amounts.editable(item.appliedManagementBasisPoints.toLong())}% sul netto. Parametri storici conservati.", style = MaterialTheme.typography.bodySmall)
            financialChanges.filter { it.reservationId == item.id }.forEach { change ->
                val before = org.json.JSONObject(change.previousJson); val after = org.json.JSONObject(change.currentJson)
                Text("Rettifica: ${change.reason}\nOTA ${Amounts.euro(before.getLong("otaCents"))} → ${Amounts.euro(after.getLong("otaCents"))} · Elledue ${Amounts.euro(before.getLong("managementCents"))} → ${Amounts.euro(after.getLong("managementCents"))}", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onTax, modifier = Modifier.fillMaxWidth().testTag("open-booking-tax")) { Text("Tassa di soggiorno e movimenti") }
            Text(if(item.appliedBusinessType=="OWNED")"Ricavi e costi dell’immobile proprio: nessun saldo con un proprietario esterno." else "Il lordo è accreditato al proprietario. Il compenso maturato non indica un pagamento già ricevuto da Elledue.", style = MaterialTheme.typography.bodyMedium)
            if (item.overlapAcknowledged && item.status != BookingStatus.CANCELLED.name) Text("Sovrapposizione autorizzata al salvataggio.", color = MaterialTheme.colorScheme.error)
            if (item.notes.isNotBlank()) { Text("Note", style = MaterialTheme.typography.titleMedium); Text(item.notes) }
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick=onDelete,enabled=!busy,modifier=Modifier.fillMaxWidth().testTag("delete-booking")){Text("Elimina definitivamente prenotazione")}
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
            listOf("Lordo accreditato ai proprietari" to active.map { it.grossCents }, "Commissioni OTA" to active.map { it.otaCents }, "Netto dopo OTA" to active.map { it.netCents }, "Compenso di gestione Elledue" to active.map { it.managementCents }).forEach { (label, values) ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(label, style = MaterialTheme.typography.labelLarge); Text(total(values), style = MaterialTheme.typography.headlineMedium) } }
            }
            Text("Il compenso maturato non è un incasso. Spese, compensazioni, pagamenti e rendiconti saranno aggiunti nelle milestone successive.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
