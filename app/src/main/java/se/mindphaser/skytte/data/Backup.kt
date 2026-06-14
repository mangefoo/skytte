package se.mindphaser.skytte.data

import kotlinx.serialization.Serializable

/**
 * Serializable snapshot of all app data. [LocalDate] values are rendered as ISO-8601 strings so the
 * JSON stays human-readable and library-independent. Ids are Firestore document ids; foreign-key
 * ids are preserved so the file is a full backup, not just a report. (On import, fresh ids are
 * generated and references remapped, so the file is merged rather than clobbering existing docs.)
 */
@Serializable
data class BackupData(
    val version: Int = 3,
    val exportedAt: String,
    val weapons: List<WeaponDto>,
    val ammunition: List<AmmunitionDto>,
    val sessions: List<SessionDto>,
)

@Serializable
data class WeaponDto(
    val id: String,
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
    val id: String,
    val name: String,
    val caliber: String,
    val notes: String,
    val costPerRound: Double? = null,
) {
    companion object {
        fun from(ammo: Ammunition) =
            AmmunitionDto(ammo.id, ammo.name, ammo.caliber, ammo.notes, ammo.costPerRound)
    }
}

@Serializable
data class SessionDto(
    val id: String,
    val date: String,
    val location: String,
    val weaponId: String?,
    val ammunitionId: String?,
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
}
