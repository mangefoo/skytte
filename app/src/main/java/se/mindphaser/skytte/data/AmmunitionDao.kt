package se.mindphaser.skytte.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AmmunitionDao {
    @Query("SELECT * FROM ammunition ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Ammunition>>

    @Query("SELECT * FROM ammunition WHERE id = :id")
    suspend fun byId(id: Long): Ammunition?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ammo: Ammunition): Long

    @Update
    suspend fun update(ammo: Ammunition)

    @Delete
    suspend fun delete(ammo: Ammunition)
}
