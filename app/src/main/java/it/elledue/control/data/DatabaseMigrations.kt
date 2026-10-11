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
}
