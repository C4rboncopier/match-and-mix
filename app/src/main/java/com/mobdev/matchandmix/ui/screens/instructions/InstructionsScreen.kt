package com.mobdev.matchandmix.ui.screens.instructions

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mobdev.matchandmix.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun InstructionsScreen(navController: NavController) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.isbackground2),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(top = 25.dp, start = 16.dp, end = 16.dp)
        ) {
            // Top Bar with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier
                ) {
                    IconButtonLogo(
                        clickedIconRes = R.drawable.backarrow,
                        defaultIconRes = R.drawable.backarrow,
                        onClick = { navController.navigateUp() },
                    )
                }
                Text(
                    text = stringResource(R.string.instructions_title),
                    fontSize = 35.sp,
                    fontFamily = FontFamily(Font(R.font.sigmarregular)),
                    fontWeight = FontWeight.ExtraLight,
                    color = MaterialTheme.colorScheme.primary
                )
                // Empty box for alignment
                Box(modifier = Modifier.size(48.dp))
            }

            // Instructions content in a scrollable column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InstructionSection(
                    title = stringResource(R.string.instructions_section_objective_title),
                    content = stringResource(R.string.instructions_section_objective_content)
                )

                InstructionSection(
                    title = stringResource(R.string.instructions_section_elements_title),
                    content = stringResource(R.string.instructions_section_elements_content)
                )

                InstructionSection(
                    title = stringResource(R.string.instructions_section_howto_title),
                    content = stringResource(R.string.instructions_section_howto_content)
                )

                InstructionSection(
                    title = stringResource(R.string.instructions_section_multiplayer_title),
                    content = stringResource(R.string.instructions_section_multiplayer_content)
                )

                InstructionSection(
                    title = stringResource(R.string.instructions_section_tips_title),
                    content = stringResource(R.string.instructions_section_tips_content)
                )
                
                // Add some space at the bottom for better scrolling
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun InstructionSection(
    title: String,
    content: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 24.sp,
                fontFamily = FontFamily(Font(R.font.dangrekregular)),
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = content,
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.ovoregular)),
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun IconButtonLogo(
    defaultIconRes: Int,
    clickedIconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isClicked by remember { mutableStateOf(false) }

    IconButton(
        onClick = {
            isClicked = true
            onClick()
            CoroutineScope(Dispatchers.Main).launch {
                delay(100) // Reset image after 100ms
                isClicked = false
            }
        },
        modifier = Modifier.size(50.dp)
    ) {
        Image(
            painter = painterResource(id = if (isClicked) clickedIconRes else defaultIconRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(35.dp)
        )
    }
}
