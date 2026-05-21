package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WinnerDao {
    @Query("SELECT * FROM winners ORDER BY id DESC")
    fun getAllWinnersFlow(): Flow<List<Winner>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWinner(winner: Winner)

    @Query("DELETE FROM winners WHERE id = :id")
    suspend fun deleteWinner(id: Int)

    @Query("DELETE FROM winners")
    suspend fun clearWinners()
}
