package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "winners")
data class Winner(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val title: String,
    val player1: String,
    val player2: String,
    val score: String = "2-1",
    val imageUri: String? = null
)
