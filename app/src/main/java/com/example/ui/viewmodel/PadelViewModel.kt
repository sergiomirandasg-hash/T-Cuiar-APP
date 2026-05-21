package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Player
import com.example.data.PlayerDatabase
import com.example.data.PlayerRepository
import com.example.data.Winner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class PadelViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PlayerDatabase.getDatabase(application)
    private val repository = PlayerRepository(db.playerDao())
    private val winnerDao = db.winnerDao()

    val allPlayers: StateFlow<List<Player>>
    val allWinners: StateFlow<List<Winner>>
    val isScheduleBypassed = MutableStateFlow(false)

    init {
        allPlayers = repository.allPlayers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allWinners = winnerDao.getAllWinnersFlow().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed some epic past winners to make the Hall of Fame feel full and alive immediately
        viewModelScope.launch {
            val list = winnerDao.getAllWinnersFlow().first()
            if (list.isEmpty()) {
                loadDefaultWinners()
            }
        }
    }

    fun areInscriptionsOpen(): Boolean {
        val current = Calendar.getInstance()
        val saturdayGame = Calendar.getInstance()
        val dayOfWeek = current.get(Calendar.DAY_OF_WEEK)
        val daysToSaturday = (Calendar.SATURDAY - dayOfWeek + 7) % 7
        val correctedDays = if (daysToSaturday == 0 && (current.get(Calendar.HOUR_OF_DAY) > 10 || (current.get(Calendar.HOUR_OF_DAY) == 10 && current.get(Calendar.MINUTE) >= 30))) {
            7
        } else {
            daysToSaturday
        }
        saturdayGame.add(Calendar.DAY_OF_MONTH, correctedDays)
        saturdayGame.set(Calendar.HOUR_OF_DAY, 10)
        saturdayGame.set(Calendar.MINUTE, 30)
        saturdayGame.set(Calendar.SECOND, 0)
        saturdayGame.set(Calendar.MILLISECOND, 0)
        
        val openingTime = saturdayGame.clone() as Calendar
        openingTime.add(Calendar.DAY_OF_MONTH, -2) // Thursday is 2 days before Saturday
        openingTime.set(Calendar.HOUR_OF_DAY, 15)
        openingTime.set(Calendar.MINUTE, 0)
        openingTime.set(Calendar.SECOND, 0)
        openingTime.set(Calendar.MILLISECOND, 0)
        
        return current.timeInMillis >= openingTime.timeInMillis && current.timeInMillis < saturdayGame.timeInMillis
    }

    fun getNextOpeningDateTime(): String {
        val current = Calendar.getInstance()
        val saturdayGame = Calendar.getInstance()
        val dayOfWeek = current.get(Calendar.DAY_OF_WEEK)
        val daysToSaturday = (Calendar.SATURDAY - dayOfWeek + 7) % 7
        val correctedDays = if (daysToSaturday == 0 && (current.get(Calendar.HOUR_OF_DAY) > 10 || (current.get(Calendar.HOUR_OF_DAY) == 10 && current.get(Calendar.MINUTE) >= 30))) {
            7
        } else {
            daysToSaturday
        }
        saturdayGame.add(Calendar.DAY_OF_MONTH, correctedDays)
        saturdayGame.set(Calendar.HOUR_OF_DAY, 10)
        saturdayGame.set(Calendar.MINUTE, 30)
        saturdayGame.set(Calendar.SECOND, 0)
        saturdayGame.set(Calendar.MILLISECOND, 0)
        
        val openingTime = saturdayGame.clone() as Calendar
        openingTime.add(Calendar.DAY_OF_MONTH, -2) // Thursday is 2 days before Saturday
        openingTime.set(Calendar.HOUR_OF_DAY, 15)
        openingTime.set(Calendar.MINUTE, 0)
        openingTime.set(Calendar.SECOND, 0)
        openingTime.set(Calendar.MILLISECOND, 0)
        
        val monthNames = arrayOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")
        return "Quinta-feira, ${openingTime.get(Calendar.DAY_OF_MONTH)} de ${monthNames[openingTime.get(Calendar.MONTH)]} às 15:00"
    }

    fun addPlayer(name: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            onError("Por favor, introduza um nome válido.")
            return
        }

        // Check if inscriptions are schedule-restricted
        if (!areInscriptionsOpen() && !isScheduleBypassed.value) {
            onError("⚠️ Inscrições Fechadas! Abrem apenas às quintas-feiras às 15:00 (Missa de Sábado).")
            return
        }
        
        viewModelScope.launch {
            val list = allPlayers.value
            if (list.any { it.name.equals(trimmedName, ignoreCase = true) }) {
                onError("Este jogador já se encontra inscrito!")
                return@launch
            }
            
            repository.insert(Player(name = trimmedName))
            onSuccess()
        }
    }

    fun removePlayer(id: Int) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clear()
        }
    }

    fun loadSamplePlayers() {
        viewModelScope.launch {
            repository.clear()
            val sampleNames = listOf(
                "Sérgio Miranda", "Vânio Guerreiro", "Ivan Silva", "José Carlos",
                "João Paulo", "Rui Santos", "Pedro Costa", "Carlos Moura", "Miguel Oliveira",
                "Luís Fernandes", "Bruno Santos", "Tiago Pinto", "Nuno Martins", "Vasco Lima",
                "Jorge Rocha", "Gonçalo Rebelo", "Filipe Abreu", "Duarte Paiva", "José Neto",
                "Nelson Varela", "André Cruz", "Rafael Neves", "Manuel Ramos", "Diogo Jorge",
                "Henrique Soares", "Daniel Marques", "Mário Cabral", "Alexandre Cruz", "Vítor Gaspar",
                "Hugo Fonseca", "Ricardo Lopes", "Cláudio Silva", // Exactly 32
                "Guilherme Castro", "Gabriel Antunes", "Artur Mendes" // Reserves
            )
            sampleNames.forEach { name ->
                repository.insert(Player(name = name))
            }
        }
    }

    // --- Winner Hall of Fame Operations ---

    fun addWinner(title: String, date: String, p1: String, p2: String, score: String, imageUri: String?) {
        viewModelScope.launch {
            winnerDao.insertWinner(
                Winner(
                    title = title,
                    date = date,
                    player1 = p1,
                    player2 = p2,
                    score = score,
                    imageUri = imageUri
                )
            )
        }
    }

    fun removeWinner(id: Int) {
        viewModelScope.launch {
            winnerDao.deleteWinner(id)
        }
    }

    fun clearWinners() {
        viewModelScope.launch {
            winnerDao.clearWinners()
        }
    }

    private suspend fun loadDefaultWinners() {
        val winners = listOf(
            Winner(
                title = "Super Finalistas - Campo 4 🏆",
                date = "Sábado Passado",
                player1 = "Sérgio Miranda",
                player2 = "Vânio Guerreiro",
                score = "6-4"
            ),
            Winner(
                title = "Campeões da Missa Principal",
                date = "Sábado, 9 de Maio",
                player1 = "Ivan Silva",
                player2 = "José Carlos",
                score = "6-3"
            )
        )
        winners.forEach { winnerDao.insertWinner(it) }
    }
}
