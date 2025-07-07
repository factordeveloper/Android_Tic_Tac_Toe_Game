package com.factordev.tic_tac_toe_game.model

enum class Player {
    X, O
}

enum class GameMode {
    SINGLE_PLAYER, MULTIPLAYER_LOCAL, MULTIPLAYER_BLUETOOTH
}

enum class GameStatus {
    PLAYING, WON, DRAW
}

data class GameState(
    val board: Array<Array<Player?>> = Array(3) { Array(3) { null } },
    val currentPlayer: Player = Player.X,
    val gameStatus: GameStatus = GameStatus.PLAYING,
    val winner: Player? = null,
    val gameMode: GameMode = GameMode.SINGLE_PLAYER,
    val isMyTurn: Boolean = true,
    val isBluetoothConnected: Boolean = false,
    val localPlayerName: String = "Jugador",
    val opponentPlayerName: String = "Oponente",
    val localPlayer: Player = Player.X, // Indica cuál Player representa al jugador local
    val showResetConfirmDialog: Boolean = false,
    val showRematchDialog: Boolean = false,
    val waitingForRematchResponse: Boolean = false,
    val rematchMessage: String = "",
    val showDisconnectionAlert: Boolean = false,
    val disconnectionMessage: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameState

        if (!board.contentDeepEquals(other.board)) return false
        if (currentPlayer != other.currentPlayer) return false
        if (gameStatus != other.gameStatus) return false
        if (winner != other.winner) return false
        if (gameMode != other.gameMode) return false
        if (isMyTurn != other.isMyTurn) return false
        if (isBluetoothConnected != other.isBluetoothConnected) return false
        if (localPlayerName != other.localPlayerName) return false
        if (opponentPlayerName != other.opponentPlayerName) return false
        if (localPlayer != other.localPlayer) return false
        if (showResetConfirmDialog != other.showResetConfirmDialog) return false
        if (showRematchDialog != other.showRematchDialog) return false
        if (waitingForRematchResponse != other.waitingForRematchResponse) return false
        if (rematchMessage != other.rematchMessage) return false
        if (showDisconnectionAlert != other.showDisconnectionAlert) return false
        if (disconnectionMessage != other.disconnectionMessage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = board.contentDeepHashCode()
        result = 31 * result + currentPlayer.hashCode()
        result = 31 * result + gameStatus.hashCode()
        result = 31 * result + (winner?.hashCode() ?: 0)
        result = 31 * result + gameMode.hashCode()
        result = 31 * result + isMyTurn.hashCode()
        result = 31 * result + isBluetoothConnected.hashCode()
        result = 31 * result + localPlayerName.hashCode()
        result = 31 * result + opponentPlayerName.hashCode()
        result = 31 * result + localPlayer.hashCode()
        result = 31 * result + showResetConfirmDialog.hashCode()
        result = 31 * result + showRematchDialog.hashCode()
        result = 31 * result + waitingForRematchResponse.hashCode()
        result = 31 * result + rematchMessage.hashCode()
        result = 31 * result + showDisconnectionAlert.hashCode()
        result = 31 * result + disconnectionMessage.hashCode()
        return result
    }
}

data class Move(
    val row: Int,
    val col: Int,
    val player: Player
)

data class PlayerInfo(
    val playerName: String,
    val assignedPlayer: Player
)

data class GameMessage(
    val type: MessageType,
    val content: String
)

enum class MessageType {
    MOVE,
    PLAYER_INFO,
    GAME_START,
    GAME_START_SYNC,
    GAME_RESET,
    REMATCH_REQUEST,
    REMATCH_RESPONSE,
    GAME_QUIT,
    OPPONENT_DISCONNECTED,
    HEARTBEAT,
    GAME_END_SYNC,
    GO_TO_GAME
} 