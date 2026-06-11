package se.mindphaser.skytte.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Transaction
    @Query("SELECT * FROM sessions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<SessionWithRefs>>

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun byId(id: Long): SessionWithRefs?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: Session): Long

    @Update
    suspend fun update(session: Session)

    @Delete
    suspend fun delete(session: Session)
}
