package com.babu.appp.screen.bottom_bar

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.URL

// ---------------------------- DATA CLASSES ----------------------------
@Serializable
data class TextItem(
    val text: String,
    val bold: Boolean = false
)

@Serializable
data class AboutData(
    val title: TextItem,
    val description: TextItem,
    val developer: TextItem,
    val team_heading: TextItem,
    val team_members: List<TextItem>,
    val join_heading: TextItem,
    val join_description: TextItem,
    val linkedin_text: TextItem,
    val linkedin_url: String
)

// ---------------------------- FETCH JSON ----------------------------
suspend fun fetchAboutJson(url: String): AboutData? = withContext(Dispatchers.IO) {
    try {
        val jsonString = URL(url).readText()
        Json.decodeFromString<AboutData>(jsonString)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// ---------------------------- ABOUT SCREEN ----------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen() {
    val uriHandler = LocalUriHandler.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val appBarColor = if (isDark) Color(0xFF1C1C1C) else Color(0xFFE5D1B5)
    val appBarTextColor = if (isDark) Color.White else Color.Black

    // ✅ Replace with your raw GitHub JSON URL
    val jsonUrl = "https://raw.githubusercontent.com/adityarrsdce/babubhaiya/refs/heads/main/Team/about.json"
    var aboutData by remember { mutableStateOf<AboutData?>(null) }

    LaunchedEffect(jsonUrl) {
        aboutData = fetchAboutJson(jsonUrl)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "About", color = appBarTextColor) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = appBarColor)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .statusBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (aboutData != null) {
                // Title
                Text(
                    text = aboutData!!.title.text,
                    fontSize = 25.sp,
                    fontWeight = if (aboutData!!.title.bold) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                Text(
                    text = aboutData!!.description.text,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = if (aboutData!!.description.bold) FontWeight.Bold else FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Developer
                Text(
                    text = "Developed by ${aboutData!!.developer.text}",
                    fontSize = 20.sp,
                    fontWeight = if (aboutData!!.developer.bold) FontWeight.Bold else FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Team Heading
                Text(
                    text = aboutData!!.team_heading.text,
                    fontSize = 18.sp,
                    fontWeight = if (aboutData!!.team_heading.bold) FontWeight.Bold else FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Team Members
                aboutData!!.team_members.forEach { member ->
                    Text(
                        text = member.text,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = if (member.bold) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Join Heading
                Text(
                    text = aboutData!!.join_heading.text,
                    fontSize = 18.sp,
                    fontWeight = if (aboutData!!.join_heading.bold) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Join Description
                Text(
                    text = aboutData!!.join_description.text,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = if (aboutData!!.join_description.bold) FontWeight.Bold else FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(16.dp))

                // LinkedIn
                Text(
                    text = aboutData!!.linkedin_text.text,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp,
                    fontWeight = if (aboutData!!.linkedin_text.bold) FontWeight.Bold else FontWeight.Normal,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clickable { uriHandler.openUri(aboutData!!.linkedin_url) }
                )

            } else {
                Text(
                    text = "Loading content...",
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
