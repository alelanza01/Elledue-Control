package it.elledue.control.data

import androidx.room.withTransaction
import it.elledue.control.domain.*
import it.elledue.control.reports.ReportCodec
import java.util.UUID

class FeedbackReconciliation(private val db:AppDatabase) {
    suspend fun reconcile(force:Boolean=false,propertyId:String?=null)=db.withTransaction {
        val sql=db.openHelper.writableDatabase
        if(!force)sql.query("SELECT 1 FROM maintenance_state WHERE id='economics-0411'").use{if(it.moveToFirst())return@withTransaction}
        val frozen=mutableSetOf<String>()
        sql.query("SELECT snapshotJson FROM reports WHERE status='CONFIRMED'").use{c->while(c.moveToNext()){
            val s=ReportCodec.decode(c.getString(0));(s.bookings+s.linkedBookings.orEmpty()+s.excludedBookings.orEmpty()).forEach{frozen.add(it.id)}
        }}
        for(b in db.reportDao().bookings()) {
            if((propertyId!=null && b.propertyId!=propertyId) || b.status=="CANCELLED" || b.id in frozen || b.accountingAllocationId!=null || db.reportDao().allocation("BOOKING",b.id)!=null)continue
            val property=db.propertyDao().get(b.propertyId) ?: continue
            val portal=property.platformCommissions.firstOrNull{it.platform.equals(b.platform,true)}
            val mode=portal?.otaTreatment?.takeIf{it!="INHERIT"} ?: if(b.platform.equals("Prenotazione diretta",true))"NONE" else property.property.otaTreatment
            val canOta=b.actualOtaCents==null && !b.otaTreatmentManual
            val candidate=b.copy(appliedOtaTreatment=if(canOta)mode else b.appliedOtaTreatment,
                appliedOtaVatBasisPoints=if(canOta)property.property.otaVatBasisPoints else b.appliedOtaVatBasisPoints,
                automaticTaxAdvance=if(b.taxAdvanceManual)b.automaticTaxAdvance else property.property.automaticTaxAdvance && property.property.businessType=="MANAGED" && b.appliedBusinessType=="MANAGED")
            val totals=BookingFinance.calculate(candidate)
            val saved=candidate.copy(otaCents=totals.otaCents,netCents=totals.netCents,managementCents=totals.managementCents)
            if(saved!=b) {
                val updated=saved.copy(updatedAt=maxOf(System.currentTimeMillis(),b.updatedAt+1))
                db.reservationDao().save(updated)
                db.accountingDao().insertFinancialChange(BookingFinancialChangeEntity(UUID.randomUUID().toString(),b.id,AccountingRepository.snapshot(b),AccountingRepository.snapshot(updated),"Adeguamento configurazione economica 0.4.11",System.currentTimeMillis()))
            }
        }
        if(propertyId==null)sql.execSQL("INSERT OR REPLACE INTO maintenance_state VALUES ('economics-0411',?,?)",arrayOf<Any>(System.currentTimeMillis(),"{\"version\":411}"))
    }
}
