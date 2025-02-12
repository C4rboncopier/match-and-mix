package com.example.matchandmixtrial.ui.screens.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.matchandmixtrial.R

@Composable
fun NumberCircle(
    number: Int,
    isRevealed: Boolean,
    isMatched: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Box(
        modifier = modifier
            .size(28.dp)
            .background(
                color = when {
                    isMatched -> Color(context.getColor(R.color.material_green))
                    isRevealed -> Color(context.getColor(R.color.white))
                    else -> Color(context.getColor(R.color.border_gray))  // Light gray for hidden numbers
                },
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = when {
                    isMatched -> Color(context.getColor(R.color.material_green))
                    else -> Color(context.getColor(R.color.border_light_gray))
                },
                shape = CircleShape
            )
            .clickable(enabled = !isMatched && !isRevealed) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isRevealed || isMatched) {
            Text(
                text = number.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isMatched) Color(context.getColor(R.color.white)) else Color(context.getColor(R.color.text_dark_gray)),
                textAlign = TextAlign.Center
            )
        }
    }
} 