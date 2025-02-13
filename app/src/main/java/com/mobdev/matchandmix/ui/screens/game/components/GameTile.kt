package com.mobdev.matchandmix.ui.screens.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mobdev.matchandmix.R
import com.mobdev.matchandmix.data.models.Tile
import com.mobdev.matchandmix.data.models.SelectedNumber

@Composable
fun GameTile(
    tile: Tile,
    onNumberClick: (Tile, Int) -> Unit,
    incorrectPair: List<SelectedNumber> = emptyList(),
    isHighlighted: Boolean = false
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
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
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxSize().padding(8.dp)
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
                    onClick = { onNumberClick(tile, 0) }
                )
                NumberCircle(
                    number = tile.numbers[1],
                    isRevealed = tile.isRevealed[1],
                    isMatched = tile.isMatched[1],
                    isIncorrect = incorrectPair.any { it.tile.id == tile.id && it.numberIndex == 1 },
                    onClick = { onNumberClick(tile, 1) }
                )
            }
            // Middle number
            NumberCircle(
                number = tile.numbers[2],
                isRevealed = tile.isRevealed[2],
                isMatched = tile.isMatched[2],
                isIncorrect = incorrectPair.any { it.tile.id == tile.id && it.numberIndex == 2 },
                onClick = { onNumberClick(tile, 2) }
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
                    onClick = { onNumberClick(tile, 3) }
                )
                NumberCircle(
                    number = tile.numbers[4],
                    isRevealed = tile.isRevealed[4],
                    isMatched = tile.isMatched[4],
                    isIncorrect = incorrectPair.any { it.tile.id == tile.id && it.numberIndex == 4 },
                    onClick = { onNumberClick(tile, 4) }
                )
            }
        }
    }
}