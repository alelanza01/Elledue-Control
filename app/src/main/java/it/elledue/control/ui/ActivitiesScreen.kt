package it.elledue.control.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import it.elledue.control.data.*
import it.elledue.control.domain.BookingDates
import kotlinx.coroutines.launch
import java.time.*

@Composable
fun ActivitiesScreen(repository: ActivityRepository, activities: List<ActivityEntity>, properties: List<PropertyWithOwner>, bookings: List<ReservationEntity>, bookingId: String?, expenses: List<ExpenseEntity> = emptyList(), open: (String)->Unit = {}, back: () -> Unit) {
    val booking = bookings.firstOrNull { it.id == bookingId }; val today=BookingDates.today()
    var filter by rememberSaveable { mutableIntStateOf(0) }; var edit by rememberSaveable { mutableStateOf(bookingId!=null) }
    var title by rememberSaveable { mutableStateOf("") }; var property by rememberSaveable(bookingId) { mutableStateOf(booking?.propertyId ?: "") }
    var busy by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }; val scope = rememberCoroutineScope()
    val day=if(filter==2) today.plusDays(1) else today
    val selectedBookings=bookings.filter { it.status!="CANCELLED" && (bookingId==null || it.id==bookingId) }
    LazyColumn(Modifier.fillMaxSize().imePadding().testTag("activities-scroll"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ConceptHeading("Attività","La tua operatività") { IconButton(onClick={edit=!edit}) { Icon(Icons.Outlined.Add,"Nuova attività",tint=ElledueGold) } };TextButton(onClick=back) { Text("Indietro") } }
        item { Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)) { listOf("Tutte","Oggi","Domani","Scadenze").forEachIndexed { i,label -> ConceptPill(filter==i,{filter=i},label={Text(label)}) } } }
        if (booking != null) item { Text("${booking.guestFirstName} ${booking.guestLastName} · prenotazione collegata",style=MaterialTheme.typography.bodySmall,color=ConceptMuted) }
        if(edit) {
            item { ConceptPropertySelector(properties, property, allOption = booking == null, enabled = booking == null && !busy) { property = it } }
            item { OutlinedTextField(title, { title = it }, label = { Text("Nuova attività") }, modifier = Modifier.fillMaxWidth().testTag("activity-title")) }
            item { Button(onClick = { busy = true; scope.launch { try { repository.save(ActivityEntity(java.util.UUID.randomUUID().toString(), property.takeIf { it.isNotBlank() }, title, null, reservationId = booking?.id)); title = ""; error = null } catch (_: Exception) { error = "Attività non salvata. Controlla titolo e collegamento." } finally { busy = false } } }, enabled = !busy && title.isNotBlank(), modifier = Modifier.testTag("save-activity")) { Text("Aggiungi attività") } }
        }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        if(filter!=3) {
            item { Text("${if(filter==2) "Domani" else "Oggi"} · ${BookingDates.display(day.toEpochDay())}",style=MaterialTheme.typography.titleMedium,color=ElledueGold) }
            val events=selectedBookings.flatMap { b -> listOf(b.checkInEpochDay to (b to true),b.checkOutEpochDay to (b to false)) }.filter { it.first==day.toEpochDay() }.sortedBy { if(it.second.second) it.second.first.expectedArrivalTime else it.second.first.expectedDepartureTime }
            items(events,key={ "event-${it.second.first.id}-${it.second.second}" }) { (_, pair) -> val(b,arrival)=pair
                ConceptTimelineRow(if(arrival)b.expectedArrivalTime else b.expectedDepartureTime,if(arrival)"Check-in" else "Check-out","${properties.firstOrNull { it.property.id==b.propertyId }?.property?.name} · ${b.guestLastName}",if(arrival)Icons.Outlined.Login else Icons.Outlined.Logout,if(arrival)ArrivalColor else DepartureColor) { open("booking/${b.id}") }
            }
            val cleaning=expenses.filter { !it.voided && it.kind in listOf("CLEANING","LAUNDRY") && it.dateEpochDay==day.toEpochDay() && (bookingId==null || it.reservationId==bookingId) }
            items(cleaning,key={"expense-${it.id}"}) { e -> ConceptTimelineRow("—",if(e.kind=="CLEANING") "Pulizia registrata" else "Lavanderia registrata",properties.firstOrNull { it.property.id==e.propertyId }?.property?.name ?: "Immobile",Icons.Outlined.CleaningServices,ElledueGold) { open("expense/${e.id}") } }
            if(events.isEmpty() && cleaning.isEmpty()) item { Text("Nessun arrivo, partenza o intervento registrato per questa giornata.",style=MaterialTheme.typography.bodySmall,color=ConceptMuted) }
        }
        val shown=activities.filter { task ->
            val due=task.dueEpochMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.of(task.zoneId)).toLocalDate() }
            (bookingId==null || task.reservationId==bookingId) && when(filter) { 0->true;1,2->due==day;else->due!=null && !task.completed }
        }
        item { Text(if(filter==0) "Task e comunicazioni" else "Attività registrate",style=MaterialTheme.typography.titleLarge) }
        if(shown.isEmpty()) item { Text("Nessuna attività per questo filtro.",style=MaterialTheme.typography.bodySmall,color=ConceptMuted) }
        items(shown,key={it.id}) { task ->
            val due=task.dueEpochMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.of(task.zoneId)) }
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { ConceptTimelineRow(due?.toLocalTime()?.toString()?.take(5) ?: "—",task.title,(properties.firstOrNull { it.property.id==task.propertyId }?.property?.name ?: "Gestione generale")+if(due==null) " · Da pianificare" else " · ${BookingDates.display(due.toLocalDate().toEpochDay())}",if(task.title.contains("manut",true)) Icons.Outlined.Build else Icons.Outlined.ChatBubbleOutline,if(task.title.contains("manut",true)) TaskColor else StayColor) }
                Checkbox(task.completed,{completed->scope.launch {try {repository.save(task.copy(completed=completed))}catch(_:Exception){error="Stato non aggiornato."}}})
            }
        }
        item { Text("Promemoria di sistema e sincronizzazione staff non attivi.",style=MaterialTheme.typography.bodySmall,color=ConceptMuted) }
    }
}
