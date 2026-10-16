package it.elledue.control.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import it.elledue.control.reports.ReportCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Explicit, complete dependency deletion. No cascade deleting unrelated costs or shared owners. */
class DeletionPlan internal constructor(val kind:String,val id:String,val label:String,val counts:Map<String,Int>,val reportIds:Set<String>,internal val backup:ByteArray,internal val fingerprint:String)
class DeletionReceipt internal constructor(internal val fingerprint:String,val uri:Uri)
class DeletionRepository(private val db:AppDatabase,private val context:Context,private val beforeCommit:()->Unit={}) {
    private val backup=BackupService(db,context)
    suspend fun prepare(kind:String,id:String):DeletionPlan=db.withTransaction {
        require(kind in listOf("property","booking"))
        val booking=if(kind=="booking")requireNotNull(db.reservationDao().get(id)) else null
        val property=requireNotNull(db.propertyDao().get(booking?.propertyId ?: id))
        val bytes=backup.export();val tables=JSONObject(JSONObject(bytes.toString(Charsets.UTF_8)).getString("payload"))
        val allReports=mutableListOf<ReportEntity>()
        db.openHelper.readableDatabase.query("SELECT id FROM reports WHERE propertyId=?",arrayOf(property.property.id)).use{c->while(c.moveToNext())allReports.add(requireNotNull(db.reportDao().get(c.getString(0))))}
        val direct=allReports.filter {report->kind=="property" || report.yearMonth==booking!!.competenceMonth || report.snapshotJson?.let {json->
            val s=ReportCodec.decode(json);(s.bookings+s.linkedBookings.orEmpty()+s.excludedBookings.orEmpty()).any{it.id==id} || s.expenses.any{it.reservationId==id} || s.movements.any{it.reservationId==id}
        }==true}
        val periods=direct.map{it.yearMonth}.toSet();val reports=allReports.filter{it.yearMonth in periods}.map{it.id}.toSet()
        fun rows(table:String)=tables.getJSONArray(table).let{array->(0 until array.length()).map{array.getJSONObject(it)}}
        val costs=rows("expenses").filter{if(kind=="property")it.optString("propertyId")==id else it.optString("reservationId")==id}.map{it.getString("id")}.toSet()
        val counts=linkedMapOf<String,Int>()
        counts["Prenotazioni"]=rows("reservations").count{if(kind=="property")it.getString("propertyId")==id else it.getString("id")==id}
        counts["Spese collegate"]=costs.size
        counts["Attività"]=rows("activities").count{if(kind=="property")it.optString("propertyId")==id else it.optString("reservationId")==id}
        counts["Movimenti e storni"]=rows("money_movements").count{if(kind=="property")it.getString("propertyId")==id else it.optString("reservationId")==id}
        counts["Rendiconti e versioni da eliminare"]=reports.size
        counts["Conteggi tassa"]=rows("tax_assessments").count{if(kind=="property")it.getString("propertyId")==id else it.getString("reservationId")==id}
        counts["Revisioni spese"]=rows("expense_revisions").count{it.getString("expenseId") in costs}
        counts["Revisioni tassa"]=rows("tax_assessment_revisions").count{if(kind=="property")it.getString("reservationId") in rows("reservations").filter{r->r.getString("propertyId")==id}.map{r->r.getString("id")} else it.getString("reservationId")==id}
        return@withTransaction DeletionPlan(kind,id,if(booking==null)property.property.name else "${booking.guestFirstName} ${booking.guestLastName}",counts,reports,bytes,ReportCodec.hash(bytes.toString(Charsets.UTF_8)))
    }
    suspend fun saveVerifiedBackup(plan:DeletionPlan,uri:Uri):DeletionReceipt=withContext(Dispatchers.IO) {
        requireNotNull(context.contentResolver.openOutputStream(uri,"wt")).use{it.write(plan.backup)}
        val bytes=requireNotNull(context.contentResolver.openInputStream(uri)).use{it.readBytes()}
        require(bytes.contentEquals(plan.backup)){"Backup scritto non verificabile: eliminazione bloccata."}
        backup.prepare(bytes)
        DeletionReceipt(plan.fingerprint,uri)
    }
    suspend fun delete(plan:DeletionPlan,receipt:DeletionReceipt,confirmation:String,includeDocuments:Boolean)=db.withTransaction {
        require(confirmation=="ELIMINA ${plan.label}"){"Digita esattamente la conferma richiesta."}
        require(receipt.fingerprint==plan.fingerprint){"Prima salva e verifica il backup di questa anteprima."}
        require(plan.reportIds.isEmpty() || includeDocuments){"Conferma esplicitamente l’eliminazione dei rendiconti e di tutte le loro versioni."}
        require(ReportCodec.hash(backup.export().toString(Charsets.UTF_8))==plan.fingerprint){"I dati sono cambiati: aggiorna l’anteprima e salva un nuovo backup."}
        val sql=db.openHelper.writableDatabase
        val propertyId=if(plan.kind=="property")plan.id else requireNotNull(db.reservationDao().get(plan.id)).propertyId
        val ownerId=db.propertyDao().get(propertyId)!!.property.ownerId
        plan.reportIds.forEach {rid->
            val report=requireNotNull(db.reportDao().get(rid))
            sql.execSQL("DELETE FROM report_booking_options WHERE reportId=?",arrayOf(rid))
            sql.execSQL("DELETE FROM report_allocations WHERE propertyId=? AND yearMonth=?",arrayOf(propertyId,report.yearMonth))
            sql.execSQL("DELETE FROM reports WHERE id=?",arrayOf(rid))
        }
        val bookings=if(plan.kind=="booking")listOf(plan.id) else db.reportDao().bookings().filter{it.propertyId==propertyId}.map{it.id}
        val expenses=db.reportDao().expenses().filter{if(plan.kind=="property")it.propertyId==propertyId else it.reservationId==plan.id}.map{it.id}
        expenses.forEach{sql.execSQL("DELETE FROM expense_revisions WHERE expenseId=?",arrayOf(it));sql.execSQL("DELETE FROM report_allocations WHERE kind='EXPENSE' AND sourceId=?",arrayOf(it));sql.execSQL("DELETE FROM expenses WHERE id=?",arrayOf(it))}
        val money=db.accountingDao().movements().filter{if(plan.kind=="property")it.propertyId==propertyId else it.reservationId==plan.id}
        money.sortedBy{when(it.kind){"REVERSAL"->0;"COMPENSATION","REMITTANCE"->1;else->2}}.forEach{sql.execSQL("DELETE FROM report_allocations WHERE kind='MOVEMENT' AND sourceId=?",arrayOf(it.id));sql.execSQL("DELETE FROM money_movements WHERE id=?",arrayOf(it.id))}
        bookings.forEach {bid->
            listOf("activities","tax_assessment_revisions","tax_assessments","booking_financial_changes","report_booking_options").forEach{table->sql.execSQL("DELETE FROM `$table` WHERE reservationId=?",arrayOf(bid))}
            sql.execSQL("DELETE FROM report_allocations WHERE kind='BOOKING' AND sourceId=?",arrayOf(bid))
            sql.execSQL("DELETE FROM reservations WHERE id=?",arrayOf(bid))
        }
        if(plan.kind=="property") {
            listOf("activities","guest_messages","message_templates","platform_commissions").forEach{sql.execSQL("DELETE FROM `$it` WHERE propertyId=?",arrayOf(propertyId))}
            sql.execSQL("DELETE FROM properties WHERE id=?",arrayOf(propertyId))
            sql.execSQL("DELETE FROM owners WHERE id=? AND NOT EXISTS(SELECT 1 FROM properties WHERE ownerId=?)",arrayOf(ownerId,ownerId))
        }
        sql.query("PRAGMA foreign_key_check").use{require(!it.moveToFirst()){"Dipendenze incoerenti: nessuna cancellazione applicata."}}
        beforeCommit() // fault injection verifies rollback of the full transaction.
    }
}
