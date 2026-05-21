package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players ORDER BY inscribedAt ASC")
    fun getAllPlayersFlow(): Flow<List<Player>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: Player)

    @Query("DELETE FROM players WHERE id = :id")
    suspend fun deletePlayer(id: Int)

    @Query("DELETE FROM players")
    suspend fun clearPlayers()
}
