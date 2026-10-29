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
        val draftPayload=JSONObject(DraftStore.get(context).snapshot()).toString()
        val bytes = JSONObject().put("draftsPayload",draftPayload).put("draftsSha256",ReportCodec.hash(draftPayload)).put("format", "ElledueControlBackup").put("formatVersion", 1).put("databaseVersion", 9)
            .put("payload", payload).put("sha256", ReportCodec.hash(payload)).toString().toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_BYTES) { "Backup oltre il limite di 32 MiB." }; bytes
    }
    private fun parse(bytes: ByteArray): JSONObject {
        require(bytes.size in 1..MAX_BYTES) { "File vuoto o troppo grande." }
        val envelope = JSONObject(bytes.toString(Charsets.UTF_8))
        require(envelope.getString("format") == "ElledueControlBackup" && envelope.getInt("formatVersion") == 1 && envelope.getInt("databaseVersion") in 4..9) { "Formato o versione backup non compatibile." }
        val payload = envelope.getString("payload")
        require(ReportCodec.hash(payload) == envelope.getString("sha256")) { "Backup corrotto: checksum non valido." }
        return JSONObject(payload).also { tables ->
            if (envelope.getInt("databaseVersion") == 4) {
                val activities = tables.getJSONArray("activities")
                for (i in 0 until activities.length()) activities.getJSONObject(i).put("reservationId", JSONObject.NULL)
                val properties = tables.getJSONArray("properties")
                for (i in 0 until properties.length()) properties.getJSONObject(i).put("coverImageBase64", JSONObject.NULL)
                val settings = tables.getJSONArray("app_settings")
                for (i in 0 until settings.length()) settings.getJSONObject(i).put("operatorFirstName", "").put("operatorLastName", "").put("operatorPhotoBase64", JSONObject.NULL)
            }
            if(envelope.getInt("databaseVersion") < 6) {
                tables.put("guest_messages", JSONArray()).put("message_categories", JSONArray())
                val tasks=tables.getJSONArray("activities")
                for(i in 0 until tasks.length()) tasks.getJSONObject(i).put("notes", "")
                val settings=tables.getJSONArray("app_settings")
                for(i in 0 until settings.length()) settings.getJSONObject(i).put("homeCover", 0).put("messageCategoriesInitialized", 0)
            }
            if(envelope.getInt("databaseVersion")<7) {
                tables.put("report_booking_options",JSONArray())
                val tasks=tables.getJSONArray("activities");for(i in 0 until tasks.length())tasks.getJSONObject(i).put("kind","GENERAL").put("hasTime",1)
                listOf("properties" to "businessType","reservations" to "appliedBusinessType").forEach{(table,column)->val rows=tables.getJSONArray(table);for(i in 0 until rows.length())rows.getJSONObject(i).put(column,"MANAGED")}
                val taxes=tables.getJSONArray("tax_assessments");for(i in 0 until taxes.length())taxes.getJSONObject(i).put("consolidated",1)
            }
            if(envelope.getInt("databaseVersion")<8) {
                tables.put("property_issues",JSONArray()).put("issue_history",JSONArray()).put("stay_cost_forecasts",JSONArray())
                val properties=tables.getJSONArray("properties")
                for(i in 0 until properties.length())properties.getJSONObject(i).put("laundryGuestsPerSet",2).put("habitualTransportCents",JSONObject.NULL)
                val options=tables.getJSONArray("report_booking_options")
                for(i in 0 until options.length())options.getJSONObject(i).put("reason","").put("auditJson","[]")
            }
            if(envelope.getInt("databaseVersion")<9) {
                tables.put("maintenance_state",JSONArray()).put("standard_approvals",JSONArray())
                val costs=tables.getJSONArray("expenses");for(i in 0 until costs.length())costs.getJSONObject(i).put("recognition","ACTUAL").put("originKey",JSONObject.NULL).put("approvalBatchId",JSONObject.NULL).put("effectiveDateConfirmed",1)
                val moves=tables.getJSONArray("money_movements");for(i in 0 until moves.length())moves.getJSONObject(i).put("collector","ELLEDUE")
            }
        }.also { require(it.keys().asSequence().toSet() == TABLES.toSet()) { "Tabelle del backup incomplete o sconosciute." } }
    }
    private fun parseDrafts(bytes:ByteArray):Map<String,String> {
        val envelope=JSONObject(bytes.toString(Charsets.UTF_8));if(!envelope.has("draftsPayload"))return emptyMap()
        val raw=envelope.getString("draftsPayload");require(ReportCodec.hash(raw)==envelope.getString("draftsSha256")){"Checksum delle bozze non valido."}
        val values=JSONObject(raw);require(values.length()<=20000)
        return values.keys().asSequence().associateWith{key->
            require(key.length<=500 && listOf("issue/","booking/","property/","activity/","expense/","tax/","messages?","movement/","settings","profile","appearance","categories","notifications","report/","activities?").any{key.startsWith(it)}){"Chiave bozza non riconosciuta."}
            values.getString(key).also{require(it.length<=500000);com.google.gson.JsonParser.parseString(it)}
        }
    }
    suspend fun prepare(bytes: ByteArray): PreparedBackup {
        val safe = bytes.copyOf(); val tables = parse(safe); parseDrafts(safe)
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
        EconomicReconciliation(db).reconcile()
        DraftStore.get(context).replace(parseDrafts(verified.bytes))
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
        staged.openHelper.readableDatabase.query("SELECT coverImageBase64 FROM properties UNION ALL SELECT operatorPhotoBase64 FROM app_settings").use { c ->
            while (c.moveToNext()) LocalImages.validate(if (c.isNull(0)) null else c.getString(0))
        }
        staged.openHelper.readableDatabase.query("SELECT a.title, a.propertyId, b.propertyId FROM activities a LEFT JOIN reservations b ON a.reservationId = b.id WHERE a.reservationId IS NOT NULL").use { c ->
            while (c.moveToNext()) require(c.getString(0).isNotBlank() && c.getString(1) == c.getString(2)) { "Attività con collegamento incoerente." }
        }
        staged.openHelper.readableDatabase.query("SELECT title,notes,zoneId,kind FROM activities").use { c -> while(c.moveToNext()) { require(c.getString(0).isNotBlank() && c.getString(0).length<=300 && c.getString(1).length<=20000); java.time.ZoneId.of(c.getString(2));require(c.getString(3) in listOf("GENERAL","CLEANING","MAINTENANCE","REGISTRATION")) } }
        staged.openHelper.readableDatabase.query("SELECT label FROM message_categories").use{c->while(c.moveToNext())require(c.getString(0).isNotBlank() && c.getString(0).length<=100)}
        staged.settingsDao().get()?.let { require(it.homeCover in 0..3) }
        staged.messageDao().all().forEach { require(it.title.isNotBlank() && it.title.length<=300 && (it.italian.isNotBlank() || it.english.isNotBlank()) && it.italian.length<=20000 && it.english.length<=20000) }
        staged.openHelper.readableDatabase.query("SELECT businessType FROM properties UNION ALL SELECT appliedBusinessType FROM reservations").use{c->while(c.moveToNext())require(c.getString(0) in listOf("MANAGED","OWNED"))}
        staged.openHelper.readableDatabase.query("SELECT o.reportId, o.reservationId, r.propertyId, b.propertyId FROM report_booking_options o JOIN reports r ON r.id=o.reportId JOIN reservations b ON b.id=o.reservationId").use{c->while(c.moveToNext())require(c.getString(2)==c.getString(3))}
        staged.issueDao().all().forEach { issue ->
            require(issue.title.isNotBlank() && issue.title.length<=300 && issue.status in IssueRepository.statuses && issue.priority in IssueRepository.priorities)
            require(issue.reservationId==null || staged.reservationDao().get(issue.reservationId)?.propertyId==issue.propertyId)
            require(issue.expenseId==null || staged.accountingDao().expense(issue.expenseId)?.propertyId==issue.propertyId)
            require((issue.status=="RESOLVED")== (issue.resolvedAt!=null))
        }
        staged.openHelper.readableDatabase.query("SELECT laundryGuestsPerSet,habitualTransportCents FROM properties").use{c->while(c.moveToNext())require(c.getInt(0)>0 && (c.isNull(1)||c.getLong(1)>=0))}
        staged.openHelper.readableDatabase.query("SELECT quantity,unitCents,overrideCents,kind FROM stay_cost_forecasts").use{c->while(c.moveToNext()){OperationalMoney.cost(c.getString(0),c.getLong(1));require(c.getLong(1)>=0 && (c.isNull(2)||c.getLong(2)>=0) && c.getString(3) in listOf("CLEANING","LAUNDRY"))}}
        val dao = staged.reportDao(); val bookings = dao.bookings(); val costs = dao.expenses(); val taxes = dao.taxes(); val money = staged.accountingDao().movements()
        bookings.forEach { b ->
            require(b.checkOutEpochDay > b.checkInEpochDay && b.guestCount > 0 && Competence.booking(b.checkInEpochDay) == b.competenceMonth && b.status in listOf("CONFIRMED", "COMPLETED", "CANCELLED")) { "Prenotazione del backup non valida." }
            val total = BookingFinance.calculate(b.grossCents, b.appliedOtaBasisPoints, b.actualOtaCents, b.appliedManagementBasisPoints)
            require(total.otaCents == b.otaCents && total.netCents == b.netCents && total.managementCents == b.managementCents) { "Calcoli prenotazione incoerenti." }
        }
        costs.forEach { require(it.actualCents >= 0 && it.revision >= 1 && it.recognition in listOf("ACTUAL","STANDARD_APPROVED")); Competence.month(it.competenceMonth) }
        taxes.forEach { require(it.expectedCents >= 0 && it.taxableGuests >= 0 && it.exemptGuests >= 0 && it.nightCents >= 0 && it.taxableNights >= 0 && it.revision >= 1) { "Tassa non valida." } }
        money.forEach { m ->
            require(m.collector in listOf("ELLEDUE","OWNER","PORTAL","OTHER"))
            require(m.amountCents > 0 && m.kind in listOf("COLLECTION", "COMPENSATION", "REMITTANCE", "OWNER_PAYMENT", "REVERSAL")) { "Movimento non valido." }; Competence.month(m.competenceMonth)
            if (m.reservationId != null) require(bookings.any { it.id == m.reservationId && it.propertyId == m.propertyId })
            if(m.kind=="COMPENSATION")require(money.firstOrNull{it.id==m.sourceId}?.collector=="ELLEDUE")
            if (m.kind in listOf("COMPENSATION", "REMITTANCE")) require(money.any { it.id == m.sourceId && it.kind == "COLLECTION" && it.propertyId == m.propertyId && it.reservationId == m.reservationId })
            if (m.kind == "REVERSAL") require(m.notes.isNotBlank() && money.any { it.id == m.reversesId && it.kind != "REVERSAL" && it.amountCents == m.amountCents && it.propertyId == m.propertyId && it.reservationId == m.reservationId })
        }
        val approvalRows=mutableMapOf<String,org.json.JSONObject>()
        staged.openHelper.readableDatabase.query("SELECT id,reportId,approvedAt,actor,sourcesJson,previewHash FROM standard_approvals").use{c->while(c.moveToNext()){
            require(c.getLong(2)>0 && c.getString(3).isNotBlank() && Regex("[0-9a-f]{64}").matches(c.getString(5)))
            org.json.JSONArray(c.getString(4));require(staged.reportDao().get(c.getString(1))?.status=="CONFIRMED")
            approvalRows[c.getString(0)]=org.json.JSONObject().put("reportId",c.getString(1))
        }}
        costs.forEach{cost->if(cost.recognition=="STANDARD_APPROVED")require(cost.originKey==StayCostRepository.expenseId(requireNotNull(cost.reservationId),cost.kind) && cost.approvalBatchId in approvalRows)}
        val active = Ledger.active(money)
        active.filter { it.kind in listOf("COMPENSATION", "REMITTANCE") }.forEach { m ->
            val source = requireNotNull(active.firstOrNull { it.id == m.sourceId && it.kind == "COLLECTION" })
            require(OperationalMoney.total(active.filter { it.kind == m.kind && it.sourceId == m.sourceId }.map { it.amountCents }) <= source.amountCents)
        }
        dao.allocations().forEach { a ->
            Competence.month(a.yearMonth)
            require(when (a.kind) { "BOOKING" -> bookings.any { it.id == a.sourceId && it.propertyId == a.propertyId && it.competenceMonth == a.yearMonth }; "EXPENSE" -> costs.any { it.id == a.sourceId && it.propertyId == a.propertyId && it.competenceMonth == a.yearMonth }; "MOVEMENT" -> money.any { it.id == a.sourceId && it.propertyId == a.propertyId && it.competenceMonth == a.yearMonth }; else -> false }) { "Attribuzione rendiconto incoerente." }
        }
        val snapshots = mutableListOf<ReportSnapshot>()
        val reports = staged.openHelper.readableDatabase.query("SELECT id, propertyId, yearMonth, version, status, snapshotJson FROM reports")
        reports.use { while (it.moveToNext()) {
            if (it.getString(4) == "CONFIRMED") {
                val snapshot = ReportCodec.decode(it.getString(5)); snapshots.add(snapshot); require(snapshot.id == it.getString(0) && snapshot.property.id == it.getString(1) && snapshot.month == it.getString(2) && snapshot.version == it.getInt(3))
            }
        } }
        val allocations = dao.allocations()
        snapshots.forEach { snapshot ->
            snapshot.allocations().forEach { item -> require(allocations.contains(item)) { "Snapshot confermato senza attribuzione contabile coerente." } }
            snapshot.previousId?.let { id -> val previous = requireNotNull(dao.get(id)); require(previous.status == "CONFIRMED" && previous.propertyId == snapshot.property.id && previous.yearMonth == snapshot.month && previous.version < snapshot.version) { "Catena delle versioni non valida." } }
        }
        allocations.forEach { allocation -> require(snapshots.any { it.allocations().contains(allocation) }) { "Attribuzione priva di un rendiconto confermato." } }
    }
    companion object {
        const val MAX_BYTES = 32 * 1024 * 1024
        val TABLES = listOf("app_settings", "owners", "properties", "platform_commissions", "message_templates", "reservations", "expense_categories", "expenses", "activities", "reports", "expense_revisions", "tax_assessments", "tax_assessment_revisions", "money_movements", "booking_financial_changes", "report_allocations", "message_categories", "guest_messages", "report_booking_options", "property_issues", "issue_history", "stay_cost_forecasts", "maintenance_state", "standard_approvals")
    }
}
