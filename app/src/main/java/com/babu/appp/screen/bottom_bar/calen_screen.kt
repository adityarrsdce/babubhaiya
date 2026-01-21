package com.babu.appp.screen

import com.babu.appp.R
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.babu.appp.json.fetchJsonFromUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ------------------- Serializable Classes -------------------
@Serializable
data class EventSemesterData(
    val semesters: Map<String, String> // semester -> file URL
)

// ------------------- Helper Functions -------------------
fun String.isPdf(): Boolean = lowercase().endsWith(".pdf")
fun String.isImage(): Boolean =
    lowercase().endsWith(".jpg") || endsWith(".jpeg") || endsWith(".png") || endsWith(".webp")

// ------------------- Main Screen -------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen() {

    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val backgroundPainter =
        if (isDark) painterResource(id = R.drawable.pyq_dark)
        else painterResource(id = R.drawable.pyq_light)

    val appBarColor = if (isDark) Color(0xFF1C1C1C) else Color(0xFFE5D1B5)
    val appBarTextColor = if (isDark) Color.White else Color.Black
    val dropdownBgColor = if (isDark) Color(0xFF2C2C2C) else Color.White
    val dropdownTextColor = if (isDark) Color.White else Color.Black

    // ------------------- State -------------------
    var eventSemesterData by remember { mutableStateOf<EventSemesterData?>(null) }
    var selectedSemester by rememberSaveable { mutableStateOf("") }
    var selectedFileUrl by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var fetchError by remember { mutableStateOf<String?>(null) }

    val jsonUrl =
        "https://raw.githubusercontent.com/adityarrsdce/babubhaiya/refs/heads/main/academic_calender/academic_calendar.json"

    // ------------------- Fetch Semester Data -------------------
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            try {
                val json = withContext(Dispatchers.IO) { fetchJsonFromUrl(jsonUrl) }
                eventSemesterData = Json.decodeFromString(json.toString())
                fetchError = null
            } catch (e: Exception) {
                fetchError = "Data not available"
            }
            isLoading = false
        }
    }

    val semesterOptions = eventSemesterData?.semesters?.keys?.toList() ?: emptyList()

    // ------------------- Scaffold -------------------
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Academic Calendar",
                        fontSize = 20.sp,
                        color = appBarTextColor
                    )
                },
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
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ------------------- Semester Dropdown -------------------
                EventDropdown(
                    label = "Select Semester",
                    options = semesterOptions,
                    selected = selectedSemester,
                    backgroundColor = dropdownBgColor,
                    textColor = dropdownTextColor
                ) { semester ->
                    selectedSemester = semester
                    selectedFileUrl = eventSemesterData?.semesters?.get(semester)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ------------------- Show File -------------------
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> CircularProgressIndicator()
                        fetchError != null -> Text(fetchError!!, color = dropdownTextColor)
                        selectedFileUrl != null -> {
                            when {
                                selectedFileUrl!!.isPdf() ->
                                    PDFViewerWebView(selectedFileUrl!!)
                                selectedFileUrl!!.isImage() ->
                                    ZoomableImage(selectedFileUrl!!)
                                else ->
                                    Text("Unsupported file type", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------- Dropdown -------------------
@Composable
fun EventDropdown(
    label: String,
    options: List<String>,
    selected: String,
    backgroundColor: Color,
    textColor: Color,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = backgroundColor)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (selected.isEmpty()) label else selected, color = textColor)
                Icon(Icons.Default.ArrowDropDown, null, tint = textColor)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(backgroundColor)
        ) {
            options.forEach {
                DropdownMenuItem(
                    text = { Text(it, color = textColor) },
                    onClick = {
                        onSelected(it)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ------------------- PDF Viewer -------------------
@Composable
fun PDFViewerWebView(url: String) {
    val viewerUrl = "https://docs.google.com/gview?embedded=true&url=$url"
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(500.dp),
        factory = { context ->
            WebView(context).apply {
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

// ------------------- Zoomable Image -------------------
@Composable
fun ZoomableImage(url: String) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
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
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        )
    }
}
