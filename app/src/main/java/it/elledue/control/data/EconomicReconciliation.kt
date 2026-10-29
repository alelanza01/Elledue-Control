package it.elledue.control.data

import androidx.room.*
import it.elledue.control.domain.*
import it.elledue.control.reports.ReportCodec
import org.json.JSONObject

@Entity(tableName="maintenance_state")
data class MaintenanceState(@PrimaryKey val id:String,val completedAt:Long,val resultJson:String)
@Entity(tableName="standard_approvals",foreignKeys=[ForeignKey(entity=ReportEntity::class,parentColumns=["id"],childColumns=["reportId"],onDelete=ForeignKey.RESTRICT)],indices=[Index("reportId")])
data class StandardApproval(@PrimaryKey val id:String,val reportId:String,val approvedAt:Long,val actor:String,val sourcesJson:String,val previewHash:String)

/** One atomic upgrade pass. Repeating or retrying it never invents cash or a second cost. */
class EconomicReconciliation(private val db:AppDatabase) {
 suspend fun reconcile(force:Boolean=false):String=db.withTransaction {
  val sql=db.openHelper.writableDatabase
  if(!force)sql.query("SELECT resultJson FROM maintenance_state WHERE id='economics-0410'").use{if(it.moveToFirst())return@withTransaction it.getString(0)}
  val bookings=db.reportDao().bookings();var reviewed=0;var protected=0;var ambiguous=0
  val frozen=mutableSetOf<String>()
  sql.query("SELECT snapshotJson FROM reports WHERE status='CONFIRMED'").use{c->while(c.moveToNext()){val snapshot=ReportCodec.decode(c.getString(0));(snapshot.bookings+snapshot.linkedBookings.orEmpty()+snapshot.excludedBookings.orEmpty()).forEach{frozen.add(it.id)}}}
  for(b in bookings) {
   if(b.status=="CANCELLED")continue
   if(b.id in frozen || db.reportDao().allocation("BOOKING",b.id)!=null){protected++;continue}
   val p=db.propertyDao().get(b.propertyId)?.property ?: continue
   StayCostRepository(db).update(b,p)
   val tax=db.accountingDao().assessment(b.id)
   val money=db.accountingDao().movements().any{it.reservationId==b.id}
   if(!money && tax?.consolidated!=true && tax?.correctionReason.isNullOrBlank()) {
    // Unknown old exemptions remain unknown: do not substitute the new-form default.
    val subjects=b.taxableGuests ?: b.exemptGuests?.takeIf{it in 0..b.guestCount}?.let{b.guestCount-it} ?: tax?.taxableGuests
    if(subjects!=null && subjects in 0..b.guestCount)TaxForecast.update(db,b.copy(taxableGuests=subjects),p) else ambiguous++
   }
   reviewed++
  }
  val result=JSONObject().put("reviewed",reviewed).put("protected",protected).put("taxExemptionsUnknown",ambiguous).toString()
  sql.execSQL("INSERT OR REPLACE INTO maintenance_state VALUES ('economics-0410',?,?)",arrayOf(System.currentTimeMillis(),result));result
 }
}
