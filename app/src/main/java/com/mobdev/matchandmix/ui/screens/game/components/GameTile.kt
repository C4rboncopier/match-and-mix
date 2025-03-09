package com.mobdev.matchandmix.ui.screens.game.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mobdev.matchandmix.R
import com.mobdev.matchandmix.data.models.Tile
import com.mobdev.matchandmix.data.models.SelectedNumber
import kotlinx.coroutines.launch

@Composable
fun GameTile(
    tile: Tile,
    onNumberClick: (Tile, Int) -> Unit,
    onTileClick: () -> Unit,
    incorrectPair: List<SelectedNumber> = emptyList(),
    isHighlighted: Boolean = false,
    isMultiplayer: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Calculate grid position
    val row = tile.position / 3
    val col = tile.position % 3

    // Create animated offset values
    val offsetX = remember { Animatable(col * 108f) }
    val offsetY = remember { Animatable(row * 108f) }

    // Update position with animation when tile moves
    LaunchedEffect(tile.position) {
        val newRow = tile.position / 3
        val newCol = tile.position % 3

        launch {
            offsetX.animateTo(
                targetValue = newCol * 108f,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            )
        }
        launch {
            offsetY.animateTo(
                targetValue = newRow * 108f,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    // Only make the tile clickable in multiplayer mode and when highlighted
    val isTileClickable = isMultiplayer && isHighlighted

    Box(
        modifier = modifier
            .offset(
                x = offsetX.value.dp,
                y = offsetY.value.dp
            )
            .size(100.dp)
            .border(
                width = if (isHighlighted) 3.dp else 2.dp,
                color = if (isHighlighted)
                    Color(context.getColor(R.color.material_blue))
                else
                    Color(context.getColor(R.color.border_gray)),
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = Color(context.getColor(R.color.white)),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = isTileClickable) { onTileClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clickable(enabled = isTileClickable) { onTileClick() }
        ) {
            // Top row numbers
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                NumberCircle(
                    number = tile.numbers[0],
                    isRevealed = tile.isRevealed[0],
                    isMatched = tile.isMatched[0],
                    isIncorrect = incorrectPair.any { it.tile.id == tile.id && it.numberIndex == 0 },
                    onClick = { if (!isTileClickable) onNumberClick(tile, 0) else onTileClick() }
                )
                NumberCircle(
                    number = tile.numbers[1],
                    isRevealed = tile.isRevealed[1],
                    isMatched = tile.isMatched[1],
                    isIncorrect = incorrectPair.any { it.tile.id == tile.id && it.numberIndex == 1 },
                    onClick = { if (!isTileClickable) onNumberClick(tile, 1) else onTileClick() }
                )
            }

            // Middle number
            NumberCircle(
                number = tile.numbers[2],
                isRevealed = tile.isRevealed[2],
                isMatched = tile.isMatched[2],
                isIncorrect = incorrectPair.any { it.tile.id == tile.id && it.numberIndex == 2 },
                onClick = { if (!isTileClickable) onNumberClick(tile, 2) else onTileClick() }
            )

            // Bottom row numbers
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                NumberCircle(
                    number = tile.numbers[3],
                    isRevealed = tile.isRevealed[3],
                    isMatched = tile.isMatched[3],
                    isIncorrect = incorrectPair.any { it.tile.id == tile.id && it.numberIndex == 3 },
                    onClick = { if (!isTileClickable) onNumberClick(tile, 3) else onTileClick() }
                )
                NumberCircle(
                    number = tile.numbers[4],
                    isRevealed = tile.isRevealed[4],
                    isMatched = tile.isMatched[4],
                    isIncorrect = incorrectPair.any { it.tile.id == tile.id && it.numberIndex == 4 },
                    onClick = { if (!isTileClickable) onNumberClick(tile, 4) else onTileClick() }
                )
            }
        }
    }
}