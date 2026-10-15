package it.elledue.control.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import it.elledue.control.domain.BookingFinance

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val columns = listOf(
                "expectedArrivalTime TEXT NOT NULL DEFAULT '15:00'", "actualArrivalTime TEXT",
                "expectedDepartureTime TEXT NOT NULL DEFAULT '10:00'", "actualDepartureTime TEXT",
                "otaCents INTEGER NOT NULL DEFAULT 0", "netCents INTEGER NOT NULL DEFAULT 0",
                "managementCents INTEGER NOT NULL DEFAULT 0", "notes TEXT NOT NULL DEFAULT ''",
                "overlapAcknowledged INTEGER NOT NULL DEFAULT 0", "createdAt INTEGER NOT NULL DEFAULT 0",
                "updatedAt INTEGER NOT NULL DEFAULT 0", "taxableGuests INTEGER", "exemptGuests INTEGER",
                "expectedTaxCents INTEGER", "collectedTaxCents INTEGER NOT NULL DEFAULT 0", "accountingAllocationId TEXT",
            )
            columns.forEach { db.execSQL("ALTER TABLE reservations ADD COLUMN $it") }
            // Even foundational v1 records keep their applied rates and get exact financial snapshots.
            db.query("SELECT id, propertyId, grossCents, appliedOtaBasisPoints, actualOtaCents, appliedManagementBasisPoints FROM reservations").use { cursor ->
                while (cursor.moveToNext()) {
                    val totals = BookingFinance.calculate(cursor.getLong(2), cursor.getInt(3), if (cursor.isNull(4)) null else cursor.getLong(4), cursor.getInt(5))
                    db.execSQL("UPDATE reservations SET otaCents = ?, netCents = ?, managementCents = ? WHERE id = ?", arrayOf(totals.otaCents, totals.netCents, totals.managementCents, cursor.getString(0)))
                }
            }
            db.execSQL("UPDATE reservations SET expectedArrivalTime = (SELECT checkInTime FROM properties WHERE id = propertyId), expectedDepartureTime = (SELECT checkOutTime FROM properties WHERE id = propertyId)")
        }
    }
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS expense_categories (id TEXT NOT NULL PRIMARY KEY, label TEXT NOT NULL, kind TEXT NOT NULL, archived INTEGER NOT NULL)")
            AccountingDefaults.categories.forEach { db.execSQL("INSERT OR IGNORE INTO expense_categories VALUES (?, ?, ?, ?)", arrayOf<Any>(it.id, it.label, it.kind, 0)) }
            db.execSQL("ALTER TABLE reservations ADD COLUMN competenceMonth TEXT NOT NULL DEFAULT ''")
            db.execSQL("UPDATE reservations SET competenceMonth = strftime('%Y-%m', checkInEpochDay * 86400, 'unixepoch')")
            val fields = listOf("competenceMonth TEXT NOT NULL DEFAULT ''", "categoryId TEXT REFERENCES expense_categories(id) ON DELETE RESTRICT", "kind TEXT NOT NULL DEFAULT 'OTHER'", "quantity TEXT", "unitCents INTEGER", "calculatedCents INTEGER", "notes TEXT NOT NULL DEFAULT ''", "setDefinition TEXT NOT NULL DEFAULT ''", "voided INTEGER NOT NULL DEFAULT 0", "revision INTEGER NOT NULL DEFAULT 1", "updatedAt INTEGER NOT NULL DEFAULT 0", "accountingAllocationId TEXT")
            fields.forEach { db.execSQL("ALTER TABLE expenses ADD COLUMN $it") }
            db.execSQL("UPDATE expenses SET competenceMonth = strftime('%Y-%m', dateEpochDay * 86400, 'unixepoch')")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_categoryId ON expenses(categoryId)")
            db.execSQL("CREATE TABLE IF NOT EXISTS expense_revisions (id TEXT NOT NULL PRIMARY KEY, expenseId TEXT NOT NULL, version INTEGER NOT NULL, snapshotJson TEXT NOT NULL, reason TEXT NOT NULL, recordedAt INTEGER NOT NULL, FOREIGN KEY(expenseId) REFERENCES expenses(id) ON DELETE RESTRICT)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_expense_revisions_expenseId_version ON expense_revisions(expenseId, version)")
            db.execSQL("CREATE TABLE IF NOT EXISTS tax_assessments (reservationId TEXT NOT NULL PRIMARY KEY, taxableGuests INTEGER NOT NULL, exemptGuests INTEGER NOT NULL, taxableNights INTEGER NOT NULL, nightCents INTEGER NOT NULL, minAge INTEGER NOT NULL, maxNights INTEGER, exemptions TEXT NOT NULL, calculatedCents INTEGER NOT NULL, expectedCents INTEGER NOT NULL, correctionReason TEXT NOT NULL, remittanceResponsible TEXT NOT NULL, remittanceNotes TEXT NOT NULL, needsReview INTEGER NOT NULL, revision INTEGER NOT NULL, propertyId TEXT NOT NULL, FOREIGN KEY(reservationId) REFERENCES reservations(id) ON DELETE RESTRICT)")
            db.execSQL("CREATE TABLE IF NOT EXISTS tax_assessment_revisions (id TEXT NOT NULL PRIMARY KEY, reservationId TEXT NOT NULL, version INTEGER NOT NULL, snapshotJson TEXT NOT NULL, recordedAt INTEGER NOT NULL, FOREIGN KEY(reservationId) REFERENCES reservations(id) ON DELETE RESTRICT)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tax_assessment_revisions_reservationId_version ON tax_assessment_revisions(reservationId, version)")
            db.execSQL("CREATE TABLE IF NOT EXISTS money_movements (id TEXT NOT NULL PRIMARY KEY, propertyId TEXT NOT NULL, reservationId TEXT, kind TEXT NOT NULL, amountCents INTEGER NOT NULL, dateEpochDay INTEGER NOT NULL, competenceMonth TEXT NOT NULL, notes TEXT NOT NULL, sourceId TEXT, reversesId TEXT, createdAt INTEGER NOT NULL, FOREIGN KEY(propertyId) REFERENCES properties(id) ON DELETE RESTRICT, FOREIGN KEY(reservationId) REFERENCES reservations(id) ON DELETE RESTRICT, FOREIGN KEY(sourceId) REFERENCES money_movements(id) ON DELETE RESTRICT, FOREIGN KEY(reversesId) REFERENCES money_movements(id) ON DELETE RESTRICT)")
            listOf("propertyId", "reservationId", "sourceId").forEach { db.execSQL("CREATE INDEX IF NOT EXISTS index_money_movements_$it ON money_movements($it)") }
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_money_movements_reversesId ON money_movements(reversesId)")
            db.execSQL("CREATE TABLE IF NOT EXISTS booking_financial_changes (id TEXT NOT NULL PRIMARY KEY, reservationId TEXT NOT NULL, previousJson TEXT NOT NULL, currentJson TEXT NOT NULL, reason TEXT NOT NULL, recordedAt INTEGER NOT NULL, FOREIGN KEY(reservationId) REFERENCES reservations(id) ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_booking_financial_changes_reservationId ON booking_financial_changes(reservationId)")
            // v2 had tax fields only as foundations, no assessment/riscossione workflow.
            // Keep any existing values without turning them into fabricated cash movements.
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            listOf("previousId TEXT", "revisionReason TEXT NOT NULL DEFAULT ''", "notes TEXT NOT NULL DEFAULT ''", "createdAt INTEGER NOT NULL DEFAULT 0").forEach { db.execSQL("ALTER TABLE reports ADD COLUMN $it") }
            db.execSQL("CREATE TABLE IF NOT EXISTS report_allocations (kind TEXT NOT NULL, sourceId TEXT NOT NULL, propertyId TEXT NOT NULL, yearMonth TEXT NOT NULL, PRIMARY KEY(kind, sourceId), FOREIGN KEY(propertyId) REFERENCES properties(id) ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_report_allocations_propertyId ON report_allocations(propertyId)")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE activities ADD COLUMN reservationId TEXT REFERENCES reservations(id) ON DELETE RESTRICT")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_activities_reservationId ON activities(reservationId)")
            db.execSQL("ALTER TABLE properties ADD COLUMN coverImageBase64 TEXT")
            db.execSQL("ALTER TABLE app_settings ADD COLUMN operatorFirstName TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE app_settings ADD COLUMN operatorLastName TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE app_settings ADD COLUMN operatorPhotoBase64 TEXT")
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE activities ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE app_settings ADD COLUMN homeCover INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE app_settings ADD COLUMN messageCategoriesInitialized INTEGER NOT NULL DEFAULT 0")
            db.execSQL("CREATE TABLE IF NOT EXISTS message_categories (id TEXT NOT NULL PRIMARY KEY, label TEXT NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS guest_messages (id TEXT NOT NULL PRIMARY KEY, propertyId TEXT, categoryId TEXT NOT NULL, title TEXT NOT NULL, italian TEXT NOT NULL, english TEXT NOT NULL, archived INTEGER NOT NULL, FOREIGN KEY(propertyId) REFERENCES properties(id) ON DELETE RESTRICT, FOREIGN KEY(categoryId) REFERENCES message_categories(id) ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_guest_messages_propertyId ON guest_messages(propertyId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_guest_messages_categoryId ON guest_messages(categoryId)")
        }
    }

}
