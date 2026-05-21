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

    val mixedPairs = MutableStateFlow<List<Pair<Player, Player>>>(emptyList())
    val mixedExtra = MutableStateFlow<Player?>(null)
    val masculinePairs = MutableStateFlow<List<Pair<Player, Player>>>(emptyList())
    val masculineExtra = MutableStateFlow<Player?>(null)

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

        // Auto-generate pairings when players are loaded/changed
        viewModelScope.launch {
            allPlayers.collect { players ->
                if (players.isNotEmpty() && mixedPairs.value.isEmpty() && masculinePairs.value.isEmpty()) {
                    generatePairings(players)
                }
            }
        }
    }

    fun generatePairings(playersList: List<Player>) {
        val activePlayers = playersList.take(32) // Top 32 active players only
        
        val femaleList = activePlayers.filter { it.category == "Feminino" }.shuffled()
        val maleList = activePlayers.filter { it.category == "Masculino" }.shuffled()

        // 1. Form active mixed pairs for courts 5 to 8 (each court gets 2 pairs -> up to 8 mixed pairs)
        val numMixedPairs = minOf(8, femaleList.size, maleList.size)
        val mixedList = mutableListOf<Pair<Player, Player>>()
        for (i in 0 until numMixedPairs) {
            mixedList.add(Pair(maleList[i], femaleList[i]))
        }
        
        val unusedFemales = femaleList.drop(numMixedPairs)
        val unusedMales = maleList.drop(numMixedPairs)
        
        // 2. Form active masculine pairs for courts 1 to 4 (each court gets 2 pairs -> up to 8 masculine pairs / 16 players)
        val numMascPairs = minOf(8, unusedMales.size / 2)
        val activeMalesForPairs = unusedMales.take(numMascPairs * 2)
        val mascPairsList = activeMalesForPairs.chunked(2).mapNotNull {
            if (it.size == 2) Pair(it[0], it[1]) else null
        }.toMutableList()
        
        val leftoverMales = unusedMales.drop(numMascPairs * 2)
        val leftoverFemales = unusedFemales

        // 3. Form extra mixed pairs (for waiting list) from leftover males and females
        val numExtraMixed = minOf(leftoverFemales.size, leftoverMales.size)
        for (i in 0 until numExtraMixed) {
            mixedList.add(Pair(leftoverMales[i], leftoverFemales[i]))
        }
        
        val finalLeftoverFemales = leftoverFemales.drop(numExtraMixed)
        val finalLeftoverMales = leftoverMales.drop(numExtraMixed)

        // 4. Form extra masculine pairs (for waiting list) from any leftover males
        val extraMascPairs = finalLeftoverMales.chunked(2).mapNotNull {
            if (it.size == 2) Pair(it[0], it[1]) else null
        }
        mascPairsList.addAll(extraMascPairs)
        
        val remainingLeftoverMales = finalLeftoverMales.drop(extraMascPairs.size * 2)

        // Assign active extras (suplentes)
        mixedPairs.value = mixedList
        mixedExtra.value = finalLeftoverFemales.firstOrNull()
        masculinePairs.value = mascPairsList
        masculineExtra.value = remainingLeftoverMales.firstOrNull()
    }

    fun swapPlayers(player1: Player, player2: Player) {
        val currentMascPairs = masculinePairs.value.map { it.copy() }.toMutableList()
        val currentMascExtra = masculineExtra.value
        val currentMixedPairs = mixedPairs.value.map { it.copy() }.toMutableList()
        val currentMixedExtra = mixedExtra.value

        // Locate positions of player1
        val p1InMascPairsIdx = currentMascPairs.indexOfFirst { it.first.id == player1.id || it.second.id == player1.id }
        val p1InMascPairsIsFirst = if (p1InMascPairsIdx != -1) currentMascPairs[p1InMascPairsIdx].first.id == player1.id else false
        val p1InMascExtra = currentMascExtra?.id == player1.id
        
        val p1InMixedPairsIdx = currentMixedPairs.indexOfFirst { it.first.id == player1.id || it.second.id == player1.id }
        val p1InMixedPairsIsFirst = if (p1InMixedPairsIdx != -1) currentMixedPairs[p1InMixedPairsIdx].first.id == player1.id else false
        val p1InMixedExtra = currentMixedExtra?.id == player1.id

        // Locate positions of player2
        val p2InMascPairsIdx = currentMascPairs.indexOfFirst { it.first.id == player2.id || it.second.id == player2.id }
        val p2InMascPairsIsFirst = if (p2InMascPairsIdx != -1) currentMascPairs[p2InMascPairsIdx].first.id == player2.id else false
        val p2InMascExtra = currentMascExtra?.id == player2.id
        
        val p2InMixedPairsIdx = currentMixedPairs.indexOfFirst { it.first.id == player2.id || it.second.id == player2.id }
        val p2InMixedPairsIsFirst = if (p2InMixedPairsIdx != -1) currentMixedPairs[p2InMixedPairsIdx].first.id == player2.id else false
        val p2InMixedExtra = currentMixedExtra?.id == player2.id

        var newMascExtra = currentMascExtra
        var newMixedExtra = currentMixedExtra

        fun setPlayerAtPosition(
            isMascPairs: Boolean, idx: Int, isFirst: Boolean, isMascExtra: Boolean,
            isMixedPairs: Boolean, idxMixed: Int, isFirstMixed: Boolean, isMixedExtra: Boolean,
            player: Player
        ) {
            if (isMascPairs && idx != -1) {
                val pair = currentMascPairs[idx]
                currentMascPairs[idx] = if (isFirst) Pair(player, pair.second) else Pair(pair.first, player)
            } else if (isMascExtra) {
                newMascExtra = player
            } else if (isMixedPairs && idxMixed != -1) {
                val pair = currentMixedPairs[idxMixed]
                currentMixedPairs[idxMixed] = if (isFirstMixed) Pair(player, pair.second) else Pair(pair.first, player)
            } else if (isMixedExtra) {
                newMixedExtra = player
            }
        }

        // Set player2 to player1's spot
        setPlayerAtPosition(
            p1InMascPairsIdx != -1, p1InMascPairsIdx, p1InMascPairsIsFirst, p1InMascExtra,
            p1InMixedPairsIdx != -1, p1InMixedPairsIdx, p1InMixedPairsIsFirst, p1InMixedExtra,
            player2
        )

        // Set player1 to player2's spot
        setPlayerAtPosition(
            p2InMascPairsIdx != -1, p2InMascPairsIdx, p2InMascPairsIsFirst, p2InMascExtra,
            p2InMixedPairsIdx != -1, p2InMixedPairsIdx, p2InMixedPairsIsFirst, p2InMixedExtra,
            player1
        )

        // Update with newly swapped structures
        masculinePairs.value = currentMascPairs
        masculineExtra.value = newMascExtra
        mixedPairs.value = currentMixedPairs
        mixedExtra.value = newMixedExtra
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

    fun addPlayer(name: String, category: String = "Masculino", onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
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
            
            repository.insert(Player(name = trimmedName, category = category))
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
            val samples = listOf(
                // 16 Feminino (Female)
                Pair("Ana Silva", "Feminino"),
                Pair("Maria Guerreiro", "Feminino"),
                Pair("Clara Lopes", "Feminino"),
                Pair("Rita Fonseca", "Feminino"),
                Pair("Sofia Bento", "Feminino"),
                Pair("Juliana Castro", "Feminino"),
                Pair("Joana Rebelo", "Feminino"),
                Pair("Inês Ramos", "Feminino"),
                Pair("Mariana Costa", "Feminino"),
                Pair("Beatriz Lima", "Feminino"),
                Pair("Carolina Santos", "Feminino"),
                Pair("Diana Oliveira", "Feminino"),
                Pair("Filipa Mendes", "Feminino"),
                Pair("Joana Pinto", "Feminino"),
                Pair("Marta Fernandes", "Feminino"),
                Pair("Gabriela Castro", "Feminino"),
                
                // 16 Masculino (Male only)
                Pair("Ivan Silva", "Masculino"),
                Pair("José Carlos", "Masculino"),
                Pair("João Paulo", "Masculino"),
                Pair("Rui Santos", "Masculino"),
                Pair("Miguel Oliveira", "Masculino"),
                Pair("Luís Fernandes", "Masculino"),
                Pair("Bruno Santos", "Masculino"),
                Pair("Tiago Pinto", "Masculino"),
                Pair("Nuno Martins", "Masculino"),
                Pair("Vasco Lima", "Masculino"),
                Pair("Jorge Rocha", "Masculino"),
                Pair("Gonçalo Rebelo", "Masculino"),
                Pair("Filipe Abreu", "Masculino"),
                Pair("José Neto", "Masculino"),
                Pair("Nelson Varela", "Masculino"),
                Pair("André Cruz", "Masculino"),
                
                // Reserves
                Pair("Rafael Neves", "Masculino"),
                Pair("Daniel Marques", "Masculino"),
                Pair("Patrícia Cruz", "Feminino")
            )
            samples.forEach { (name, type) ->
                repository.insert(Player(name = name, category = type))
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
