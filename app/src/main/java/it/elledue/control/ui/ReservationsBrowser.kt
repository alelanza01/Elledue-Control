package it.elledue.control.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import it.elledue.control.data.*
import it.elledue.control.domain.*
import java.time.YearMonth

@Composable fun ReservationsBrowser(records:List<ReservationEntity>,properties:List<PropertyWithOwner>,onOpen:(String)->Unit,onNew:()->Unit,onCalendar:()->Unit) {
    var query by rememberSaveable{mutableStateOf("")};var property by rememberSaveable{mutableStateOf("")}
    var state by rememberSaveable{mutableStateOf("ALL")};var period by rememberSaveable{mutableStateOf("ALL")}
    var month by rememberSaveable{mutableStateOf(YearMonth.from(BookingDates.today()).toString())}
    var start by rememberSaveable{mutableStateOf("")};var end by rememberSaveable{mutableStateOf("")}
    var archived by rememberSaveable{mutableStateOf(false)};var descending by rememberSaveable{mutableStateOf(false)}
    var filters by rememberSaveable{mutableStateOf(false)}
    val available=properties.filter{archived || !it.property.archived};val ids=available.map{it.property.id}.toSet()
    LaunchedEffect(ids){if(property.isNotBlank() && property !in ids)property=""}
    val result=runCatching {
        val from=when(period){"MONTH"->YearMonth.parse(month).atDay(1).toEpochDay();"RANGE"->start.takeIf{it.isNotBlank()}?.let{BookingDates.parse(it).toEpochDay()};else->null}
        val to=when(period){"MONTH"->YearMonth.parse(month).atEndOfMonth().toEpochDay();"RANGE"->end.takeIf{it.isNotBlank()}?.let{BookingDates.parse(it).toEpochDay()};else->null}
        BookingSearch(query,property,state,from,to,descending).apply(records.filter{it.propertyId in ids})
    }
    LazyColumn(Modifier.fillMaxSize().testTag("reservations-browser"),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item {ConceptHeading("Prenotazioni","Cerca un ospite in tutto lo storico")}
        item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick=onNew,enabled=properties.any{!it.property.archived},modifier=Modifier.weight(1f).testTag("browser-new-booking")){Text("Nuova prenotazione")};TextButton(onClick=onCalendar){Text("Calendario")}}}
        item {OutlinedTextField(query,{query=it},label={Text("Cerca nome o cognome")},singleLine=true,modifier=Modifier.fillMaxWidth().testTag("booking-search"))}
        item {Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){BookingSearch.states.forEach{(value,label)->ConceptPill(state==value,{state=value},label={Text(label)},modifier=Modifier.testTag("browser-state-$value"))}}}
        item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton(onClick={filters=!filters},modifier=Modifier.weight(1f).testTag("browser-filters")){Text(if(filters)"Chiudi filtri" else "Immobile e periodo")};TextButton(onClick={descending=!descending},modifier=Modifier.weight(1f).testTag("browser-order")){Text(if(descending)"Check-in: decrescente" else "Check-in: crescente")}}}
        if(filters) {
            item {ConceptPropertySelector(available,property,allOption=true){property=it}}
            item {ChoiceField("Periodo",period,listOf("ALL" to "Tutto lo storico","MONTH" to "Mese","RANGE" to "Intervallo")){period=it}}
            if(period=="MONTH")item{Row{TextButton(onClick={month=YearMonth.parse(month).minusMonths(1).toString()}){Text("Precedente")};Text(month,Modifier.weight(1f).padding(12.dp));TextButton(onClick={month=YearMonth.parse(month).plusMonths(1).toString()}){Text("Successivo")}}}
            if(period=="RANGE")item {BookingDateField("Dal (facoltativo)",start,true,"browser-period-start",Modifier.fillMaxWidth()){start=it};BookingDateField("Al (facoltativo)",end,true,"browser-period-end",Modifier.fillMaxWidth()){end=it};Text("Include arrivi, partenze e soggiorni che toccano il periodo.",style=MaterialTheme.typography.bodySmall)}
            item {Row{Checkbox(archived,{archived=it;if(!it && available.firstOrNull{p->p.property.id==property}?.property?.archived==true)property=""},modifier=Modifier.testTag("browser-archived"));Text("Includi immobili archiviati",Modifier.padding(top=12.dp))}}
            item {TextButton(onClick={query="";property="";state="ALL";period="ALL";archived=false;start="";end=""}){Text("Azzera ricerca e filtri")}}
        }
        if(result.isFailure)item{Text(result.exceptionOrNull()?.message ?: "Controlla il periodo.",color=MaterialTheme.colorScheme.error)}
        val rows=result.getOrDefault(emptyList())
        item {Text("${rows.size} ${if(rows.size==1)"prenotazione" else "prenotazioni"} · "+when(period){"ALL"->"tutto lo storico";"MONTH"->month;else->"periodo selezionato"},style=MaterialTheme.typography.labelMedium,modifier=Modifier.testTag("browser-results"))}
        if(rows.isEmpty() && result.isSuccess)item{EmptyState("Nessuna prenotazione trovata","Prova un altro nome o modifica i filtri. Le annullate hanno una vista dedicata.")}
        items(rows,key={it.id}){b->BookingRow(b,available.firstOrNull{it.property.id==b.propertyId}?.let{it.property.name+if(it.property.archived)" · archivio" else ""} ?: "Immobile"){onOpen(b.id)}}
    }
}
