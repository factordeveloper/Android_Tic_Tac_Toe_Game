package com.factordev.tic_tac_toe_game.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.factordev.tic_tac_toe_game.model.GameMode
import com.factordev.tic_tac_toe_game.model.GameStatus
import com.factordev.tic_tac_toe_game.model.Player
import com.factordev.tic_tac_toe_game.viewmodel.GameViewModel

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onBluetoothClick: () -> Unit,
    onMoveBluetoothSend: (com.factordev.tic_tac_toe_game.model.Move) -> Unit = {},
    onBackToWelcome: () -> Unit,
    onGameReset: () -> Unit = {},
    onRematchRequest: () -> Unit = {},
    onRematchResponse: (Boolean) -> Unit = {},
    onGameQuit: () -> Unit = {}
) {
    val gameState by viewModel.gameState.collectAsState()
    val showGameModeDialog by viewModel.showGameModeDialog.collectAsState()
    
    // Controlar cuando mostrar fuegos artificiales
    LaunchedEffect(gameState.gameStatus) {
        if (gameState.gameStatus == GameStatus.WON) {
            // Pequeño delay para que se vea el estado ganador antes de los fuegos artificiales
            kotlinx.coroutines.delay(500)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header con título y botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackToWelcome) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver al menú principal"
                        )
                    }
                    Text(
                        text = "Tic Tac Toe",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Row {
                    IconButton(onClick = { viewModel.showGameModeDialog() }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurar modo de juego"
                        )
                    }
                    if (gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
                        IconButton(onClick = onBluetoothClick) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = "Configurar Bluetooth",
                                tint = if (gameState.isBluetoothConnected) Color.Green else Color.Red
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Información del juego
            GameInfoCard(gameState, viewModel)

            Spacer(modifier = Modifier.height(24.dp))

            // Tablero de juego
            GameBoard(
                board = gameState.board,
                onCellClick = { row, col -> 
                    // Enviar movimiento por Bluetooth si está en modo multijugador
                    if (gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
                        val move = com.factordev.tic_tac_toe_game.model.Move(row, col, gameState.currentPlayer)
                        onMoveBluetoothSend(move)
                    }
                    viewModel.makeMove(row, col)
                },
                enabled = gameState.gameStatus == GameStatus.PLAYING && gameState.isMyTurn
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Mensaje de estado
            when (gameState.gameStatus) {
                GameStatus.WON -> {
                    Text(
                        text = viewModel.getWinnerName(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                GameStatus.DRAW -> {
                    Text(
                        text = "¡Empate!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                GameStatus.PLAYING -> {
                    Text(
                        text = viewModel.getCurrentPlayerName(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH && gameState.isMyTurn) 
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de reiniciar
            Button(
                onClick = { viewModel.showResetConfirmDialog() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nuevo Juego")
            }
        }
    }

    // Diálogo de selección de modo de juego
    if (showGameModeDialog) {
        GameModeDialog(
            onDismiss = { viewModel.hideGameModeDialog() },
            onModeSelected = { mode -> viewModel.setGameMode(mode) }
        )
    }
    
    // Diálogo de confirmación de reinicio
    if (gameState.showResetConfirmDialog) {
        ResetConfirmDialog(
            onConfirm = { 
                if (gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
                    // En modo Bluetooth, enviar mensaje de reinicio
                    onGameReset()
                } else {
                    // En modo local o individual, reiniciar directamente
                    viewModel.resetGame()
                }
            },
            onDismiss = { viewModel.hideResetConfirmDialog() }
        )
    }
    
    // Diálogo de revancha
    if (gameState.showRematchDialog) {
        RematchDialog(
            message = gameState.rematchMessage,
            onAccept = { 
                if (gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
                    // Enviar solicitud de revancha al oponente
                    onRematchRequest()
                    viewModel.setWaitingForRematchResponse(true, "Esperando respuesta del oponente...")
                    viewModel.hideRematchDialog()
                } else {
                    viewModel.handleRematchRequest(true)
                }
            },
            onDecline = { 
                if (gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
                    // Enviar respuesta negativa al oponente
                    onRematchResponse(false)
                    onGameQuit()
                }
                viewModel.handleRematchRequest(false)
            },
            onDismiss = { 
                if (gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
                    // Si cancela, enviar respuesta negativa
                    onRematchResponse(false)
                    onGameQuit()
                }
                viewModel.hideRematchDialog() 
            }
        )
    }
    
    // Mostrar esperando respuesta de revancha
    if (gameState.waitingForRematchResponse) {
        WaitingForRematchDialog(
            message = gameState.rematchMessage,
            onDismiss = { 
                viewModel.setWaitingForRematchResponse(false)
                onGameQuit()
            }
        )
    }
    
    // Alerta de desconexión
    if (gameState.showDisconnectionAlert) {
        DisconnectionAlert(
            message = gameState.disconnectionMessage,
            onDismiss = { 
                viewModel.hideDisconnectionAlert()
                onBackToWelcome()
            }
        )
    }
    
    // Animaciones de resultado
    if (gameState.gameStatus == GameStatus.WON) {
        WinnerAnimation(
            winner = viewModel.getWinnerName(),
            isLocalPlayer = viewModel.isLocalPlayerWinner(),
            onDismiss = { 
                viewModel.resetGame()
            }
        )
    } else if (gameState.gameStatus == GameStatus.DRAW) {
        if (gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
            // En modo Bluetooth, mostrar diálogo de revancha directamente
            LaunchedEffect(gameState.gameStatus) {
                if (gameState.gameStatus == GameStatus.DRAW && !gameState.showRematchDialog && !gameState.waitingForRematchResponse) {
                    kotlinx.coroutines.delay(1000)
                    viewModel.showRematchDialog()
                }
            }
        } else {
            // En modo local o individual, mostrar animación de empate
            DrawAnimation(
                onDismiss = { viewModel.resetGame() }
            )
        }
    }
}

@Composable
fun GameInfoCard(gameState: com.factordev.tic_tac_toe_game.model.GameState, viewModel: GameViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Modo de Juego",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (gameState.gameMode) {
                    GameMode.SINGLE_PLAYER -> "Contra la computadora"
                    GameMode.MULTIPLAYER_LOCAL -> "Multijugador local"
                    GameMode.MULTIPLAYER_BLUETOOTH -> "Multijugador Bluetooth"
                },
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (gameState.isBluetoothConnected) "Conectado" else "Desconectado",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (gameState.isBluetoothConnected) Color.Green else Color.Red
                )
                
                if (gameState.isBluetoothConnected && gameState.opponentPlayerName != "Oponente") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Vas a jugar contra ${gameState.opponentPlayerName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun GameBoard(
    board: Array<Array<Player?>>,
    onCellClick: (Int, Int) -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.size(300.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            for (row in 0..2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    for (col in 0..2) {
                        GameCell(
                            player = board[row][col],
                            onClick = { onCellClick(row, col) },
                            enabled = enabled,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        )
                        if (col < 2) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.outline)
                            )
                        }
                    }
                }
                if (row < 2) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.outline)
                    )
                }
            }
        }
    }
}

@Composable
fun GameCell(
    player: Player?,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable(enabled = enabled && player == null) { onClick() }
            .background(
                if (player != null) MaterialTheme.colorScheme.surfaceVariant 
                else MaterialTheme.colorScheme.surface
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = player?.name ?: "",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = when (player) {
                Player.X -> MaterialTheme.colorScheme.primary
                Player.O -> MaterialTheme.colorScheme.secondary
                null -> Color.Transparent
            }
        )
    }
}

@Composable
fun GameModeDialog(
    onDismiss: () -> Unit,
    onModeSelected: (GameMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Seleccionar Modo de Juego",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                GameModeOption(
                    title = "Contra la computadora",
                    description = "Juega contra la IA",
                    icon = Icons.Default.Person,
                    onClick = { onModeSelected(GameMode.SINGLE_PLAYER) }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                GameModeOption(
                    title = "Multijugador Local",
                    description = "Juega con alguien en el mismo dispositivo",
                    icon = Icons.Default.Person,
                    onClick = { onModeSelected(GameMode.MULTIPLAYER_LOCAL) }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                GameModeOption(
                    title = "Multijugador Bluetooth",
                    description = "Juega con alguien por Bluetooth",
                    icon = Icons.Default.Bluetooth,
                    onClick = { onModeSelected(GameMode.MULTIPLAYER_BLUETOOTH) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun GameModeOption(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun WinnerAnimation(
    winner: String,
    isLocalPlayer: Boolean,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    
    // Animación de escala pulsante
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // Animación de rotación suave
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    // Auto-dismiss después de 4 segundos
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(4000)
        onDismiss()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    rotationZ = if (isLocalPlayer) rotation * 0.1f else 0f
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (isLocalPlayer) 
                    Color(0xFF4CAF50).copy(alpha = 0.95f) 
                else 
                    Color(0xFFF44336).copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isLocalPlayer) "🎉" else "😔",
                    fontSize = 80.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = if (isLocalPlayer) "¡FELICIDADES!" else "¡HAS PERDIDO!",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontSize = 28.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (isLocalPlayer) 
                        "¡Excelente jugada!" 
                    else 
                        "Esta vez no tuviste suerte",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Toca para continuar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun ResetConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Reiniciar Juego",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "¿Estás seguro de que deseas reiniciar el juego? Esta acción reiniciará el juego para ambos jugadores.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            ) {
                Text("Sí, reiniciar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun RematchDialog(
    message: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "¡Empate!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🤝",
                    fontSize = 48.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "¿Deseas la revancha?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (message.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAccept()
                    onDismiss()
                }
            ) {
                Text("¡Sí, revancha!")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    onDecline()
                    onDismiss()
                }
            ) {
                Text("No, gracias")
            }
        }
    )
}

@Composable
fun WaitingForRematchDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Esperando respuesta...",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun DrawAnimation(
    onDismiss: () -> Unit
) {
    val scale by rememberInfiniteTransition().animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // Auto-dismiss después de 3 segundos
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        onDismiss()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🤝",
                    fontSize = 80.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = "¡EMPATE!",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontSize = 28.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "¡Buen juego para ambos!",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Toca para continuar",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun DisconnectionAlert(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "⚠️ Conexión Perdida",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Serás redirigido al menú principal.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Entendido")
            }
        }
    )
}