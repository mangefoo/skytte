package se.mindphaser.skytte.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weapons")
data class Weapon(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val caliber: String = "",
    val notes: String = ""
)
