package it.elledue.control

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.elledue.control.data.*
import it.elledue.control.domain.*
import kotlinx.coroutines.flow.first
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
class AccountingMigrationTest {
    @Test fun publishedVersionTwoMigratesWithBookingExpenseAndAllHistoricalSettingsIntact() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>(); val name = "migration-2-3.db"; context.deleteDatabase(name)
        val schema = JSONObject(File("schemas/it.elledue.control.data.AppDatabase/2.json").readText()).getJSONObject("database")
        val path = context.getDatabasePath(name); path.parentFile!!.mkdirs()
        val property = PropertyDraft(name = "Casa storica", address = "Via Test", ownerName = "Proprietario", maxGuests = "4", platformRates = "Diretta=0").toRecord()
        val booking = BookingDraft(propertyId = property.property.id, firstName = "Ospite", lastName = "Storico", checkIn = "30/12/2026", checkOut = "05/01/2027", gross = "1000", actualOta = "150").toRecord(null, false)
        val expense = ExpenseEntity("historical-expense", property.property.id, booking.id, BookingDates.parse("02/01/2027").toEpochDay(), "Acquisto", "Detergenti", 500, true, true)
        val activity = ActivityEntity("activity", property.property.id, "Controllo", 1800000000000)
        val report = ReportEntity("report", property.property.id, "2026-11", 1, "DRAFT", null, null)
        try {
            SQLiteDatabase.openOrCreateDatabase(path, null).use { sqlite ->
                val entities = schema.getJSONArray("entities")
                for (i in 0 until entities.length()) {
                    val entity = entities.getJSONObject(i); val table = entity.getString("tableName")
                    sqlite.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                    val indices = entity.optJSONArray("indices")
                    if (indices != null) for (j in 0 until indices.length()) sqlite.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                    val record: Any? = when (table) {
                        "owners" -> property.owner; "properties" -> property.property; "reservations" -> booking; "expenses" -> expense
                        "app_settings" -> AppSettings(organizationName = "Gestione storica"); "activities" -> activity; "reports" -> report
                        "platform_commissions" -> property.platformCommissions.first(); "message_templates" -> MessageTemplateEntity(property.property.id, "access", "it", "Istruzioni storiche")
                        else -> null
                    }
                    if (record != null) {
                        val values = ContentValues(); val fields = entity.getJSONArray("fields")
                        for (j in 0 until fields.length()) {
                            val column = fields.getJSONObject(j).getString("columnName"); val field = record.javaClass.getDeclaredField(column); field.isAccessible = true
                            when (val value = field.get(record)) { null -> values.putNull(column); is String -> values.put(column, value); is Long -> values.put(column, value); is Int -> values.put(column, value); is Boolean -> values.put(column, if (value) 1 else 0) }
                        }
                        sqlite.insertOrThrow(table, null, values)
                    }
                }
                val queries = schema.getJSONArray("setupQueries"); for (i in 0 until queries.length()) sqlite.execSQL(queries.getString(i))
                sqlite.version = 2
            }
            Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(DatabaseMigrations.MIGRATION_2_3, DatabaseMigrations.MIGRATION_3_4, DatabaseMigrations.MIGRATION_4_5).build().withClosing { db ->
                val preserved = db.propertyDao().get(property.property.id)!!
                assertEquals("Casa storica", preserved.property.name); assertEquals("Proprietario", preserved.owner.name)
                assertEquals(0, preserved.platformCommissions.single().basisPoints); assertEquals("Istruzioni storiche", preserved.templates.single().body)
                val migrated = db.reservationDao().get(booking.id)!!
                assertEquals("2026-12", migrated.competenceMonth); assertEquals(15000L, migrated.actualOtaCents); assertEquals(17000L, migrated.managementCents)
                val cost = db.accountingDao().expense(expense.id)!!
                assertEquals(500L, cost.actualCents); assertEquals(booking.id, cost.reservationId); assertEquals("2027-01", cost.competenceMonth)
                assertEquals(8, db.accountingDao().observeCategories().first().size)
                assertTrue(db.accountingDao().movements().isEmpty()); assertTrue(db.accountingDao().observeAssessments().first().isEmpty())
                db.openHelper.readableDatabase.query("SELECT organizationName FROM app_settings").use { it.moveToFirst(); assertEquals("Gestione storica", it.getString(0)) }
                db.openHelper.readableDatabase.query("SELECT title FROM activities").use { it.moveToFirst(); assertEquals("Controllo", it.getString(0)) }
                db.openHelper.readableDatabase.query("SELECT status FROM reports").use { it.moveToFirst(); assertEquals("DRAFT", it.getString(0)) }
                assertEquals(5, db.openHelper.readableDatabase.version)
                AccountingRepository(db).saveExpense(ExpenseDraft.from(cost).copy(amount = "6", reason = "Rettifica dopo migrazione"))
                assertEquals(2, db.accountingDao().expenseHistory(cost.id).size)
            }
        } finally { context.deleteDatabase(name) }
    }
}
