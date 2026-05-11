package ch.lottomatchapp.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RoundDao {
    @Insert
    suspend fun insert(round: RoundEntity)

    @Query("SELECT * FROM rounds ORDER BY roundNumber ASC")
    fun observeAll(): Flow<List<RoundEntity>>

    @Query("SELECT * FROM rounds ORDER BY roundNumber ASC")
    suspend fun getAllOnce(): List<RoundEntity>

    @Query("DELETE FROM rounds")
    suspend fun deleteAll()
}
