package ch.lottomatchapp.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RoundDao {
    @Insert
    fun insert(round: RoundEntity)

    @Query("SELECT * FROM rounds ORDER BY roundNumber ASC")
    fun getAll(): List<RoundEntity>

    @Query("SELECT * FROM rounds WHERE date = :date ORDER BY roundNumber ASC")
    fun getByDate(date: String): List<RoundEntity>

    @Query("DELETE FROM rounds")
    fun deleteAll()
}
