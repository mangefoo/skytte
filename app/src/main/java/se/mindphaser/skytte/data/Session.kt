package se.mindphaser.skytte.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.LocalDate

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = Weapon::class,
            parentColumns = ["id"],
            childColumns = ["weaponId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Ammunition::class,
            parentColumns = ["id"],
            childColumns = ["ammunitionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("weaponId"), Index("ammunitionId")]
)
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val location: String,
    val weaponId: Long?,
    val ammunitionId: Long?,
    val ammoCount: Int,
    val shootingType: String
)

data class SessionWithRefs(
    @Embedded val session: Session,
    @Relation(parentColumn = "weaponId", entityColumn = "id")
    val weapon: Weapon?,
    @Relation(parentColumn = "ammunitionId", entityColumn = "id")
    val ammunition: Ammunition?
)
