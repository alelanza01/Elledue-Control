package it.elledue.control.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import it.elledue.control.data.*
import it.elledue.control.domain.*

@Composable fun BusinessScopeSelector(value:String,change:(String)->Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=16.dp,vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
        listOf("MANAGED" to "In gestione","OWNED" to "Immobili propri","ALL" to "Tutti").forEach{(id,label)->ConceptPill(value==id,{change(id)},label={Text(label)},modifier=Modifier.testTag("business-$id"))}
    }
}
@Composable fun ArchiveScreen(properties:List<PropertyWithOwner>,bookings:List<ReservationEntity>,data:AccountingData?,reports:List<ReportEntity>,open:(String)->Unit,back:()->Unit) {
    LazyColumn(Modifier.fillMaxSize().testTag("archive-scroll"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item{TextButton(onClick=back){Text("Indietro")};ConceptHeading("Archivio storico","Escluso dalle viste operative ordinarie")}
        if(properties.none{it.property.archived})item{EmptyState("Nessun immobile archiviato","Lo storico rimane qui quando archivi un immobile.")}
        items(properties.filter{it.property.archived},key={it.property.id}){p->Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            PortfolioTile(p){open("property/${p.property.id}")}
            val stays=bookings.filter{it.propertyId==p.property.id};val costs=data?.expenses.orEmpty().filter{it.propertyId==p.property.id && !it.voided}
            Text("${stays.size} prenotazioni · ${costs.size} spese · dati storici",style=MaterialTheme.typography.bodySmall)
            stays.forEach{b->TextButton(onClick={open("booking/${b.id}")}){Text("${b.guestFirstName} ${b.guestLastName} · ${BookingDates.display(b.checkInEpochDay)}")}}
            costs.forEach{e->TextButton(onClick={open("expense/${e.id}")}){Text("${e.description} · ${Amounts.euro(e.actualCents)}")}}
            reports.filter{it.propertyId==p.property.id}.forEach{r->TextButton(onClick={open("report/${r.id}")}){Text("Rendiconto ${r.yearMonth} · v${r.version} · ${r.status}")}}
        }}
    }
}
