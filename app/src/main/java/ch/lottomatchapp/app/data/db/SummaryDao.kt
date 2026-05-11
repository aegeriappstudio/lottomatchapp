package ch.lottomatchapp.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(summary: SummaryEntity)

    @Query("SELECT * FROM daily_summaries ORDER BY date DESC")
    fun getAll(): List<SummaryEntity>

    @Query("DELETE FROM daily_summaries")
    fun deleteAll()
}
