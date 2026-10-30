package it.elledue.control.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import it.elledue.control.R
import it.elledue.control.data.scope
import it.elledue.control.domain.PropertyDraft
import it.elledue.control.domain.BookingDraft
import it.elledue.control.domain.BookingDates
import it.elledue.control.domain.BookingStatus

private data class Section(val route: String, val title: String, val icon: ImageVector)
private val sections = listOf(
    Section("dashboard", "Home", Icons.Outlined.Home),
    Section("properties", "Immobili", Icons.Outlined.Apartment),
    Section("calendar", "Calendario", Icons.Outlined.CalendarMonth),
    Section("economy", "Economia", Icons.Outlined.AccountBalanceWallet),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElledueApp(settingsModel: SettingsViewModel, propertiesModel: PropertyViewModel, bookingsModel: ReservationViewModel, accountingModel: AccountingViewModel, reportsModel: ReportViewModel, backupService: it.elledue.control.data.BackupService, activity:it.elledue.control.MainActivity?=null) {
    val snackbar=remember{SnackbarHostState()}
    val notice by bookingsModel.notice.collectAsStateWithLifecycle()
    LaunchedEffect(notice){notice?.let{snackbar.showSnackbar(it);bookingsModel.clearNotice()}}
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as it.elledue.control.ElledueApplication
    val issues by app.issueRepository.issues.collectAsStateWithLifecycle(initialValue=emptyList())
    val forecasts by app.stayCosts.forecasts.collectAsStateWithLifecycle(initialValue=emptyList())
    val activities by app.activityRepository.activities.collectAsStateWithLifecycle(initialValue = emptyList())
    val navigation = rememberNavController()
    val requestedRoute by (activity?.notificationRoute ?: remember{ kotlinx.coroutines.flow.MutableStateFlow<String?>(null) }).collectAsState()
    LaunchedEffect(requestedRoute){requestedRoute?.let{navigation.navigate(it){launchSingleTop=true};activity?.notificationRoute?.value=null}}
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
    var businessType by rememberSaveable{mutableStateOf("MANAGED")}
    val operational by key(businessType){remember(businessType){app.operations.scope(businessType)}.collectAsStateWithLifecycle(initialValue=null)}
    val normalRecords=operational?.properties.orEmpty();val normalBookings=operational?.bookings.orEmpty()
    val normalIds=normalRecords.map{it.property.id}.toSet()
    val normalAccounting=it.elledue.control.data.OperationalRepository.accounting(accountingData,normalIds)
    val normalActivities=activities.filter{it.propertyId==null || it.propertyId in normalIds}
    val showNavigation = route == "reservations" || route == null || sections.any { it.route == route } || route?.startsWith("activities") == true || route?.startsWith("report") == true
    val goProperties = { navigation.navigate("properties") { launchSingleTop = true } }
    val largeFonts = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.2f
    var quick by remember { mutableStateOf(false) }
    val jump: (String) -> Unit = { target -> navigation.navigate(target) { launchSingleTop = true } }
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
    Column(Modifier.fillMaxSize().background(ConceptBackground)) {

        if (showNavigation || route?.startsWith("activities") == true || (route?.startsWith("report") == true) || route in listOf("settings", "profile", "activities", "notifications", "accounting", "analysis")) {
            Row(Modifier.fillMaxWidth().zIndex(2f).background(ElledueNight).statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(R.drawable.elledue_logo_gold), "Intestazione Elledue", Modifier.width(112.dp).height(28.dp))
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { jump("notifications") }) { Icon(Icons.Outlined.NotificationsNone, "Centro notifiche", tint = ElledueGold) }
                IconButton(onClick = { jump("profile") }, modifier = Modifier.testTag("open-profile")) {
                    if (settings?.operatorPhotoBase64 != null) LocalPhoto(settings?.operatorPhotoBase64, "Profilo operatore", Modifier.size(36.dp).clip(androidx.compose.foundation.shape.CircleShape))
                    else Icon(Icons.Outlined.AccountCircle, "Profilo operatore", tint = ElledueGold)
                }
                IconButton(onClick = { jump("settings") }, modifier = Modifier.testTag("nav-settings")) { Icon(Icons.Outlined.MoreHoriz, "Impostazioni", tint = ElledueGold) }
            }
        }

        if(route in listOf("dashboard","calendar","reservations","economy","analysis"))BusinessScopeSelector(businessType){businessType=it}
        if(route=="dashboard")TextButton(onClick={jump("reservations")},modifier=Modifier.fillMaxWidth().testTag("open-reservations")){Text("Prenotazioni · elenco e ricerca")}
        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
        NavHost(navigation, startDestination = "dashboard", modifier = Modifier.fillMaxSize()) {
            composable("dashboard") {
                BookingDataSurface(bookingState, state, { bookingsModel.retry(); propertiesModel.retry() }) {
                    if(operational==null)LoadingScreen() else CommandHome(normalRecords, normalBookings, reports.filter{it.propertyId in normalIds}, normalAccounting, settings, normalActivities, jump,businessType,issues.filter{it.propertyId in normalIds},forecasts.filter{f->normalBookings.any{it.id==f.reservationId}})
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
                val draft = rememberSaveable(stateSaver = PropertyDraftSaver) { mutableStateOf(PropertyDraft(businessType=if(businessType=="OWNED")"OWNED" else "MANAGED")) }
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
                        onPhoto = { propertiesModel.setCover(record.property.id, it) },
                        onDelete = { jump("delete/property/${record.property.id}") },issueContent={
                            issues.filter{issue->issue.propertyId==record.property.id && it.elledue.control.data.IssueRepository.open(issue)}.forEach{i->OutlinedButton(onClick={jump("issue/${i.id}")}){Text("Problema aperto · ${i.title}")}}
                            TextButton(onClick={jump("issue-create?property=${record.property.id}")}){Text("Segnala manutenzione")}
                        },
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
                        propertiesModel.save(value) { savedId->val resumed=navigation.previousBackStackEntry?.destination?.route=="drafts";navigation.popBackStack();if(resumed)navigation.navigate("property/$savedId"){launchSingleTop=true} }
                    }
                }
            }
            composable("guest-message/{booking}/{kind}") {d->
                val b=bookings.firstOrNull{it.id==d.arguments?.getString("booking")}
                if(b!=null)GuestReminderScreen(b,d.arguments?.getString("kind") ?: "out",records.firstOrNull{it.property.id==b.propertyId},app.messageRepository,accountingData,app.reminderStore){navigation.popBackStack()} else LoadingScreen()
            }
            composable("issues") { IssuesScreen(issues.filter{it.propertyId in normalIds},normalRecords,jump){navigation.popBackStack()} }
            composable("day/{day}?property={property}",arguments=listOf(androidx.navigation.navArgument("property"){type=androidx.navigation.NavType.StringType;defaultValue=""})) { d->OperationalDayScreen(d.arguments?.getString("day")?.toLongOrNull() ?: BookingDates.today().toEpochDay(),normalRecords.filter{d.arguments?.getString("property").isNullOrBlank() || it.property.id==d.arguments?.getString("property")},normalBookings,normalActivities,issues.filter{it.propertyId in normalIds},jump,expenses=normalAccounting?.expenses.orEmpty()){navigation.popBackStack()} }
            composable("issue-create?property={property}&booking={booking}",arguments=listOf("property","booking").map{name->androidx.navigation.navArgument(name){defaultValue=""}}) {d->
                val initial=remember{it.elledue.control.data.PropertyIssue(propertyId=d.arguments?.getString("property") ?: "",reservationId=d.arguments?.getString("booking")?.ifBlank{null})}
                IssueEditor(initial,records,bookings,accountingData?.expenses.orEmpty(),app.issueRepository,{navigation.popBackStack()}){id->navigation.popBackStack();jump("issue/$id")}
            }
            composable("issue/new") { _ -> IssueEditor(remember{it.elledue.control.data.PropertyIssue()},records,bookings,accountingData?.expenses.orEmpty(),app.issueRepository,{navigation.popBackStack()}){id->navigation.popBackStack();jump("issue/$id")} }
            composable("issue/{id}") {d->
                val item=issues.firstOrNull{it.id==d.arguments?.getString("id")}
                if(item!=null)key(item.id,item.updatedAt){IssueEditor(item,records,bookings,accountingData?.expenses.orEmpty(),app.issueRepository,{navigation.popBackStack()}){navigation.popBackStack();jump("issue/$it")}} else EmptyState("Segnalazione non disponibile","Verifica il registro problemi.")
            }
            composable("reservations") {
                BookingDataSurface(bookingState,state,{bookingsModel.retry();propertiesModel.retry()}) {
                    val scoped=records.filter{businessType=="ALL" || it.property.businessType==businessType}
                    ReservationsBrowser(bookings,scoped,{jump("booking/$it")},{bookingsModel.clearError();jump("booking/new")},{jump("calendar")})
                }
            }
            composable("calendar") {
                BookingDataSurface(bookingState, state, { bookingsModel.retry(); propertiesModel.retry() }) {
                    ReservationsScreen(normalBookings, normalRecords,
                        onNew = { bookingsModel.clearError(); navigation.navigate("booking/new") },
                        onOpen = { bookingsModel.clearError(); navigation.navigate("booking/$it") }, activities=normalActivities, onActivity={jump("activity/$it")}, onCopy={property,month,day->jump("calendar-copy?property=$property&month=$month&day=$day")}, onDay={jump("day/$it")},issues=issues.filter{it.propertyId in normalIds},onReservations={jump("reservations")},onFilteredDay={day,property->jump("day/$day?property=$property")})
                }
            }
            composable("booking/new") {
                BookingDataSurface(bookingState, state, { bookingsModel.retry(); propertiesModel.retry() }) {
                    val initial = rememberSaveable(stateSaver = BookingDraftSaver) {
                        mutableStateOf(BookingDraft())
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
                        onBack = { navigation.popBackStack() }, onEdit = { bookingsModel.clearError(); navigation.navigate("booking/${item.id}/edit") }, onCancel = { bookingsModel.cancel(item.id) }, onDelete={jump("delete/booking/${item.id}")}, onTax = { accountingModel.clear(); navigation.navigate("tax/${item.id}") }, onExpense = { kind -> accountingModel.clear(); val existing=accountingData?.expenses?.firstOrNull{it.reservationId==item.id && it.kind==kind && !it.voided && kind!="OTHER"};navigation.navigate(existing?.let{"expense/${it.id}/edit"} ?: "expense/new/$kind?booking=${item.id}") }, onActivities = { jump("activities?booking=${item.id}") }, onMessages={jump("messages?booking=${item.id}")}, financialChanges = accountingData?.financialChanges.orEmpty(),operationalContent={
                            StayOperationsPanel(item,forecasts,accountingData,issues,activities,app.stayCosts,jump)
                            reports.filter{it.propertyId==item.propertyId && it.yearMonth==item.competenceMonth}.maxByOrNull{it.version}?.let{report->OutlinedButton(onClick={jump("report/${report.id}")}){Text("Apri rendiconto · ${report.yearMonth}")}}
                        })
                }
            }
            composable("booking/{id}/edit") { destination ->
                BookingDataSurface(bookingState, state, { bookingsModel.retry(); propertiesModel.retry() }) {
                    val item = bookings.firstOrNull { it.id == destination.arguments?.getString("id") }
                    LaunchedEffect(savedBookingId) { savedBookingId?.let{id->
                        val resumed=navigation.previousBackStackEntry?.destination?.route=="drafts"
                        bookingsModel.consumeSaved();navigation.popBackStack()
                        if(resumed)navigation.navigate("booking/$id"){launchSingleTop=true}
                    } }
                    if (item == null) EmptyState("Prenotazione non trovata", "Torna al calendario.")
                    else BookingEditorScreen(BookingDraft.from(item), records, false, bookingBusy, bookingError, pendingOverlap,
                        onBack = { bookingsModel.dismissOverlap(); navigation.popBackStack() }, onSave = { bookingsModel.save(it) }, onConfirm = bookingsModel::confirmOverlap, onDismiss = bookingsModel::dismissOverlap)
                }
            }
            composable("economy") {
                if(operational==null)LoadingScreen() else EconomyHub(normalBookings, normalRecords, normalAccounting, jump,businessType)
            }
            composable("analysis") { EconomyAnalysis(normalBookings, normalRecords, normalAccounting,businessType) { navigation.popBackStack() } }
            composable("tax-overview") {
                AccountingSurface(accountingState,accountingModel::retry){ AccountingHome(normalAccounting ?: accountingData!!,normalBookings,normalRecords,accountingModel,{jump("expense/new/$it")},{jump("expense/$it")},{jump("tax/$it")},{jump("categories")},{jump("reports")},initialTab=2) }
            }
            composable("expenses") {
                AccountingSurface(accountingState,accountingModel::retry){AccountingHome(normalAccounting ?: accountingData!!,normalBookings,normalRecords,accountingModel,{jump("expense/new/$it")},{jump("expense/$it")},{jump("tax/$it")},{jump("categories")},{jump("reports")},initialTab=1)}
            }
            composable("payments") {
                AccountingSurface(accountingState,accountingModel::retry){AccountingHome(normalAccounting ?: accountingData!!,normalBookings,normalRecords,accountingModel,{jump("expense/new/$it")},{jump("expense/$it")},{jump("tax/$it")},{jump("categories")},{jump("reports")},paymentsOnly=true)}
            }
            composable("accounting") {
                BookingDataSurface(bookingState, state, { bookingsModel.retry(); propertiesModel.retry() }) {
                    AccountingSurface(accountingState, accountingModel::retry) {
                        AccountingHome(normalAccounting ?: accountingData!!, normalBookings, normalRecords, accountingModel,
                            onNew = { accountingModel.clear(); navigation.navigate("expense/new/$it") },
                            onExpense = { accountingModel.clear(); navigation.navigate("expense/$it") },
                            onTax = { accountingModel.clear(); navigation.navigate("tax/$it") },
                            onCategories = { accountingModel.clear(); navigation.navigate("categories") }, onReports = { reportsModel.clear(); navigation.navigate("reports") })
                    }
                }
            }
            composable("expense/new/{kind}?booking={booking}", arguments = listOf(androidx.navigation.navArgument("booking") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = null })) { destination ->
                AccountingSurface(accountingState, accountingModel::retry) {
                    val kind = destination.arguments?.getString("kind") ?: "OTHER"
                    val linked = bookings.firstOrNull { it.id == destination.arguments?.getString("booking") }
                    val selectedProperty = records.firstOrNull { it.property.id == linked?.propertyId }?.property ?: normalRecords.firstOrNull { !it.property.archived }?.property
                    val category = accountingData!!.categories.firstOrNull { it.kind == kind && !it.archived }
                    val initial by rememberSaveable(stateSaver = ExpenseDraftSaver) { mutableStateOf(app.drafts.load("expense/new/$kind?booking=${linked?.id ?: ""}",it.elledue.control.domain.ExpenseDraft::class.java) ?: it.elledue.control.domain.ExpenseDraft(id=if(linked!=null && kind in listOf("CLEANING","LAUNDRY"))it.elledue.control.data.StayCostRepository.expenseId(linked.id,kind) else java.util.UUID.randomUUID().toString(),propertyId = selectedProperty?.id ?: "", reservationId = linked?.id ?: "", categoryId = category?.id ?: "other",date=if(linked!=null && kind in listOf("CLEANING","LAUNDRY"))BookingDates.display(linked.checkOutEpochDay) else BookingDates.display(BookingDates.today().toEpochDay()),month=if(linked!=null && kind in listOf("CLEANING","LAUNDRY"))linked.competenceMonth else java.time.YearMonth.from(BookingDates.today()).toString(), quantity = if(kind in listOf("CLEANING","LAUNDRY") && linked!=null)forecasts.firstOrNull{it.reservationId==linked.id && it.kind==kind}?.quantity ?: "1" else if(kind!="OTHER")"1" else "", unit = when (kind) { "CLEANING" -> it.elledue.control.domain.Amounts.editable(selectedProperty?.cleaningHourlyCents ?: 800); "LAUNDRY" -> it.elledue.control.domain.Amounts.editable(selectedProperty?.laundrySetCents ?: 500); else -> if(category?.id=="transport")selectedProperty?.habitualTransportCents?.let(it.elledue.control.domain.Amounts::editable) ?: "" else "" }, advanced=selectedProperty?.businessType!="OWNED", reimbursable=selectedProperty?.businessType!="OWNED")) }
                    LaunchedEffect(accountingSaved) { accountingSaved?.takeIf { it == initial.id }?.let { accountingModel.clear(); navigation.popBackStack(); navigation.navigate("expense/$it") } }
                    ExpenseEditor(initial, records, bookings, accountingData.categories, accountingModel,draftRoute="expense/new/$kind?booking=${linked?.id ?: ""}") { navigation.popBackStack() }
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
                    LaunchedEffect(accountingSaved) { if (accountingSaved == item?.id && item != null) { val resumed=navigation.previousBackStackEntry?.destination?.route=="drafts";accountingModel.clear(); navigation.popBackStack();if(resumed)navigation.navigate("expense/${item.id}"){launchSingleTop=true} } }
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
                    is ReportState.Ready -> ReportsHome(reports.filter{it.propertyId in normalIds}, normalRecords.filter{it.property.businessType!="OWNED"}, reportsModel, { navigation.popBackStack() }, { navigation.navigate("report/$it") })
                }
            }
            composable("report/{id}") { destination ->
                val report = reports.firstOrNull { it.id == destination.arguments?.getString("id") }
                when {
                    reportsState is ReportState.Loading -> LoadingScreen()
                    report?.snapshotJson == null -> EmptyState("Documento non disponibile", "Riapri la bozza dai rendiconti.")
                    else -> {
                        val snapshot = remember(report.snapshotJson) { runCatching { it.elledue.control.reports.ReportCodec.decode(report.snapshotJson) }.getOrNull() }
                        LaunchedEffect(report.id){if(report.status=="DRAFT")reportsModel.refresh(report.id)}
                        val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                        DisposableEffect(report.id, report.status, owner) {
                            val observer = androidx.lifecycle.LifecycleEventObserver { _, event -> if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && report.status == "DRAFT") reportsModel.refresh(report.id) }
                            owner.lifecycle.addObserver(observer); onDispose { owner.lifecycle.removeObserver(observer) }
                        }
                        LaunchedEffect(savedReportId) { savedReportId?.let { reportsModel.clear(); navigation.navigate("report/$it") } }
                        if (snapshot != null && runCatching { snapshot.summary(); snapshot.gross(); snapshot.ota(); snapshot.net() }.isFailure) EmptyState("Importi non validi", "Correggi le voci nella contabilità prima di confermare.", "Apri contabilità", { navigation.navigate("accounting") })
                        else if (snapshot == null) EmptyState("Snapshot non valido", "Aggiorna la bozza senza confermarla.", "Aggiorna", { reportsModel.refresh(report.id) })
                        else {
                            ReportDetail(report, snapshot, reportsModel, { navigation.popBackStack() },
                                onBooking = { bookingsModel.clearError(); navigation.navigate("booking/$it/edit") },
                                onExpense = { accountingModel.clear(); navigation.navigate("expense/$it/edit") },
                                onTax = { accountingModel.clear(); navigation.navigate("tax/$it") },
                                onEconomy = { navigation.navigate("accounting") }, onStandard={jump("booking/$it")})
                        }
                    }
                }
            }
            composable("activities?booking={booking}", arguments = listOf(androidx.navigation.navArgument("booking") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = null })) { destination ->
                ActivitiesScreen(app.activityRepository, normalActivities, normalRecords, normalBookings, destination.arguments?.getString("booking"), expenses=normalAccounting?.expenses.orEmpty(),open=jump) { navigation.popBackStack() }
            }
            composable("delete/{kind}/{id}"){d->DeletionScreen(app.deletionRepository,d.arguments?.getString("kind") ?: "",d.arguments?.getString("id") ?: "",{navigation.popBackStack()}){navigation.navigate("properties"){popUpTo("dashboard");launchSingleTop=true}}}
            composable("archive"){ArchiveScreen(records,bookings,accountingData,reports,jump){navigation.popBackStack()}}
            composable("drafts"){DraftsScreen(jump){navigation.popBackStack()}}
            composable("movement/new?property={property}&booking={booking}&kind={kind}&source={source}",arguments=listOf("property","booking","kind","source").map{name->androidx.navigation.navArgument(name){defaultValue=""}}){d->
                val property=d.arguments?.getString("property") ?: "";val booking=d.arguments?.getString("booking")?.ifBlank{null};val kind=d.arguments?.getString("kind") ?: "";val source=d.arguments?.getString("source")
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)){TextButton(onClick={navigation.popBackStack()}){Text("Indietro")};MovementPanel(kind,property,booking,accountingData?.movements?.firstOrNull{it.id==source},java.time.YearMonth.from(BookingDates.today()).toString(),accountingModel){navigation.popBackStack()}}
                LaunchedEffect(accountingSaved){if(accountingSaved=="movement"){accountingModel.clear();navigation.popBackStack()}}
            }
            composable("activity/new") { ActivityEditor(app.activityRepository,null,normalRecords,normalBookings){navigation.popBackStack()} }
            composable("activity/{id}") { d ->
                val actionScope=rememberCoroutineScope()
                val task=activities.firstOrNull {it.id==d.arguments?.getString("id")}
                if(task!=null) ActivityEditor(app.activityRepository,task,records,bookings,actionScope=actionScope){navigation.popBackStack()} else EmptyState("Attività non disponibile","Torna all’elenco.")
            }
            composable("calendar-copy?property={property}&month={month}&day={day}",arguments=listOf(
                androidx.navigation.navArgument("property"){defaultValue=""},androidx.navigation.navArgument("month"){defaultValue=java.time.YearMonth.from(BookingDates.today()).toString()},androidx.navigation.navArgument("day"){type=androidx.navigation.NavType.LongType;defaultValue=BookingDates.today().toEpochDay()}
            )) { d -> CopyCalendarScreen(normalBookings,normalRecords,d.arguments?.getString("property") ?: "",d.arguments?.getString("month") ?: java.time.YearMonth.from(BookingDates.today()).toString(),d.arguments?.getLong("day") ?: BookingDates.today().toEpochDay()){navigation.popBackStack()} }
            composable("messages?booking={booking}&draft={draft}",arguments=listOf("booking","draft").map{name->androidx.navigation.navArgument(name){nullable=true;defaultValue=null}}) {d -> BookingDataSurface(bookingState,state,{bookingsModel.retry();propertiesModel.retry()}) { MessagesScreen(app.messageRepository,normalRecords,normalBookings,d.arguments?.getString("booking"),accountingData,d.arguments?.getString("draft")){navigation.popBackStack()} } }
            composable("appearance") { AppearanceScreen(settings,settingsModel){navigation.popBackStack()} }
            composable("profile") { ProfileScreen(settings, settingsModel) { navigation.popBackStack() } }
            composable("notifications") { NotificationSettings { navigation.popBackStack() } }
            composable("access") { if(activity!=null)AccessSettings(activity){navigation.popBackStack()} }
            composable("settings") {
                Scaffold(topBar = { TopAppBar(title = { Text("Impostazioni") }) }) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("La tua organizzazione", style = MaterialTheme.typography.headlineMedium)
                        val current = settings
                        if (settingsLoadFailed) Text("Impostazioni non disponibili. Riavvia l’app senza eliminare i dati.", color = MaterialTheme.colorScheme.error)
                        else if (current == null) CircularProgressIndicator() else {
                            var name by rememberPersistentDraft("settings#name",current.organizationName)
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
                        OutlinedButton(onClick={jump("notifications")},modifier=Modifier.fillMaxWidth()){Text("Notifiche Android") }
                        OutlinedButton(onClick={jump("access")},modifier=Modifier.fillMaxWidth()){Text("Accesso protetto") }
                        OutlinedButton(onClick={jump("drafts")},modifier=Modifier.fillMaxWidth()){Text("Bozze da recuperare") }
                        OutlinedButton(onClick={jump("appearance")},modifier=Modifier.fillMaxWidth()){Text("Aspetto · copertina Home")}
                        OutlinedButton(onClick={jump("issues")},modifier=Modifier.fillMaxWidth()){Text("Manutenzioni")}
                        OutlinedButton(onClick={jump("messages")},modifier=Modifier.fillMaxWidth()){Text("Messaggi ospiti IT/EN")}
                        OutlinedButton(onClick={jump("archive")},modifier=Modifier.fillMaxWidth()){Text("Archivio storico")}
                        Text("Elledue Control · 0.4.11", style = MaterialTheme.typography.labelLarge)
                        Text("Dati locali. Esporta un backup verificato prima di cambiare dispositivo o firma Android.", style = MaterialTheme.typography.bodyMedium)
                        BackupControls(backupService){navigation.navigate("dashboard"){popUpTo(navigation.graph.id){inclusive=true};launchSingleTop=true}}
                    }
                }
            }
        }
        }
        SnackbarHost(snackbar)
        if (showNavigation) Surface(modifier=Modifier.zIndex(2f),color = MaterialTheme.colorScheme.surfaceContainer) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().heightIn(min = 66.dp), verticalAlignment = Alignment.CenterVertically) {
                sections.take(2).forEach { section -> ConceptNavItem(section.icon, section.title, (route == section.route || (section.route=="calendar" && route=="reservations") || (section.route=="economy" && (route?.startsWith("activities")==true || route?.startsWith("report")==true))), !largeFonts, Modifier.weight(1f).testTag("nav-${section.route}")) { jump(section.route) } }
                Box(Modifier.padding(horizontal=8.dp).size(48.dp).clip(androidx.compose.foundation.shape.CircleShape).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(androidx.compose.ui.graphics.Color(0xFFEBD2A1),ElledueGold,androidx.compose.ui.graphics.Color(0xFFB8975B)))).border(1.dp,ElledueGold,androidx.compose.foundation.shape.CircleShape).clickable { quick = true }.testTag("quick-action"),contentAlignment=Alignment.Center) { Icon(Icons.Outlined.Add,"Azioni rapide",tint=ElledueNight,modifier=Modifier.size(25.dp)) }
                sections.drop(2).forEach { section -> ConceptNavItem(section.icon, section.title, (route == section.route || (section.route=="calendar" && route=="reservations") || (section.route=="economy" && (route?.startsWith("activities")==true || route?.startsWith("report")==true))), !largeFonts, Modifier.weight(1f).testTag("nav-${section.route}")) { jump(section.route) } }

            }
        }

    }
    }
    if (quick) ModalBottomSheet(onDismissRequest = { quick = false },containerColor=ConceptCard,tonalElevation=0.dp,shape=MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Azione rapida", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f)); IconButton(onClick = { quick = false }, modifier = Modifier.testTag("close-quick")) { Icon(Icons.Outlined.Close, "Chiudi azioni") } }
            val actions = if (records.none { !it.property.archived }) listOf("Nuovo immobile" to "property/new", "Nuova attività" to "activity/new") else when (route) {
                "properties" -> listOf("Nuovo immobile" to "property/new")
                "economy" -> listOf("Nuova spesa" to "expense/new/OTHER", "Registra pulizia" to "expense/new/CLEANING", "Tassa e anticipi" to "tax-overview")
                "calendar" -> listOf("Nuova prenotazione" to "booking/new", "Nuova attività" to "activity/new")
                else -> listOf("Nuova prenotazione" to "booking/new", "Nuova spesa" to "expense/new/OTHER", "Nuova attività" to "activity/new")
            }
            val completeActions=if(records.any{!it.property.archived})actions+listOf("Nuova prenotazione" to "booking/new","Nuova spesa" to "expense/new/OTHER","Nuova attività" to "activity/new","Nuova manutenzione" to "issue-create") else actions
            completeActions.distinctBy{it.second}.forEach { (label, target) -> OutlinedButton(onClick = { quick = false; propertiesModel.clearError(); bookingsModel.clearError(); accountingModel.clear(); jump(target) }, modifier = Modifier.fillMaxWidth()) { Text(label) } }
            Spacer(Modifier.height(20.dp))
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
