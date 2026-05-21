package com.example.data

import kotlinx.coroutines.flow.Flow

class PlayerRepository(private val playerDao: PlayerDao) {
    val allPlayers: Flow<List<Player>> = playerDao.getAllPlayersFlow()

    suspend fun insert(player: Player) {
        playerDao.insertPlayer(player)
    }

    suspend fun delete(id: Int) {
        playerDao.deletePlayer(id)
    }

    suspend fun clear() {
        playerDao.clearPlayers()
    }
}
