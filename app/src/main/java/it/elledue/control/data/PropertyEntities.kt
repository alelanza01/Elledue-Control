package it.elledue.control.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation
import java.util.UUID

@Entity(tableName = "owners")
data class OwnerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String = "",
    val email: String = "",
)

@Entity(
    tableName = "properties",
    foreignKeys = [ForeignKey(entity = OwnerEntity::class, parentColumns = ["id"], childColumns = ["ownerId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("ownerId")],
)
data class PropertyEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val ownerId: String,
    val name: String,
    val address: String,
    val usefulContacts: String = "",
    val beds: Int = 1,
    val maxGuests: Int = 1,
    val checkInTime: String = "15:00",
    val checkOutTime: String = "10:00",
    val operationalNotes: String = "",
    val managementBasisPoints: Int = 2_000,
    val otaBasisPoints: Int = 1_950,
    val cleaningHourlyCents: Long = 800,
    val laundrySetCents: Long = 500,
    val touristTaxNightCents: Long = 300,
    val touristTaxMinAge: Int = 16,
    val touristTaxMaxNights: Int? = null,
    val touristTaxExemptions: String = "",
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val coverImageBase64: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "'MANAGED'") val businessType:String = "MANAGED",
)

@Entity(
    tableName = "platform_commissions",
    primaryKeys = ["propertyId", "platform"],
    foreignKeys = [ForeignKey(entity = PropertyEntity::class, parentColumns = ["id"], childColumns = ["propertyId"], onDelete = ForeignKey.RESTRICT)],
)
data class PlatformCommissionEntity(val propertyId: String, val platform: String, val basisPoints: Int)

/** Property-specific editable texts; full guest messaging workflow is milestone 6. */
@Entity(
    tableName = "message_templates",
    primaryKeys = ["propertyId", "kind", "language"],
    foreignKeys = [ForeignKey(entity = PropertyEntity::class, parentColumns = ["id"], childColumns = ["propertyId"], onDelete = ForeignKey.RESTRICT)],
)
data class MessageTemplateEntity(val propertyId: String, val kind: String, val language: String, val body: String)

data class PropertyWithOwner(
    @Embedded val property: PropertyEntity,
    @Relation(parentColumn = "ownerId", entityColumn = "id") val owner: OwnerEntity,
    @Relation(parentColumn = "id", entityColumn = "propertyId") val platformCommissions: List<PlatformCommissionEntity>,
    @Relation(parentColumn = "id", entityColumn = "propertyId") val templates: List<MessageTemplateEntity>,
)
