package com.factordev.tic_tac_toe_game.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.factordev.tic_tac_toe_game.bluetooth.BluetoothService

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScreen(
    bluetoothService: BluetoothService,
    onBackClick: () -> Unit,
    onGoToGame: () -> Unit = {}
) {
    val discoveredDevices by bluetoothService.discoveredDevices.collectAsState()
    val connectionState by bluetoothService.connectionState.collectAsState()
    val localDeviceInfo = bluetoothService.getLocalDeviceInfo()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración Bluetooth") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Estado de conexión
                ConnectionStatusCard(connectionState, onGoToGame)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Información del dispositivo local
                LocalDeviceInfoCard(localDeviceInfo)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Si está conectado, mostrar mensaje y botón en lugar de los controles
                if (connectionState == BluetoothService.ConnectionState.CONNECTED) {
                    ConnectedSuccessCard(onGoToGame)
                } else {
                    // Botones de control
                    BluetoothControlButtons(
                        bluetoothService = bluetoothService,
                        connectionState = connectionState
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Lista de dispositivos
                    Text(
                        text = "Dispositivos Disponibles",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    if (discoveredDevices.isEmpty()) {
                        EmptyDeviceList()
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                                                    items(discoveredDevices) { device ->
                            BluetoothDeviceItem(
                                device = device,
                                bluetoothService = bluetoothService,
                                onConnect = { bluetoothService.connectToDevice(device) },
                                connectionState = connectionState
                            )
                        }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusCard(connectionState: BluetoothService.ConnectionState, onGoToGame: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                BluetoothService.ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                BluetoothService.ConnectionState.CONNECTING -> MaterialTheme.colorScheme.secondaryContainer
                BluetoothService.ConnectionState.LISTENING -> MaterialTheme.colorScheme.tertiaryContainer
                BluetoothService.ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (connectionState) {
                    BluetoothService.ConnectionState.CONNECTED -> Icons.Default.Bluetooth
                    BluetoothService.ConnectionState.CONNECTING -> Icons.Default.BluetoothSearching
                    BluetoothService.ConnectionState.LISTENING -> Icons.Default.Visibility
                    BluetoothService.ConnectionState.DISCONNECTED -> Icons.Default.Bluetooth
                },
                contentDescription = null,
                tint = when (connectionState) {
                    BluetoothService.ConnectionState.CONNECTED -> Color.Green
                    BluetoothService.ConnectionState.CONNECTING -> Color(0xFFFFA500)
                    BluetoothService.ConnectionState.LISTENING -> Color.Blue
                    BluetoothService.ConnectionState.DISCONNECTED -> Color.Red
                },
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (connectionState) {
                        BluetoothService.ConnectionState.CONNECTED -> "Conectado"
                        BluetoothService.ConnectionState.CONNECTING -> "Conectando..."
                        BluetoothService.ConnectionState.LISTENING -> "Esperando conexión..."
                        BluetoothService.ConnectionState.DISCONNECTED -> "Desconectado"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = when (connectionState) {
                        BluetoothService.ConnectionState.CONNECTED -> "Listo para jugar"
                        BluetoothService.ConnectionState.CONNECTING -> "Estableciendo conexión"
                        BluetoothService.ConnectionState.LISTENING -> "Visible para otros dispositivos"
                        BluetoothService.ConnectionState.DISCONNECTED -> "Busca dispositivos o hazte visible"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            if (connectionState == BluetoothService.ConnectionState.CONNECTING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
fun BluetoothControlButtons(
    bluetoothService: BluetoothService,
    connectionState: BluetoothService.ConnectionState
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Botón para buscar dispositivos
        Button(
            onClick = { bluetoothService.startDiscovery() },
            modifier = Modifier.weight(1f),
            enabled = connectionState == BluetoothService.ConnectionState.DISCONNECTED
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Buscar")
        }
        
        // Botón para hacerse visible
        OutlinedButton(
            onClick = { bluetoothService.startServer() },
            modifier = Modifier.weight(1f),
            enabled = connectionState == BluetoothService.ConnectionState.DISCONNECTED
        ) {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Visible")
        }
    }
    
    if (connectionState != BluetoothService.ConnectionState.DISCONNECTED) {
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = { bluetoothService.disconnect() },
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Desconectar")
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun BluetoothDeviceItem(
    device: BluetoothDevice,
    bluetoothService: BluetoothService,
    onConnect: () -> Unit,
    connectionState: BluetoothService.ConnectionState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DeviceHub,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bluetoothService.getDeviceDisplayName(device),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = if (device.name?.contains("TicTacToe") == true) {
                        "🎮 Juego Tic-Tac-Toe disponible"
                    } else {
                        "Dispositivo Bluetooth estándar"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (device.name?.contains("TicTacToe") == true) {
                        Color(0xFF4CAF50)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    }
                )
            }
            
            Button(
                onClick = onConnect,
                enabled = connectionState == BluetoothService.ConnectionState.DISCONNECTED
            ) {
                Text("Conectar")
            }
        }
    }
}

@Composable
fun EmptyDeviceList() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.BluetoothSearching,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No se encontraron dispositivos",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Presiona 'Buscar' para encontrar dispositivos cercanos o 'Visible' para que otros te encuentren",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ConnectedSuccessCard(onGoToGame: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Green.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎉",
                fontSize = 64.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "¡Conexión Establecida!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Green,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "La conexión Bluetooth se ha establecido exitosamente. ¡Ahora puedes empezar a jugar!",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onGoToGame,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color.Green
                )
            ) {
                Icon(
                    imageVector = Icons.Default.DeviceHub,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ir al Juego",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun LocalDeviceInfoCard(localDeviceInfo: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DeviceHub,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tu dispositivo aparece como:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Text(
                    text = localDeviceInfo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}