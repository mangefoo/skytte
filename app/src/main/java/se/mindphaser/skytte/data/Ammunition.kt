package se.mindphaser.skytte.data

/** A type of ammunition. [id] is a Firestore document id; blank means "not yet persisted". */
data class Ammunition(
    val id: String = "",
    val name: String,
    val caliber: String = "",
    val notes: String = "",
    val costPerRound: Double? = null
)
