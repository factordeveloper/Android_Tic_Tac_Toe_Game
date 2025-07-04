package com.factordev.tic_tac_toe_game.viewmodel

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
        _gameState.value = _gameState.value.copy(gameMode = mode)
        _showGameModeDialog.value = false
        resetGame()
    }

    fun setPlayerNames(localName: String, opponentName: String) {
        _gameState.value = _gameState.value.copy(
            localPlayerName = localName,
            opponentPlayerName = opponentName
        )
    }

    fun setLocalPlayer(player: Player) {
        _gameState.value = _gameState.value.copy(localPlayer = player)
    }

    fun makeMove(row: Int, col: Int) {
        val currentState = _gameState.value
        
        // Verificar si el movimiento es válido
        if (currentState.board[row][col] != null || 
            currentState.gameStatus != GameStatus.PLAYING ||
            (!currentState.isMyTurn && currentState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH)) {
            return
        }

        // Hacer el movimiento
        val newBoard = currentState.board.map { it.clone() }.toTypedArray()
        newBoard[row][col] = currentState.currentPlayer
        
        val newGameState = currentState.copy(
            board = newBoard,
            currentPlayer = if (currentState.currentPlayer == Player.X) Player.O else Player.X,
            isMyTurn = currentState.gameMode != GameMode.MULTIPLAYER_BLUETOOTH // En Bluetooth, no es nuestro turno después de mover
        )
        
        val updatedState = checkGameEnd(newGameState)
        _gameState.value = updatedState

        // Si es modo individual y el juego continúa, hacer movimiento de AI
        if (currentState.gameMode == GameMode.SINGLE_PLAYER && 
            updatedState.gameStatus == GameStatus.PLAYING) {
            makeAIMove()
        }
    }

    private fun makeAIMove() {
        viewModelScope.launch {
            delay(500) // Pequeña pausa para mejor UX
            
            val currentState = _gameState.value
            val bestMove = getBestMove(currentState.board)
            
            if (bestMove != null) {
                val newBoard = currentState.board.map { it.clone() }.toTypedArray()
                newBoard[bestMove.row][bestMove.col] = Player.O
                
                val newGameState = currentState.copy(
                    board = newBoard,
                    currentPlayer = Player.X
                )
                
                _gameState.value = checkGameEnd(newGameState)
            }
        }
    }

    private fun getBestMove(board: Array<Array<Player?>>): Move? {
        // Implementación de AI usando minimax simplificado
        
        // Primero, verificar si AI puede ganar
        for (row in 0..2) {
            for (col in 0..2) {
                if (board[row][col] == null) {
                    val testBoard = board.map { it.clone() }.toTypedArray()
                    testBoard[row][col] = Player.O
                    if (checkWinner(testBoard) == Player.O) {
                        return Move(row, col, Player.O)
                    }
                }
            }
        }
        
        // Luego, verificar si necesita bloquear al jugador
        for (row in 0..2) {
            for (col in 0..2) {
                if (board[row][col] == null) {
                    val testBoard = board.map { it.clone() }.toTypedArray()
                    testBoard[row][col] = Player.X
                    if (checkWinner(testBoard) == Player.X) {
                        return Move(row, col, Player.O)
                    }
                }
            }
        }
        
        // Preferir el centro
        if (board[1][1] == null) {
            return Move(1, 1, Player.O)
        }
        
        // Preferir esquinas
        val corners = listOf(Pair(0, 0), Pair(0, 2), Pair(2, 0), Pair(2, 2))
        val availableCorners = corners.filter { board[it.first][it.second] == null }
        if (availableCorners.isNotEmpty()) {
            val corner = availableCorners[Random.nextInt(availableCorners.size)]
            return Move(corner.first, corner.second, Player.O)
        }
        
        // Cualquier movimiento disponible
        for (row in 0..2) {
            for (col in 0..2) {
                if (board[row][col] == null) {
                    return Move(row, col, Player.O)
                }
            }
        }
        
        return null
    }

    private fun checkGameEnd(gameState: GameState): GameState {
        val winner = checkWinner(gameState.board)
        
        return when {
            winner != null -> gameState.copy(
                gameStatus = GameStatus.WON,
                winner = winner
            )
            isBoardFull(gameState.board) -> gameState.copy(
                gameStatus = GameStatus.DRAW
            )
            else -> gameState
        }
    }

    private fun checkWinner(board: Array<Array<Player?>>): Player? {
        // Verificar filas
        for (row in 0..2) {
            if (board[row][0] != null && 
                board[row][0] == board[row][1] && 
                board[row][1] == board[row][2]) {
                return board[row][0]
            }
        }
        
        // Verificar columnas
        for (col in 0..2) {
            if (board[0][col] != null && 
                board[0][col] == board[1][col] && 
                board[1][col] == board[2][col]) {
                return board[0][col]
            }
        }
        
        // Verificar diagonales
        if (board[0][0] != null && 
            board[0][0] == board[1][1] && 
            board[1][1] == board[2][2]) {
            return board[0][0]
        }
        
        if (board[0][2] != null && 
            board[0][2] == board[1][1] && 
            board[1][1] == board[2][0]) {
            return board[0][2]
        }
        
        return null
    }

    private fun isBoardFull(board: Array<Array<Player?>>): Boolean {
        for (row in board) {
            for (cell in row) {
                if (cell == null) return false
            }
        }
        return true
    }

    fun resetGame() {
        _gameState.value = _gameState.value.copy(
            board = Array(3) { Array(3) { null } },
            currentPlayer = Player.X,
            gameStatus = GameStatus.PLAYING,
            winner = null,
            isMyTurn = if (_gameState.value.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) 
                        _gameState.value.localPlayer == Player.X else true,
            showResetConfirmDialog = false,
            showRematchDialog = false,
            waitingForRematchResponse = false,
            rematchMessage = "",
            showDisconnectionAlert = false,
            disconnectionMessage = ""
        )
    }
    
    fun showResetConfirmDialog() {
        _gameState.value = _gameState.value.copy(showResetConfirmDialog = true)
    }
    
    fun hideResetConfirmDialog() {
        _gameState.value = _gameState.value.copy(showResetConfirmDialog = false)
    }
    
    fun showRematchDialog() {
        _gameState.value = _gameState.value.copy(showRematchDialog = true)
    }
    
    fun hideRematchDialog() {
        _gameState.value = _gameState.value.copy(showRematchDialog = false)
    }
    
    fun setWaitingForRematchResponse(waiting: Boolean, message: String = "") {
        _gameState.value = _gameState.value.copy(
            waitingForRematchResponse = waiting,
            rematchMessage = message
        )
    }
    
    fun handleRematchRequest(accept: Boolean) {
        if (accept) {
            resetGame()
        }
        hideRematchDialog()
    }
    
    fun handleRematchResponse(accepted: Boolean) {
        if (accepted) {
            resetGame()
        }
        setWaitingForRematchResponse(false)
    }
    
    fun showDisconnectionAlert(message: String) {
        _gameState.value = _gameState.value.copy(
            showDisconnectionAlert = true,
            disconnectionMessage = message
        )
    }
    
    fun hideDisconnectionAlert() {
        _gameState.value = _gameState.value.copy(
            showDisconnectionAlert = false,
            disconnectionMessage = ""
        )
    }

    fun updateBluetoothConnection(isConnected: Boolean) {
        _gameState.value = _gameState.value.copy(isBluetoothConnected = isConnected)
    }

    fun makeRemoteMove(row: Int, col: Int, player: Player) {
        val currentState = _gameState.value
        
        // Verificar si el movimiento es válido
        if (currentState.board[row][col] != null || 
            currentState.gameStatus != GameStatus.PLAYING) {
            return
        }

        // Hacer el movimiento remoto
        val newBoard = currentState.board.map { it.clone() }.toTypedArray()
        newBoard[row][col] = player
        
        val newGameState = currentState.copy(
            board = newBoard,
            currentPlayer = if (player == Player.X) Player.O else Player.X,
            isMyTurn = true // Ahora es nuestro turno
        )
        
        _gameState.value = checkGameEnd(newGameState)
    }
    
    fun getCurrentPlayerName(): String {
        val currentState = _gameState.value
        return when (currentState.gameMode) {
            GameMode.SINGLE_PLAYER -> {
                if (currentState.currentPlayer == Player.X) "Tu turno" else "Turno de la computadora"
            }
            GameMode.MULTIPLAYER_LOCAL -> {
                "Turno del jugador ${currentState.currentPlayer.name}"
            }
            GameMode.MULTIPLAYER_BLUETOOTH -> {
                if (currentState.isMyTurn) {
                    "Es tu turno"
                } else {
                    "Turno de ${currentState.opponentPlayerName}"
                }
            }
        }
    }
    
    fun isLocalPlayerWinner(): Boolean {
        val currentState = _gameState.value
        return when (currentState.gameMode) {
            GameMode.SINGLE_PLAYER -> currentState.winner == Player.X
            GameMode.MULTIPLAYER_LOCAL -> true // En local, siempre mostrar celebración
            GameMode.MULTIPLAYER_BLUETOOTH -> currentState.winner == currentState.localPlayer
        }
    }
    
    fun getWinnerName(): String {
        val currentState = _gameState.value
        return when (currentState.gameMode) {
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
    }
} 