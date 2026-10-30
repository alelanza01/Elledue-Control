package it.elledue.control

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.elledue.control.data.*
import it.elledue.control.reports.ReportCodec
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=android.app.Application::class)
class ConsolidatedMigrationTest {
    @Test fun publishedSevenMigratesToEightWithoutRecalculationOrLossOfConfirmedReports()=runBlocking<Unit> {
        val context=ApplicationProvider.getApplicationContext<Context>();val source=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        val snapshot=reportFixture(source,context)
        source.settingsDao().save(AppSettings(organizationName="Storica",operatorFirstName="Operatore"))
        source.activityDao().save(ActivityEntity("task",snapshot.property.id,"Storica",null,reservationId=snapshot.bookings.first().id))
        val current=source.reportDao().get(snapshot.id)!!
        val historical=JSONObject(current.snapshotJson!!).apply{
            remove("bookingOptions");remove("hiddenBookingIds");remove("excludedBookings")
            getJSONObject("property").remove("businessType")
            getJSONArray("bookings").let{rows->for(i in 0 until rows.length())rows.getJSONObject(i).remove("appliedBusinessType")}
            getJSONArray("taxes").let{rows->for(i in 0 until rows.length())rows.getJSONObject(i).remove("consolidated")}
        }.toString()
        source.reportDao().save(current.copy(snapshotJson=historical))
        val oldSnapshot=historical
        val tables=JSONObject(JSONObject(BackupService(source,context).export().toString(Charsets.UTF_8)).getString("payload"));source.close()
        val schema=JSONObject(File("schemas/it.elledue.control.data.AppDatabase/7.json").readText()).getJSONObject("database")
        val name="consolidated-migrate-7-8.db";context.deleteDatabase(name);val path=context.getDatabasePath(name);path.parentFile!!.mkdirs()
        try {
            SQLiteDatabase.openOrCreateDatabase(path,null).use {sql ->
                val entities=schema.getJSONArray("entities")
                for(i in 0 until entities.length()) {
                    val e=entities.getJSONObject(i);val table=e.getString("tableName")
                    sql.execSQL(e.getString("createSql").replace("\${TABLE_NAME}",table))
                    val indices=e.optJSONArray("indices");if(indices!=null)for(j in 0 until indices.length())sql.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}",table))
                    val rows=tables.getJSONArray(table);val fields=e.getJSONArray("fields")
                    for(j in 0 until rows.length()) {
                        val row=rows.getJSONObject(j);val cv=ContentValues()
                        for(k in 0 until fields.length()){val col=fields.getJSONObject(k).getString("columnName");when(val value=row.get(col)){JSONObject.NULL->cv.putNull(col);is String->cv.put(col,value);is Int->cv.put(col,value);is Long->cv.put(col,value)}}
                        sql.insertOrThrow(table,null,cv)
                    }
                }
                val setup=schema.getJSONArray("setupQueries");for(i in 0 until setup.length())sql.execSQL(setup.getString(i));sql.version=7
            }
            Room.databaseBuilder(context,AppDatabase::class.java,name).addMigrations(DatabaseMigrations.MIGRATION_7_8, DatabaseMigrations.MIGRATION_8_9,DatabaseMigrations.MIGRATION_9_10).build().withClosing {db ->
                assertEquals(10,db.openHelper.readableDatabase.version)
                assertTrue(db.reportDao().bookings().all{it.appliedBusinessType=="MANAGED"})
                assertEquals("MANAGED",db.propertyDao().get(snapshot.property.id)!!.property.businessType)
                assertEquals(2,db.propertyDao().get(snapshot.property.id)!!.property.laundryGuestsPerSet)
                assertTrue(db.issueDao().all().isEmpty());assertTrue(db.stayCostDao().get(snapshot.bookings.first().id).isEmpty())
                assertEquals(9,db.accountingDao().observeCategories().first().size)
                assertTrue(db.reportDao().taxes().all{it.consolidated})
                assertEquals("GENERAL",db.activityDao().get("task")!!.kind);assertTrue(db.activityDao().get("task")!!.hasTime)
                assertTrue(db.reportDao().options(snapshot.id).isEmpty())
                assertEquals(oldSnapshot,db.reportDao().get(snapshot.id)!!.snapshotJson)
                assertEquals(17000L,ReportCodec.decode(oldSnapshot!!).summary().balance)
                assertTrue(db.reportDao().allocations().isNotEmpty());assertTrue(db.accountingDao().movements().isNotEmpty())
                assertEquals("Operatore",db.settingsDao().get()!!.operatorFirstName);assertEquals(0,db.settingsDao().get()!!.homeCover)
                assertEquals("Storica",db.activityDao().get("task")!!.title);assertEquals("",db.activityDao().get("task")!!.notes)
                assertTrue(db.messageDao().all().isEmpty())
                db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use {assertFalse(it.moveToFirst())}
            }
        }finally{context.deleteDatabase(name)}
    }
}
