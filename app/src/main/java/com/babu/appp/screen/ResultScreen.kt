package com.babu.appp.screen

import com.babu.appp.R
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(navController: NavHostController) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bgImage = if (isDark) R.drawable.pyq_dark else R.drawable.pyq_light

    val inputBg = if (isDark) Color.Black else Color.White
    val inputText = if (isDark) Color.White else Color.Black

    var regNo by rememberSaveable { mutableStateOf("") }
    var selectedSemester by rememberSaveable { mutableStateOf("") }
    var resultUrl by remember { mutableStateOf<String?>(null) }

    var semesterLinks by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(false) }
    var isRotating by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        if (isRotating) 360f else 0f,
        tween(600),
        label = ""
    )

    val jsonUrl =
        "https://raw.githubusercontent.com/adityarrsdce/babubhaiya/main/Resutl/result.json"

    LaunchedEffect(Unit) {
        semesterLinks = fetchSemesterLinks(jsonUrl) ?: emptyMap()
    }

    val semesters = semesterLinks.keys.sorted()

    fun buildResultUrl() {
        try {
            val json = JSONObject(semesterLinks[selectedSemester] ?: return)
            val name = Uri.encode(json.getString("name"))
            val session = json.getString("session")
            val examHeld = Uri.encode(json.getString("exam_held"))
            val roman = convertSemesterToRoman(selectedSemester)

            resultUrl =
                "https://beu-bih.ac.in/result-three?" +
                        "name=$name&semester=$roman&session=$session&regNo=$regNo&exam_held=$examHeld"

        } catch (_: Exception) {
            Toast.makeText(context, "Result not available", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Result Viewer") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) Color(0xFF1F1F1F) else Color(0xFFE5D1B5),
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = {
                        if (resultUrl != null) {
                            isRotating = true
                            isLoading = true
                            buildResultUrl()
                            scope.launch {
                                delay(600)
                                isRotating = false
                            }
                        }
                    }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.rotate(rotation)
                        )
                    }
                }
            )
        }
    ) { pad ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
        ) {

            Image(
                painter = painterResource(bgImage),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ---------------- REG NO (FIXED BG) ----------------
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = regNo,
                        onValueChange = { regNo = it },
                        label = { Text("Reg. No") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg,
                            disabledContainerColor = inputBg,
                            focusedTextColor = inputText,
                            unfocusedTextColor = inputText,
                            focusedLabelColor = inputText,
                            unfocusedLabelColor = inputText,
                            cursorColor = inputText
                        )
                    )

                    Column {
                        IconButton(onClick = {
                            regNo = (regNo.toLongOrNull()?.plus(1)).toString()
                        }) {
                            Icon(Icons.Default.KeyboardArrowUp, null)
                        }
                        IconButton(onClick = {
                            regNo = (regNo.toLongOrNull()?.minus(1)).toString()
                        }) {
                            Icon(Icons.Default.KeyboardArrowDown, null)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ---------------- SEMESTER (FIXED BG) ----------------
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(expanded, { expanded = !expanded }) {
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedSemester.ifBlank { "Select Semester" },
                        onValueChange = {},
                        label = { Text("Semester") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg,
                            disabledContainerColor = inputBg,
                            focusedTextColor = inputText,
                            unfocusedTextColor = inputText,
                            focusedLabelColor = inputText,
                            unfocusedLabelColor = inputText
                        )
                    )

                    ExposedDropdownMenu(expanded, { expanded = false }) {
                        semesters.forEach {
                            DropdownMenuItem(
                                text = { Text("Semester $it") },
                                onClick = {
                                    selectedSemester = it
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            if (regNo.isBlank() || selectedSemester.isBlank()) {
                                Toast.makeText(context, "Fill all fields", Toast.LENGTH_SHORT).show()
                            } else {
                                isLoading = true
                                buildResultUrl()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Check Result") }

                    Button(
                        onClick = { navController.navigate("bulk_result") },
                        modifier = Modifier.weight(1f)
                    ) { Text("Check Bulk Result") }
                }

                Spacer(Modifier.height(12.dp))

                if (isLoading) CircularProgressIndicator()

                resultUrl?.let { url ->
                    Spacer(Modifier.height(12.dp))
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                    }
                                }
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                loadUrl(url)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(500.dp)
                    )
                }
            }
        }
    }
}

// ---------------- HELPERS ----------------

suspend fun fetchSemesterLinks(jsonUrl: String): Map<String, String>? =
    withContext(Dispatchers.IO) {
        try {
            val json = JSONObject(URL(jsonUrl).readText())
            val map = mutableMapOf<String, String>()
            json.keys().forEach {
                map[it] = json.getJSONObject(it).toString()
            }
            map
        } catch (_: Exception) {
            null
        }
    }

fun convertSemesterToRoman(sem: String) = mapOf(
    "1" to "I", "2" to "II", "3" to "III", "4" to "IV",
    "5" to "V", "6" to "VI", "7" to "VII", "8" to "VIII"
)[sem] ?: sem
