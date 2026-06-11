package se.mindphaser.skytte.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WeaponDao {
    @Query("SELECT * FROM weapons ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Weapon>>

    @Query("SELECT * FROM weapons WHERE id = :id")
    suspend fun byId(id: Long): Weapon?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(weapon: Weapon): Long

    @Update
    suspend fun update(weapon: Weapon)

    @Delete
    suspend fun delete(weapon: Weapon)
}
