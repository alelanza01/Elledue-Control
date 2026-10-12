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
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { Text("Economia", style = MaterialTheme.typography.headlineLarge); Text("La gestione interna · $month", style = MaterialTheme.typography.bodyMedium) }
        item { Column(Modifier.fillMaxWidth().background(ElledueNight, MaterialTheme.shapes.large).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Compensi Elledue maturati", color = ElledueGold)
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
        item { ListItem(headlineContent = { Text("Analisi e confronti") }, supportingContent = { Text("Immobili, notti e andamento mensile") }, leadingContent = { Icon(Icons.Outlined.BarChart, null) }, modifier = Modifier.clickable { navigate("analysis") }.testTag("open-analysis")) }
        item { ListItem(headlineContent = { Text("Contabilità operativa") }, supportingContent = { Text("Spese, pulizie, lavanderia, tassa e anticipi") }, leadingContent = { Icon(Icons.Outlined.AccountBalanceWallet, null) }, modifier = Modifier.clickable { navigate("accounting") }.testTag("open-accounting")) }
        item { HorizontalDivider(); Text("Documenti per i proprietari", style = MaterialTheme.typography.titleLarge) }
        item { OutlinedButton(onClick = { navigate("reports") }, modifier = Modifier.fillMaxWidth().testTag("open-reports")) { Icon(Icons.Outlined.Description, null); Spacer(Modifier.width(10.dp)); Text("Rendiconti mensili") } }
    }
}

@Composable
fun EconomyAnalysis(bookings: List<ReservationEntity>, properties: List<PropertyWithOwner>, back: () -> Unit) {
    var period by rememberSaveable { mutableStateOf(YearMonth.from(BookingDates.today()).toString()) }
    val month = YearMonth.parse(period)
    val metrics = properties.map { it to PortfolioAnalytics.calculate(month, it.property.id, bookings) }
    val maxGross = metrics.maxOfOrNull { it.second.gross }?.coerceAtLeast(1) ?: 1
    LazyColumn(Modifier.fillMaxSize().testTag("analysis-scroll"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { TextButton(onClick = back) { Text("Indietro") }; Text("Analisi del portfolio", style = MaterialTheme.typography.headlineLarge) }
        item { Row { IconButton(onClick = { period = month.minusMonths(1).toString() }) { Icon(Icons.Outlined.ChevronLeft, "Analisi mese precedente") }; Text(period, Modifier.weight(1f).padding(12.dp), style = MaterialTheme.typography.titleLarge); IconButton(onClick = { period = month.plusMonths(1).toString() }) { Icon(Icons.Outlined.ChevronRight, "Analisi mese successivo") } } }
        item { Text("Economia e notti dei soggiorni attribuiti al check-in. Occupazione: giorni occupati nel mese / giorni del calendario; blocchi e disponibilità commerciale non sono ancora registrati.", style = MaterialTheme.typography.bodySmall) }
        items(metrics, key = { it.first.property.id }) { (p, m) ->
            val previous = PortfolioAnalytics.calculate(month.minusMonths(1), p.property.id, bookings)
            Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(p.property.name, style = MaterialTheme.typography.titleLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Lordo ${Amounts.euro(m.gross)}"); Text("Elledue ${Amounts.euro(m.fees)}", color = MaterialTheme.colorScheme.primary) }
                // Only the visual ratio uses floats; all accounting calculations use exact cents.
                LinearProgressIndicator(progress = { (m.gross.toDouble() / maxGross).toFloat() }, modifier = Modifier.fillMaxWidth().height(6.dp), color = ElledueGold)
                Text("${m.bookings} ${if (m.bookings == 1) "prenotazione" else "prenotazioni"} · ${m.nights} notti · media ${m.averageStay.toPlainString().replace('.', ',')} notti")
                Text("Occupazione calendario ${m.calendarPercent.toPlainString().replace('.', ',')}% (${m.calendarOccupiedDays}/${m.calendarDays})")
                Text("Lordo rispetto al mese precedente: ${Amounts.euro(m.gross - previous.gross)}", style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth().height(92.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
                    val trend = (5 downTo 0).map { offset -> val mo = month.minusMonths(offset.toLong()); mo to PortfolioAnalytics.calculate(mo, p.property.id, bookings).gross }
                    val max = trend.maxOf { it.second }.coerceAtLeast(1)
                    trend.forEach { (mo, value) -> Column(Modifier.weight(1f), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth(.7f).height((value.toDouble() / max * 64).dp.coerceAtLeast(2.dp)).background(if (mo == month) ElledueGold else MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small))
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
