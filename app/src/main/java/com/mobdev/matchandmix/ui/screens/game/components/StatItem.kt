package com.mobdev.matchandmix.ui.screens.game.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mobdev.matchandmix.R

@Composable
fun StatItem(label: String, value: Int) {
    val context = LocalContext.current
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .padding(horizontal = 4.dp)
    ) {
        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color(context.getColor(R.color.text_gray)),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Value
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(32.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(context.getColor(R.color.material_blue)),
                textAlign = TextAlign.Center
            )
        }
    }
}