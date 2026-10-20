package it.elledue.control.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import it.elledue.control.data.*
import it.elledue.control.domain.*
import kotlinx.coroutines.launch

@Composable fun StayOperationsPanel(booking:ReservationEntity,forecasts:List<StayCostForecast>,accounting:AccountingData?,issues:List<PropertyIssue>,tasks:List<ActivityEntity>,repository:StayCostRepository,open:(String)->Unit) {
 val costs=accounting?.expenses.orEmpty().filter{it.reservationId==booking.id && !it.voided}
 val tax=accounting?.taxes.orEmpty().firstOrNull{it.reservationId==booking.id}
 val money=Ledger.active(accounting?.movements.orEmpty()).filter{it.reservationId==booking.id}
 val collected=OperationalMoney.total(money.filter{it.kind=="COLLECTION"}.map{it.amountCents})
 Text("Riepilogo soggiorno",style=MaterialTheme.typography.titleLarge)
 Text("Tassa prevista ${tax?.expectedCents?.let(Amounts::euro) ?: "da verificare"} · riscossa ${Amounts.euro(collected)} · residua ${tax?.let{Amounts.euro(Ledger.residual(it.expectedCents,collected))} ?: "da verificare"}")
 Text("Riversata ${Amounts.euro(OperationalMoney.total(money.filter{it.kind=="REMITTANCE"}.map{it.amountCents}))}",style=MaterialTheme.typography.bodySmall)
 forecasts.filter{it.reservationId==booking.id}.forEach{f->
  val confirmed=costs.firstOrNull{it.kind==f.kind}
  if(confirmed!=null)OutlinedButton(onClick={open("expense/${confirmed.id}")}){Text("${confirmed.category} confermata · ${Amounts.euro(confirmed.actualCents)}")}
  else ForecastEditor(f,repository,booking,open)
 }
 costs.forEach{e->TextButton(onClick={open("expense/${e.id}")}){Text("${e.category} · ${Amounts.euro(e.actualCents)} · ${e.quantity ?: ""}")}}
 Text("Attività collegate",style=MaterialTheme.typography.titleMedium)
 val linked=tasks.filter{it.reservationId==booking.id}
 if(linked.isEmpty())Text("Nessuna attività collegata",color=ConceptMuted)
 linked.forEach{a->TextButton(onClick={open("activity/${a.id}")}){Text((if(a.completed)"✓ " else "○ ")+a.title)}}
 Text("Problemi e interventi",style=MaterialTheme.typography.titleMedium)
 issues.filter{it.reservationId==booking.id && IssueRepository.open(it)}.forEach{i->TextButton(onClick={open("issue/${i.id}")}){Text(i.title+" · "+IssueRepository.statuses[i.status])}}
 OutlinedButton(onClick={open("issue-create?property=${booking.propertyId}&booking=${booking.id}")}){Text("Segnala problema nel soggiorno")}
}
@Composable private fun ForecastEditor(f:StayCostForecast,repository:StayCostRepository,b:ReservationEntity,open:(String)->Unit) {
 var qty by remember(f){mutableStateOf(f.quantity)};var amount by remember(f){mutableStateOf(f.overrideCents?.let(Amounts::editable) ?: "")}
 val scope=rememberCoroutineScope();var error by remember{mutableStateOf<String?>(null)};var busy by remember{mutableStateOf(false)}
 OutlinedCard(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  Text(if(f.kind=="CLEANING")"Pulizie previste · da confermare" else "Lavanderia prevista · da confermare",style=MaterialTheme.typography.titleMedium)
  AccountingField(if(f.kind=="CLEANING")"Ore previste / effettive" else "Set previsti / effettivi",qty){qty=it}
  AccountingField("Importo effettivo € (facoltativo)",amount){amount=it}
  Text("Tariffa ${Amounts.euro(f.unitCents)} · previsione ${runCatching{Amounts.euro(OperationalMoney.cost(qty,f.unitCents))}.getOrDefault("da verificare")}")
  Text("La previsione non è una spesa sostenuta. Conferma il consuntivo dal check-out.",style=MaterialTheme.typography.bodySmall)
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   TextButton(enabled=!busy,onClick={busy=true;scope.launch{try{repository.save(f.copy(quantity=qty,overrideCents=amount.takeIf{it.isNotBlank()}?.let(Amounts::parseCents)))}catch(e:Exception){error=e.message}finally{busy=false}}}){Text("Salva previsione")}
   TextButton(enabled=!busy && BookingDates.today().toEpochDay()>=b.checkOutEpochDay,onClick={busy=true;scope.launch{try{val id=repository.confirm(f,qty,amount.takeIf{it.isNotBlank()}?.let(Amounts::parseCents));open("expense/$id")}catch(e:Exception){error=e.message}finally{busy=false}}},modifier=Modifier.testTag("confirm-cost-${f.kind}")){Text("Conferma spesa")}
  }
  error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
 }}
}
