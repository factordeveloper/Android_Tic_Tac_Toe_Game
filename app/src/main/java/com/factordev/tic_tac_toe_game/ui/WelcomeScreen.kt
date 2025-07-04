package com.factordev.tic_tac_toe_game.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.factordev.tic_tac_toe_game.model.GameMode
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(
    onModeSelected: (GameMode) -> Unit
) {
    var showAnimation by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(200)
        showAnimation = true
    }
    
    val alphaAnimation by animateFloatAsState(
        targetValue = if (showAnimation) 1f else 0f,
        animationSpec = tween(800)
    )
    
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
                .alpha(alphaAnimation),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Título
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎮",
                    fontSize = 72.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Tic Tac Toe",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 48.sp
                )
                Text(
                    text = "¡Elige tu modo de juego!",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            // Opciones de modo de juego
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GameModeCard(
                    title = "🤖 Contra la Computadora",
                    description = "Juega contra la IA",
                    icon = Icons.Default.Computer,
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    onClick = { onModeSelected(GameMode.SINGLE_PLAYER) }
                )
                
                GameModeCard(
                    title = "👥 Multijugador Local",
                    description = "Juega con alguien en el mismo dispositivo",
                    icon = Icons.Default.Group,
                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = { onModeSelected(GameMode.MULTIPLAYER_LOCAL) }
                )
                
                GameModeCard(
                    title = "📱 Multijugador Bluetooth",
                    description = "Juega con alguien por Bluetooth",
                    icon = Icons.Default.Bluetooth,
                    backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                    onClick = { onModeSelected(GameMode.MULTIPLAYER_BLUETOOTH) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameModeCard(
    title: String,
    description: String,
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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