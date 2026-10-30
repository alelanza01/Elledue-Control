package it.elledue.control.domain

import it.elledue.control.data.*

data class BusinessTotals(val managedGross:Long,val managedNet:Long,val fees:Long,val reimbursements:Long,val balance:Long,val ownGross:Long,val ownNet:Long,val ownCosts:Long,val ownResult:Long,val legacyOwnBookings:Int)
object BusinessAnalytics {
    fun calculate(month:String,properties:List<PropertyWithOwner>,bookings:List<ReservationEntity>,expenses:List<ExpenseEntity>,taxes:List<TaxAssessmentEntity>,money:List<MoneyMovementEntity>):BusinessTotals {
        val active=properties.filter{!it.property.archived};val managed=active.filter{it.property.businessType!="OWNED"}.map{it.property.id}.toSet();val own=active.filter{it.property.businessType=="OWNED"}.map{it.property.id}.toSet()
        val m=bookings.filter{it.propertyId in managed && it.appliedBusinessType!="OWNED"};val o=bookings.filter{it.propertyId in own && it.appliedBusinessType=="OWNED" && it.status!="CANCELLED" && it.competenceMonth==month}
        val summary=operationalSummary(month,"",m,expenses.filter{it.propertyId in managed},taxes.filter{it.propertyId in managed},money.filter{it.propertyId in managed})
        val current=m.filter{it.status!="CANCELLED" && it.competenceMonth==month};val costs=OperationalMoney.total(expenses.filter{it.propertyId in own && !it.voided && it.competenceMonth==month}.map{it.actualCents})
        val net=OperationalMoney.total(o.map{it.netCents})
        return BusinessTotals(OperationalMoney.total(current.map{it.grossCents}),OperationalMoney.total(current.map{it.netCents}),summary.fees,summary.reimbursable,summary.balance,OperationalMoney.total(o.map{it.grossCents}),net,costs,Math.subtractExact(net,costs),bookings.count{it.propertyId in own && it.appliedBusinessType!="OWNED" && it.competenceMonth==month && it.status!="CANCELLED"})
    }
}
