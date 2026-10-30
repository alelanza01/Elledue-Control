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
class ProductMigrationTest {
    @Test fun publishedSchemaFourUpgradesWithoutLosingAccountingAndAudit() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>(); val name = "migration-4-5.db"; context.deleteDatabase(name)
        val schema = JSONObject(File("schemas/it.elledue.control.data.AppDatabase/4.json").readText()).getJSONObject("database")
        val property = PropertyDraft(id = "p", ownerId = "o", name = "Casa storica", address = "Via Test", ownerName = "Proprietario storico", maxGuests = "4").toRecord()
        val booking = BookingDraft(id = "b", propertyId = "p", firstName = "Ospite", lastName = "Storico", guests = "3", checkIn = "30/12/2026", checkOut = "05/01/2027", gross = "1000", actualOta = "195").toRecord(null, false).copy(taxableGuests = 3, exemptGuests = 0, expectedTaxCents = 3600, collectedTaxCents = 3000)
        val cost = ExpenseDraft(id = "e", propertyId = "p", month = "2026-12", date = "30/12/2026", categoryId = "detergents", amount = "5").record(ExpenseCategoryEntity("detergents", "Detergenti"))
        val tax = TaxAssessmentEntity("b", 3, 0, 4, 300, 16, null, "", 3600, 3600, "", propertyId = "p")
        val cash = MoneyMovementEntity("cash", "p", "b", "COLLECTION", 3000, booking.checkOutEpochDay, "2026-12", "Confermato")
        val path = context.getDatabasePath(name); path.parentFile!!.mkdirs()
        try {
            SQLiteDatabase.openOrCreateDatabase(path, null).use { sqlite ->
                val entities = schema.getJSONArray("entities")
                for (i in 0 until entities.length()) {
                    val entity = entities.getJSONObject(i); val table = entity.getString("tableName")
                    sqlite.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                    val indices = entity.optJSONArray("indices"); if (indices != null) for (j in 0 until indices.length()) sqlite.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                    val records: List<Any> = when(table) {
                        "owners" -> listOf(property.owner); "properties" -> listOf(property.property); "reservations" -> listOf(booking); "expenses" -> listOf(cost); "expense_categories" -> listOf(ExpenseCategoryEntity("detergents", "Detergenti"))
                        "tax_assessments" -> listOf(tax); "money_movements" -> listOf(cash, cash.copy(id = "comp", kind = "COMPENSATION", sourceId = "cash"))
                        "expense_revisions" -> listOf(ExpenseRevisionEntity("er", "e", 1, AccountingRepository.snapshot(cost), "Inserimento storico", 1))
                        "tax_assessment_revisions" -> listOf(TaxAssessmentRevisionEntity("tr", "b", 1, AccountingRepository.snapshot(tax), 1))
                        "booking_financial_changes" -> listOf(BookingFinancialChangeEntity("bc", "b", AccountingRepository.snapshot(booking), AccountingRepository.snapshot(booking), "Verifica storica", 1))
                        "app_settings" -> listOf(AppSettings(organizationName = "Organizzazione storica")); "reports" -> listOf(ReportEntity("draft", "p", "2026-12", 1, "DRAFT", null, null)); else -> emptyList()
                    }
                    records.forEach { record ->
                        val values = ContentValues(); val fields = entity.getJSONArray("fields")
                        for (j in 0 until fields.length()) { val column = fields.getJSONObject(j).getString("columnName"); val field = record.javaClass.getDeclaredField(column); field.isAccessible = true
                            when(val value = field.get(record)) { null -> values.putNull(column); is String -> values.put(column, value); is Long -> values.put(column, value); is Int -> values.put(column, value); is Boolean -> values.put(column, if(value) 1 else 0) }
                        }; sqlite.insertOrThrow(table, null, values)
                    }
                }
                val setup = schema.getJSONArray("setupQueries"); for (i in 0 until setup.length()) sqlite.execSQL(setup.getString(i)); sqlite.version = 4
            }
            Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(DatabaseMigrations.MIGRATION_3_4, DatabaseMigrations.MIGRATION_4_5, DatabaseMigrations.MIGRATION_5_6, DatabaseMigrations.MIGRATION_6_7,DatabaseMigrations.MIGRATION_7_8, DatabaseMigrations.MIGRATION_8_9,DatabaseMigrations.MIGRATION_9_10).build().withClosing { db ->
                assertEquals(10, db.openHelper.readableDatabase.version); assertEquals("2026-12", db.reservationDao().get("b")!!.competenceMonth)
                assertEquals(3000L, db.reservationDao().get("b")!!.collectedTaxCents); assertEquals(500L, db.accountingDao().expense("e")!!.actualCents)
                assertEquals(2, db.accountingDao().movements().size); assertEquals(1, db.accountingDao().expenseHistory("e").size); assertEquals(1, db.accountingDao().taxHistory("b").size)
                assertEquals(1, db.accountingDao().observeFinancialChanges().first().size); assertTrue(db.reportDao().allocations().isEmpty())
                assertEquals("DRAFT", db.reportDao().get("draft")!!.status); assertEquals("Casa storica", db.propertyDao().get("p")!!.property.name)
                db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
            }
        } finally { context.deleteDatabase(name) }
    }
}
