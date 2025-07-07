package com.factordev.tic_tac_toe_game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.factordev.tic_tac_toe_game.bluetooth.BluetoothService

enum class BluetoothSetupStep {
    PERMISSIONS,
    PLAYER_NAME,
    CONNECTION_MODE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothSetupScreen(
    bluetoothService: BluetoothService,
    onBackPressed: () -> Unit,
    onSetupComplete: (String, Boolean) -> Unit,
    onRequestPermissions: () -> Unit,
    hasPermissions: Boolean
) {
    var currentStep by remember { mutableStateOf(BluetoothSetupStep.PERMISSIONS) }
    var playerName by remember { mutableStateOf("") }
    var showNameError by remember { mutableStateOf(false) }
    
    LaunchedEffect(hasPermissions) {
        if (hasPermissions) {
            currentStep = BluetoothSetupStep.PLAYER_NAME
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Configuración Bluetooth",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            when (currentStep) {
                BluetoothSetupStep.PERMISSIONS -> {
                    PermissionsStep(
                        onRequestPermissions = onRequestPermissions,
                        hasPermissions = hasPermissions
                    )
                }
                BluetoothSetupStep.PLAYER_NAME -> {
                    PlayerNameStep(
                        playerName = playerName,
                        onNameChange = { 
                            playerName = it
                            showNameError = false
                        },
                        showError = showNameError,
                        onNext = {
                            val trimmedName = playerName.trim()
                            if (trimmedName.isNotEmpty()) {
                                playerName = trimmedName // Actualizar con el nombre sin espacios
                                currentStep = BluetoothSetupStep.CONNECTION_MODE
                            } else {
                                showNameError = true
                            }
                        }
                    )
                }
                BluetoothSetupStep.CONNECTION_MODE -> {
                    ConnectionModeStep(
                        playerName = playerName,
                        onModeSelected = { isSearching ->
                            onSetupComplete(playerName.trim(), isSearching)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionsStep(
    onRequestPermissions: () -> Unit,
    hasPermissions: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                text = "🔒",
                fontSize = 64.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "Permisos Necesarios",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Para jugar por Bluetooth necesitamos acceso a:",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📡 Bluetooth")
                Text("🔍 Buscar dispositivos")
                Text("📍 Ubicación (requerida por Android)")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (hasPermissions) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Green.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = "✅ Permisos concedidos",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Green,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Conceder Permisos")
                }
            }
        }
    }
}

@Composable
fun PlayerNameStep(
    playerName: String,
    onNameChange: (String) -> Unit,
    showError: Boolean,
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "¿Cuál es tu nombre?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Tu oponente verá este nombre durante el juego",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedTextField(
                value = playerName,
                onValueChange = { newName ->
                    // Capitalizar automáticamente la primera letra
                    val capitalizedName = if (newName.isNotEmpty()) {
                        newName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    } else {
                        newName
                    }
                    onNameChange(capitalizedName)
                },
                label = { Text("Nombre del jugador") },
                placeholder = { Text("Escribe tu nombre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = showError,
                supportingText = if (showError) {
                    { Text("Por favor ingresa tu nombre") }
                } else null
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continuar")
            }
        }
    }
}

@Composable
fun ConnectionModeStep(
    playerName: String,
    onModeSelected: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                text = "¡Hola, ${playerName.trim()}! 👋",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "¿Cómo quieres conectarte?",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            ConnectionModeCard(
                title = "🔍 Buscar Dispositivos",
                description = "Buscar otros jugadores cercanos",
                icon = Icons.Default.BluetoothSearching,
                onClick = { onModeSelected(true) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ConnectionModeCard(
                title = "👁️ Hacerse Visible",
                description = "Permitir que otros te encuentren",
                icon = Icons.Default.Visibility,
                onClick = { onModeSelected(false) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionModeCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
} 