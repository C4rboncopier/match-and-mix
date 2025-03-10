package com.mobdev.matchandmix.ui.screens.game.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobdev.matchandmix.R
import kotlinx.coroutines.delay

// Predefined distinct light colors for number pairs
private val numberColors = listOf(
    Color(0xFFFFF9C4), // Light Yellow
    Color(0xFFD1C4E9), // Light Purple
    Color(0xFFFFE0B2), // Light Orange
    Color(0xFFF8BBD0), // Light Pink
    Color(0xFF80DEEA), // Soft Cyan
    Color(0xFFE6EE9C), // Light Lime
    Color(0xFFFFD54F), // Light Gold
    Color(0xFFFF8A65), // Light Peach
    Color(0xFFFFF59D), // Light Cream
    Color(0xFFB2EBF2), // Light Aqua
    Color(0xFFC5CAE9), // Soft Periwinkle
    Color(0xFFAED581), // Fresh Green
    Color(0xFFFFAB91), // Light Coral
    Color(0xFFFFCC80), // Warm Apricot
    Color(0xFF90CAF9), // Light Sky Blue
    Color(0xFF9CCC65), // Soft Green
    Color(0xFFFFECB3), // Soft Gold
    Color(0xFF81D4FA), // Soft Teal
    Color(0xFFFFB74D), // Soft Tangerine
    Color(0xFF64FFDA)  // Soft Turquoise
).mapIndexed { index, color -> index + 1 to color }.toMap() // Assign colors to numbers

@Composable
fun NumberCircle(
    number: Int,
    isRevealed: Boolean,
    isMatched: Boolean,
    isIncorrect: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val assignedColor = numberColors[number] ?: Color.Gray // Get assigned color

    val rotation = remember { Animatable(if (isRevealed) 180f else 0f) }
    var wasRevealed by remember { mutableStateOf(isRevealed) }
    var showColors by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(90000)
        showColors = false
    }

    LaunchedEffect(isRevealed) {
        if (isRevealed && !wasRevealed) {
            wasRevealed = true
            rotation.animateTo(
                targetValue = 180f,
                animationSpec = tween(
                    durationMillis = 400,
                    easing = FastOutSlowInEasing
                )
            )
        } else if (!isRevealed && wasRevealed) {
            wasRevealed = false
            rotation.snapTo(0f)
        } else if (isRevealed && wasRevealed) {
            rotation.snapTo(180f)
        }
    }

    Box(
        modifier = modifier
            .size(28.dp)
            .graphicsLayer {
                rotationY = rotation.value
                cameraDistance = 12 * density
            }
            .background(
                color = when {
                    isMatched -> assignedColor
                    isIncorrect -> Color(context.getColor(R.color.material_red))
                    isRevealed && showColors -> assignedColor
                    else -> Color(context.getColor(R.color.border_gray))
                },
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = when {
                    isMatched -> assignedColor
                    isIncorrect -> Color(context.getColor(R.color.material_red))
                    else -> Color(context.getColor(R.color.border_light_gray))
                },
                shape = CircleShape
            )
            .clickable(enabled = !isMatched && !isRevealed) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isRevealed || isMatched) {
            val shouldShowContent = rotation.value > 90f

            Box(
                modifier = Modifier.graphicsLayer {
                    rotationY = if (shouldShowContent) 180f else 0f
                    alpha = if (shouldShowContent) 1f else 0f
                },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isMatched -> Color.Black
                        isIncorrect -> Color.White
                        else -> Color(context.getColor(R.color.text_dark_gray))
                    },
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
