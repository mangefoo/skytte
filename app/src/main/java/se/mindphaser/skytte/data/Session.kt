package se.mindphaser.skytte.data

import java.time.LocalDate

/**
 * A shooting session. [id] is a Firestore document id; blank means "not yet persisted".
 * [weaponId]/[ammunitionId] reference Weapon/Ammunition document ids (or null). They are
 * resolved in-memory (see [SessionWithRefs]); a dangling/deleted reference simply resolves to
 * null, matching the old `onDelete = SET_NULL` behavior.
 */
data class Session(
    val id: String = "",
    val date: LocalDate,
    val location: String,
    val weaponId: String?,
    val ammunitionId: String?,
    val ammoCount: Int,
    val shootingType: String,
    val fee: Double? = null,
    val feeIncludesAmmo: Boolean = false
)

/** A session joined with its referenced weapon/ammunition (resolved in-memory, not by Firestore). */
data class SessionWithRefs(
    val session: Session,
    val weapon: Weapon?,
    val ammunition: Ammunition?
)

/** Total cost of the session: the fee, plus ammunition cost unless the fee already includes it. */
fun SessionWithRefs.totalCost(): Double {
    val fee = session.fee ?: 0.0
    if (session.feeIncludesAmmo) return fee
    return fee + (ammunition?.costPerRound ?: 0.0) * session.ammoCount
}
