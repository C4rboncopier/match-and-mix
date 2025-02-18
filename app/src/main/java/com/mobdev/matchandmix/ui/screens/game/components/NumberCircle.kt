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

    val rotation = remember { Animatable(if (isRevealed) 180f else 0f) }

    var wasRevealed by remember { mutableStateOf(isRevealed) }

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
                    isMatched -> Color(context.getColor(R.color.material_green))
                    isIncorrect -> Color(context.getColor(R.color.material_red))
                    isRevealed -> Color(context.getColor(R.color.white))
                    else -> Color(context.getColor(R.color.border_gray))
                },
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = when {
                    isMatched -> Color(context.getColor(R.color.material_green))
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
                        isMatched -> Color(context.getColor(R.color.white))
                        isIncorrect -> Color(context.getColor(R.color.white))
                        else -> Color(context.getColor(R.color.text_dark_gray))
                    },
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}