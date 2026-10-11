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
import it.elledue.control.domain.PropertyDraft

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
fun ElledueApp(settingsModel: SettingsViewModel, propertiesModel: PropertyViewModel) {
    val navigation = rememberNavController()
    val entry by navigation.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val state by propertiesModel.state.collectAsStateWithLifecycle()
    val busy by propertiesModel.busy.collectAsStateWithLifecycle()
    val error by propertiesModel.error.collectAsStateWithLifecycle()
    val settings by settingsModel.settings.collectAsStateWithLifecycle()
    val saveState by settingsModel.saveState.collectAsStateWithLifecycle()
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
                    label = { Text(section.title, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag("nav-${section.route}"),
                )
            }
        }
    }) { outerPadding ->
        NavHost(navigation, startDestination = "dashboard", modifier = Modifier.padding(outerPadding)) {
            composable("dashboard") {
                Scaffold(topBar = { TopAppBar(title = {
                    Column { Text("ELLEDUE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary); Text("La tua gestione", style = MaterialTheme.typography.titleLarge) }
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
            composable("calendar") { UpcomingScreen("Calendario", "Prenotazioni e soggiorni", "Il calendario sarà disponibile con la gestione delle prenotazioni. Puoi già preparare gli immobili.", goProperties) }
            composable("economy") { UpcomingScreen("Economia", "Rendiconti e contabilità", "Commissioni, spese e rendiconti saranno disponibili nelle prossime versioni. Le tariffe degli immobili sono già configurabili.", goProperties) }
            composable("activities") { UpcomingScreen("Attività", "La tua operatività", "Scadenze, promemoria e checklist saranno disponibili nelle prossime versioni.", goProperties) }
            composable("settings") {
                Scaffold(topBar = { TopAppBar(title = { Text("Impostazioni") }) }) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("La tua organizzazione", style = MaterialTheme.typography.headlineMedium)
                        val current = settings
                        if (current == null) CircularProgressIndicator() else {
                            var name by rememberSaveable { mutableStateOf(current.organizationName) }
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
                        Text("Elledue Control · 0.1.0", style = MaterialTheme.typography.labelLarge)
                        Text("Dati conservati su questo dispositivo. Backup esportabile non ancora disponibile: non disinstallare l’app per conservare i dati.", style = MaterialTheme.typography.bodyMedium)
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
