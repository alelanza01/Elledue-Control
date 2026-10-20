package it.elledue.control

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.elledue.control.data.*
import it.elledue.control.domain.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class DatabaseMigrationTest {
    @Test fun publishedVersionOneUpgradesAndPreservesPropertyOwnerSettingsAndBooking() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>(); val name = "migration-1-2.db"
        context.deleteDatabase(name)
        val schema = JSONObject(File("schemas/it.elledue.control.data.AppDatabase/1.json").readText()).getJSONObject("database")
        val path = context.getDatabasePath(name); path.parentFile!!.mkdirs()
        val item = PropertyDraft(name = "Immobile storico", address = "Via Test", ownerName = "Proprietario storico", checkInTime = "16:00", maxGuests = "4").toRecord()
        val booking = BookingDraft(propertyId = item.property.id, firstName = "Ospite", lastName = "Storico", checkIn = "10/06/2026", checkOut = "12/06/2026", gross = "1000").toRecord(null, false)
        try {
            SQLiteDatabase.openOrCreateDatabase(path, null).use { sqlite ->
                val entities = schema.getJSONArray("entities")
                for (i in 0 until entities.length()) {
                    val entity = entities.getJSONObject(i); val table = entity.getString("tableName")
                    sqlite.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                    val indices = entity.optJSONArray("indices")
                    if (indices != null) for (j in 0 until indices.length()) sqlite.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                    val record: Any? = when (table) { "owners" -> item.owner; "properties" -> item.property; "reservations" -> booking; "app_settings" -> AppSettings(organizationName = "Organizzazione storica"); else -> null }
                    if (record != null) {
                        val values = ContentValues(); val fields = entity.getJSONArray("fields")
                        for (j in 0 until fields.length()) {
                            val column = fields.getJSONObject(j).getString("columnName")
                            val field = record.javaClass.getDeclaredField(column); field.isAccessible = true
                            when (val value = field.get(record)) { null -> values.putNull(column); is String -> values.put(column, value); is Long -> values.put(column, value); is Int -> values.put(column, value); is Boolean -> values.put(column, if (value) 1 else 0) }
                        }
                        sqlite.insertOrThrow(table, null, values)
                    }
                }
                val queries = schema.getJSONArray("setupQueries")
                for (i in 0 until queries.length()) sqlite.execSQL(queries.getString(i))
                sqlite.version = 1
            }
            Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(DatabaseMigrations.MIGRATION_1_2, DatabaseMigrations.MIGRATION_2_3, DatabaseMigrations.MIGRATION_3_4, DatabaseMigrations.MIGRATION_4_5, DatabaseMigrations.MIGRATION_5_6, DatabaseMigrations.MIGRATION_6_7,DatabaseMigrations.MIGRATION_7_8).build().withClosing { db ->
                assertEquals("Immobile storico", db.propertyDao().get(item.property.id)!!.property.name)
                assertEquals("Proprietario storico", db.propertyDao().get(item.property.id)!!.owner.name)
                val migrated = db.reservationDao().get(booking.id)!!
                assertEquals(19500L, migrated.otaCents); assertEquals(80500L, migrated.netCents); assertEquals(16100L, migrated.managementCents)
                assertEquals("16:00", migrated.expectedArrivalTime)
                assertNull(migrated.expectedTaxCents); assertEquals(0L, migrated.collectedTaxCents)
                db.openHelper.readableDatabase.query("SELECT organizationName FROM app_settings").use { it.moveToFirst(); assertEquals("Organizzazione storica", it.getString(0)) }
                assertEquals(8, db.openHelper.readableDatabase.version)
            }
        } finally { context.deleteDatabase(name) }
    }
}
