package it.elledue.control.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import it.elledue.control.R
import it.elledue.control.domain.PropertyDraft
import it.elledue.control.domain.BookingDraft
import it.elledue.control.domain.BookingDates
import it.elledue.control.domain.BookingStatus

private data class Section(val route: String, val title: String, val icon: ImageVector)
private val sections = listOf(
    Section("dashboard", "Dashboard", Icons.Outlined.Dashboard),
    Section("properties", "Immobili", Icons.Outlined.Apartment),
    Section("calendar", "Calendario", Icons.Outlined.CalendarMonth),
    Section("economy", "Economia", Icons.Outlined.AccountBalanceWallet),
    Section("activities", "Attività", Icons.Outlined.TaskAlt),
    Section("settings", "Impostazioni", Icons.Outlined.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElledueApp(settingsModel: SettingsViewModel, propertiesModel: PropertyViewModel, bookingsModel: ReservationViewModel, accountingModel: AccountingViewModel, reportsModel: ReportViewModel, backupService: it.elledue.control.data.BackupService) {
    val navigation = rememberNavController()
    val entry by navigation.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val state by propertiesModel.state.collectAsStateWithLifecycle()
    val busy by propertiesModel.busy.collectAsStateWithLifecycle()
    val error by propertiesModel.error.collectAsStateWithLifecycle()
    val settings by settingsModel.settings.collectAsStateWithLifecycle()
    val saveState by settingsModel.saveState.collectAsStateWithLifecycle()
    val settingsLoadFailed by settingsModel.loadFailed.collectAsStateWithLifecycle()
    val reportsState by reportsModel.state.collectAsStateWithLifecycle()
    val savedReportId by reportsModel.savedId.collectAsStateWithLifecycle()
    val reports = (reportsState as? ReportState.Ready)?.reports.orEmpty()
    val accountingState by accountingModel.state.collectAsStateWithLifecycle()
    val accountingSaved by accountingModel.saved.collectAsStateWithLifecycle()
    val accountingData = (accountingState as? AccountingState.Ready)?.data
    val bookingState by bookingsModel.state.collectAsStateWithLifecycle()
    val bookingBusy by bookingsModel.busy.collectAsStateWithLifecycle()
    val bookingError by bookingsModel.error.collectAsStateWithLifecycle()
    val pendingOverlap by bookingsModel.pending.collectAsStateWithLifecycle()
    val savedBookingId by bookingsModel.savedId.collectAsStateWithLifecycle()
    val bookings = (bookingState as? BookingLoadState.Ready)?.items.orEmpty()
    val records = (state as? PropertyLoadState.Ready)?.items.orEmpty()
    val showNavigation = route == null || sections.any { it.route == route }
    val goProperties = { navigation.navigate("properties") { launchSingleTop = true } }
    Scaffold(bottomBar = {
        if (showNavigation) NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            sections.forEach { section ->
                NavigationBarItem(
                    selected = route == section.route,
                    onClick = {
                        settingsModel.clearSaveState()
                        navigation.navigate(section.route) {
                            popUpTo(navigation.graph.findStartDestination().id) { this.saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(section.icon, contentDescription = section.title) },
                    label = { Text(if (section.route == "settings") "Impost." else section.title, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag("nav-${section.route}"),
                )
            }
        }
    }) { outerPadding ->
        NavHost(navigation, startDestination = "dashboard", modifier = Modifier.padding(outerPadding)) {
            composable("dashboard") {
                Scaffold(topBar = { TopAppBar(title = {
                    Column { Surface(color = ElledueNight, shape = MaterialTheme.shapes.small) { androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(R.drawable.elledue_logo_gold), "Logo ufficiale Elledue", modifier = Modifier.padding(6.dp).width(120.dp).height(22.dp)) }; Text("La tua gestione", style = MaterialTheme.typography.titleLarge) }
                }) }) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        Text("Tutto parte dai tuoi immobili.", style = MaterialTheme.typography.headlineLarge)
                        when (state) {
                            PropertyLoadState.Loading -> CircularProgressIndicator()
                            PropertyLoadState.Failed -> LoadError(propertiesModel::retry)
                            is PropertyLoadState.Ready -> {
                                val active = records.filter { !it.property.archived }
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(active.size.toString(), style = MaterialTheme.typography.headlineLarge)
                                        Text("Immobili attivi", style = MaterialTheme.typography.titleMedium)
                                        Text("${records.count { it.property.archived }} archiviati · storico conservato", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                if (active.isEmpty()) EmptyState("Benvenuto in Elledue Control", "Organizza schede, proprietari e tariffe in un unico posto.", "Aggiungi un immobile") { propertiesModel.clearError(); navigation.navigate("property/new") }
                                else {
                                    Text("Oggi · ${BookingDates.display(BookingDates.today().toEpochDay())}", style = MaterialTheme.typography.titleLarge)
                                    when (bookingState) {
                                        BookingLoadState.Loading -> CircularProgressIndicator()
                                        BookingLoadState.Failed -> LoadError(bookingsModel::retry)
                                        is BookingLoadState.Ready -> {
                                            val today = BookingDates.today().toEpochDay()
                                            val current = bookings.filter { it.status == BookingStatus.CONFIRMED.name }
                                            val arrivals = current.filter { it.checkInEpochDay == today }
                                            val departures = current.filter { it.checkOutEpochDay == today }
                                            val stays = current.filter { it.checkInEpochDay <= today && it.checkOutEpochDay > today }
                                            Text("${arrivals.size} arrivi · ${departures.size} partenze · ${stays.sumOf { it.guestCount.toLong() }} ospiti nei soggiorni in corso", style = MaterialTheme.typography.bodyMedium)
                                            (arrivals + departures + stays).distinctBy { it.id }.forEach { booking ->
                                                BookingRow(booking, records.firstOrNull { it.property.id == booking.propertyId }?.property?.name ?: "Immobile") { navigation.navigate("booking/${booking.id}") }
                                            }
                                            TextButton(onClick = { navigation.navigate("calendar") }) { Text("Apri prenotazioni e calendario") }
                                        }
                                    }
                                    Text("Il tuo portafoglio", style = MaterialTheme.typography.titleLarge)
                                    active.take(3).forEach { item ->
                                        OutlinedCard(onClick = { navigation.navigate("property/${item.property.id}") }, modifier = Modifier.fillMaxWidth()) {
                                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(item.property.name, style = MaterialTheme.typography.titleMedium); Text(item.property.address, style = MaterialTheme.typography.bodyMedium) }
                                        }
                                    }
                                    TextButton(onClick = goProperties) { Text("Tutti gli immobili") }
                                }
                            }
                        }
                    }
                }
            }
            composable("properties") {
                when (state) {
                    PropertyLoadState.Loading -> LoadingScreen()
                    PropertyLoadState.Failed -> LoadError(propertiesModel::retry)
                    is PropertyLoadState.Ready -> PropertiesScreen(records,
                        onCreate = { propertiesModel.clearError(); navigation.navigate("property/new") },
                        onOpen = { propertiesModel.clearError(); navigation.navigate("property/$it") },
                    )
                }
            }
            composable("property/new") {
                val draft = rememberSaveable(stateSaver = PropertyDraftSaver) { mutableStateOf(PropertyDraft()) }
                PropertyEditorScreen(draft.value, true, busy, error, onBack = { navigation.popBackStack() }) { value ->
                    propertiesModel.save(value) { id -> navigation.popBackStack(); navigation.navigate("property/$id") }
                }
            }
            composable("property/{id}") { destination ->
                val id = destination.arguments?.getString("id")
                val record = records.firstOrNull { it.property.id == id }
                when {
                    state is PropertyLoadState.Loading -> LoadingScreen()
                    state is PropertyLoadState.Failed -> LoadError(propertiesModel::retry)
                    record == null -> EmptyState("Immobile non trovato", "Torna all’elenco per selezionare una scheda.", "Immobili", goProperties)
                    else -> PropertyDetailScreen(record, busy, error,
                        onBack = { navigation.popBackStack() },
                        onEdit = { propertiesModel.clearError(); navigation.navigate("property/$id/edit") },
                        onArchive = { propertiesModel.archive(record.property.id, it) },
                    )
                }
            }
            composable("property/{id}/edit") { destination ->
                val id = destination.arguments?.getString("id")
                val record = records.firstOrNull { it.property.id == id }
                when {
                    state is PropertyLoadState.Loading -> LoadingScreen()
                    state is PropertyLoadState.Failed -> LoadError(propertiesModel::retry)
                    record == null -> EmptyState("Immobile non trovato", "Torna all’elenco.", "Immobili", goProperties)
                    else -> PropertyEditorScreen(PropertyDraft.from(record), false, busy, error, onBack = { navigation.popBackStack() }) { value ->
                        propertiesModel.save(value) { navigation.popBackStack() }
                    }
                }
            }
            composable("calendar") {
                BookingDataSurface(bookingState, state, { bookingsModel.retry(); propertiesModel.retry() }) {
                    ReservationsScreen(bookings, records,
                        onNew = { bookingsModel.clearError(); navigation.navigate("booking/new") },
                        onOpen = { bookingsModel.clearError(); navigation.navigate("booking/$it") })
                }
            }
            composable("booking/new") {
                BookingDataSurface(bookingState, state, { bookingsModel.retry(); propertiesModel.retry() }) {
                    val initial = rememberSaveable(stateSaver = BookingDraftSaver) {
                        mutableStateOf(records.firstOrNull { !it.property.archived }?.let { BookingDraft().withPropertyDefaults(it) } ?: BookingDraft())
                    }
                    LaunchedEffect(savedBookingId) { savedBookingId?.let { bookingsModel.consumeSaved(); navigation.popBackStack(); navigation.navigate("booking/$it") } }
                    BookingEditorScreen(initial.value, records, true, bookingBusy, bookingError, pendingOverlap,
                        onBack = { bookingsModel.dismissOverlap(); navigation.popBackStack() }, onSave = { bookingsModel.save(it) }, onConfirm = bookingsModel::confirmOverlap, onDismiss = bookingsModel::dismissOverlap)
                }
            }
            composable("booking/{id}") { destination ->
                BookingDataSurface(bookingState, state, { bookingsModel.retry(); propertiesModel.retry() }) {
                    val item = bookings.firstOrNull { it.id == destination.arguments?.getString("id") }
                    if (item == null) EmptyState("Prenotazione non trovata", "Torna al calendario.", "Calendario", { navigation.navigate("calendar") })
                    else BookingDetailScreen(item, records.firstOrNull { it.property.id == item.propertyId }?.property?.name ?: "Immobile", bookingBusy, bookingError,
                        onBack = { navigation.popBackStack() }, onEdit = { bookingsModel.clearError(); navigation.navigate("booking/${item.id}/edit") }, onCancel = { bookingsModel.cancel(item.id) }, onTax = { accountingModel.clear(); navigation.navigate("tax/${item.id}") }, financialChanges = accountingData?.financialChanges.orEmpty())
                }
            }
            composable("booking/{id}/edit") { destination ->
                BookingDataSurface(bookingState, state, { bookingsModel.retry(); propertiesModel.retry() }) {
                    val item = bookings.firstOrNull { it.id == destination.arguments?.getString("id") }
                    LaunchedEffect(savedBookingId) { if (savedBookingId != null) { bookingsModel.consumeSaved(); navigation.popBackStack() } }
                    if (item == null) EmptyState("Prenotazione non trovata", "Torna al calendario.")
                    else BookingEditorScreen(BookingDraft.from(item), records, false, bookingBusy, bookingError, pendingOverlap,
                        onBack = { bookingsModel.dismissOverlap(); navigation.popBackStack() }, onSave = { bookingsModel.save(it) }, onConfirm = bookingsModel::confirmOverlap, onDismiss = bookingsModel::dismissOverlap)
                }
            }
            composable("economy") {
                BookingDataSurface(bookingState, state, { bookingsModel.retry(); propertiesModel.retry() }) {
                    AccountingSurface(accountingState, accountingModel::retry) {
                        AccountingHome(accountingData!!, bookings, records, accountingModel,
                            onNew = { accountingModel.clear(); navigation.navigate("expense/new/$it") },
                            onExpense = { accountingModel.clear(); navigation.navigate("expense/$it") },
                            onTax = { accountingModel.clear(); navigation.navigate("tax/$it") },
                            onCategories = { accountingModel.clear(); navigation.navigate("categories") }, onReports = { reportsModel.clear(); navigation.navigate("reports") })
                    }
                }
            }
            composable("expense/new/{kind}") { destination ->
                AccountingSurface(accountingState, accountingModel::retry) {
                    val kind = destination.arguments?.getString("kind") ?: "OTHER"
                    val selectedProperty = records.firstOrNull { !it.property.archived }?.property
                    val category = accountingData!!.categories.firstOrNull { it.kind == kind && !it.archived }
                    val initial by rememberSaveable(stateSaver = ExpenseDraftSaver) { mutableStateOf(it.elledue.control.domain.ExpenseDraft(propertyId = selectedProperty?.id ?: "", categoryId = category?.id ?: "other", quantity = if (kind != "OTHER") "1" else "", unit = when (kind) { "CLEANING" -> it.elledue.control.domain.Amounts.editable(selectedProperty?.cleaningHourlyCents ?: 800); "LAUNDRY" -> it.elledue.control.domain.Amounts.editable(selectedProperty?.laundrySetCents ?: 500); else -> "" })) }
                    LaunchedEffect(accountingSaved) { accountingSaved?.takeIf { it == initial.id }?.let { accountingModel.clear(); navigation.popBackStack(); navigation.navigate("expense/$it") } }
                    ExpenseEditor(initial, records, bookings, accountingData.categories, accountingModel) { navigation.popBackStack() }
                }
            }
            composable("expense/{id}") { destination ->
                AccountingSurface(accountingState, accountingModel::retry) {
                    val item = accountingData!!.expenses.firstOrNull { it.id == destination.arguments?.getString("id") }
                    if (item == null) EmptyState("Spesa non trovata", "Torna alla contabilità.")
                    else ExpenseDetail(item, records.firstOrNull { it.property.id == item.propertyId }?.property?.name ?: "Immobile", accountingModel,
                        onBack = { accountingModel.clear(); navigation.popBackStack() }, onEdit = { accountingModel.clear(); navigation.navigate("expense/${item.id}/edit") }, bookingName = bookings.firstOrNull { it.id == item.reservationId }?.let { "${it.guestFirstName} ${it.guestLastName} · ${BookingDates.display(it.checkInEpochDay)}" })
                }
            }
            composable("expense/{id}/edit") { destination ->
                AccountingSurface(accountingState, accountingModel::retry) {
                    val item = accountingData!!.expenses.firstOrNull { it.id == destination.arguments?.getString("id") }
                    LaunchedEffect(accountingSaved) { if (accountingSaved == item?.id && item != null) { accountingModel.clear(); navigation.popBackStack() } }
                    if (item == null) EmptyState("Spesa non trovata", "Torna alla contabilità.")
                    else ExpenseEditor(it.elledue.control.domain.ExpenseDraft.from(item), records, bookings, accountingData.categories, accountingModel) { navigation.popBackStack() }
                }
            }
            composable("categories") {
                AccountingSurface(accountingState, accountingModel::retry) { CategoryScreen(accountingData!!.categories, accountingModel) { navigation.popBackStack() } }
            }
            composable("tax/{id}") { destination ->
                BookingDataSurface(bookingState, state, { bookingsModel.retry(); propertiesModel.retry() }) {
                    AccountingSurface(accountingState, accountingModel::retry) {
                        val booking = bookings.firstOrNull { it.id == destination.arguments?.getString("id") }
                        val property = records.firstOrNull { it.property.id == booking?.propertyId }?.property
                        if (booking == null || property == null) EmptyState("Prenotazione non trovata", "Torna al calendario.")
                        else TaxScreen(booking, property, accountingData!!, accountingModel) { accountingModel.clear(); navigation.popBackStack() }
                    }
                }
            }
            composable("reports") {
                when (reportsState) {
                    ReportState.Loading -> LoadingScreen()
                    ReportState.Failed -> LoadError(reportsModel::retry)
                    is ReportState.Ready -> ReportsHome(reports, records, reportsModel, { navigation.popBackStack() }, { navigation.navigate("report/$it") })
                }
            }
            composable("report/{id}") { destination ->
                val report = reports.firstOrNull { it.id == destination.arguments?.getString("id") }
                when {
                    reportsState is ReportState.Loading -> LoadingScreen()
                    report?.snapshotJson == null -> EmptyState("Documento non disponibile", "Riapri la bozza dai rendiconti.")
                    else -> {
                        val snapshot = remember(report.snapshotJson) { runCatching { it.elledue.control.reports.ReportCodec.decode(report.snapshotJson) }.getOrNull() }
                        val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                        DisposableEffect(report.id, report.status, owner) {
                            val observer = androidx.lifecycle.LifecycleEventObserver { _, event -> if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && report.status == "DRAFT") reportsModel.refresh(report.id) }
                            owner.lifecycle.addObserver(observer); onDispose { owner.lifecycle.removeObserver(observer) }
                        }
                        LaunchedEffect(savedReportId) { savedReportId?.let { reportsModel.clear(); navigation.navigate("report/$it") } }
                        if (snapshot != null && runCatching { snapshot.summary(); snapshot.gross(); snapshot.ota(); snapshot.net() }.isFailure) EmptyState("Importi non validi", "Correggi le voci nella contabilità prima di confermare.", "Apri contabilità", { navigation.navigate("economy") })
                        else if (snapshot == null) EmptyState("Snapshot non valido", "Aggiorna la bozza senza confermarla.", "Aggiorna", { reportsModel.refresh(report.id) })
                        else {
                            val export = rememberPdfExporter(snapshot)
                            ReportDetail(report, snapshot, reportsModel, { navigation.popBackStack() },
                                onBooking = { bookingsModel.clearError(); navigation.navigate("booking/$it/edit") },
                                onExpense = { accountingModel.clear(); navigation.navigate("expense/$it/edit") },
                                onTax = { accountingModel.clear(); navigation.navigate("tax/$it") },
                                onEconomy = { navigation.navigate("economy") }, onExport = export)
                        }
                    }
                }
            }
            composable("activities") { UpcomingScreen("Attività", "La tua operatività", "Scadenze, promemoria e checklist saranno disponibili nelle prossime versioni.", goProperties) }
            composable("settings") {
                Scaffold(topBar = { TopAppBar(title = { Text("Impostazioni") }) }) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("La tua organizzazione", style = MaterialTheme.typography.headlineMedium)
                        val current = settings
                        if (settingsLoadFailed) Text("Impostazioni non disponibili. Riavvia l’app senza eliminare i dati.", color = MaterialTheme.colorScheme.error)
                        else if (current == null) CircularProgressIndicator() else {
                            var name by rememberSaveable(current.organizationName) { mutableStateOf(current.organizationName) }
                            OutlinedTextField(name, { name = it; settingsModel.clearSaveState() }, label = { Text("Nome organizzazione") }, singleLine = true, enabled = saveState != SaveState.Saving, modifier = Modifier.fillMaxWidth())
                            Button(onClick = { settingsModel.save(name) }, enabled = saveState != SaveState.Saving) { Text("Salva") }
                            when (saveState) {
                                SaveState.Saved -> Text("Impostazioni salvate")
                                SaveState.Invalid -> Text("Inserisci un nome.", color = MaterialTheme.colorScheme.error)
                                SaveState.Failed -> Text("Salvataggio non riuscito. Riprova.", color = MaterialTheme.colorScheme.error)
                                SaveState.Saving -> CircularProgressIndicator()
                                SaveState.Idle -> Unit
                            }
                        }
                        HorizontalDivider()
                        Text("Elledue Control · 0.4.0", style = MaterialTheme.typography.labelLarge)
                        Text("Dati locali. Esporta un backup verificato prima di cambiare dispositivo o firma Android.", style = MaterialTheme.typography.bodyMedium)
                        BackupControls(backupService)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }

@Composable
private fun LoadError(retry: () -> Unit) { EmptyState("Dati non disponibili", "Il caricamento non è riuscito. Riprova senza eliminare i dati.", "Riprova", retry) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpcomingScreen(title: String, heading: String, description: String, onProperties: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(title) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) { EmptyState(heading, description, "Gestisci immobili", onProperties) }
    }
}

@Composable
private fun BookingDataSurface(state: BookingLoadState, properties: PropertyLoadState, retry: () -> Unit, content: @Composable () -> Unit) {
    when {
        state is BookingLoadState.Failed || properties is PropertyLoadState.Failed -> LoadError(retry)
        state is BookingLoadState.Loading || properties is PropertyLoadState.Loading -> LoadingScreen()
        else -> content()
    }
}

@Composable
private fun AccountingSurface(state: AccountingState, retry: () -> Unit, content: @Composable () -> Unit) {
    when (state) { AccountingState.Loading -> LoadingScreen(); AccountingState.Failed -> LoadError(retry); is AccountingState.Ready -> content() }
}
