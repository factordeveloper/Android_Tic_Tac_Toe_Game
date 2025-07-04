package com.factordev.tic_tac_toe_game.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

data class Firework(
    val x: Float,
    val y: Float,
    val color: Color,
    val particles: List<Particle>
)

data class Particle(
    val startX: Float,
    val startY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val color: Color,
    val size: Float
)

@Composable
fun FireworksAnimation(
    winner: String,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val screenWidth = with(density) { 400.dp.toPx() }
    val screenHeight = with(density) { 600.dp.toPx() }
    
    var animationStarted by remember { mutableStateOf(false) }
    var currentFireworks by remember { mutableStateOf<List<Firework>>(emptyList()) }
    var showText by remember { mutableStateOf(false) }
    
    // Colores vibrantes para los fuegos artificiales
    val fireworkColors = listOf(
        Color(0xFFFF6B6B), // Rojo
        Color(0xFF4ECDC4), // Turquesa
        Color(0xFF45B7D1), // Azul
        Color(0xFF96CEB4), // Verde
        Color(0xFFFFEB3B), // Amarillo
        Color(0xFFFF9800), // Naranja
        Color(0xFFE91E63), // Rosa
        Color(0xFF9C27B0)  // Morado
    )
    
    val infiniteTransition = rememberInfiniteTransition()
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    // Crear fuegos artificiales
    LaunchedEffect(animationStarted) {
        if (!animationStarted) {
            animationStarted = true
            delay(500)
            showText = true
            
            // Crear múltiples fuegos artificiales
            repeat(5) { index ->
                delay(index * 500L)
                val firework = createFirework(
                    x = Random.nextFloat() * screenWidth,
                    y = Random.nextFloat() * screenHeight * 0.7f + 100f,
                    color = fireworkColors[index % fireworkColors.size]
                )
                currentFireworks = currentFireworks + firework
            }
        }
    }
    
    // Limpiar después de un tiempo
    LaunchedEffect(showText) {
        if (showText) {
            delay(8000)
            onDismiss()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Fondo semi-transparente
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.8f)
        ) {
            // Canvas para dibujar fuegos artificiales
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                currentFireworks.forEach { firework ->
                    drawFirework(firework, animationProgress)
                }
            }
        }
        
        // Texto del ganador
        if (showText) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🎉",
                    fontSize = 72.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = "¡FELICITACIONES!",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontSize = 32.sp
                )
                
                Text(
                    text = winner,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Yellow,
                    textAlign = TextAlign.Center,
                    fontSize = 28.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                Text(
                    text = "¡HAS GANADO!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontSize = 24.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "🏆 ¡Eres increíble! 🏆",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun createFirework(x: Float, y: Float, color: Color): Firework {
    val particleCount = 30
    val particles = (0 until particleCount).map { i ->
        val angle = (i * 2 * PI / particleCount).toFloat()
        val speed = Random.nextFloat() * 200f + 50f
        Particle(
            startX = x,
            startY = y,
            velocityX = cos(angle) * speed,
            velocityY = sin(angle) * speed,
            color = color.copy(alpha = Random.nextFloat() * 0.5f + 0.5f),
            size = Random.nextFloat() * 6f + 2f
        )
    }
    return Firework(x, y, color, particles)
}

private fun DrawScope.drawFirework(firework: Firework, progress: Float) {
    val gravity = 100f
    val drag = 0.98f
    
    firework.particles.forEach { particle ->
        val timeSquared = progress * progress
        val currentX = particle.startX + particle.velocityX * progress * drag
        val currentY = particle.startY + particle.velocityY * progress * drag + gravity * timeSquared
        
        // Fade out effect
        val alpha = (1f - progress).coerceIn(0f, 1f)
        val particleColor = particle.color.copy(alpha = alpha)
        
        // Draw particle as a circle
        drawCircle(
            color = particleColor,
            radius = particle.size * (1f - progress * 0.5f),
            center = androidx.compose.ui.geometry.Offset(currentX, currentY)
        )
        
        // Draw sparkle effect
        if (progress < 0.7f) {
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.8f),
                radius = particle.size * 0.3f,
                center = androidx.compose.ui.geometry.Offset(currentX, currentY)
            )
        }
    }
} 