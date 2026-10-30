package it.elledue.control.data

import kotlinx.coroutines.flow.*

/** Projections for normal operations; raw/history repositories remain available only explicitly. */
class OperationalRepository(private val db:AppDatabase) {
    fun properties(type:String="ALL")=db.propertyDao().observeActive(type)
    val bookings=db.reservationDao().observeOperational()
    companion object {
        fun propertyIds(items:List<PropertyWithOwner>,type:String="ALL")=items.filter{!it.property.archived && (type=="ALL" || it.property.businessType==type)}.map{it.property.id}.toSet()
        fun accounting(data:it.elledue.control.ui.AccountingData?,ids:Set<String>)=data?.copy(expenses=data.expenses.filter{it.propertyId in ids},taxes=data.taxes.filter{it.propertyId in ids},movements=data.movements.filter{it.propertyId in ids},financialChanges=data.financialChanges)
    }
}

data class OperationalScope(val properties:List<PropertyWithOwner>,val bookings:List<ReservationEntity>)
fun OperationalRepository.scope(type:String):Flow<OperationalScope> = combine(properties(type),bookings){p,b->val ids=p.map{it.property.id}.toSet();OperationalScope(p,b.filter{it.propertyId in ids})}
