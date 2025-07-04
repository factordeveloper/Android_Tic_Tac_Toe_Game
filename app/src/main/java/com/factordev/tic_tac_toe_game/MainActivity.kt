package com.factordev.tic_tac_toe_game

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    private lateinit var bluetoothService: BluetoothService
    private lateinit var bluetoothAdapter: BluetoothAdapter
    
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
        if (result.resultCode != RESULT_OK) {
            Toast.makeText(this, "Se requiere activar Bluetooth para jugar multijugador", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Inicializar Bluetooth
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothService = BluetoothService(this)
        
        setContent {
            Tic_Tac_Toe_GameTheme {
                TicTacToeApp(
                    bluetoothService = bluetoothService,
                    onRequestBluetoothPermissions = { requestBluetoothPermissions() },
                    onEnableBluetooth = { enableBluetooth() },
                    hasBluetoothPermissions = { hasBluetoothPermissions() }
                )
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        bluetoothService.cleanup()
    }
    
    private fun requestBluetoothPermissions() {
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
    }
    
    private fun enableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        }
    }
    
    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
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
    val viewModel: GameViewModel = viewModel()
    
    val gameState by viewModel.gameState.collectAsState()
    val receivedMove by bluetoothService.receivedMove.collectAsState()
    val connectionState by bluetoothService.connectionState.collectAsState()
    val receivedPlayerInfo by bluetoothService.receivedPlayerInfo.collectAsState()
    val connectionEstablished by bluetoothService.connectionEstablished.collectAsState()
    val receivedGameReset by bluetoothService.receivedGameReset.collectAsState()
    val receivedRematchRequest by bluetoothService.receivedRematchRequest.collectAsState()
    val receivedRematchResponse by bluetoothService.receivedRematchResponse.collectAsState()
    val receivedGameQuit by bluetoothService.receivedGameQuit.collectAsState()
    val opponentDisconnected by bluetoothService.opponentDisconnected.collectAsState()
    
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
            viewModel.resetGame()
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
                if (!accepted) {
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
    
    // Escuchar cambios en el estado de conexión Bluetooth
    LaunchedEffect(connectionState) {
        if (connectionState == BluetoothService.ConnectionState.DISCONNECTED && 
            gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH && 
            gameState.gameStatus == GameStatus.PLAYING) {
            // Solo mostrar alerta si estaba jugando
            viewModel.showDisconnectionAlert("Se perdió la conexión Bluetooth.")
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
                            currentScreen = "bluetooth_setup"
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
                    onBackToWelcome = { currentScreen = "welcome" },
                    onGameReset = {
                        if (gameState.gameMode == GameMode.MULTIPLAYER_BLUETOOTH) {
                            bluetoothService.sendGameReset()
                        }
                        viewModel.resetGame()
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
                            if (!accepted) {
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
}