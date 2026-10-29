package it.elledue.control.data

import it.elledue.control.domain.*
import kotlinx.coroutines.flow.first

object StandardCostProjection {
 fun defaults(b:ReservationEntity,p:PropertyEntity):List<StayCostForecast> {
  if(b.status=="CANCELLED")return emptyList()
  require(p.laundryGuestsPerSet>0)
  return (if(b.appliedBusinessType=="MANAGED")listOf(StayCostForecast(b.id,"CLEANING","2",p.cleaningHourlyCents)) else emptyList())+StayCostForecast(b.id,"LAUNDRY",((b.guestCount.toLong()+p.laundryGuestsPerSet-1)/p.laundryGuestsPerSet).toString(),p.laundrySetCents)
 }
 data class Result(val proposed:List<ExpenseEntity>,val related:List<ExpenseEntity>,val warnings:List<String>)
 suspend fun forReport(db:AppDatabase,report:ReportEntity,bookings:List<ReservationEntity>,p:PropertyEntity,allCosts:List<ExpenseEntity>):Result {
  val warnings=mutableListOf<String>();val proposals=mutableListOf<ExpenseEntity>()
  val forecasts=db.stayCostDao().observe().first()
  for(b in bookings.filter{it.status!="CANCELLED" && it.appliedBusinessType=="MANAGED"}) {
   for(default in defaults(b,p)) {
    val linked=allCosts.filter{it.reservationId==b.id && it.kind==default.kind}
    if(linked.isNotEmpty()) {
     if(linked.count{!it.voided}>1)warnings.add("${b.guestFirstName} ${b.guestLastName}: più interventi ${if(default.kind=="CLEANING")"pulizia" else "lavanderia"} già registrati; verifica i collegamenti, nessun costo automatico aggiunto.")
     continue
    }
    val operationMonth=Competence.booking(b.checkOutEpochDay)
    val unlinked=allCosts.any{it.propertyId==b.propertyId && it.reservationId==null && it.kind==default.kind && !it.voided && it.competenceMonth in listOf(b.competenceMonth,operationMonth)}
    if(unlinked) {warnings.add("${b.guestFirstName} ${b.guestLastName}: ${if(default.kind=="CLEANING")"pulizia" else "lavanderia"} da associare. Esistono spese senza soggiorno nel periodo: nessuna seconda voce proposta.");continue}
    val f=forecasts.firstOrNull{it.reservationId==b.id && it.kind==default.kind} ?: default
    proposals.add(ExpenseEntity(id=StayCostRepository.expenseId(b.id,f.kind),propertyId=b.propertyId,reservationId=b.id,
     dateEpochDay=b.checkOutEpochDay,description=if(f.kind=="CLEANING")"Pulizia standard" else "Lavanderia · letto e asciugamani",
     category=if(f.kind=="CLEANING")"Pulizie" else "Lavanderia",actualCents=f.overrideCents ?: OperationalMoney.cost(f.quantity,f.unitCents),
     advancedByElledue=true,reimbursable=true,competenceMonth=report.yearMonth,categoryId=if(f.kind=="CLEANING")"cleaning" else "laundry",
     kind=f.kind,quantity=f.quantity,unitCents=f.unitCents,calculatedCents=OperationalMoney.cost(f.quantity,f.unitCents),setDefinition=if(f.kind=="LAUNDRY")"Set letto + asciugamani · ${p.laundryGuestsPerSet} ospiti" else "",
     recognition="PROPOSED",originKey=StayCostRepository.expenseId(b.id,f.kind),effectiveDateConfirmed=false))
   }
  }
  val related=allCosts.filter{!it.voided && it.reservationId in bookings.map{b->b.id} && it.competenceMonth!=report.yearMonth}
  return Result(proposals.sortedBy{it.id},related.sortedBy{it.id},warnings.distinct())
 }
}
