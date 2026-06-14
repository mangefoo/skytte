package se.mindphaser.skytte.data

/** A firearm. [id] is a Firestore document id; blank means "not yet persisted". */
data class Weapon(
    val id: String = "",
    val name: String,
    val caliber: String = "",
    val notes: String = ""
)
