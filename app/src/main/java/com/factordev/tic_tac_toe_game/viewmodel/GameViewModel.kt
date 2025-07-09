package com.factordev.tic_tac_toe_game.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factordev.tic_tac_toe_game.model.GameMode
import com.factordev.tic_tac_toe_game.model.GameState
import com.factordev.tic_tac_toe_game.model.GameStatus
import com.factordev.tic_tac_toe_game.model.Move
import com.factordev.tic_tac_toe_game.model.Player
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "GameViewModel"
        private const val BOARD_SIZE = 3
    }
    
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _showGameModeDialog = MutableStateFlow(false)
    val showGameModeDialog: StateFlow<Boolean> = _showGameModeDialog.asStateFlow()

    fun showGameModeDialog() {
        _showGameModeDialog.value = true
    }

    fun hideGameModeDialog() {
        _showGameModeDialog.value = false
    }

    fun setGameMode(mode: GameMode) {
        try {
            _gameState.value = _gameState.value.copy(gameMode = mode)
            _showGameModeDialog.value = false
            resetGame()
            Log.d(TAG, "Modo de juego cambiado a: $mode")
        } catch (e: Exception) {
            Log.e(TAG, "Error al establecer modo de juego: ${e.message}", e)
        }
    }

    fun setPlayerNames(localName: String, opponentName: String) {
        try {
            val safeLocalName = localName.trim().takeIf { it.isNotEmpty() } ?: "Jugador"
            val safeOpponentName = opponentName.trim().takeIf { it.isNotEmpty() } ?: "Oponente"
            
            _gameState.value = _gameState.value.copy(
                localPlayerName = safeLocalName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                opponentPlayerName = safeOpponentName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            )
            Log.d(TAG, "Nombres de jugadores establecidos: $safeLocalName vs $safeOpponentName")
        } catch (e: Exception) {
            Log.e(TAG, "Error al establecer nombres de jugadores: ${e.message}", e)
        }
    }

    fun setLocalPlayer(player: Player) {
        try {
            _gameState.value = _gameState.value.copy(localPlayer = player)
            Log.d(TAG, "Jugador local establecido como: $player")
        } catch (e: Exception) {
            Log.e(TAG, "Error al establecer jugador local: ${e.message}", e)
        }
    }

    fun makeMove(row: Int, col: Int) {
        try {
            // Validar bounds del tablero
            if (!isValidPosition(row, col)) {
                Log.w(TAG, "Posición inválida: ($row, $col)")
                return
            }
            
            val currentState = _gameState.value
            
            // Verificar si el movimiento es válido
            if (currentState.board[row][col] != null || 
                currentState.gameStatus != GameStatus.PLAYING ||
                (!currentState.isMyTurn && currentState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH)) {
                Log.d(TAG, "Movimiento rechazado - posición ocupada o no es tu turno")
                return
            }

            // Hacer el movimiento
            val newBoard = safelyCopyBoard(currentState.board)
            newBoard[row][col] = currentState.currentPlayer
            
            val newGameState = currentState.copy(
                board = newBoard,
                currentPlayer = if (currentState.currentPlayer == Player.X) Player.O else Player.X,
                isMyTurn = when (currentState.gameMode) {
                    GameMode.MULTIPLAYER_BLUETOOTH -> false // En Bluetooth, no es nuestro turno después de mover
                    else -> true
                }
            )
            
            val updatedState = checkGameEnd(newGameState)
            _gameState.value = updatedState
            
            Log.d(TAG, "Movimiento realizado en ($row, $col) por ${currentState.currentPlayer}")

            // Si es modo individual y el juego continúa, hacer movimiento de AI
            if (currentState.gameMode == GameMode.SINGLE_PLAYER && 
                updatedState.gameStatus == GameStatus.PLAYING) {
                makeAIMove()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al realizar movimiento: ${e.message}", e)
        }
    }

    private fun makeAIMove() {
        viewModelScope.launch {
            try {
                delay(500) // Pequeña pausa para mejor UX
                
                val currentState = _gameState.value
                val bestMove = getBestMove(currentState.board)
                
                if (bestMove != null && isValidPosition(bestMove.row, bestMove.col)) {
                    val newBoard = safelyCopyBoard(currentState.board)
                    newBoard[bestMove.row][bestMove.col] = Player.O
                    
                    val newGameState = currentState.copy(
                        board = newBoard,
                        currentPlayer = Player.X
                    )
                    
                    _gameState.value = checkGameEnd(newGameState)
                    Log.d(TAG, "AI realizó movimiento en (${bestMove.row}, ${bestMove.col})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en movimiento de AI: ${e.message}", e)
            }
        }
    }

    private fun getBestMove(board: Array<Array<Player?>>): Move? {
        try {
            // Implementación de AI usando minimax simplificado
            
            // Primero, verificar si AI puede ganar
            for (row in 0 until BOARD_SIZE) {
                for (col in 0 until BOARD_SIZE) {
                    if (isValidPosition(row, col) && board[row][col] == null) {
                        val testBoard = safelyCopyBoard(board)
                        testBoard[row][col] = Player.O
                        if (checkWinner(testBoard) == Player.O) {
                            return Move(row, col, Player.O)
                        }
                    }
                }
            }
            
            // Luego, verificar si necesita bloquear al jugador
            for (row in 0 until BOARD_SIZE) {
                for (col in 0 until BOARD_SIZE) {
                    if (isValidPosition(row, col) && board[row][col] == null) {
                        val testBoard = safelyCopyBoard(board)
                        testBoard[row][col] = Player.X
                        if (checkWinner(testBoard) == Player.X) {
                            return Move(row, col, Player.O)
                        }
                    }
                }
            }
            
            // Preferir el centro
            if (isValidPosition(1, 1) && board[1][1] == null) {
                return Move(1, 1, Player.O)
            }
            
            // Preferir esquinas
            val corners = listOf(Pair(0, 0), Pair(0, 2), Pair(2, 0), Pair(2, 2))
            val availableCorners = corners.filter { 
                isValidPosition(it.first, it.second) && board[it.first][it.second] == null 
            }
            if (availableCorners.isNotEmpty()) {
                val corner = availableCorners[Random.nextInt(availableCorners.size)]
                return Move(corner.first, corner.second, Player.O)
            }
            
            // Cualquier movimiento disponible
            for (row in 0 until BOARD_SIZE) {
                for (col in 0 until BOARD_SIZE) {
                    if (isValidPosition(row, col) && board[row][col] == null) {
                        return Move(row, col, Player.O)
                    }
                }
            }
            
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error al calcular mejor movimiento: ${e.message}", e)
            return null
        }
    }

    private fun checkGameEnd(gameState: GameState): GameState {
        try {
            val winner = checkWinner(gameState.board)
            
            return when {
                winner != null -> gameState.copy(
                    gameStatus = GameStatus.WON,
                    winner = winner,
                    isMyTurn = false // Cuando termina el juego, nadie tiene turno
                )
                isBoardFull(gameState.board) -> gameState.copy(
                    gameStatus = GameStatus.DRAW,
                    isMyTurn = false // Cuando termina el juego, nadie tiene turno
                )
                else -> gameState
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar fin de juego: ${e.message}", e)
            return gameState
        }
    }

    private fun checkWinner(board: Array<Array<Player?>>): Player? {
        try {
            // Verificar filas
            for (row in 0 until BOARD_SIZE) {
                if (isValidPosition(row, 0) && isValidPosition(row, 1) && isValidPosition(row, 2) &&
                    board[row][0] != null && 
                    board[row][0] == board[row][1] && 
                    board[row][1] == board[row][2]) {
                    return board[row][0]
                }
            }
            
            // Verificar columnas
            for (col in 0 until BOARD_SIZE) {
                if (isValidPosition(0, col) && isValidPosition(1, col) && isValidPosition(2, col) &&
                    board[0][col] != null && 
                    board[0][col] == board[1][col] && 
                    board[1][col] == board[2][col]) {
                    return board[0][col]
                }
            }
            
            // Verificar diagonales
            if (isValidPosition(0, 0) && isValidPosition(1, 1) && isValidPosition(2, 2) &&
                board[0][0] != null && 
                board[0][0] == board[1][1] && 
                board[1][1] == board[2][2]) {
                return board[0][0]
            }
            
            if (isValidPosition(0, 2) && isValidPosition(1, 1) && isValidPosition(2, 0) &&
                board[0][2] != null && 
                board[0][2] == board[1][1] && 
                board[1][1] == board[2][0]) {
                return board[0][2]
            }
            
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar ganador: ${e.message}", e)
            return null
        }
    }

    private fun isBoardFull(board: Array<Array<Player?>>): Boolean {
        try {
            for (row in 0 until BOARD_SIZE) {
                for (col in 0 until BOARD_SIZE) {
                    if (isValidPosition(row, col) && board[row][col] == null) {
                        return false
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar si el tablero está lleno: ${e.message}", e)
            return false
        }
    }
    
    private fun isValidPosition(row: Int, col: Int): Boolean {
        return row in 0 until BOARD_SIZE && col in 0 until BOARD_SIZE
    }
    
    private fun safelyCopyBoard(board: Array<Array<Player?>>): Array<Array<Player?>> {
        return try {
            Array(BOARD_SIZE) { row ->
                Array(BOARD_SIZE) { col ->
                    if (isValidPosition(row, col)) board[row][col] else null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al copiar tablero: ${e.message}", e)
            Array(BOARD_SIZE) { Array(BOARD_SIZE) { null } }
        }
    }

    fun resetGame() {
        try {
            _gameState.value = _gameState.value.copy(
                board = Array(BOARD_SIZE) { Array(BOARD_SIZE) { null } },
                currentPlayer = Player.X,
                gameStatus = GameStatus.PLAYING,
                winner = null,
                isMyTurn = when (_gameState.value.gameMode) {
                    GameMode.MULTIPLAYER_BLUETOOTH -> _gameState.value.localPlayer == Player.X
                    else -> true
                },
                showResetConfirmDialog = false,
                showRematchDialog = false,
                waitingForRematchResponse = false,
                rematchMessage = "",
                showDisconnectionAlert = false,
                disconnectionMessage = ""
            )
            Log.d(TAG, "Juego reiniciado")
        } catch (e: Exception) {
            Log.e(TAG, "Error al reiniciar juego: ${e.message}", e)
        }
    }
    
    // Nueva función específica para reset sincronizado en Bluetooth
    fun resetGameBluetooth() {
        try {
            _gameState.value = _gameState.value.copy(
                board = Array(BOARD_SIZE) { Array(BOARD_SIZE) { null } },
                currentPlayer = Player.X,
                gameStatus = GameStatus.PLAYING,
                winner = null,
                // En Bluetooth, siempre Player.X inicia (host)
                isMyTurn = _gameState.value.localPlayer == Player.X,
                showResetConfirmDialog = false,
                showRematchDialog = false,
                waitingForRematchResponse = false,
                rematchMessage = "",
                showDisconnectionAlert = false,
                disconnectionMessage = ""
            )
            Log.d(TAG, "Juego reiniciado para Bluetooth")
        } catch (e: Exception) {
            Log.e(TAG, "Error al reiniciar juego Bluetooth: ${e.message}", e)
        }
    }
    
    // Función para forzar sincronización del estado del juego
    fun syncGameState() {
        try {
            val currentState = _gameState.value
            if (currentState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
                // Asegurar que el estado de turnos esté correcto
                val correctedState = currentState.copy(
                    isMyTurn = when {
                        currentState.gameStatus != GameStatus.PLAYING -> false
                        currentState.currentPlayer == currentState.localPlayer -> true
                        else -> false
                    }
                )
                _gameState.value = correctedState
                Log.d(TAG, "Estado de juego sincronizado")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al sincronizar estado de juego: ${e.message}", e)
        }
    }
    
    // Función para synchronizar el estado cuando el juego termina
    fun synchronizeGameEnd() {
        try {
            val currentState = _gameState.value
            if (currentState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH && 
                (currentState.gameStatus == GameStatus.WON || currentState.gameStatus == GameStatus.DRAW)) {
                // Cuando termina el juego, ambos jugadores deben tener isMyTurn = false
                val syncedState = currentState.copy(
                    isMyTurn = false
                )
                _gameState.value = syncedState
                Log.d(TAG, "Fin de juego sincronizado")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al sincronizar fin de juego: ${e.message}", e)
        }
    }
    
    fun showResetConfirmDialog() {
        try {
            _gameState.value = _gameState.value.copy(showResetConfirmDialog = true)
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar diálogo de confirmación: ${e.message}", e)
        }
    }
    
    fun hideResetConfirmDialog() {
        try {
            _gameState.value = _gameState.value.copy(showResetConfirmDialog = false)
        } catch (e: Exception) {
            Log.e(TAG, "Error al ocultar diálogo de confirmación: ${e.message}", e)
        }
    }
    
    fun showRematchDialog() {
        try {
            _gameState.value = _gameState.value.copy(showRematchDialog = true)
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar diálogo de revancha: ${e.message}", e)
        }
    }
    
    fun hideRematchDialog() {
        try {
            _gameState.value = _gameState.value.copy(showRematchDialog = false)
        } catch (e: Exception) {
            Log.e(TAG, "Error al ocultar diálogo de revancha: ${e.message}", e)
        }
    }
    
    fun setWaitingForRematchResponse(waiting: Boolean, message: String = "") {
        try {
            _gameState.value = _gameState.value.copy(
                waitingForRematchResponse = waiting,
                rematchMessage = message
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al establecer espera de respuesta: ${e.message}", e)
        }
    }
    
    fun handleRematchRequest(accept: Boolean) {
        try {
            if (accept) {
                if (_gameState.value.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
                    resetGameBluetooth()
                } else {
                    resetGame()
                }
            }
            hideRematchDialog()
            Log.d(TAG, "Solicitud de revancha manejada: $accept")
        } catch (e: Exception) {
            Log.e(TAG, "Error al manejar solicitud de revancha: ${e.message}", e)
        }
    }
    
    fun handleRematchResponse(accepted: Boolean) {
        try {
            if (accepted) {
                if (_gameState.value.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
                    resetGameBluetooth()
                } else {
                    resetGame()
                }
            }
            setWaitingForRematchResponse(false)
            Log.d(TAG, "Respuesta de revancha manejada: $accepted")
        } catch (e: Exception) {
            Log.e(TAG, "Error al manejar respuesta de revancha: ${e.message}", e)
        }
    }
    
    fun showDisconnectionAlert(message: String) {
        try {
            val safeMessage = message.takeIf { it.isNotEmpty() } ?: "Se perdió la conexión"
            _gameState.value = _gameState.value.copy(
                showDisconnectionAlert = true,
                disconnectionMessage = safeMessage
            )
            Log.d(TAG, "Alerta de desconexión mostrada: $safeMessage")
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar alerta de desconexión: ${e.message}", e)
        }
    }
    
    fun hideDisconnectionAlert() {
        try {
            _gameState.value = _gameState.value.copy(
                showDisconnectionAlert = false,
                disconnectionMessage = ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al ocultar alerta de desconexión: ${e.message}", e)
        }
    }

    fun updateBluetoothConnection(isConnected: Boolean) {
        try {
            _gameState.value = _gameState.value.copy(isBluetoothConnected = isConnected)
            Log.d(TAG, "Estado de conexión Bluetooth actualizado: $isConnected")
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar conexión Bluetooth: ${e.message}", e)
        }
    }

    fun makeRemoteMove(row: Int, col: Int, player: Player) {
        try {
            // Validar bounds del tablero
            if (!isValidPosition(row, col)) {
                Log.w(TAG, "Posición inválida para movimiento remoto: ($row, $col)")
                return
            }
            
            val currentState = _gameState.value
            
            // Verificar si el movimiento es válido
            if (currentState.board[row][col] != null || 
                currentState.gameStatus != GameStatus.PLAYING) {
                Log.d(TAG, "Movimiento remoto rechazado - posición ocupada o juego terminado")
                return
            }

            // Hacer el movimiento remoto
            val newBoard = safelyCopyBoard(currentState.board)
            newBoard[row][col] = player
            
            val newGameState = currentState.copy(
                board = newBoard,
                currentPlayer = if (player == Player.X) Player.O else Player.X,
                isMyTurn = true // Ahora es nuestro turno después del movimiento remoto
            )
            
            val updatedState = checkGameEnd(newGameState)
            _gameState.value = updatedState
            
            Log.d(TAG, "Movimiento remoto realizado en ($row, $col) por $player")
        } catch (e: Exception) {
            Log.e(TAG, "Error al realizar movimiento remoto: ${e.message}", e)
        }
    }
    
    fun getCurrentPlayerName(): String {
        return try {
            val currentState = _gameState.value
            when (currentState.gameMode) {
                GameMode.SINGLE_PLAYER -> {
                    if (currentState.currentPlayer == Player.X) "Tu turno" else "Turno de la computadora"
                }
                GameMode.MULTIPLAYER_LOCAL -> {
                    "Turno del jugador ${currentState.currentPlayer.name}"
                }
                GameMode.MULTIPLAYER_BLUETOOTH -> {
                    when (currentState.gameStatus) {
                        GameStatus.PLAYING -> {
                            if (currentState.isMyTurn) {
                                "Es tu turno"
                            } else {
                                "Turno de ${currentState.opponentPlayerName}"
                            }
                        }
                        GameStatus.WON -> {
                            if (currentState.winner == currentState.localPlayer) {
                                "¡Has ganado!"
                            } else {
                                "¡${currentState.opponentPlayerName} ha ganado!"
                            }
                        }
                        GameStatus.DRAW -> "¡Empate!"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener nombre del jugador actual: ${e.message}", e)
            "Turno del jugador"
        }
    }
    
    fun isLocalPlayerWinner(): Boolean {
        return try {
            val currentState = _gameState.value
            when (currentState.gameMode) {
                GameMode.SINGLE_PLAYER -> currentState.winner == Player.X
                GameMode.MULTIPLAYER_LOCAL -> true // En local, siempre mostrar celebración
                GameMode.MULTIPLAYER_BLUETOOTH -> currentState.winner == currentState.localPlayer
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar si jugador local ganó: ${e.message}", e)
            false
        }
    }
    
    fun getWinnerName(): String {
        return try {
            val currentState = _gameState.value
            when (currentState.gameMode) {
                GameMode.SINGLE_PLAYER -> {
                    if (currentState.winner == Player.X) "¡Has ganado!" else "¡La computadora ha ganado!"
                }
                GameMode.MULTIPLAYER_LOCAL -> {
                    "¡Jugador ${currentState.winner?.name} ha ganado!"
                }
                GameMode.MULTIPLAYER_BLUETOOTH -> {
                    if (currentState.winner == currentState.localPlayer) {
                        "¡Has ganado!"
                    } else {
                        "¡${currentState.opponentPlayerName} ha ganado!"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener nombre del ganador: ${e.message}", e)
            "¡Juego terminado!"
        }
    }
} 