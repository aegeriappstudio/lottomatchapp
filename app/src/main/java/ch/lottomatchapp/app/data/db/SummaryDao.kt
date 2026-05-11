package ch.lottomatchapp.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(summary: SummaryEntity)

    @Query("SELECT * FROM daily_summaries ORDER BY date DESC")
    fun observeAll(): Flow<List<SummaryEntity>>

    @Query("DELETE FROM daily_summaries")
    suspend fun deleteAll()
}
