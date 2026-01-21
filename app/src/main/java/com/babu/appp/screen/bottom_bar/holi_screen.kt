package com.babu.appp.screen

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.babu.appp.R
import com.babu.appp.json.fetchJsonFromUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/* ---------------- CACHE CONFIG ---------------- */

private const val HOLIDAY_PREF = "holiday_cache"
private const val HOLIDAY_JSON_KEY = "holiday_json"
private const val HOLIDAY_TIME_KEY = "holiday_time"
private const val FIRST_LAUNCH_KEY = "first_launch_done"
private const val CACHE_VALIDITY = 24 * 60 * 60 * 1000L // 24 hours

/* ---------------- HELPERS ---------------- */

fun String.holiPdf() = lowercase().endsWith(".pdf")
fun String.holiImage() =
    lowercase().endsWith(".jpg") ||
            lowercase().endsWith(".jpeg") ||
            lowercase().endsWith(".png") ||
            lowercase().endsWith(".webp")

/* ---------------- MAIN SCREEN ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidayScreen() {

    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val backgroundPainter =
        if (isDark) painterResource(R.drawable.pyq_dark)
        else painterResource(R.drawable.pyq_light)

    val appBarColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFE5D1B5)
    val appBarTextColor = if (isDark) Color.White else Color.Black

    var fileUrl by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val holidayJsonUrl =
        "https://raw.githubusercontent.com/adityarrsdce/babubhaiya/refs/heads/main/Holiday/holiday.json"

    /* ---------------- LOAD + CACHE ---------------- */

    LaunchedEffect(Unit) {

        val prefs = context.getSharedPreferences(HOLIDAY_PREF, Context.MODE_PRIVATE)

        val isFirstLaunch = !prefs.getBoolean(FIRST_LAUNCH_KEY, false)
        val cachedJson = prefs.getString(HOLIDAY_JSON_KEY, null)
        val cachedTime = prefs.getLong(HOLIDAY_TIME_KEY, 0L)
        val now = System.currentTimeMillis()

        try {
            val jsonString = when {
                isFirstLaunch -> {
                    val fresh = withContext(Dispatchers.IO) {
                        fetchJsonFromUrl(holidayJsonUrl)
                    }
                    prefs.edit()
                        .putString(HOLIDAY_JSON_KEY, fresh)
                        .putLong(HOLIDAY_TIME_KEY, now)
                        .putBoolean(FIRST_LAUNCH_KEY, true)
                        .apply()
                    fresh
                }

                cachedJson != null && now - cachedTime < CACHE_VALIDITY -> cachedJson

                else -> {
                    val fresh = withContext(Dispatchers.IO) {
                        fetchJsonFromUrl(holidayJsonUrl)
                    }
                    prefs.edit()
                        .putString(HOLIDAY_JSON_KEY, fresh)
                        .putLong(HOLIDAY_TIME_KEY, now)
                        .apply()
                    fresh
                }
            }

            val map: Map<String, String> = Json.decodeFromString(jsonString)
            fileUrl = map.values.firstOrNull()
            error = if (fileUrl == null) "No holiday data available" else null

        } catch (e: Exception) {
            if (cachedJson != null) {
                val map: Map<String, String> = Json.decodeFromString(cachedJson)
                fileUrl = map.values.firstOrNull()
            } else {
                error = "Failed to load holiday calendar"
            }
        }

        isLoading = false
    }

    /* ---------------- UI ---------------- */

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Holiday Calendar", color = appBarTextColor) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = appBarColor)
            )
        }
    ) { paddingValues ->

        Box(modifier = Modifier.fillMaxSize()) {

            Image(
                painter = backgroundPainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {

                when {
                    isLoading -> CircularProgressIndicator()

                    error != null -> Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error
                    )

                    fileUrl != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            when {
                                fileUrl!!.holiPdf() ->
                                    HolidayPdfWebView(
                                        url = fileUrl!!,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                fileUrl!!.holiImage() ->
                                    ZoomableImageViewer(fileUrl!!)

                                else ->
                                    Text(
                                        "Unsupported file type",
                                        color = MaterialTheme.colorScheme.error
                                    )
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ---------------- PDF VIEWER (FIXED UX) ---------------- */

@Composable
fun HolidayPdfWebView(
    url: String,
    modifier: Modifier = Modifier
) {
    val viewerUrl = "https://docs.google.com/gview?embedded=true&url=$url"

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 12.dp),   // 🔑 visual centering
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    settings.javaScriptEnabled = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    webViewClient = WebViewClient()
                    loadUrl(viewerUrl)
                }
            }
        )
    }
}


/* ---------------- IMAGE ZOOM ---------------- */

@Composable
fun ZoomableImageViewer(url: String) {

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        )
    }
}