package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class Player(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val inscribedAt: Long = System.currentTimeMillis(),
    val category: String = "Masculino" // "Masculino" or "Feminino"
)
