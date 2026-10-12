package it.elledue.control.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.elledue.control.R
import it.elledue.control.data.*
import it.elledue.control.domain.*
import kotlinx.coroutines.*

@Composable
fun LocalPhoto(value: String?, description: String, modifier: Modifier = Modifier, crop: Boolean = true, placeholder: androidx.compose.ui.graphics.vector.ImageVector = Icons.Outlined.Apartment) {
    val image = remember(value) { value?.let { runCatching { val bytes = Base64.decode(it, Base64.NO_WRAP); BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull() } }
    if (image != null) Image(image, description, modifier, contentScale = if (crop) ContentScale.Crop else ContentScale.Fit)
    else Box(modifier.background(Brush.linearGradient(listOf(ElledueNight, Color(0xFF283C52)))), contentAlignment = Alignment.Center) {
        Icon(placeholder, description, tint = ElledueGold.copy(alpha = .7f), modifier = Modifier.size(40.dp))
    }
}

@Composable
fun PhotoControls(value: String?, avatar: Boolean = false, onPhoto: (String?) -> Unit) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    var fit by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { busy = true; error = null
            try { val photo = withContext(Dispatchers.IO) { LocalImages.import(context, uri) }; onPhoto(photo) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = "Fotografia non leggibile o troppo grande. La precedente è conservata." }
            finally { busy = false }
        }
    }
    LocalPhoto(value, "Anteprima fotografia", Modifier.fillMaxWidth().height(180.dp).clip(MaterialTheme.shapes.medium), crop = !fit, placeholder = if (avatar) Icons.Outlined.Person else Icons.Outlined.Apartment)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TextButton(onClick = { picker.launch(arrayOf("image/*")) }, enabled = !busy, modifier = Modifier.testTag("select-photo")) { Text(if (value == null) "Aggiungi foto" else "Sostituisci") }
        if (value != null) { TextButton(onClick = { onPhoto(null) }, enabled = !busy, modifier = Modifier.testTag("remove-photo")) { Text("Rimuovi") }; TextButton(onClick = { fit = !fit }) { Text(if (fit) "Copertina" else "Immagine intera") } }
    }
    Text("La foto è conservata nell’app e nel backup. La vista copertina non modifica l’immagine salvata.", style = MaterialTheme.typography.bodySmall)
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}

@Composable
fun PortfolioTile(item: PropertyWithOwner, open: () -> Unit) {
    val stacked = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 360 || androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.2f
    OutlinedCard(onClick = open, modifier = Modifier.fillMaxWidth()) {
        if (stacked) {
            LocalPhoto(item.property.coverImageBase64, "Copertina ${item.property.name}", Modifier.fillMaxWidth().height(140.dp))
            PortfolioInfo(item, Modifier.padding(16.dp))
        } else Row(Modifier.heightIn(min = 124.dp), verticalAlignment = Alignment.CenterVertically) {
            LocalPhoto(item.property.coverImageBase64, "Copertina ${item.property.name}", Modifier.width(116.dp).height(132.dp))
            PortfolioInfo(item, Modifier.weight(1f).padding(14.dp))
            Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.padding(end = 8.dp))
        }
    }
}

@Composable
private fun PortfolioInfo(item: PropertyWithOwner, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(item.property.name, style = MaterialTheme.typography.titleLarge, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Text(item.property.address, style = MaterialTheme.typography.bodySmall)
        Text("${item.property.maxGuests} ospiti · ${item.property.beds} ${if (item.property.beds == 1) "letto" else "letti"}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
        Text(item.owner.name, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ProfileScreen(settings: AppSettings?, model: SettingsViewModel, back: () -> Unit) {
    if (settings == null) { CircularProgressIndicator(); return }
    val status by model.saveState.collectAsStateWithLifecycle()
    var first by rememberSaveable(settings.operatorFirstName) { mutableStateOf(settings.operatorFirstName) }
    var last by rememberSaveable(settings.operatorLastName) { mutableStateOf(settings.operatorLastName) }
    // Persist photos immediately; don't put large base64 values in the Android saved-instance bundle.
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = back) { Text("Indietro") }
        Text("Il tuo profilo", style = MaterialTheme.typography.headlineLarge)
        Text("Operatore locale · nessun account remoto", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(first, { first = it }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("profile-first"))
        OutlinedTextField(last, { last = it }, label = { Text("Cognome") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("profile-last"))
        PhotoControls(settings.operatorPhotoBase64, avatar = true) { model.profile(first, last, it) }
        Button(onClick = { model.profile(first, last, settings.operatorPhotoBase64) }, enabled = status != SaveState.Saving, modifier = Modifier.testTag("save-profile")) { Text("Salva profilo") }
        if (status == SaveState.Saved) Text("Profilo salvato")
        if (status == SaveState.Failed) Text("Salvataggio non riuscito", color = MaterialTheme.colorScheme.error)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandHome(properties: List<PropertyWithOwner>, bookings: List<ReservationEntity>, reports: List<ReportEntity>, accounting: AccountingData?, settings: AppSettings?, activities: List<ActivityEntity>, navigate: (String) -> Unit) {
    val today = BookingDates.today().toEpochDay()
    val active = bookings.filter { it.status != "CANCELLED" }
    val arrivals = active.filter { it.checkInEpochDay == today }; val departures = active.filter { it.checkOutEpochDay == today }
    val present = active.filter { it.status != "COMPLETED" && it.checkInEpochDay <= today && it.checkOutEpochDay > today }
    val upcoming = active.filter { it.checkInEpochDay > today }.sortedBy { it.checkInEpochDay }.take(3)
    val drafts = reports.filter { it.status == "DRAFT" }
    val review = accounting?.taxes?.count { it.needsReview } ?: 0
    androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize().testTag("home-scroll"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(ElledueNight, Color(0xFF1D354B))), MaterialTheme.shapes.large).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${if (java.time.LocalTime.now(java.time.ZoneId.of("Europe/Rome")).hour < 14) "Buongiorno" else "Benvenuto"}${settings?.operatorFirstName?.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""}", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                Text(BookingDates.display(today), color = ElledueGold, style = MaterialTheme.typography.labelLarge)
                Text("${arrivals.size} ${if (arrivals.size == 1) "arrivo" else "arrivi"} · ${departures.size} ${if (departures.size == 1) "partenza" else "partenze"}", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text("${present.sumOf { it.guestCount }} ospiti in casa · ${properties.count { !it.property.archived }} ${if (properties.count { !it.property.archived } == 1) "immobile attivo" else "immobili attivi"}", color = Color(0xFFB4C3D6))
            }
        }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Oggi", style = MaterialTheme.typography.titleLarge); TextButton(onClick = { navigate("activities") }, modifier = Modifier.testTag("nav-activities")) { Text("Operatività") } } }
        val events = (arrivals + departures).distinctBy { it.id }
        if (events.isEmpty()) item { Text("Nessun arrivo o partenza oggi. Il calendario resta a portata di mano.", style = MaterialTheme.typography.bodyMedium) }
        events.take(5).forEach { b -> item(key = "today-${b.id}") {
            ListItem(headlineContent = { Text("${b.guestFirstName} ${b.guestLastName}") }, supportingContent = { Text("${properties.firstOrNull { it.property.id == b.propertyId }?.property?.name ?: "Immobile"} · ${if (b.checkInEpochDay == today) "Arrivo ${b.expectedArrivalTime}" else "Partenza ${b.expectedDepartureTime}"}") }, leadingContent = { Icon(if (b.checkInEpochDay == today) Icons.AutoMirrored.Outlined.Login else Icons.AutoMirrored.Outlined.Logout, null, tint = ElledueGold) }, modifier = Modifier.clickable { navigate("booking/${b.id}") })
        } }
        item {
            Text("Da verificare", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { navigate("activities") }) { Text("${activities.count { !it.completed }} attività incomplete") }
            TextButton(onClick = { navigate("reports") }) { Text("${drafts.size} bozze rendiconto da verificare") }
            if (review > 0) TextButton(onClick = { navigate("accounting") }) { Text("$review conteggi tassa da ricontrollare") }
            val residue = accounting?.let { runCatching { operationalSummary(java.time.YearMonth.from(BookingDates.today()).toString(), "", bookings, it.expenses, it.taxes, it.movements).residualTax }.getOrNull() }
            TextButton(onClick = { navigate("accounting") }) { Text(residue?.let { "Tassa da incassare nel mese: ${Amounts.euro(it)}" } ?: "Apri contabilità e tasse") }
            val missingTax = accounting?.let { data -> active.count { b -> b.checkOutEpochDay >= today && data.taxes.none { it.reservationId == b.id } } } ?: 0
            if (missingTax > 0) Text("$missingTax soggiorni con tassa non ancora configurata", style = MaterialTheme.typography.bodySmall)
            activities.filter { !it.completed }.take(3).forEach { task -> TextButton(onClick = { navigate("activities") }) { Text(task.title) } }

        }
        if (upcoming.isNotEmpty()) item { Text("Prossimi soggiorni", style = MaterialTheme.typography.titleLarge) }
        upcoming.forEach { b -> item(key = "next-${b.id}") { ListItem(headlineContent = { Text("${b.guestFirstName} ${b.guestLastName}") }, supportingContent = { Text("${BookingDates.display(b.checkInEpochDay)} · ${properties.firstOrNull { it.property.id == b.propertyId }?.property?.name ?: "Immobile"}") }, modifier = Modifier.clickable { navigate("booking/${b.id}") }) } }
        item { Text("Il tuo portfolio", style = MaterialTheme.typography.titleLarge) }
        properties.filter { !it.property.archived }.take(2).forEach { item -> item(key = "property-${item.property.id}") { PortfolioTile(item) { navigate("property/${item.property.id}") } } }
        if (properties.isEmpty()) item { EmptyState("Pronto per il primo immobile", "Costruisci il tuo portfolio, poi aggiungi i soggiorni.", "Aggiungi un immobile", { navigate("property/new") }) }
    }
}
