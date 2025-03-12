package com.mobdev.matchandmix.ui.screens.credits

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
fun CreditsScreen(navController: NavController) {
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
                .padding(top = 25.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally // Center everything
        ) {
            // Top Bar with Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButtonLogo(
                    clickedIconRes = R.drawable.backarrow,
                    defaultIconRes = R.drawable.backarrow,
                    onClick = { navController.navigateUp() }
                )

                Text(
                    text = "Credits",
                    fontSize = 35.sp,
                    fontFamily = FontFamily(Font(R.font.sigmarregular)),
                    fontWeight = FontWeight.ExtraLight,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                // Empty box for alignment
                Box(modifier = Modifier.size(48.dp))
            }
            // Scrollable Column for Credits Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally // Center all content
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Match & Mix",
                            fontSize = 32.sp,
                            fontFamily = FontFamily(Font(R.font.sigmarregular)),
                            fontWeight = FontWeight.ExtraLight,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Mobile Application Development Final Project",
                            fontSize = 18.sp,
                            fontFamily = FontFamily(Font(R.font.ovoregular)),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                        )
                    }
                }
                CreditSection(
                    title = "Game Developers",
                    content ="Gimril N. Lozarita\nRhudsel James M. Uy"
                )
                CreditSection(
                    title = "Course Code & Section",
                    content = "CPE144L - A361"
                )
                CreditSection(
                    title = "Instructor",
                    content = "Engr. Ariel C. Magbanua"
                )
                CreditSection(
                    title = "Development Tools & Frameworks",
                    content = "Jetpack Compose - UI Development\nFirestore Firebase - Real-time Database & Multiplayer\nGoogle Sign-In - Authentication & User Management\nAndroid Studio - Development Environment"
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Thank You for Playing!",
                            fontSize = 22.sp,
                            fontFamily = FontFamily(Font(R.font.sigmarregular)),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// Reusable Composable for Credit Sections
@Composable
private fun CreditSection(title: String, content: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally // Center content
        ) {
            Text(
                text = title,
                fontSize = 24.sp,
                fontFamily = FontFamily(Font(R.font.dangrekregular)),
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = content,
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.ovoregular)),
                textAlign = TextAlign.Center,
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