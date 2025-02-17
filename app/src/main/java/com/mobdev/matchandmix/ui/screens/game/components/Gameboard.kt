package com.mobdev.matchandmix.ui.screens.game.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mobdev.matchandmix.R
import com.mobdev.matchandmix.data.models.Tile
import com.mobdev.matchandmix.data.models.SelectedNumber

@Composable
fun GameBoard(
    tiles: List<Tile>,
    emptyPosition: Int,
    onNumberClick: (Tile, Int) -> Unit,
    incorrectPair: List<SelectedNumber> = emptyList(),
    highlightedPositions: Set<Int> = emptySet()
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        // Empty spaces grid
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (row in 0..2) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (col in 0..2) {
                        val position = row * 3 + col
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .border(
                                    width = 2.dp,
                                    color = if (position in highlightedPositions)
                                        Color(context.getColor(R.color.material_blue))
                                    else
                                        Color(context.getColor(R.color.border_light_gray)),
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
                    }
                }
            }
        }

        // Animated tiles layer
        tiles.forEach { tile ->
            if (tile.position != emptyPosition) {
                GameTile(
                    tile = tile,
                    onNumberClick = onNumberClick,
                    incorrectPair = incorrectPair,
                    isHighlighted = tile.position in highlightedPositions,
                    modifier = Modifier.zIndex(1f)
                )
            }
        }
    }
}