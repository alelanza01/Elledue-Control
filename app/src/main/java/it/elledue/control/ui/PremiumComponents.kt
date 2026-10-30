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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        if(placeholder==Icons.Outlined.Apartment) Image(painterResource(R.drawable.elledue_symbol_gold),description,modifier=Modifier.size(144.dp).alpha(.3f))
        else Icon(placeholder, description, tint = ElledueGold.copy(alpha = .7f), modifier = Modifier.size(40.dp))
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
    val large = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.2f
    Box(Modifier.fillMaxWidth().heightIn(min = if(large) 178.dp else 134.dp).clip(MaterialTheme.shapes.medium).border(1.dp, ElledueGold.copy(.4f), MaterialTheme.shapes.medium).clickable(onClick = open)) {
        LocalPhoto(item.property.coverImageBase64, "Copertina ${item.property.name}", Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color.Transparent, ConceptBackground.copy(.76f),ConceptBackground.copy(.96f)))))
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(if(large) 40.dp else 98.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { StatusBadge(if(item.property.archived) "Archiviato" else "Attivo", if(item.property.archived) ConceptMuted else ArrivalColor) }
                Text(item.property.name, style = MaterialTheme.typography.titleLarge, maxLines = 3)
                Text(item.property.address, style = MaterialTheme.typography.bodySmall, color = ConceptMuted, maxLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.PeopleOutline, null, Modifier.size(14.dp), tint = ElledueGold); Text(item.property.maxGuests.toString(), style = MaterialTheme.typography.labelMedium)
                    Icon(Icons.Outlined.Bed, null, Modifier.size(14.dp), tint = ElledueGold); Text(item.property.beds.toString(), style = MaterialTheme.typography.labelMedium)
                    Icon(Icons.Outlined.ChevronRight, null, Modifier.size(20.dp), tint = ElledueGold)
                }
                Text(item.owner.name, style = MaterialTheme.typography.bodySmall, color = ConceptMuted, maxLines = 2)
            }
        }
    }
}

@Composable
fun ProfileScreen(settings: AppSettings?, model: SettingsViewModel, back: () -> Unit) {
    if (settings == null) { CircularProgressIndicator(); return }
    val status by model.saveState.collectAsStateWithLifecycle()
    var first by rememberPersistentDraft("profile#first",settings.operatorFirstName)
    var last by rememberPersistentDraft("profile#last",settings.operatorLastName)
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

@Composable
private fun HomeMetric(label: String, value: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, ring: Float? = null) {
    Column(modifier.clip(MaterialTheme.shapes.medium).background(Brush.verticalGradient(listOf(ConceptCard,Color(0xFF0E1B2E)))).border(1.dp,ConceptBorder,MaterialTheme.shapes.medium).padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = ConceptMuted)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = MaterialTheme.typography.headlineMedium.copy(fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,fontSize=if(value.length>6) 20.sp else 26.sp), modifier = Modifier.weight(1f))
            if(ring != null) Canvas(Modifier.size(29.dp)) { drawArc(ConceptBorder,-90f,360f,false,style=androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()));drawArc(ElledueGold,-90f,ring*360f,false,style=androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx())) }
            else Icon(icon,null,Modifier.size(24.dp),tint=ElledueGold)
        }
        Text(detail, style = MaterialTheme.typography.bodySmall, color = ConceptMuted)
    }
}

@Composable
fun CommandHome(properties: List<PropertyWithOwner>, bookings: List<ReservationEntity>, reports: List<ReportEntity>, accounting: AccountingData?, settings: AppSettings?, activities: List<ActivityEntity>, navigate: (String) -> Unit,businessType:String="MANAGED",issues:List<PropertyIssue> = emptyList(),forecasts:List<StayCostForecast> = emptyList()) {
    val today = BookingDates.today().toEpochDay(); val month = java.time.YearMonth.from(BookingDates.today())
    val active = bookings.filter { it.status != "CANCELLED" }
    val arrivals = active.filter { it.checkInEpochDay == today }; val departures = active.filter { it.checkOutEpochDay == today }
    val present = active.filter { it.status != "COMPLETED" && it.checkInEpochDay <= today && it.checkOutEpochDay > today }
    val portfolio = properties.filter { !it.property.archived }
    val metrics = portfolio.map { PortfolioAnalytics.calculate(month,it.property.id,bookings) }
    val occupied = metrics.sumOf { it.calendarOccupiedDays }; val available = metrics.sumOf { it.calendarDays }
    val ratio = if(available == 0) 0f else occupied.toFloat()/available
    val gross = OperationalMoney.total(active.filter { it.competenceMonth == month.toString() }.map { it.grossCents })
    val business=BusinessAnalytics.calculate(month.toString(),portfolio,bookings,accounting?.expenses.orEmpty(),accounting?.taxes.orEmpty(),accounting?.movements.orEmpty())
    val fees = OperationalMoney.total(active.filter { it.competenceMonth == month.toString() && it.appliedBusinessType!="OWNED" }.map { it.managementCents })
    androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize().testTag("home-scroll"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Box(Modifier.fillMaxWidth().heightIn(min = 206.dp).clip(MaterialTheme.shapes.large).border(1.dp,ElledueGold.copy(.22f),MaterialTheme.shapes.large)) {
                val custom=portfolio.firstOrNull { it.property.coverImageBase64 != null }?.property?.coverImageBase64
                if((settings?.homeCover ?: 0)==0 && custom!=null) LocalPhoto(custom,"Copertina della tua gestione",Modifier.matchParentSize())
                else Image(painterResource(homeCovers[((settings?.homeCover ?: 1).coerceIn(1,3))-1]),"Copertina artistica Elledue",Modifier.matchParentSize(),contentScale=ContentScale.Crop)
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(ConceptBackground.copy(.78f),ConceptBackground.copy(.55f),ConceptBackground.copy(.94f)))))
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Buongiorno${settings?.operatorFirstName?.takeIf { it.isNotBlank() }?.let { ",\n$it" } ?: ""}",style=MaterialTheme.typography.headlineLarge)
                    Text("${arrivals.size} ${if(arrivals.size==1) "arrivo" else "arrivi"} · ${departures.size} ${if(departures.size==1) "partenza" else "partenze"}",style=MaterialTheme.typography.bodyMedium)
                    Text("Tutto sotto controllo, un giorno alla volta.",style=MaterialTheme.typography.bodySmall,color=ConceptMuted)
                    Spacer(Modifier.height(23.dp))
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                        StatusBadge(BookingDates.display(today),ElledueGold)
                        Text(portfolio.firstOrNull()?.property?.name ?: "ELLEDUE CONTROL",style=MaterialTheme.typography.labelSmall,color=ConceptMuted,modifier=Modifier.weight(1f).padding(start=12.dp),maxLines=2)
                    }
                }
            }
        }
        if(businessType=="ALL")item { Text("In gestione: ${Amounts.euro(OperationalMoney.total(active.filter{it.appliedBusinessType!="OWNED" && it.competenceMonth==month.toString()}.map{it.grossCents}))} · Ricavi propri: ${Amounts.euro(OperationalMoney.total(active.filter{it.appliedBusinessType=="OWNED" && it.competenceMonth==month.toString()}.map{it.grossCents}))}",style=MaterialTheme.typography.bodySmall) }
        item {
            Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
                Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                    HomeMetric("Movimenti oggi",(arrivals.size+departures.size).toString(),"${arrivals.size} check-in · ${departures.size} check-out",Icons.Outlined.CalendarMonth,Modifier.weight(1f))
                    HomeMetric("Ospiti in casa",present.sumOf { it.guestCount }.toString(),"su ${portfolio.sumOf { it.property.maxGuests }} posti",Icons.Outlined.PeopleOutline,Modifier.weight(1f))
                }
                Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                    HomeMetric("Occupazione calendario",if(available==0) "—" else "${(ratio*100).toInt()}%",if(available==0) "Aggiungi il primo immobile" else "Giorni occupati nel mese",Icons.Outlined.DonutLarge,Modifier.weight(1f),ratio)
                    HomeMetric(if(businessType=="OWNED")"Ricavi immobili propri" else "Lordo proprietari gestiti",Amounts.euro(if(businessType=="OWNED")business.ownGross else business.managedGross),if(businessType=="OWNED")"Risultato operativo ${Amounts.euro(business.ownResult)}" else "Compenso di gestione Elledue ${Amounts.euro(business.fees)}",Icons.Outlined.BarChart,Modifier.weight(1f))
                }
            }
        }
        if(businessType=="ALL")item { OwnBusinessCard(business) }
        item { Text("Il tuo portafoglio",style=MaterialTheme.typography.titleLarge) }
        portfolio.take(2).forEach { p -> item(key="property-${p.property.id}") { PortfolioTile(p) { navigate("property/${p.property.id}") } } }
        if(portfolio.size>2)item{TextButton(onClick={navigate("properties")},modifier=Modifier.testTag("home-all-properties")){Text("Tutti gli immobili (${portfolio.size})")}}
        if(properties.isEmpty()) item { EmptyState("Pronto per il primo immobile","Aggiungi la copertina e costruisci il tuo portfolio.","Aggiungi un immobile",{navigate("property/new")}) }
        item {
            Text("Azioni rapide",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(bottom=8.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                ActionTile("Prenotazione",Icons.Outlined.AddHome,Modifier.weight(1f)) { navigate(if(portfolio.isEmpty()) "property/new" else "booking/new") }
                ActionTile("Check-in",Icons.Outlined.Login,Modifier.weight(1f)) { navigate(arrivals.firstOrNull()?.let { "booking/${it.id}" } ?: "calendar") }
                ActionTile("Pulizie",Icons.Outlined.CleaningServices,Modifier.weight(1f)) { navigate(if(portfolio.isEmpty()) "property/new" else "expense/new/CLEANING") }
                ActionTile("Rendiconti",Icons.Outlined.Description,Modifier.weight(1f)) { navigate("reports") }
            }
        }
        item {Text("Da fare",style=MaterialTheme.typography.titleLarge,modifier=Modifier.testTag("home-attention-heading"))}
        item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){TextButton(onClick={navigate("activities")}){Text("Tutte le attività")};TextButton(onClick={navigate("issues")},modifier=Modifier.testTag("home-maintenance")){Text("Manutenzioni")}}}
        val pending=activities.filter{!it.completed}.sortedBy{it.dueEpochMillis ?: Long.MAX_VALUE}.take(3)
        val openIssues=issues.filter{IssueRepository.open(it)}.sortedBy{if(it.priority=="URGENT")0 else 1}.take(3)
        val drafts=reports.filter{it.status=="DRAFT"}.take(3)
        val taxExceptions=active.filter{b->val tax=accounting?.taxes?.firstOrNull{it.reservationId==b.id};tax?.needsReview==true || (tax==null && b.taxableGuests==null) || (!b.automaticTaxAdvance && tax!=null && Ledger.residual(tax.expectedCents,b.collectedTaxCents)>0 && b.checkInEpochDay<=today)}.take(3)
        if(pending.isEmpty() && openIssues.isEmpty() && drafts.isEmpty() && taxExceptions.isEmpty())item{Text("Nessuna attività in evidenza.",style=MaterialTheme.typography.bodySmall,color=ConceptMuted)}
        pending.forEach{a->item(key="pending-${a.id}"){TextButton(onClick={navigate("activity/${a.id}")}){Text("Da completare · ${a.title}")}}}
        openIssues.forEach{i->item(key="issue-${i.id}"){TextButton(onClick={navigate("issue/${i.id}")}){Text("Manutenzione aperta · ${i.title}")}}}
        taxExceptions.forEach{b->item(key="tax-${b.id}"){TextButton(onClick={navigate("tax/${b.id}")},modifier=Modifier.testTag("home-tax-exception-${b.id}")){Text("Eccezione tassa da verificare · ${b.guestLastName}")}}}
        drafts.forEach{report->item(key="report-${report.id}"){TextButton(onClick={navigate("report/${report.id}")}){Text("Rendiconto da verificare · ${report.yearMonth}")}}}
        item {TextButton(onClick={navigate("drafts")},modifier=Modifier.testTag("open-drafts")){Text("Riprendi bozze dei moduli")}}
        item { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) { Text("Oggi",style=MaterialTheme.typography.titleLarge);TextButton(onClick={navigate("activities")},modifier=Modifier.testTag("nav-activities")) { Text("Operatività  ›") } } }
        val events = (arrivals+departures).distinctBy { it.id }
        if(events.isEmpty()) item { Text("Nessun arrivo o partenza oggi.",color=ConceptMuted,style=MaterialTheme.typography.bodySmall) }
        events.take(3).forEach { b -> item(key="today-${b.id}") {
            val arriving=b.checkInEpochDay==today
            ConceptTimelineRow(if(arriving) b.expectedArrivalTime else b.expectedDepartureTime,if(arriving) "Check-in" else "Check-out","${properties.firstOrNull { it.property.id==b.propertyId }?.property?.name} · ${b.guestLastName}",if(arriving) Icons.Outlined.Login else Icons.Outlined.Logout,if(arriving) ArrivalColor else DepartureColor) { navigate("booking/${b.id}") }
        } }
        val upcoming=active.filter { it.checkInEpochDay>today }.sortedBy { it.checkInEpochDay }.take(3)
        if(upcoming.isNotEmpty()) item { Text("Prossimi soggiorni",style=MaterialTheme.typography.titleLarge) }
        upcoming.forEach { b -> item(key="next-${b.id}") { ConceptTimelineRow(BookingDates.display(b.checkInEpochDay).take(5),"${b.guestFirstName} ${b.guestLastName}",properties.firstOrNull { it.property.id==b.propertyId }?.property?.name ?: "Immobile",Icons.Outlined.Apartment,StayColor) { navigate("booking/${b.id}") } } }
        val leaving=active.filter{it.checkOutEpochDay>today}.sortedBy{it.checkOutEpochDay}.take(3)
        if(leaving.isNotEmpty())item{Text("Prossime partenze",style=MaterialTheme.typography.titleLarge)}
        leaving.forEach{b->item(key="departure-${b.id}"){ConceptTimelineRow(BookingDates.display(b.checkOutEpochDay).take(5),"Check-out · ${b.guestLastName}",properties.firstOrNull{it.property.id==b.propertyId}?.property?.name ?: "Immobile",Icons.Outlined.CalendarMonth,DepartureColor){navigate("booking/${b.id}")}}}

    }
}
