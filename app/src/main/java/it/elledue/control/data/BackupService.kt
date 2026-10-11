package it.elledue.control.data

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import it.elledue.control.domain.*
import it.elledue.control.reports.*
import org.json.*

class PreparedBackup internal constructor(internal val bytes: ByteArray, val rowCount: Int)

/** Portable SQL data snapshot, no WAL copying. Validation happens in an isolated Room database. */
class BackupService(private val db: AppDatabase, private val context: Context) {
    suspend fun export(): ByteArray = db.withTransaction {
        val tables = JSONObject()
        val sql = db.openHelper.readableDatabase
        TABLES.forEach { table ->
            val rows = JSONArray()
            sql.query("SELECT * FROM `$table`").use { c ->
                while (c.moveToNext()) {
                    val row = JSONObject()
                    c.columnNames.forEachIndexed { i, name -> when (c.getType(i)) {
                        android.database.Cursor.FIELD_TYPE_NULL -> row.put(name, JSONObject.NULL)
                        android.database.Cursor.FIELD_TYPE_INTEGER -> row.put(name, c.getLong(i))
                        android.database.Cursor.FIELD_TYPE_STRING -> row.put(name, c.getString(i))
                        else -> throw IllegalArgumentException("Tipo di dato non supportato nel backup.")
                    } }; rows.put(row)
                }
            }; tables.put(table, rows)
        }
        val payload = tables.toString()
        val bytes = JSONObject().put("format", "ElledueControlBackup").put("formatVersion", 1).put("databaseVersion", 4)
            .put("payload", payload).put("sha256", ReportCodec.hash(payload)).toString().toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_BYTES) { "Backup oltre il limite di 32 MiB." }; bytes
    }
    private fun parse(bytes: ByteArray): JSONObject {
        require(bytes.size in 1..MAX_BYTES) { "File vuoto o troppo grande." }
        val envelope = JSONObject(bytes.toString(Charsets.UTF_8))
        require(envelope.getString("format") == "ElledueControlBackup" && envelope.getInt("formatVersion") == 1 && envelope.getInt("databaseVersion") == 4) { "Formato o versione backup non compatibile." }
        val payload = envelope.getString("payload")
        require(ReportCodec.hash(payload) == envelope.getString("sha256")) { "Backup corrotto: checksum non valido." }
        return JSONObject(payload).also { require(it.keys().asSequence().toSet() == TABLES.toSet()) { "Tabelle del backup incomplete o sconosciute." } }
    }
    suspend fun prepare(bytes: ByteArray): PreparedBackup {
        val safe = bytes.copyOf(); val tables = parse(safe)
        val staged = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            staged.withTransaction { load(staged.openHelper.writableDatabase, tables) }
            validate(staged)
            return PreparedBackup(safe, TABLES.sumOf { tables.getJSONArray(it).length() })
        } finally { staged.close() }
    }
    suspend fun restore(prepared: PreparedBackup, confirmed: Boolean = false) {
        require(confirmed) { "Conferma esplicitamente la sostituzione dei dati." }
        val verified = prepare(prepared.bytes)
        val data = parse(verified.bytes)
        db.withTransaction {
            val sql = db.openHelper.writableDatabase
            sql.execSQL("PRAGMA defer_foreign_keys = ON")
            TABLES.asReversed().forEach {
                if (it == "money_movements") {
                    sql.execSQL("DELETE FROM money_movements WHERE kind = 'REVERSAL'")
                    sql.execSQL("DELETE FROM money_movements WHERE kind IN ('COMPENSATION','REMITTANCE')")
                }
                sql.execSQL("DELETE FROM `$it`")
            }
            load(sql, data)
        }
        db.invalidationTracker.refreshVersionsAsync()
    }
    private fun load(sql: SupportSQLiteDatabase, data: JSONObject) {
        sql.execSQL("PRAGMA defer_foreign_keys = ON")
        TABLES.forEach { table ->
            val columns = sql.query("PRAGMA table_info(`$table`)").use { c -> buildList { while (c.moveToNext()) add(c.getString(1)) } }
            val rows = data.getJSONArray(table)
            require(rows.length() <= 200000) { "Troppi record nel backup." }
            val statement = sql.compileStatement("INSERT INTO `$table` (${columns.joinToString(",") { "`$it`" }}) VALUES (${columns.joinToString(",") { "?" }})")
            try { for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index); require(row.keys().asSequence().toSet() == columns.toSet()) { "Colonne del backup incompatibili: $table." }
                statement.clearBindings()
                columns.forEachIndexed { i, name -> when (val v = row.get(name)) {
                    JSONObject.NULL -> statement.bindNull(i + 1)
                    is String -> statement.bindString(i + 1, v)
                    is Int -> statement.bindLong(i + 1, v.toLong())
                    is Long -> statement.bindLong(i + 1, v)
                    else -> throw IllegalArgumentException("Valore non valido nel backup: $table.$name.")
                } }; statement.executeInsert()
            } } finally { statement.close() }
        }
        sql.query("PRAGMA foreign_key_check").use { require(!it.moveToFirst()) { "Relazioni del backup incoerenti." } }
    }
    private suspend fun validate(staged: AppDatabase) {
        val dao = staged.reportDao(); val bookings = dao.bookings(); val costs = dao.expenses(); val taxes = dao.taxes(); val money = staged.accountingDao().movements()
        bookings.forEach { b ->
            require(b.checkOutEpochDay > b.checkInEpochDay && b.guestCount > 0 && Competence.booking(b.checkInEpochDay) == b.competenceMonth && b.status in listOf("CONFIRMED", "COMPLETED", "CANCELLED")) { "Prenotazione del backup non valida." }
            val total = BookingFinance.calculate(b.grossCents, b.appliedOtaBasisPoints, b.actualOtaCents, b.appliedManagementBasisPoints)
            require(total.otaCents == b.otaCents && total.netCents == b.netCents && total.managementCents == b.managementCents) { "Calcoli prenotazione incoerenti." }
        }
        costs.forEach { require(it.actualCents >= 0 && it.revision >= 1); Competence.month(it.competenceMonth) }
        taxes.forEach { require(it.expectedCents >= 0 && it.taxableGuests >= 0 && it.exemptGuests >= 0 && it.nightCents >= 0 && it.taxableNights >= 0 && it.revision >= 1) { "Tassa non valida." } }
        money.forEach { m ->
            require(m.amountCents > 0 && m.kind in listOf("COLLECTION", "COMPENSATION", "REMITTANCE", "OWNER_PAYMENT", "REVERSAL")) { "Movimento non valido." }; Competence.month(m.competenceMonth)
            if (m.reservationId != null) require(bookings.any { it.id == m.reservationId && it.propertyId == m.propertyId })
            if (m.kind in listOf("COMPENSATION", "REMITTANCE")) require(money.any { it.id == m.sourceId && it.kind == "COLLECTION" && it.propertyId == m.propertyId && it.reservationId == m.reservationId })
            if (m.kind == "REVERSAL") require(m.notes.isNotBlank() && money.any { it.id == m.reversesId && it.kind != "REVERSAL" && it.amountCents == m.amountCents && it.propertyId == m.propertyId && it.reservationId == m.reservationId })
        }
        val active = Ledger.active(money)
        active.filter { it.kind in listOf("COMPENSATION", "REMITTANCE") }.forEach { m ->
            val source = requireNotNull(active.firstOrNull { it.id == m.sourceId && it.kind == "COLLECTION" })
            require(OperationalMoney.total(active.filter { it.kind == m.kind && it.sourceId == m.sourceId }.map { it.amountCents }) <= source.amountCents)
        }
        dao.allocations().forEach { a ->
            Competence.month(a.yearMonth)
            require(when (a.kind) { "BOOKING" -> bookings.any { it.id == a.sourceId && it.propertyId == a.propertyId && it.competenceMonth == a.yearMonth }; "EXPENSE" -> costs.any { it.id == a.sourceId && it.propertyId == a.propertyId && it.competenceMonth == a.yearMonth }; "MOVEMENT" -> money.any { it.id == a.sourceId && it.propertyId == a.propertyId && it.competenceMonth == a.yearMonth }; else -> false }) { "Attribuzione rendiconto incoerente." }
        }
        val reports = staged.openHelper.readableDatabase.query("SELECT id, propertyId, yearMonth, version, status, snapshotJson FROM reports")
        reports.use { while (it.moveToNext()) {
            if (it.getString(4) == "CONFIRMED") {
                val snapshot = ReportCodec.decode(it.getString(5)); require(snapshot.id == it.getString(0) && snapshot.property.id == it.getString(1) && snapshot.month == it.getString(2) && snapshot.version == it.getInt(3))
            }
        } }
    }
    companion object {
        const val MAX_BYTES = 32 * 1024 * 1024
        val TABLES = listOf("app_settings", "owners", "properties", "platform_commissions", "message_templates", "reservations", "expense_categories", "expenses", "activities", "reports", "expense_revisions", "tax_assessments", "tax_assessment_revisions", "money_movements", "booking_financial_changes", "report_allocations")
    }
}
