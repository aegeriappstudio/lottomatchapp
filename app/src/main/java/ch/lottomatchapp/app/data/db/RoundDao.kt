package ch.lottomatchapp.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RoundDao {
    /** Liefert die generierte Row-ID, damit ein Undo die Runde wieder löschen kann. */
    @Insert
    suspend fun insert(round: RoundEntity): Long

    @Query("SELECT * FROM rounds ORDER BY roundNumber ASC")
    fun observeAll(): Flow<List<RoundEntity>>

    @Query("SELECT * FROM rounds ORDER BY roundNumber ASC")
    suspend fun getAllOnce(): List<RoundEntity>

    @Query("SELECT * FROM rounds WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): RoundEntity?

    @Query("DELETE FROM rounds WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM rounds")
    suspend fun deleteAll()
}
