package com.example.matchandmixtrial.ui.screens.game.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.matchandmixtrial.R
import com.example.matchandmixtrial.data.models.Tile

@Composable
fun GameBoard(
    tiles: List<Tile>,
    emptyPosition: Int,
    onNumberClick: (Tile, Int) -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (row in 0..2) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (col in 0..2) {
                    val position = row * 3 + col
                    Box(
                        modifier = Modifier.size(100.dp)
                    ) {
                        if (position == emptyPosition) {
                            // Empty space with dashed border
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(
                                        width = 2.dp,
                                        color = Color(context.getColor(R.color.border_light_gray)),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            )
                        } else {
                            val tileAtPosition = tiles.find { it.position == position }
                            if (tileAtPosition != null) {
                                GameTile(
                                    tile = tileAtPosition,
                                    onNumberClick = onNumberClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} 