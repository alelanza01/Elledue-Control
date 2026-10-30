package it.elledue.control.data

import androidx.room.*
import it.elledue.control.domain.*
import kotlinx.coroutines.flow.Flow

/** A proposal is not a posted expense. One stable expense ID is used on confirmation. */
@Entity(tableName="stay_cost_forecasts",primaryKeys=["reservationId","kind"],foreignKeys=[ForeignKey(entity=ReservationEntity::class,parentColumns=["id"],childColumns=["reservationId"],onDelete=ForeignKey.RESTRICT)])
data class StayCostForecast(val reservationId:String,val kind:String,val quantity:String,val unitCents:Long,val overrideCents:Long?=null,val manual:Boolean=false)
@Dao interface StayCostDao {
 @Query("SELECT * FROM stay_cost_forecasts") fun observe():Flow<List<StayCostForecast>>
 @Query("SELECT * FROM stay_cost_forecasts WHERE reservationId=:id") suspend fun get(id:String):List<StayCostForecast>
 @Upsert suspend fun save(item:StayCostForecast)
}
class StayCostRepository(private val db:AppDatabase) {
 val forecasts=db.stayCostDao().observe()
 suspend fun update(b:ReservationEntity,p:PropertyEntity) {
  if(b.status=="CANCELLED")return
  if(db.reportDao().allocation("BOOKING",b.id)!=null)return
  val old=db.stayCostDao().get(b.id)
  val existing=db.reportDao().expenses().filter{it.reservationId==b.id}
  StandardCostProjection.defaults(b,p).forEach { item ->
   if(old.firstOrNull{it.kind==item.kind}?.manual!=true && existing.none{it.kind==item.kind} && old.firstOrNull{it.kind==item.kind}!=item)db.stayCostDao().save(item)
  }
 }
 suspend fun save(item:StayCostForecast)=db.withTransaction {
  require(item.kind in listOf("CLEANING","LAUNDRY"));require(item.unitCents>=0);OperationalMoney.cost(item.quantity,item.unitCents)
  require(item.kind!="LAUNDRY" || OperationalMoney.quantity(item.quantity).stripTrailingZeros().scale()<=0){"I set di lavanderia devono essere interi."}
  require(item.overrideCents==null || item.overrideCents>=0);requireNotNull(db.reservationDao().get(item.reservationId))
  db.stayCostDao().save(item.copy(manual=true))
 }
 suspend fun confirm(item:StayCostForecast,actualQuantity:String,actualCents:Long?,date:Long=BookingDates.today().toEpochDay()):String=db.withTransaction {
  val b=requireNotNull(db.reservationDao().get(item.reservationId));require(b.status!="CANCELLED")
  require(date>=b.checkOutEpochDay){"Conferma il consuntivo dal giorno del check-out."}
  val id=expenseId(b.id,item.kind)
  val old=db.accountingDao().expense(id)
  require(db.reportDao().expenses().none{it.reservationId==b.id && it.kind==item.kind && !it.voided}){"Intervento già registrato: apri e rettifica la spesa esistente."}
  require(old==null){"Consuntivo già registrato: rettifica la spesa esistente."}
  AccountingRepository(db).initialize()
  val amount=actualCents ?: item.overrideCents ?: OperationalMoney.cost(actualQuantity,item.unitCents)
  AccountingRepository(db).saveExpense(ExpenseDraft(id=id,propertyId=b.propertyId,reservationId=b.id,categoryId=if(item.kind=="CLEANING")"cleaning" else "laundry",quantity=actualQuantity,unit=Amounts.editable(item.unitCents),amount=Amounts.editable(amount),date=BookingDates.display(date),month=Competence.booking(date),advanced=b.appliedBusinessType=="MANAGED",reimbursable=b.appliedBusinessType=="MANAGED"))
 }
 companion object {fun expenseId(booking:String,kind:String)="stay-cost:$booking:$kind"}
}
