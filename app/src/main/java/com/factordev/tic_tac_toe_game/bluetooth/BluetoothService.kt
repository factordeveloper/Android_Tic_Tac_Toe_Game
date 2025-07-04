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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

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
    
    private var bluetoothSocket: BluetoothSocket? = null
    private var bluetoothServerSocket: BluetoothServerSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    
    private val serviceUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val serviceName = "TicTacToe_Game_${System.currentTimeMillis() % 10000}"
    
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isHost = false // Si este dispositivo es el host (servidor)
    
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
                    listenForMessages()
                }
            } catch (e: IOException) {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        if (!hasBluetoothPermissions()) return
        
        isHost = false
        scope.launch {
            try {
                _connectionState.value = ConnectionState.CONNECTING
                
                val socket = device.createRfcommSocketToServiceRecord(serviceUUID)
                bluetoothSocket = socket
                
                bluetoothAdapter.cancelDiscovery()
                
                socket.connect()
                setupStreams(socket)
                _connectionState.value = ConnectionState.CONNECTED
                listenForMessages()
            } catch (e: IOException) {
                _connectionState.value = ConnectionState.DISCONNECTED
                bluetoothSocket?.close()
                bluetoothSocket = null
            }
        }
    }
    
    private fun setupStreams(socket: BluetoothSocket) {
        inputStream = socket.inputStream
        outputStream = socket.outputStream
    }
    
    private fun listenForMessages() {
        scope.launch {
            val buffer = ByteArray(1024)
            
            while (_connectionState.value == ConnectionState.CONNECTED) {
                try {
                    val bytes = inputStream?.read(buffer)
                    if (bytes != null && bytes > 0) {
                        val message = String(buffer, 0, bytes)
                        parseMessage(message)
                    } else if (bytes == -1) {
                        // Conexión cerrada por el otro lado
                        _opponentDisconnected.value = true
                        _connectionState.value = ConnectionState.DISCONNECTED
                        break
                    }
                } catch (e: IOException) {
                    // Error de conexión - el oponente se desconectó
                    _opponentDisconnected.value = true
                    _connectionState.value = ConnectionState.DISCONNECTED
                    break
                }
            }
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
                    MessageType.GAME_RESET -> {
                        _receivedGameReset.value = true
                    }
                    MessageType.REMATCH_REQUEST -> {
                        _receivedRematchRequest.value = true
                    }
                    MessageType.REMATCH_RESPONSE -> {
                        if (parts.size == 2) {
                            val response = parts[1] == "true"
                            _receivedRematchResponse.value = response
                        }
                    }
                    MessageType.GAME_QUIT -> {
                        _receivedGameQuit.value = true
                    }
                    MessageType.OPPONENT_DISCONNECTED -> {
                        _opponentDisconnected.value = true
                    }
                }
            }
        } catch (e: Exception) {
            // Ignorar mensajes inválidos
        }
    }
    
    fun sendMove(move: Move) {
        scope.launch {
            try {
                val message = "${MessageType.MOVE}|${move.row}|${move.col}|${move.player}"
                outputStream?.write(message.toByteArray())
                outputStream?.flush()
            } catch (e: IOException) {
                // Error al enviar mensaje
            }
        }
    }
    
    fun sendPlayerInfo(playerName: String, assignedPlayer: Player) {
        scope.launch {
            try {
                val message = "${MessageType.PLAYER_INFO}|${playerName}|${assignedPlayer}"
                outputStream?.write(message.toByteArray())
                outputStream?.flush()
            } catch (e: IOException) {
                // Error al enviar mensaje
            }
        }
    }
    
    fun sendGameStart() {
        scope.launch {
            try {
                val message = "${MessageType.GAME_START}|start"
                outputStream?.write(message.toByteArray())
                outputStream?.flush()
            } catch (e: IOException) {
                // Error al enviar mensaje
            }
        }
    }
    
    fun sendGameReset() {
        scope.launch {
            try {
                val message = "${MessageType.GAME_RESET}|reset"
                outputStream?.write(message.toByteArray())
                outputStream?.flush()
            } catch (e: IOException) {
                // Error al enviar mensaje
            }
        }
    }
    
    fun sendRematchRequest() {
        scope.launch {
            try {
                val message = "${MessageType.REMATCH_REQUEST}|request"
                outputStream?.write(message.toByteArray())
                outputStream?.flush()
            } catch (e: IOException) {
                // Error al enviar mensaje
            }
        }
    }
    
    fun sendRematchResponse(accept: Boolean) {
        scope.launch {
            try {
                val message = "${MessageType.REMATCH_RESPONSE}|$accept"
                outputStream?.write(message.toByteArray())
                outputStream?.flush()
            } catch (e: IOException) {
                // Error al enviar mensaje
            }
        }
    }
    
    fun sendGameQuit() {
        scope.launch {
            try {
                val message = "${MessageType.GAME_QUIT}|quit"
                outputStream?.write(message.toByteArray())
                outputStream?.flush()
            } catch (e: IOException) {
                // Error al enviar mensaje
            }
        }
    }
    
    fun sendOpponentDisconnected() {
        scope.launch {
            try {
                val message = "${MessageType.OPPONENT_DISCONNECTED}|disconnected"
                outputStream?.write(message.toByteArray())
                outputStream?.flush()
            } catch (e: IOException) {
                // Error al enviar mensaje
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
                    kotlinx.coroutines.delay(100) // Dar tiempo para que se envíe el mensaje
                }
                
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