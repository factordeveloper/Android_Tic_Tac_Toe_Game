package com.factordev.tic_tac_toe_game

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.factordev.tic_tac_toe_game.bluetooth.BluetoothService
import com.factordev.tic_tac_toe_game.model.GameMode
import com.factordev.tic_tac_toe_game.model.GameStatus
import com.factordev.tic_tac_toe_game.model.Player
import com.factordev.tic_tac_toe_game.ui.BluetoothScreen
import com.factordev.tic_tac_toe_game.ui.BluetoothSetupScreen
import com.factordev.tic_tac_toe_game.ui.GameScreen
import com.factordev.tic_tac_toe_game.ui.WelcomeScreen
import com.factordev.tic_tac_toe_game.ui.theme.Tic_Tac_Toe_GameTheme
import com.factordev.tic_tac_toe_game.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    private var bluetoothService: BluetoothService? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private val requestBluetoothPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, "Se requieren permisos de Bluetooth para jugar multijugador", Toast.LENGTH_LONG).show()
        }
    }
    
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Bluetooth activado correctamente", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Se requiere activar Bluetooth para jugar multijugador", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            enableEdgeToEdge()
            
            // Inicializar Bluetooth con verificaciones de seguridad
            initializeBluetooth()
            
            setContent {
                Tic_Tac_Toe_GameTheme {
                    bluetoothService?.let { service ->
                        TicTacToeApp(
                            bluetoothService = service,
                            onRequestBluetoothPermissions = { requestBluetoothPermissions() },
                            onEnableBluetooth = { enableBluetooth() },
                            hasBluetoothPermissions = { hasBluetoothPermissions() }
                        )
                    } ?: run {
                        // Fallback UI si Bluetooth no está disponible
                        TicTacToeAppWithoutBluetooth()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate: ${e.message}", e)
            // Crear UI sin Bluetooth en caso de error
            setContent {
                Tic_Tac_Toe_GameTheme {
                    TicTacToeAppWithoutBluetooth()
                }
            }
        }
    }
    
    private fun initializeBluetooth() {
        try {
            val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager
            if (bluetoothManager == null) {
                Log.w(TAG, "BluetoothManager no disponible")
                return
            }
            
            bluetoothAdapter = bluetoothManager.adapter
            if (bluetoothAdapter == null) {
                Log.w(TAG, "BluetoothAdapter no disponible - dispositivo sin Bluetooth")
                return
            }
            
            bluetoothService = BluetoothService(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar Bluetooth: ${e.message}", e)
            bluetoothAdapter = null
            bluetoothService = null
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            bluetoothService?.cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "Error en cleanup: ${e.message}", e)
        }
    }
    
    private fun requestBluetoothPermissions() {
        try {
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }
            
            requestBluetoothPermissions.launch(permissions)
        } catch (e: Exception) {
            Log.e(TAG, "Error al solicitar permisos: ${e.message}", e)
            Toast.makeText(this, "Error al solicitar permisos de Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun enableBluetooth() {
        try {
            bluetoothAdapter?.let { adapter ->
                if (!adapter.isEnabled) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    enableBluetoothLauncher.launch(enableBtIntent)
                }
            } ?: run {
                Toast.makeText(this, "Bluetooth no disponible en este dispositivo", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al activar Bluetooth: ${e.message}", e)
            Toast.makeText(this, "Error al activar Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun hasBluetoothPermissions(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            } else {
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar permisos: ${e.message}", e)
            false
        }
    }
}

@Composable
fun TicTacToeApp(
    bluetoothService: BluetoothService,
    onRequestBluetoothPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    hasBluetoothPermissions: () -> Boolean
) {
    var currentScreen by remember { mutableStateOf("welcome") }
    var playerName by remember { mutableStateOf("") }
    var showBluetoothDisabledDialog by remember { mutableStateOf(false) }
    val viewModel: GameViewModel = viewModel()
    
    val gameState by viewModel.gameState.collectAsState()
    val receivedMove by bluetoothService.receivedMove.collectAsState()
    val connectionState by bluetoothService.connectionState.collectAsState()
    val receivedPlayerInfo by bluetoothService.receivedPlayerInfo.collectAsState()
    val connectionEstablished by bluetoothService.connectionEstablished.collectAsState()
    val receivedGameReset by bluetoothService.receivedGameReset.collectAsState()
    val receivedGameStartSync by bluetoothService.receivedGameStartSync.collectAsState()
    val receivedRematchRequest by bluetoothService.receivedRematchRequest.collectAsState()
    val receivedRematchResponse by bluetoothService.receivedRematchResponse.collectAsState()
    val receivedGameQuit by bluetoothService.receivedGameQuit.collectAsState()
    val opponentDisconnected by bluetoothService.opponentDisconnected.collectAsState()
    val receivedGameEndSync by bluetoothService.receivedGameEndSync.collectAsState()
    val receivedGoToGame by bluetoothService.receivedGoToGame.collectAsState()
    
    // Escuchar movimientos recibidos por Bluetooth
    LaunchedEffect(receivedMove) {
        receivedMove?.let { move ->
            if (gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
                viewModel.makeRemoteMove(move.row, move.col, move.player)
            }
        }
    }
    
    // Actualizar estado de conexión Bluetooth en el ViewModel
    LaunchedEffect(connectionState) {
        val isConnected = connectionState == BluetoothService.ConnectionState.CONNECTED
        viewModel.updateBluetoothConnection(isConnected)
        
        // Cuando se establece la conexión, enviar información del jugador
        if (isConnected && gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH && playerName.isNotEmpty()) {
            val localPlayer = if (bluetoothService.isHostDevice()) Player.X else Player.O
            viewModel.setLocalPlayer(localPlayer)
            viewModel.setPlayerNames(playerName, "Conectando...")
            bluetoothService.sendPlayerInfo(playerName, localPlayer)
        }
    }
    
    // Escuchar información de jugador recibida por Bluetooth
    LaunchedEffect(receivedPlayerInfo) {
        receivedPlayerInfo?.let { playerInfo ->
            if (gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
                viewModel.setPlayerNames(gameState.localPlayerName, playerInfo.playerName)
                bluetoothService.sendGameStart()
            }
        }
    }
    
    // Cuando se establece la conexión completa, actualizar la interfaz
    LaunchedEffect(connectionEstablished) {
        if (connectionEstablished && gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
            // La conexión está lista para jugar
        }
    }
    
    // Escuchar reinicio de juego por Bluetooth
    LaunchedEffect(receivedGameReset) {
        if (receivedGameReset && gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
            viewModel.resetGameBluetooth()
            // Enviar confirmación de sincronización
            bluetoothService.sendGameStartSync()
        }
    }
    
    // Escuchar sincronización de inicio de juego
    LaunchedEffect(receivedGameStartSync) {
        if (receivedGameStartSync && gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
            // El oponente confirmó que está listo - asegurar sincronización
            if (gameState.gameStatus == GameStatus.PLAYING && gameState.localPlayer == Player.X) {
                // Solo el host (Player.X) confirma que es su turno
                viewModel.resetGameBluetooth()
            }
        }
    }
    
    // Escuchar solicitud de revancha por Bluetooth
    LaunchedEffect(receivedRematchRequest) {
        if (receivedRematchRequest && gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
            viewModel.setWaitingForRematchResponse(false) // Cancelar cualquier espera previa
            viewModel.showRematchDialog()
        }
    }
    
    // Escuchar respuesta de revancha por Bluetooth
    LaunchedEffect(receivedRematchResponse) {
        receivedRematchResponse?.let { accepted ->
            if (gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
                viewModel.handleRematchResponse(accepted)
                if (accepted) {
                    // Si se acepta la revancha, enviar sincronización
                    bluetoothService.sendGameStartSync()
                } else {
                    // Si el oponente no quiere revancha, volver al menú principal
                    currentScreen = "welcome"
                }
            }
        }
    }
    
    // Escuchar salida del juego por Bluetooth
    LaunchedEffect(receivedGameQuit) {
        if (receivedGameQuit && gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
            // El oponente salió del juego, mostrar alerta y volver al menú principal
            viewModel.showDisconnectionAlert("Tu oponente abandonó el juego.")
        }
    }
    
    // Escuchar desconexión del oponente
    LaunchedEffect(opponentDisconnected) {
        if (opponentDisconnected && gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
            // El oponente se desconectó, mostrar alerta
            viewModel.showDisconnectionAlert("Se perdió la conexión con tu oponente.")
        }
    }
    
    // Escuchar sincronización de fin de juego
    LaunchedEffect(receivedGameEndSync) {
        if (receivedGameEndSync && gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
            // Sincronizar el estado del juego cuando termina
            viewModel.synchronizeGameEnd()
        }
    }
    
    // Escuchar navegación automática al juego
    LaunchedEffect(receivedGoToGame) {
        if (receivedGoToGame && gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
            // Navegar automáticamente al juego cuando el oponente hace clic en "Ir al Juego"
            currentScreen = "game"
        }
    }
    
    // Escuchar cambios en el estado de conexión Bluetooth
    LaunchedEffect(connectionState) {
        if (connectionState == BluetoothService.ConnectionState.DISCONNECTED && 
            gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH && 
            gameState.gameStatus == GameStatus.PLAYING) {
            // Solo mostrar alerta si estaba jugando
            viewModel.showDisconnectionAlert("Se perdió la conexión Bluetooth.")
        }
    }
    
    // Resetear flags de Bluetooth cuando se cambia de pantalla
    LaunchedEffect(currentScreen) {
        if (currentScreen == "welcome") {
            // Resetear flags cuando se vuelve al menú principal
            bluetoothService.resetConnectionFlags()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (currentScreen) {
            "welcome" -> {
                WelcomeScreen(
                    onModeSelected = { mode ->
                        viewModel.setGameMode(mode)
                        if (mode == GameMode.MULTIPLAYER_BLUETOOTH) {
                            // Verificar si Bluetooth está activado
                            if (bluetoothService.isBluetoothEnabled()) {
                                currentScreen = "bluetooth_setup"
                            } else {
                                showBluetoothDisabledDialog = true
                            }
                        } else {
                            currentScreen = "game"
                        }
                    }
                )
            }
            "bluetooth_setup" -> {
                BluetoothSetupScreen(
                    bluetoothService = bluetoothService,
                    onBackPressed = { currentScreen = "welcome" },
                    onSetupComplete = { name, isSearching ->
                        playerName = name
                        if (isSearching) {
                            bluetoothService.startDiscovery()
                        } else {
                            bluetoothService.startServer()
                        }
                        currentScreen = "bluetooth"
                    },
                    onRequestPermissions = onRequestBluetoothPermissions,
                    hasPermissions = hasBluetoothPermissions()
                )
            }
            "game" -> {
                GameScreen(
                    viewModel = viewModel,
                    onBluetoothClick = {
                        if (gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
                            onRequestBluetoothPermissions()
                            onEnableBluetooth()
                            currentScreen = "bluetooth"
                        }
                    },
                    onMoveBluetoothSend = { move ->
                        if (gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
                            bluetoothService.sendMove(move)
                        }
                    },
                    onGameEndSync = {
                        if (gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
                            bluetoothService.sendGameEndSync()
                        }
                    },
                    onBackToWelcome = { currentScreen = "welcome" },
                    onGameReset = {
                        if (gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
                            bluetoothService.sendGameReset()
                            viewModel.resetGameBluetooth()
                        } else {
                            viewModel.resetGame()
                        }
                    },
                    onRematchRequest = {
                        if (gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
                            bluetoothService.sendRematchRequest()
                            viewModel.setWaitingForRematchResponse(true, "Esperando respuesta del oponente...")
                        }
                    },
                    onRematchResponse = { accepted ->
                        if (gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
                            bluetoothService.sendRematchResponse(accepted)
                            if (accepted) {
                                // Si acepto la revancha, enviar sincronización
                                bluetoothService.sendGameStartSync()
                            } else {
                                bluetoothService.sendGameQuit()
                                currentScreen = "welcome"
                            }
                        }
                    },
                    onGameQuit = {
                        if (gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
                            bluetoothService.sendGameQuit()
                        }
                        currentScreen = "welcome"
                    }
                )
            }
            "bluetooth" -> {
                BluetoothScreen(
                    bluetoothService = bluetoothService,
                    onBackClick = { currentScreen = "game" },
                    onGoToGame = { currentScreen = "game" }
                )
            }
        }
    }
    
    // Diálogo de Bluetooth desactivado
    if (showBluetoothDisabledDialog) {
        BluetoothDisabledDialog(
            onDismiss = { showBluetoothDisabledDialog = false },
            onEnableBluetooth = {
                onEnableBluetooth()
                showBluetoothDisabledDialog = false
            }
        )
    }
    
    // Verificar si el Bluetooth se activó para proceder automáticamente
    LaunchedEffect(showBluetoothDisabledDialog) {
        if (!showBluetoothDisabledDialog && bluetoothService.isBluetoothEnabled() && 
            gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH && currentScreen == "welcome") {
            // Bluetooth activado, proceder a configuración
            currentScreen = "bluetooth_setup"
        }
    }
}

@Composable
fun TicTacToeAppWithoutBluetooth() {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val viewModel: GameViewModel = viewModel()
        var currentScreen by remember { mutableStateOf("welcome") }
        
        when (currentScreen) {
            "welcome" -> {
                WelcomeScreen(
                    onModeSelected = { mode ->
                        viewModel.setGameMode(mode)
                        if (mode == GameMode.MULTIPLAYER_BLUETOOTH) {
                            Toast.makeText(
                                context, 
                                "Bluetooth no disponible en este dispositivo", 
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            currentScreen = "game"
                        }
                    }
                )
            }
            "game" -> {
                GameScreen(
                    viewModel = viewModel,
                    onBluetoothClick = { /* Sin función Bluetooth */ },
                    onMoveBluetoothSend = { /* Sin función Bluetooth */ },
                    onGameEndSync = { /* Sin función Bluetooth */ },
                    onBackToWelcome = { currentScreen = "welcome" },
                    onGameReset = { viewModel.resetGame() },
                    onRematchRequest = { /* Sin función multijugador */ },
                    onRematchResponse = { /* Sin función multijugador */ },
                    onGameQuit = { currentScreen = "welcome" }
                )
            }
        }
    }
}

@Composable
fun BluetoothDisabledDialog(
    onDismiss: () -> Unit,
    onEnableBluetooth: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "📶",
                    fontSize = 24.sp
                )
                Text(
                    text = "Bluetooth Desactivado",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Para jugar en modo multijugador Bluetooth necesitas activar el Bluetooth en tu dispositivo.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "El Bluetooth es necesario para:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Buscar otros jugadores cercanos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "• Conectarte con otro dispositivo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "• Sincronizar las partidas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "¿Deseas activarlo ahora?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onEnableBluetooth
            ) {
                Text("Activar Bluetooth")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss
            ) {
                Text("Cancelar")
            }
        }
    )
}