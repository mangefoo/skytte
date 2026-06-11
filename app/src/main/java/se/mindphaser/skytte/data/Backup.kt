package se.mindphaser.skytte.data

import kotlinx.serialization.Serializable

/**
 * Serializable snapshot of all app data. [LocalDate] values are rendered as
 * ISO-8601 strings so the JSON stays human-readable and library-independent.
 * Foreign-key ids are preserved so the file is a full backup, not just a report.
 */
@Serializable
data class BackupData(
    val version: Int = 1,
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
}

@Serializable
data class AmmunitionDto(
    val id: Long,
    val name: String,
    val caliber: String,
    val notes: String,
) {
    companion object {
        fun from(ammo: Ammunition) = AmmunitionDto(ammo.id, ammo.name, ammo.caliber, ammo.notes)
    }
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
        )
    }
}
