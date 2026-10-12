package it.elledue.control.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import it.elledue.control.data.*
import kotlinx.coroutines.launch

@Composable
fun ActivitiesScreen(repository: ActivityRepository, activities: List<ActivityEntity>, properties: List<PropertyWithOwner>, bookings: List<ReservationEntity>, bookingId: String?, back: () -> Unit) {
    val booking = bookings.firstOrNull { it.id == bookingId }
    var title by rememberSaveable { mutableStateOf("") }; var property by rememberSaveable(bookingId) { mutableStateOf(booking?.propertyId ?: "") }
    var busy by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }; val scope = rememberCoroutineScope()
    LazyColumn(Modifier.fillMaxSize().imePadding().testTag("activities-scroll"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { TextButton(onClick = back) { Text("Indietro") }; Text("La tua operatività", style = MaterialTheme.typography.headlineLarge); Text("Attività locali · nessuna notifica di sistema o sincronizzazione staff", style = MaterialTheme.typography.bodySmall) }
        if (booking != null) item { Text("${booking.guestFirstName} ${booking.guestLastName} · prenotazione collegata") }
        item { PropertySelector(properties, property, allOption = booking == null, enabled = booking == null && !busy) { property = it } }
        item { OutlinedTextField(title, { title = it }, label = { Text("Nuova attività") }, modifier = Modifier.fillMaxWidth().testTag("activity-title")) }
        item { Button(onClick = { busy = true; scope.launch { try { repository.save(ActivityEntity(java.util.UUID.randomUUID().toString(), property.takeIf { it.isNotBlank() }, title, null, reservationId = booking?.id)); title = ""; error = null } catch (_: Exception) { error = "Attività non salvata. Controlla titolo e collegamento." } finally { busy = false } } }, enabled = !busy && title.isNotBlank(), modifier = Modifier.testTag("save-activity")) { Text("Aggiungi attività") } }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        val shown = activities.filter { bookingId == null || it.reservationId == bookingId }
        if (shown.isEmpty()) item { Text("Nessuna attività registrata.") }
        items(shown, key = { it.id }) { task -> ListItem(headlineContent = { Text(task.title) }, supportingContent = { Text(properties.firstOrNull { it.property.id == task.propertyId }?.property?.name ?: "Gestione generale") }, leadingContent = { Checkbox(task.completed, { completed -> scope.launch { try { repository.save(task.copy(completed = completed)) } catch (_: Exception) { error = "Stato non aggiornato." } } }) }) }
    }
}
