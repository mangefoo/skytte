package se.mindphaser.skytte.data

import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * Serializable snapshot of all app data. [LocalDate] values are rendered as
 * ISO-8601 strings so the JSON stays human-readable and library-independent.
 * Foreign-key ids are preserved so the file is a full backup, not just a report.
 */
@Serializable
data class BackupData(
    val version: Int = 2,
    val exportedAt: String,
    val weapons: List<WeaponDto>,
    val ammunition: List<AmmunitionDto>,
    val sessions: List<SessionDto>,
)

@Serializable
data class WeaponDto(
    val id: Long,
    val name: String,
    val caliber: String,
    val notes: String,
) {
    companion object {
        fun from(weapon: Weapon) = WeaponDto(weapon.id, weapon.name, weapon.caliber, weapon.notes)
    }

    fun toEntity() = Weapon(id = id, name = name, caliber = caliber, notes = notes)
}

@Serializable
data class AmmunitionDto(
    val id: Long,
    val name: String,
    val caliber: String,
    val notes: String,
    val costPerRound: Double? = null,
) {
    companion object {
        fun from(ammo: Ammunition) =
            AmmunitionDto(ammo.id, ammo.name, ammo.caliber, ammo.notes, ammo.costPerRound)
    }

    fun toEntity() = Ammunition(
        id = id, name = name, caliber = caliber, notes = notes, costPerRound = costPerRound
    )
}

@Serializable
data class SessionDto(
    val id: Long,
    val date: String,
    val location: String,
    val weaponId: Long?,
    val ammunitionId: Long?,
    val ammoCount: Int,
    val shootingType: String,
    val fee: Double? = null,
    val feeIncludesAmmo: Boolean = false,
) {
    companion object {
        fun from(session: Session) = SessionDto(
            id = session.id,
            date = session.date.toString(),
            location = session.location,
            weaponId = session.weaponId,
            ammunitionId = session.ammunitionId,
            ammoCount = session.ammoCount,
            shootingType = session.shootingType,
            fee = session.fee,
            feeIncludesAmmo = session.feeIncludesAmmo,
        )
    }

    fun toEntity() = Session(
        id = id,
        date = LocalDate.parse(date),
        location = location,
        weaponId = weaponId,
        ammunitionId = ammunitionId,
        ammoCount = ammoCount,
        shootingType = shootingType,
        fee = fee,
        feeIncludesAmmo = feeIncludesAmmo,
    )
}
