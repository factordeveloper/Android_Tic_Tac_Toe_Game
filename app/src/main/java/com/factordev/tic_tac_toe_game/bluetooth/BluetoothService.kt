package com.factordev.tic_tac_toe_game.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.factordev.tic_tac_toe_game.model.Move
import com.factordev.tic_tac_toe_game.model.PlayerInfo
import com.factordev.tic_tac_toe_game.model.GameMessage
import com.factordev.tic_tac_toe_game.model.MessageType
import com.factordev.tic_tac_toe_game.model.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.isActive
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

class BluetoothService(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    
    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _receivedMove = MutableStateFlow<Move?>(null)
    val receivedMove: StateFlow<Move?> = _receivedMove.asStateFlow()
    
    private val _receivedPlayerInfo = MutableStateFlow<PlayerInfo?>(null)
    val receivedPlayerInfo: StateFlow<PlayerInfo?> = _receivedPlayerInfo.asStateFlow()
    
    private val _connectionEstablished = MutableStateFlow<Boolean>(false)
    val connectionEstablished: StateFlow<Boolean> = _connectionEstablished.asStateFlow()
    
    private val _receivedGameStartSync = MutableStateFlow<Boolean>(false)
    val receivedGameStartSync: StateFlow<Boolean> = _receivedGameStartSync.asStateFlow()
    
    private val _receivedGameReset = MutableStateFlow<Boolean>(false)
    val receivedGameReset: StateFlow<Boolean> = _receivedGameReset.asStateFlow()
    
    private val _receivedRematchRequest = MutableStateFlow<Boolean>(false)
    val receivedRematchRequest: StateFlow<Boolean> = _receivedRematchRequest.asStateFlow()
    
    private val _receivedRematchResponse = MutableStateFlow<Boolean?>(null)
    val receivedRematchResponse: StateFlow<Boolean?> = _receivedRematchResponse.asStateFlow()
    
    private val _receivedGameQuit = MutableStateFlow<Boolean>(false)
    val receivedGameQuit: StateFlow<Boolean> = _receivedGameQuit.asStateFlow()
    
    private val _opponentDisconnected = MutableStateFlow<Boolean>(false)
    val opponentDisconnected: StateFlow<Boolean> = _opponentDisconnected.asStateFlow()
    
    private val _receivedGameEndSync = MutableStateFlow<Boolean>(false)
    val receivedGameEndSync: StateFlow<Boolean> = _receivedGameEndSync.asStateFlow()
    
    private val _receivedGoToGame = MutableStateFlow<Boolean>(false)
    val receivedGoToGame: StateFlow<Boolean> = _receivedGoToGame.asStateFlow()
    
    private var bluetoothSocket: BluetoothSocket? = null
    private var bluetoothServerSocket: BluetoothServerSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    
    private val serviceUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val serviceName = "TicTacToe_Game_${System.currentTimeMillis() % 10000}"
    
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isHost = false // Si este dispositivo es el host (servidor)
    
    // Nuevas variables para heartbeat y detección de desconexión
    private var heartbeatJob: Job? = null
    private var connectionCheckJob: Job? = null
    private val lastHeartbeatReceived = AtomicLong(System.currentTimeMillis())
    private val lastMessageSent = AtomicLong(0)
    private var isConnectionAlive = true
    
    companion object {
        private const val HEARTBEAT_INTERVAL = 5000L // 5 segundos
        private const val CONNECTION_TIMEOUT = 15000L // 15 segundos
        private const val SEND_TIMEOUT = 3000L // 3 segundos para envío
    }
    
    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, LISTENING
    }
    
    private val deviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as? BluetoothDevice
                    }
                    
                    device?.let { bluetoothDevice ->
                        val currentDevices = _discoveredDevices.value.toMutableList()
                        if (!currentDevices.contains(bluetoothDevice)) {
                            currentDevices.add(bluetoothDevice)
                            _discoveredDevices.value = currentDevices
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    // Descubrimiento terminado
                }
            }
        }
    }
    
    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(deviceReceiver, filter)
    }
    
    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (!hasBluetoothPermissions()) return
        
        _discoveredDevices.value = emptyList()
        
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        
        bluetoothAdapter.startDiscovery()
    }
    
    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        if (!hasBluetoothPermissions()) return
        
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
    }
    
    @SuppressLint("MissingPermission")
    fun startServer() {
        if (!hasBluetoothPermissions()) return
        
        isHost = true
        resetConnectionFlags()
        
        scope.launch {
            try {
                _connectionState.value = ConnectionState.LISTENING
                
                bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
                    serviceName, serviceUUID
                )
                
                val socket = bluetoothServerSocket?.accept()
                socket?.let {
                    bluetoothSocket = it
                    setupStreams(it)
                    _connectionState.value = ConnectionState.CONNECTED
                    startConnectionMonitoring()
                    listenForMessages()
                }
            } catch (e: IOException) {
                _connectionState.value = ConnectionState.DISCONNECTED
                handleDisconnection("Error al iniciar servidor: ${e.message}")
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        if (!hasBluetoothPermissions()) return
        
        isHost = false
        resetConnectionFlags()
        
        scope.launch {
            try {
                _connectionState.value = ConnectionState.CONNECTING
                
                val socket = device.createRfcommSocketToServiceRecord(serviceUUID)
                bluetoothSocket = socket
                
                bluetoothAdapter.cancelDiscovery()
                
                socket.connect()
                setupStreams(socket)
                _connectionState.value = ConnectionState.CONNECTED
                startConnectionMonitoring()
                listenForMessages()
            } catch (e: IOException) {
                _connectionState.value = ConnectionState.DISCONNECTED
                handleDisconnection("Error al conectar con dispositivo: ${e.message}")
                bluetoothSocket?.close()
                bluetoothSocket = null
            }
        }
    }
    
    private fun setupStreams(socket: BluetoothSocket) {
        inputStream = socket.inputStream
        outputStream = socket.outputStream
        isConnectionAlive = true
        lastHeartbeatReceived.set(System.currentTimeMillis())
    }
    
    private fun startConnectionMonitoring() {
        // Iniciar heartbeat
        heartbeatJob = scope.launch {
            while (isConnectionAlive && _connectionState.value == ConnectionState.CONNECTED) {
                delay(HEARTBEAT_INTERVAL)
                if (isConnectionAlive) {
                    sendHeartbeat()
                }
            }
        }
        
        // Iniciar monitoreo de conexión
        connectionCheckJob = scope.launch {
            while (isConnectionAlive && _connectionState.value == ConnectionState.CONNECTED) {
                delay(CONNECTION_TIMEOUT / 3) // Verificar cada 5 segundos
                
                val timeSinceLastHeartbeat = System.currentTimeMillis() - lastHeartbeatReceived.get()
                if (timeSinceLastHeartbeat > CONNECTION_TIMEOUT) {
                    handleDisconnection("Timeout de conexión - no se recibió respuesta del oponente")
                    break
                }
            }
        }
    }
    
    private fun sendHeartbeat() {
        scope.launch {
            if (sendMessageWithTimeout("${MessageType.HEARTBEAT}|ping")) {
                lastMessageSent.set(System.currentTimeMillis())
            } else {
                handleDisconnection("Error al enviar heartbeat")
            }
        }
    }
    
    fun resetConnectionFlags() {
        _receivedMove.value = null
        _receivedPlayerInfo.value = null
        _connectionEstablished.value = false
        _receivedGameStartSync.value = false
        _receivedGameReset.value = false
        _receivedRematchRequest.value = false
        _receivedRematchResponse.value = null
        _receivedGameQuit.value = false
        _opponentDisconnected.value = false
        _receivedGameEndSync.value = false
        _receivedGoToGame.value = false
        isConnectionAlive = true
        lastHeartbeatReceived.set(System.currentTimeMillis())
        lastMessageSent.set(0)
    }
    
    // Nueva función para resetear solo flags de juego
    fun resetGameFlags() {
        _receivedMove.value = null
        _receivedGameStartSync.value = false
        _receivedGameReset.value = false
        _receivedRematchRequest.value = false
        _receivedRematchResponse.value = null
        _receivedGameEndSync.value = false
    }
    
    private fun listenForMessages() {
        scope.launch {
            val buffer = ByteArray(1024)
            
            while (isConnectionAlive && _connectionState.value == ConnectionState.CONNECTED) {
                try {
                    val bytes = inputStream?.read(buffer)
                    if (bytes != null && bytes > 0) {
                        val message = String(buffer, 0, bytes)
                        parseMessage(message)
                        lastHeartbeatReceived.set(System.currentTimeMillis())
                    } else if (bytes == -1) {
                        // Conexión cerrada por el otro lado
                        handleDisconnection("El oponente cerró la conexión")
                        break
                    }
                } catch (e: IOException) {
                    // Error de conexión - el oponente se desconectó
                    handleDisconnection("Error de conexión: ${e.message}")
                    break
                }
            }
        }
    }
    
    private fun handleDisconnection(reason: String = "Conexión perdida") {
        isConnectionAlive = false
        heartbeatJob?.cancel()
        connectionCheckJob?.cancel()
        
        if (_connectionState.value == ConnectionState.CONNECTED) {
            _opponentDisconnected.value = true
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }
    
    private fun parseMessage(message: String) {
        try {
            val parts = message.split("|")
            if (parts.size >= 2) {
                val messageType = MessageType.valueOf(parts[0])
                when (messageType) {
                    MessageType.MOVE -> {
                        if (parts.size == 4) {
                            val row = parts[1].toInt()
                            val col = parts[2].toInt()
                            val player = if (parts[3] == "X") Player.X else Player.O
                            _receivedMove.value = Move(row, col, player)
                        }
                    }
                    MessageType.PLAYER_INFO -> {
                        if (parts.size == 3) {
                            val playerName = parts[1]
                            val assignedPlayer = if (parts[2] == "X") Player.X else Player.O
                            _receivedPlayerInfo.value = PlayerInfo(playerName, assignedPlayer)
                        }
                    }
                    MessageType.GAME_START -> {
                        _connectionEstablished.value = true
                    }
                    MessageType.GAME_START_SYNC -> {
                        _receivedGameStartSync.value = true
                        // Auto-resetear el flag después de un breve delay
                        scope.launch {
                            kotlinx.coroutines.delay(100)
                            _receivedGameStartSync.value = false
                        }
                    }
                    MessageType.GAME_RESET -> {
                        _receivedGameReset.value = true
                        // Auto-resetear el flag después de un breve delay para asegurar que se procese
                        scope.launch {
                            kotlinx.coroutines.delay(100)
                            _receivedGameReset.value = false
                        }
                    }
                    MessageType.REMATCH_REQUEST -> {
                        _receivedRematchRequest.value = true
                        // Auto-resetear el flag después de un breve delay
                        scope.launch {
                            kotlinx.coroutines.delay(100)
                            _receivedRematchRequest.value = false
                        }
                    }
                    MessageType.REMATCH_RESPONSE -> {
                        if (parts.size == 2) {
                            val response = parts[1] == "true"
                            _receivedRematchResponse.value = response
                            // Auto-resetear el flag después de un breve delay
                            scope.launch {
                                kotlinx.coroutines.delay(100)
                                _receivedRematchResponse.value = null
                            }
                        }
                    }
                    MessageType.GAME_QUIT -> {
                        _receivedGameQuit.value = true
                    }
                    MessageType.OPPONENT_DISCONNECTED -> {
                        _opponentDisconnected.value = true
                    }
                    MessageType.HEARTBEAT -> {
                        if (parts.size == 2 && parts[1] == "ping") {
                            // Responder al ping
                            scope.launch {
                                sendMessageWithTimeout("${MessageType.HEARTBEAT}|pong")
                            }
                        } else if (parts.size == 2 && parts[1] == "pong") {
                            // Heartbeat recibido, conexión está viva
                            lastHeartbeatReceived.set(System.currentTimeMillis())
                        }
                    }
                    MessageType.GAME_END_SYNC -> {
                        _receivedGameEndSync.value = true
                        // Auto-resetear el flag después de un breve delay
                        scope.launch {
                            kotlinx.coroutines.delay(100)
                            _receivedGameEndSync.value = false
                        }
                    }
                    MessageType.GO_TO_GAME -> {
                        _receivedGoToGame.value = true
                        // Auto-resetear el flag después de un breve delay
                        scope.launch {
                            kotlinx.coroutines.delay(100)
                            _receivedGoToGame.value = false
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignorar mensajes inválidos
        }
    }
    
    private suspend fun sendMessageWithTimeout(message: String): Boolean {
        return withTimeoutOrNull(SEND_TIMEOUT) {
            try {
                outputStream?.write(message.toByteArray())
                outputStream?.flush()
                true
            } catch (e: IOException) {
                false
            }
        } ?: false
    }
    
    fun sendMove(move: Move) {
        scope.launch {
            val message = "${MessageType.MOVE}|${move.row}|${move.col}|${move.player}"
            if (!sendMessageWithTimeout(message)) {
                handleDisconnection("Error al enviar movimiento")
            }
        }
    }
    
    fun sendPlayerInfo(playerName: String, assignedPlayer: Player) {
        scope.launch {
            val message = "${MessageType.PLAYER_INFO}|${playerName}|${assignedPlayer}"
            if (!sendMessageWithTimeout(message)) {
                handleDisconnection("Error al enviar información del jugador")
            }
        }
    }
    
    fun sendGameStart() {
        scope.launch {
            val message = "${MessageType.GAME_START}|start"
            if (!sendMessageWithTimeout(message)) {
                handleDisconnection("Error al enviar inicio de juego")
            }
        }
    }
    
    fun sendGameStartSync() {
        scope.launch {
            val message = "${MessageType.GAME_START_SYNC}|sync"
            if (!sendMessageWithTimeout(message)) {
                handleDisconnection("Error al enviar sincronización de inicio")
            }
        }
    }
    
    fun sendGameReset() {
        scope.launch {
            val message = "${MessageType.GAME_RESET}|reset"
            if (!sendMessageWithTimeout(message)) {
                handleDisconnection("Error al enviar reinicio de juego")
            }
        }
    }
    
    fun sendRematchRequest() {
        scope.launch {
            val message = "${MessageType.REMATCH_REQUEST}|request"
            if (!sendMessageWithTimeout(message)) {
                handleDisconnection("Error al enviar solicitud de revancha")
            }
        }
    }
    
    fun sendRematchResponse(accept: Boolean) {
        scope.launch {
            val message = "${MessageType.REMATCH_RESPONSE}|$accept"
            if (!sendMessageWithTimeout(message)) {
                handleDisconnection("Error al enviar respuesta de revancha")
            }
        }
    }
    
    fun sendGameQuit() {
        scope.launch {
            val message = "${MessageType.GAME_QUIT}|quit"
            if (!sendMessageWithTimeout(message)) {
                handleDisconnection("Error al enviar salida del juego")
            }
        }
    }
    
    fun sendOpponentDisconnected() {
        scope.launch {
            val message = "${MessageType.OPPONENT_DISCONNECTED}|disconnected"
            sendMessageWithTimeout(message) // No manejar error aquí ya que nos estamos desconectando
        }
    }
    
    fun sendGameEndSync() {
        scope.launch {
            val message = "${MessageType.GAME_END_SYNC}|sync"
            if (!sendMessageWithTimeout(message)) {
                handleDisconnection("Error al enviar sincronización de fin de juego")
            }
        }
    }
    
    fun sendGoToGame() {
        scope.launch {
            val message = "${MessageType.GO_TO_GAME}|navigate"
            if (!sendMessageWithTimeout(message)) {
                handleDisconnection("Error al enviar navegación al juego")
            }
        }
    }
    
    fun isHostDevice(): Boolean = isHost
    
    fun disconnect() {
        scope.launch {
            try {
                // Notificar al oponente antes de desconectarse
                if (_connectionState.value == ConnectionState.CONNECTED) {
                    sendOpponentDisconnected()
                    delay(200) // Dar más tiempo para que se envíe el mensaje
                }
                
                // Detener monitoreo
                isConnectionAlive = false
                heartbeatJob?.cancel()
                connectionCheckJob?.cancel()
                
                bluetoothSocket?.close()
                bluetoothServerSocket?.close()
                inputStream?.close()
                outputStream?.close()
            } catch (e: IOException) {
                // Ignorar errores al cerrar
            } finally {
                bluetoothSocket = null
                bluetoothServerSocket = null
                inputStream = null
                outputStream = null
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    fun makeDiscoverable() {
        if (!hasBluetoothPermissions()) return
        
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        }
        // Necesitaría ser llamado desde una Activity para funcionar
    }
    
    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter.isEnabled
    
    @SuppressLint("MissingPermission")
    fun getLocalDeviceInfo(): String {
        if (!hasBluetoothPermissions()) return "Dispositivo Bluetooth"
        
        val deviceName = bluetoothAdapter.name ?: "Dispositivo sin nombre"
        val deviceAddress = bluetoothAdapter.address ?: "Sin dirección"
        return "$deviceName (${deviceAddress.takeLast(8)})"
    }
    
    @SuppressLint("MissingPermission")
    fun getDeviceDisplayName(device: BluetoothDevice): String {
        if (!hasBluetoothPermissions()) return "Dispositivo Bluetooth"
        
        val deviceName = device.name ?: "Dispositivo sin nombre"
        val shortAddress = device.address.takeLast(8)
        
        return if (deviceName.contains("TicTacToe")) {
            "$deviceName"
        } else {
            "$deviceName ($shortAddress)"
        }
    }
    
    fun cleanup() {
        context.unregisterReceiver(deviceReceiver)
        disconnect()
    }
} 