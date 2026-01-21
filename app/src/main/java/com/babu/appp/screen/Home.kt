package com.babu.appp.screen

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.babu.appp.R
import com.google.android.gms.ads.*

@Composable
fun HomeScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit
) {
    val context = LocalContext.current
    var backPressedTime by remember { mutableStateOf(0L) }

    // 🔄 Animation for theme icon
    val rotation by animateFloatAsState(
        targetValue = if (isDarkMode) 180f else 0f,
        animationSpec = tween(400),
        label = "rotation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isDarkMode) 1.15f else 1f,
        animationSpec = tween(250),
        label = "scale"
    )

    // ✅ Back press exit handling
    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - backPressedTime < 2000) {
            (context as? Activity)?.finish()
        } else {
            backPressedTime = currentTime
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    // 🔑 BACKGROUND COLOR CONTROL (FIX)
    val backgroundColor = if (isDarkMode) {
        Color(0xFF0E0E14)   // deep dark background
    } else {
        Color(0xFFF7F3F7)   // soft light background
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)   // ✅ FIXED
            .padding(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding(),
                start = 16.dp,
                end = 16.dp
            )
    ) {

        // 🔰 HEADER WITH DARK/LIGHT ICON
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Image(
                painter = painterResource(id = R.drawable.pyq_icon),
                contentDescription = "Logo",
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Study Saathi",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = if (isDarkMode) Color.White else Color.Black
            )

            Spacer(modifier = Modifier.weight(1f))

            // 🌙 THEME TOGGLE ICON
            Icon(
                painter = painterResource(
                    id = if (isDarkMode)
                        R.drawable.ic_light_mode
                    else
                        R.drawable.ic_dark_mode
                ),
                contentDescription = "Toggle Theme",
                tint = if (isDarkMode) Color.White else Color.Black,
                modifier = Modifier
                    .size(25.dp)
                    .graphicsLayer {
                        rotationZ = rotation
                        scaleX = scale
                        scaleY = scale
                    }
                    .clickable { onThemeToggle() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Study Materials",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = if (isDarkMode) Color.White else Color.Black
        )

        Spacer(modifier = Modifier.height(12.dp))

        CardGrid(
            isDarkMode = isDarkMode,   // 🔴 IMPORTANT: pass this
            items = listOf(
                Triple("PYQs", R.drawable.pyq_icon, Color(0xFFFF8C00)),
                Triple("Results", R.drawable.result_icon, Color(0xFFFFD100)),
                Triple("Syllabus", R.drawable.syllabus_icon, Color(0xFF00B894)),
                Triple("Important Questions", R.drawable.important_icon, Color(0xFF6C5CE7)),
                Triple("Feedback", R.drawable.feedback_icon, Color(0xFFE64A19)),
                Triple("Competitive Exam", R.drawable.comp_icon, Color(0xFFEF476F))
            ),
            onItemClick = { label ->
                when (label) {
                    "PYQs" -> navController.navigate("pyq")
                    "Syllabus" -> navController.navigate("syllabus")
                    "Results" -> navController.navigate("result")
                    "Important Questions" -> navController.navigate("imp")
                    "Feedback" -> navController.navigate("feedback")
                    "Competitive Exam" -> navController.navigate("comp_exam")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ShowBannerAd(adUnitId = "ca-app-pub-4302526630220985/1663343480")

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun CardGrid(
    isDarkMode: Boolean,   // 🔴 FIX: use this, NOT isSystemInDarkTheme()
    items: List<Triple<String, Int, Color>>,
    onItemClick: (String) -> Unit
) {
    Column {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                rowItems.forEach { (label, iconRes, _) ->

                    // 🔑 CARD COLOR CONTROL (FIXED)
                    val cardColor = if (isDarkMode) {
                        Color(0xFF3A3F4B)   // dark grey card (like your screenshot)
                    } else {
                        Color(0xFFFFFFFF)  // pure white card
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp)
                            .clickable { onItemClick(label) },

                        shape = RoundedCornerShape(18.dp),

                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isDarkMode) 2.dp else 6.dp
                        ),

                        border = if (isDarkMode)
                            BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        else
                            BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f)),

                        colors = CardDefaults.cardColors(
                            containerColor = cardColor
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = iconRes),
                                contentDescription = label,
                                modifier = Modifier.size(48.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                color = if (isDarkMode) Color.White else Color.Black
                            )
                        }
                    }
                }

                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ShowBannerAd(adUnitId: String) {
    AndroidView(
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    )
}
