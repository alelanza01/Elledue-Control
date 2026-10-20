package it.elledue.control.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName="property_issues",foreignKeys=[
 ForeignKey(entity=PropertyEntity::class,parentColumns=["id"],childColumns=["propertyId"],onDelete=ForeignKey.RESTRICT),
 ForeignKey(entity=ReservationEntity::class,parentColumns=["id"],childColumns=["reservationId"],onDelete=ForeignKey.SET_NULL),
 ForeignKey(entity=ExpenseEntity::class,parentColumns=["id"],childColumns=["expenseId"],onDelete=ForeignKey.SET_NULL)
],indices=[Index("propertyId"),Index("reservationId"),Index("expenseId")])
data class PropertyIssue(
 @PrimaryKey val id:String=UUID.randomUUID().toString(),val propertyId:String="",val title:String="",
 val description:String="",val category:String="Manutenzione",val reportedDay:Long=it.elledue.control.domain.BookingDates.today().toEpochDay(),
 val priority:String="NORMAL",val status:String="OPEN",val reservationId:String?=null,
 val scheduledAt:Long?=null,val hasTime:Boolean=false,val assignee:String="",val notes:String="",
 val expenseId:String?=null,val resolvedAt:Long?=null,val updatedAt:Long=0,
)
@Entity(tableName="issue_history",foreignKeys=[ForeignKey(entity=PropertyIssue::class,parentColumns=["id"],childColumns=["issueId"],onDelete=ForeignKey.RESTRICT)],indices=[Index("issueId")])
data class IssueHistory(@PrimaryKey val id:String=UUID.randomUUID().toString(),val issueId:String,val recordedAt:Long,val snapshotJson:String)
@Dao interface IssueDao {
 @Query("SELECT * FROM property_issues ORDER BY reportedDay DESC") fun observe():Flow<List<PropertyIssue>>
 @Query("SELECT * FROM property_issues") suspend fun all():List<PropertyIssue>
 @Query("SELECT * FROM property_issues WHERE id=:id") suspend fun get(id:String):PropertyIssue?
 @Upsert suspend fun save(issue:PropertyIssue)
 @Insert suspend fun history(event:IssueHistory)
 @Query("SELECT * FROM issue_history WHERE issueId=:id ORDER BY recordedAt DESC") suspend fun history(id:String):List<IssueHistory>
}
class IssueRepository(private val db:AppDatabase) {
 val issues=db.issueDao().observe()
 suspend fun history(id:String)=db.issueDao().history(id)
 suspend fun save(value:PropertyIssue):String=db.withTransaction {
  val old=db.issueDao().get(value.id)
  require(old==null || old.updatedAt==value.updatedAt){"La segnalazione è cambiata: riapri la scheda."}
  val property=requireNotNull(db.propertyDao().get(value.propertyId)){"Seleziona un appartamento."}
  require(!property.property.archived || old?.propertyId==value.propertyId){"L’immobile è archiviato."}
  require(value.title.isNotBlank() && value.title.length<=300){"Inserisci un titolo (massimo 300 caratteri)."}
  require(value.description.length<=20000 && value.notes.length<=20000 && value.category.isNotBlank())
  require(value.status in statuses && value.priority in priorities)
  require(value.reservationId==null || db.reservationDao().get(value.reservationId)?.propertyId==value.propertyId){"Prenotazione di un altro appartamento."}
  require(value.expenseId==null || db.accountingDao().expense(value.expenseId)?.propertyId==value.propertyId){"Spesa di un altro appartamento."}
  val now=maxOf(System.currentTimeMillis(),(old?.updatedAt ?: 0)+1)
  val item=value.copy(title=value.title.trim(),updatedAt=now,resolvedAt=if(value.status=="RESOLVED")old?.resolvedAt ?: now else null)
  db.issueDao().save(item);db.issueDao().history(IssueHistory(issueId=item.id,recordedAt=now,snapshotJson=AccountingRepository.snapshot(item)));item.id
 }
 companion object {
  val statuses=linkedMapOf("OPEN" to "Aperto","ORGANIZE" to "Da organizzare","SCHEDULED" to "Intervento programmato","WORKING" to "In lavorazione","RESOLVED" to "Risolto","CANCELLED" to "Annullato / non pertinente")
  val priorities=linkedMapOf("LOW" to "Bassa","NORMAL" to "Normale","HIGH" to "Alta","URGENT" to "Urgente")
  fun open(i:PropertyIssue)=i.status !in listOf("RESOLVED","CANCELLED")
 }
}
