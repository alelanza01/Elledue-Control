package it.elledue.control

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.elledue.control.data.AppDatabase
import it.elledue.control.data.PropertyRepository
import it.elledue.control.domain.PropertyDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class PropertyRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: PropertyRepository
    private val draft = PropertyDraft(name = "Casa Test", address = "Via Test 1", ownerName = "Proprietario Test")
    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        repository = PropertyRepository(database)
    }
    @After fun tearDown() { database.close() }

    @Test fun createEditArchiveAndReactivateKeepIdentityAndHistory() = runBlocking {
        repository.save(draft)
        val created = repository.get(draft.id)!!
        repository.save(draft.copy(name = "Casa Modificata", managementPercent = "22,5"))
        repository.archive(draft.id, true)
        val archived = repository.get(draft.id)!!
        assertTrue(archived.property.archived)
        assertEquals("Casa Modificata", archived.property.name)
        assertEquals(created.property.createdAt, archived.property.createdAt)
        assertEquals(created.owner.id, archived.owner.id)
        assertEquals(2250, archived.property.managementBasisPoints)
        assertEquals(1, repository.properties.first().size)
        repository.save(PropertyDraft.from(archived).copy(notes = "Nota aggiornata"))
        assertTrue(repository.get(draft.id)!!.property.archived)
        repository.archive(draft.id, false)
        assertFalse(repository.get(draft.id)!!.property.archived)
    }
    @Test fun rejectedEditKeepsPreviouslySavedRecord() {
        runBlocking { repository.save(draft) }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.save(draft.copy(name = "")) } }
        runBlocking { assertEquals("Casa Test", repository.get(draft.id)!!.property.name) }
    }
    @Test fun settingsAndAllRelationsSurviveFileDatabaseReopening() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = "property-persistence.db"
        context.deleteDatabase(file)
        val full = draft.copy(platformRates = "Booking.com=19,5", accessItalian = "Istruzioni", taxMaxNights = "5", notes = "Nota persistente")
        try {
            Room.databaseBuilder(context, AppDatabase::class.java, file).build().withClosing { db ->
                PropertyRepository(db).save(full)
                PropertyRepository(db).archive(full.id, true)
            }
            Room.databaseBuilder(context, AppDatabase::class.java, file).build().withClosing { db ->
                val item = PropertyRepository(db).get(full.id)!!
                assertTrue(item.property.archived)
                assertEquals("Nota persistente", item.property.operationalNotes)
                assertEquals(1950, item.platformCommissions.single().basisPoints)
                assertEquals("Istruzioni", item.templates.single().body)
                assertEquals("Proprietario Test", item.owner.name)
            }
        } finally { context.deleteDatabase(file) }
    }
    @Test fun editingAccessInstructionsPreservesUnrelatedFutureTemplates() = runBlocking {
        repository.save(draft)
        database.openHelper.writableDatabase.execSQL(
            "INSERT INTO message_templates VALUES (?, ?, ?, ?)",
            arrayOf(draft.id, "welcome", "it", "Benvenuto personalizzato"),
        )
        repository.save(draft.copy(accessItalian = "Accesso aggiornato"))
        val texts = repository.get(draft.id)!!.templates
        assertEquals(2, texts.size)
        assertEquals("Benvenuto personalizzato", texts.single { it.kind == "welcome" }.body)
    }

    @Test fun futureReservationCannotBeOrphanedAndSurvivesArchiving() = runBlocking {
        repository.save(draft)
        val sql = database.openHelper.writableDatabase
        sql.execSQL("INSERT INTO reservations VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", arrayOf<Any?>("reservation-test", draft.id, "Ospite", "Test", 2, 20000, 20002, "Diretta", 10000, 0, null, 2000, "CONFIRMED"))
        repository.archive(draft.id, true)
        sql.query("SELECT COUNT(*) FROM reservations WHERE propertyId = ?", arrayOf(draft.id)).use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
        assertThrows(android.database.sqlite.SQLiteConstraintException::class.java) { sql.execSQL("DELETE FROM properties WHERE id = ?", arrayOf(draft.id)) }
        assertThrows(android.database.sqlite.SQLiteConstraintException::class.java) {
            sql.execSQL("INSERT INTO reservations VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", arrayOf<Any?>("orphan", "missing", "Ospite", "Test", 2, 20000, 20002, "Diretta", 10000, 0, null, 2000, "CONFIRMED"))
        }
        Unit
    }
}
