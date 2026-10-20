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
fun EconomyHub(bookings: List<ReservationEntity>, properties: List<PropertyWithOwner>, accounting: AccountingData?, navigate: (String) -> Unit,businessType:String="MANAGED") {
    var period by rememberSaveable{mutableStateOf(YearMonth.from(BookingDates.today()).toString())};val month=YearMonth.parse(period)
    var tab by rememberSaveable{mutableIntStateOf(0)}
    val totals=BusinessAnalytics.calculate(period,properties,bookings,accounting?.expenses.orEmpty(),accounting?.taxes.orEmpty(),accounting?.movements.orEmpty())
    LazyColumn(Modifier.fillMaxSize().testTag("economy-scroll"),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item{ConceptHeading("Economia","Previsioni, movimenti e saldi distinti")}
        item{Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedButton(onClick={navigate("reports")},enabled=properties.any{it.property.businessType!="OWNED"},modifier=Modifier.weight(1f).testTag("open-reports")){Text("Rendiconti")}
                OutlinedButton(onClick={navigate("analysis")},modifier=Modifier.weight(1f).testTag("open-analysis")){Text("Analisi")}
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedButton(onClick={navigate("accounting")},modifier=Modifier.weight(1f).testTag("open-accounting")){Text("Contabilità")}
                OutlinedButton(onClick={navigate("messages")},modifier=Modifier.weight(1f).testTag("open-messages")){Text("Messaggi")}
            }
        }}
        item{Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){IconButton(onClick={period=month.minusMonths(1).toString()}){Icon(Icons.Outlined.ChevronLeft,"Economia mese precedente")};Text(period,Modifier.weight(1f),style=MaterialTheme.typography.titleLarge);IconButton(onClick={period=month.plusMonths(1).toString()}){Icon(Icons.Outlined.ChevronRight,"Economia mese successivo")}}}
        item{Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("Panoramica","Movimenti","Rendiconti").forEachIndexed{i,label->ConceptPill(tab==i,{tab=i},label={Text(label)},modifier=Modifier.testTag("economy-tab-$i"))}}}
        if(tab==0) {
            if(businessType!="OWNED")item{Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=ConceptCard)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text("Immobili in gestione",style=MaterialTheme.typography.titleMedium,color=ElledueGold)
                Text("Compenso di gestione Elledue",style=MaterialTheme.typography.labelMedium);Text(Amounts.euro(totals.fees),style=MaterialTheme.typography.headlineLarge)
                AmountLine("Netto dei portali ai proprietari",totals.managedNet);AmountLine("Anticipi da rimborsare",totals.reimbursements)
                AmountLine(if(totals.balance<0)"Credito da riconciliare" else "Saldo da riconciliare con proprietari",totals.balance)
                Text("Compensi maturati diversi dagli incassi. Il saldo non è un ordine di bonifico né un rendiconto confermato.",style=MaterialTheme.typography.bodySmall,color=ConceptMuted)
            }}}
            if(businessType!="MANAGED")item{Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=ConceptCard)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text("Immobili propri / in comproprietà",style=MaterialTheme.typography.titleMedium,color=ArrivalColor)
                AmountLine("Ricavi prenotazioni registrati",totals.ownGross);AmountLine("Netto dopo OTA",totals.ownNet);AmountLine("Costi operativi",totals.ownCosts)
                Text("Risultato operativo complessivo",style=MaterialTheme.typography.labelMedium);Text(Amounts.euro(totals.ownResult),style=MaterialTheme.typography.headlineLarge)
                Text("Nessuna commissione di gestione o ripartizione automatica fra soci. Non è un saldo con proprietari esterni.",style=MaterialTheme.typography.bodySmall,color=ConceptMuted)
                if(totals.legacyOwnBookings>0)Text("${totals.legacyOwnBookings} soggiorni conservano il modello storico in gestione: risultato parziale finché non verifichi la classificazione.",color=ElledueGold)
            }}}
            item{Text("Registra una sola volta",style=MaterialTheme.typography.titleLarge)}
            item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ActionTile("Spesa",Icons.Outlined.ReceiptLong,Modifier.weight(1f)){navigate("expense/new/OTHER")};ActionTile("Pulizia",Icons.Outlined.CleaningServices,Modifier.weight(1f)){navigate("expense/new/CLEANING")};ActionTile("Lavanderia",Icons.Outlined.LocalLaundryService,Modifier.weight(1f)){navigate("expense/new/LAUNDRY")}}}
            item{OutlinedButton(onClick={navigate("tax-overview")},modifier=Modifier.fillMaxWidth().testTag("economy-tax")){Text("Tassa · previsione, incasso, compensazione e riversamento")}}
            item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ActionTile("Analisi e confronti",Icons.Outlined.BarChart,Modifier.weight(1f)){navigate("analysis")};ActionTile("Operatività",Icons.Outlined.TaskAlt,Modifier.weight(1f)){navigate("activities")}}}
        }
        if(tab==1){
            item{Text("Registrazioni effettive · $period",style=MaterialTheme.typography.titleLarge)}
            val costs=accounting?.expenses.orEmpty().filter{!it.voided && it.competenceMonth==period}
            items(costs,key={"cost-${it.id}"}){e->TextButton(onClick={navigate("expense/${e.id}")}){Text("${e.category} · ${e.description} · ${Amounts.euro(e.actualCents)}")}}
            val money=accounting?.movements.orEmpty().filter{it.competenceMonth==period}
            items(money,key={"money-${it.id}"}){m->TextButton(onClick={navigate(m.reservationId?.let{"tax/$it"} ?: "accounting")}){Text("${movementLabel(m.kind)} · ${Amounts.euro(m.amountCents)}")}}
            if(costs.isEmpty() && money.isEmpty())item{EmptyState("Nessun movimento registrato","Le previsioni delle prenotazioni non sono incassi. Registra soltanto somme effettive.")}
        }
        item{HorizontalDivider();Text("Rendiconti per proprietari esterni",style=MaterialTheme.typography.titleLarge)}
        item{OutlinedButton(onClick={navigate("reports")},modifier=Modifier.fillMaxWidth(),enabled=properties.any{it.property.businessType!="OWNED"}){Text("Rendiconti mensili")};if(properties.all{it.property.businessType=="OWNED"})Text("Per gli immobili propri usa il risultato economico complessivo in Analisi.",style=MaterialTheme.typography.bodySmall)}
        item{OutlinedButton(onClick={navigate("issues")},modifier=Modifier.fillMaxWidth()){Text("Problemi e interventi")}}
        item{TextButton(onClick={navigate("archive")}){Text("Archivio storico")}}
    }
}

@Composable
fun EconomyAnalysis(bookings: List<ReservationEntity>, properties: List<PropertyWithOwner>, accounting:AccountingData?=null,businessType:String="MANAGED", back: () -> Unit) {
    var analysisTab by rememberSaveable{mutableIntStateOf(0)}
    var period by rememberSaveable { mutableStateOf(YearMonth.from(BookingDates.today()).toString()) }
    val month = YearMonth.parse(period)
    var property by rememberSaveable { mutableStateOf("") }
    val metrics = properties.filter {property.isBlank() || it.property.id==property}.map { it to PortfolioAnalytics.calculate(month, it.property.id, bookings) }
    val maxGross = metrics.maxOfOrNull { it.second.gross }?.coerceAtLeast(1) ?: 1
    LazyColumn(Modifier.fillMaxSize().testTag("analysis-scroll"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { TextButton(onClick = back) { Text("Indietro") }; Text("Analisi del portfolio", style = MaterialTheme.typography.headlineLarge) }
        item { Row { IconButton(onClick = { period = month.minusMonths(1).toString() }) { Icon(Icons.Outlined.ChevronLeft, "Analisi mese precedente") }; Text(period, Modifier.weight(1f).padding(12.dp), style = MaterialTheme.typography.titleLarge); IconButton(onClick = { period = month.plusMonths(1).toString() }) { Icon(Icons.Outlined.ChevronRight, "Analisi mese successivo") } } }
        item { ConceptPropertySelector(properties,property,allOption=true){property=it} }
        item { Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("Operatività","Economia","Elledue privata").forEachIndexed{i,t->ConceptPill(analysisTab==i,{analysisTab=i},label={Text(t)})} } }
        if(analysisTab==0) {
            item { OperationalAnalysisSummary(month,properties.filter{property.isBlank() || it.property.id==property},bookings) }
        }
        if(analysisTab!=0) {

        item {
            val selected=properties.filter{property.isBlank() || it.property.id==property}
            val selectedIds=selected.map{it.property.id}.toSet()
            val totals=BusinessAnalytics.calculate(period,selected,bookings,accounting?.expenses.orEmpty(),accounting?.taxes.orEmpty(),accounting?.movements.orEmpty())
            val old=BusinessAnalytics.calculate(month.minusMonths(1).toString(),selected,bookings,accounting?.expenses.orEmpty(),accounting?.taxes.orEmpty(),accounting?.movements.orEmpty())
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                if(analysisTab==2 && selected.any{it.property.businessType!="OWNED"})Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=ConceptCard),border=BorderStroke(1.dp,ConceptBorder)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Text("Immobili in gestione",style=MaterialTheme.typography.titleMedium,color=ElledueGold)
                    Text("Compenso di gestione Elledue",style=MaterialTheme.typography.labelMedium);Text(Amounts.euro(totals.fees),style=MaterialTheme.typography.headlineLarge)
                    AmountLine("Lordo prenotazioni ai proprietari",totals.managedGross);AmountLine("Netto dopo OTA ai proprietari",totals.managedNet)
                    Text("Variazione compenso: ${Amounts.euro(totals.fees-old.fees)} · "+if(old.fees==0L) "base precedente assente" else java.math.BigDecimal.valueOf(totals.fees-old.fees).multiply(java.math.BigDecimal(100)).divide(java.math.BigDecimal.valueOf(old.fees),1,java.math.RoundingMode.HALF_UP).toPlainString()+"%",style=MaterialTheme.typography.bodySmall)
                    Text("Compensi maturati distinti dagli incassi Elledue.",style=MaterialTheme.typography.bodySmall)
                }}
                if(analysisTab==2 && selected.any{it.property.businessType=="OWNED"})OwnBusinessCard(totals)
            }
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
        }
        item { Text("Economia attribuita al check-in. Notti vendute distribuite nei mesi effettivi. Occupazione: notti uniche occupate / notti del calendario; blocchi e disponibilità commerciale non sono ancora registrati.", style = MaterialTheme.typography.bodySmall) }
        items(metrics, key = { it.first.property.id }) { (p, m) ->
            val previous = PortfolioAnalytics.calculate(month.minusMonths(1), p.property.id, bookings)
            Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(p.property.name, style = MaterialTheme.typography.titleLarge)
                Text(if(p.property.businessType=="OWNED")"Immobile proprio / in comproprietà" else "Immobile in gestione",color=ElledueGold,style=MaterialTheme.typography.labelMedium)
                if(analysisTab!=0)Text("Lordo prenotazioni ${Amounts.euro(m.gross)}")
                if(analysisTab==2 && p.property.businessType!="OWNED") Text("Compenso di gestione Elledue ${Amounts.euro(m.fees)}",color=MaterialTheme.colorScheme.primary)
                // Only the visual ratio uses floats; all accounting calculations use exact cents.
                LinearProgressIndicator(progress = { if(analysisTab==0)(m.calendarOccupiedDays.toFloat()/m.calendarDays) else (m.gross.toDouble() / maxGross).toFloat() }, modifier = Modifier.fillMaxWidth().height(6.dp), color = propertyColor(p.property.id))
                Text("${m.bookings} ${if (m.bookings == 1) "prenotazione" else "prenotazioni"} · ${m.nights} notti · media ${m.averageStay.toPlainString().replace('.', ',')} notti")
                Text("Occupazione calendario ${m.calendarPercent.toPlainString().replace('.', ',')}% (${m.calendarOccupiedDays}/${m.calendarDays})")
                if(analysisTab!=0)Text("Lordo rispetto al mese precedente: ${Amounts.euro(m.gross - previous.gross)}", style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth().height(92.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
                    val trend = (5 downTo 0).map { offset -> val mo = month.minusMonths(offset.toLong()); val metric=PortfolioAnalytics.calculate(mo,p.property.id,bookings);mo to if(analysisTab==0)metric.nights.toLong() else metric.gross }
                    val max = trend.maxOf { it.second }.coerceAtLeast(1)
                    trend.forEach { (mo, value) -> Column(Modifier.weight(1f), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth(.7f).height((value.toDouble() / max * 64).dp).background(propertyColor(p.property.id).copy(alpha=if(mo==month)1f else .4f), MaterialTheme.shapes.small))
                        Text(mo.monthValue.toString(), style = MaterialTheme.typography.labelSmall)
                    } }
                }
                Text(if(analysisTab==0)"Notti vendute · ultimi 6 mesi" else "Andamento lordo · ultimi 6 mesi", style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
            }
        }
        if (properties.isEmpty()) item { EmptyState("Nessun dato da confrontare", "Aggiungi immobili e prenotazioni per vedere i dati effettivi.") }
    }
}

@Composable fun OwnBusinessCard(totals:BusinessTotals) {
    Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=ConceptCard),border=BorderStroke(1.dp,ArrivalColor.copy(alpha=.3f))){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text("Immobili propri / in comproprietà",style=MaterialTheme.typography.titleMedium,color=ArrivalColor)
        AmountLine("Ricavi prenotazioni registrati",totals.ownGross);AmountLine("Netto dopo OTA",totals.ownNet);AmountLine("Costi operativi",totals.ownCosts)
        Text("Risultato operativo complessivo",style=MaterialTheme.typography.labelMedium);Text(Amounts.euro(totals.ownResult),style=MaterialTheme.typography.headlineLarge)
        Text("Nessun compenso di gestione o saldo con proprietari esterni. Quote dei soci non configurate: nessuna ripartizione automatica.",style=MaterialTheme.typography.bodySmall)
        if(totals.legacyOwnBookings>0)Text("${totals.legacyOwnBookings} soggiorni conservano il modello storico in gestione: verifica la classificazione, senza ricalcoli automatici.",color=ElledueGold)
    }}
}

@Composable private fun OperationalAnalysisSummary(month:YearMonth,properties:List<PropertyWithOwner>,bookings:List<ReservationEntity>) {
 val metrics=properties.map{PortfolioAnalytics.calculate(month,it.property.id,bookings)}
 val sold=metrics.sumOf{it.calendarOccupiedDays};val available=metrics.sumOf{it.calendarDays}
 val ratio=if(available==0)0f else sold.toFloat()/available
 val start=month.atDay(1).toEpochDay();val end=month.plusMonths(1).atDay(1).toEpochDay()
 val ids=properties.map{it.property.id}.toSet();val active=bookings.filter{it.propertyId in ids && it.status!="CANCELLED"}
 OutlinedCard(Modifier.fillMaxWidth()){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
  Text("Occupazione reale",style=MaterialTheme.typography.titleLarge)
  Row(horizontalArrangement=Arrangement.spacedBy(20.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
   Box(Modifier.size(104.dp),contentAlignment=androidx.compose.ui.Alignment.Center){
    Canvas(Modifier.fillMaxSize().padding(10.dp)){drawArc(ConceptBorder,-90f,360f,false,style=androidx.compose.ui.graphics.drawscope.Stroke(10.dp.toPx()));drawArc(ElledueGold,-90f,ratio*360f,false,style=androidx.compose.ui.graphics.drawscope.Stroke(10.dp.toPx(),cap=androidx.compose.ui.graphics.StrokeCap.Round))}
    Text("${java.math.BigDecimal.valueOf((ratio*100).toDouble()).setScale(1,java.math.RoundingMode.HALF_UP)}%",style=MaterialTheme.typography.titleLarge)
   }
   Column(verticalArrangement=Arrangement.spacedBy(7.dp)){Text("$sold notti vendute",style=MaterialTheme.typography.titleMedium);Text("$available notti calendario disponibili",style=MaterialTheme.typography.bodySmall);Text("${active.count{it.checkInEpochDay in start until end}} check-in · ${active.count{it.checkOutEpochDay in start until end}} check-out",style=MaterialTheme.typography.bodySmall)}
  }
  Text("Confronto tra appartamenti",style=MaterialTheme.typography.titleMedium)
  properties.zip(metrics).sortedByDescending{it.second.calendarOccupiedDays}.forEach{(p,m)->
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(p.property.name,Modifier.weight(1f));Text("${m.nights}/${m.calendarDays} notti",style=MaterialTheme.typography.labelMedium)}
   LinearProgressIndicator(progress={m.calendarOccupiedDays.toFloat()/m.calendarDays},modifier=Modifier.fillMaxWidth().height(8.dp),color=propertyColor(p.property.id))
  }
  Text("Notti uniche: eventuali sovrapposizioni non aumentano l’occupazione. Nessuna disponibilità commerciale o blocco inventato.",style=MaterialTheme.typography.bodySmall,color=ConceptMuted)
 }}
}
