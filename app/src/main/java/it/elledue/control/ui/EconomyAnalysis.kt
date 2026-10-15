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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import it.elledue.control.data.*
import it.elledue.control.domain.*
import java.time.YearMonth

@Composable
fun EconomyHub(bookings: List<ReservationEntity>, properties: List<PropertyWithOwner>, accounting: AccountingData?, navigate: (String) -> Unit) {
    val month = YearMonth.from(BookingDates.today())
    val current = bookings.filter { it.status != "CANCELLED" && it.competenceMonth == month.toString() }
    LazyColumn(Modifier.fillMaxSize().testTag("economy-scroll"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Economia", style = MaterialTheme.typography.headlineLarge); Text("La gestione interna · $month", style = MaterialTheme.typography.bodySmall,color=ConceptMuted) }
        item { Column(Modifier.fillMaxWidth().background(ConceptCard, MaterialTheme.shapes.medium).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Compensi Elledue maturati", color = ElledueGold,style=MaterialTheme.typography.labelMedium)
            Text(Amounts.euro(OperationalMoney.total(current.map { it.managementCents })), color = Color.White, style = MaterialTheme.typography.headlineLarge)
            Text("Lordo ai proprietari ${Amounts.euro(OperationalMoney.total(current.map { it.grossCents }))}", color = Color.White)
            Text("Il compenso maturato non è un pagamento ricevuto.", color = Color(0xFFB4C3D6), style = MaterialTheme.typography.bodySmall)
        } }
        if (accounting != null) item {
            val costs = accounting.expenses.filter { !it.voided && it.competenceMonth == month.toString() }
            val payments = Ledger.active(accounting.movements).filter { it.competenceMonth == month.toString() && it.kind == "OWNER_PAYMENT" }
            Text("Spese ${Amounts.euro(OperationalMoney.total(costs.map { it.actualCents }))} · anticipate ${Amounts.euro(OperationalMoney.total(costs.filter { it.advancedByElledue }.map { it.actualCents }))}", style = MaterialTheme.typography.bodyMedium)
            Text("Anticipi / pagamenti riconosciuti ${Amounts.euro(OperationalMoney.total(payments.map { it.amountCents }))}", style = MaterialTheme.typography.bodySmall)
        }
        item { Text("Gestione e controllo", style = MaterialTheme.typography.titleLarge) }
        item { Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            ActionTile("Analisi e confronti",Icons.Outlined.BarChart,Modifier.weight(1f).testTag("open-analysis")) { navigate("analysis") }
            ActionTile("Operatività",Icons.Outlined.TaskAlt,Modifier.weight(1f)) { navigate("activities") }
        } }
        item { ActionTile("Contabilità operativa · Spese, tassa e anticipi",Icons.Outlined.AccountBalanceWallet,Modifier.fillMaxWidth().testTag("open-accounting")) { navigate("accounting") } }
        item { OutlinedButton(onClick={navigate("messages")},modifier=Modifier.fillMaxWidth().testTag("open-messages")){Text("Messaggi ospiti · IT/EN")} }
        item { HorizontalDivider(); Text("Documenti per i proprietari", style = MaterialTheme.typography.titleLarge) }
        item { OutlinedButton(onClick = { navigate("reports") }, modifier = Modifier.fillMaxWidth().testTag("open-reports")) { Icon(Icons.Outlined.Description, null); Spacer(Modifier.width(10.dp)); Text("Rendiconti mensili") } }
    }
}

@Composable
fun EconomyAnalysis(bookings: List<ReservationEntity>, properties: List<PropertyWithOwner>, accounting:AccountingData?=null, back: () -> Unit) {
    var period by rememberSaveable { mutableStateOf(YearMonth.from(BookingDates.today()).toString()) }
    val month = YearMonth.parse(period)
    var property by rememberSaveable { mutableStateOf("") }
    val metrics = properties.filter {property.isBlank() || it.property.id==property}.map { it to PortfolioAnalytics.calculate(month, it.property.id, bookings) }
    val maxGross = metrics.maxOfOrNull { it.second.gross }?.coerceAtLeast(1) ?: 1
    LazyColumn(Modifier.fillMaxSize().testTag("analysis-scroll"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { TextButton(onClick = back) { Text("Indietro") }; Text("Analisi del portfolio", style = MaterialTheme.typography.headlineLarge) }
        item { Row { IconButton(onClick = { period = month.minusMonths(1).toString() }) { Icon(Icons.Outlined.ChevronLeft, "Analisi mese precedente") }; Text(period, Modifier.weight(1f).padding(12.dp), style = MaterialTheme.typography.titleLarge); IconButton(onClick = { period = month.plusMonths(1).toString() }) { Icon(Icons.Outlined.ChevronRight, "Analisi mese successivo") } } }
        item { ConceptPropertySelector(properties,property,allOption=true){property=it} }
        item {
            val current=bookings.filter {it.status!="CANCELLED" && it.competenceMonth==period && (property.isBlank() || it.propertyId==property)}
            val previous=bookings.filter {it.status!="CANCELLED" && it.competenceMonth==month.minusMonths(1).toString() && (property.isBlank() || it.propertyId==property)}
            val fees=OperationalMoney.total(current.map{it.managementCents});val old=OperationalMoney.total(previous.map{it.managementCents})
            Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=ConceptCard),border=BorderStroke(1.dp,ConceptBorder)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text("Compensi maturati",style=MaterialTheme.typography.labelMedium,color=ElledueGold)
                Text(Amounts.euro(fees),style=MaterialTheme.typography.headlineLarge)
                AmountLine("Lordo proprietari",OperationalMoney.total(current.map{it.grossCents}))
                AmountLine("Netto dopo OTA",OperationalMoney.total(current.map{it.netCents}))
                Text("Scostamento compensi: ${Amounts.euro(fees-old)} · "+if(old==0L) "base precedente assente" else java.math.BigDecimal.valueOf(fees-old).multiply(java.math.BigDecimal(100)).divide(java.math.BigDecimal.valueOf(old),1,java.math.RoundingMode.HALF_UP).toPlainString()+"%",style=MaterialTheme.typography.bodySmall)
                Text("${current.size} prenotazioni · precedente ${previous.size}. Compensi diversi dagli incassi Elledue.",style=MaterialTheme.typography.bodySmall)
            }}
        }
        item {
            val costs=accounting?.expenses.orEmpty().filter{!it.voided && it.competenceMonth==period && (property.isBlank() || it.propertyId==property)}
            val groups=costs.groupBy{it.category}.mapValues{OperationalMoney.total(it.value.map{e->e.actualCents})}
            val total=OperationalMoney.total(groups.values)
            Text("Spese per categoria",style=MaterialTheme.typography.titleLarge)
            if(total==0L) Text("Nessuna spesa nel periodo selezionato.",color=ConceptMuted)
            else {
                Row(Modifier.fillMaxWidth().padding(vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    Canvas(Modifier.size(112.dp)) {var angle=-90f;groups.forEach{(key,value)->val sweep=(value.toDouble()/total*360).toFloat();drawArc(propertyColor(key),angle,sweep,false,style=androidx.compose.ui.graphics.drawscope.Stroke(17.dp.toPx()));angle+=sweep}}
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){groups.forEach{(key,value)->Text("$key · ${Amounts.euro(value)}",color=propertyColor(key),style=MaterialTheme.typography.bodySmall)}}
                }
                AmountLine("Costo operativo totale",total)
                AmountLine("Anticipate da Elledue",OperationalMoney.total(costs.filter{it.advancedByElledue}.map{it.actualCents}))
                AmountLine("Anticipate e rimborsabili",OperationalMoney.total(costs.filter{it.advancedByElledue && it.reimbursable}.map{it.actualCents}))
            }
        }
        item { Text("Economia e notti dei soggiorni attribuiti al check-in. Occupazione: giorni occupati nel mese / giorni del calendario; blocchi e disponibilità commerciale non sono ancora registrati.", style = MaterialTheme.typography.bodySmall) }
        items(metrics, key = { it.first.property.id }) { (p, m) ->
            val previous = PortfolioAnalytics.calculate(month.minusMonths(1), p.property.id, bookings)
            Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(p.property.name, style = MaterialTheme.typography.titleLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Lordo ${Amounts.euro(m.gross)}"); Text("Elledue ${Amounts.euro(m.fees)}", color = MaterialTheme.colorScheme.primary) }
                // Only the visual ratio uses floats; all accounting calculations use exact cents.
                LinearProgressIndicator(progress = { (m.gross.toDouble() / maxGross).toFloat() }, modifier = Modifier.fillMaxWidth().height(6.dp), color = propertyColor(p.property.id))
                Text("${m.bookings} ${if (m.bookings == 1) "prenotazione" else "prenotazioni"} · ${m.nights} notti · media ${m.averageStay.toPlainString().replace('.', ',')} notti")
                Text("Occupazione calendario ${m.calendarPercent.toPlainString().replace('.', ',')}% (${m.calendarOccupiedDays}/${m.calendarDays})")
                Text("Lordo rispetto al mese precedente: ${Amounts.euro(m.gross - previous.gross)}", style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth().height(92.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
                    val trend = (5 downTo 0).map { offset -> val mo = month.minusMonths(offset.toLong()); mo to PortfolioAnalytics.calculate(mo, p.property.id, bookings).gross }
                    val max = trend.maxOf { it.second }.coerceAtLeast(1)
                    trend.forEach { (mo, value) -> Column(Modifier.weight(1f), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth(.7f).height((value.toDouble() / max * 64).dp).background(propertyColor(p.property.id).copy(alpha=if(mo==month)1f else .4f), MaterialTheme.shapes.small))
                        Text(mo.monthValue.toString(), style = MaterialTheme.typography.labelSmall)
                    } }
                }
                Text("Andamento lordo · ultimi 6 mesi", style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
            }
        }
        if (properties.isEmpty()) item { EmptyState("Nessun dato da confrontare", "Aggiungi immobili e prenotazioni per vedere i dati effettivi.") }
    }
}
