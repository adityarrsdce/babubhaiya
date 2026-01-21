package com.babu.appp.screen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.babu.appp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL

// ---------------- DATA CLASS ----------------
data class ButtonItem(
    val label: String,
    val link: String,
    val color: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(navController: NavController, jsonUrl: String = "https://raw.githubusercontent.com/adityarrsdce/babubhaiya/refs/heads/main/Feedback/button.json") {
    val context = LocalContext.current
    var buttons by remember { mutableStateOf(listOf<ButtonItem>()) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    @DrawableRes val backgroundResId = if (isDark) R.drawable.pyq_dark else R.drawable.pyq_light
    val backgroundPainter = painterResource(id = backgroundResId)

    val appBarColor = if (isDark) Color(0xFF1C1C1C) else Color(0xFFE5D1B5)
    val appBarTextColor = if (isDark) Color.White else Color.Black

    // ✅ Load buttons from JSON
    LaunchedEffect(Unit) {
        buttons = loadButtonsFromJson(jsonUrl)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feedback", color = appBarTextColor) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = appBarColor)
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Image(
                painter = backgroundPainter,
                contentDescription = "background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (buttons.isEmpty()) {
                    Text(
                        "Loading...",
                        fontSize = 16.sp,
                        color = if (isDark) Color.White else Color.Black
                    )
                } else {
                    // ✅ Map buttons from JSON
                    buttons.forEach { button ->
                        val buttonColor = try {
                            Color(android.graphics.Color.parseColor(button.color))
                        } catch (e: Exception) {
                            Color.Gray
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(button.link))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = buttonColor),
                            elevation = CardDefaults.cardElevation(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = button.label, color = Color.White, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- HELPER FUNCTION ----------------
suspend fun loadButtonsFromJson(jsonUrl: String): List<ButtonItem> {
    return withContext(Dispatchers.IO) {
        try {
            val jsonString = URL(jsonUrl).readText()
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<ButtonItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val label = obj.optString("label", "Unknown")
                val link = obj.optString("link", "")
                val color = obj.optString("color", "#3498DB")
                list.add(ButtonItem(label, link, color))
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
