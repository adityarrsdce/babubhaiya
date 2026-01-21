package com.babu.appp.screen

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babu.appp.R
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState

// ---------------------------- DATA CLASSES ----------------------------
data class BranchList(val branches: Map<String, String>)
data class SemesterList(val semesters: Map<String, String>)
typealias SubjectYearMap = Map<String, Map<String, String>>

// ---------------------------- MAIN SCREEN ----------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PyqScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Dropdown states
    var branches by remember { mutableStateOf<List<String>>(emptyList()) }
    var semesters by remember { mutableStateOf<List<String>>(emptyList()) }
    var subjects by remember { mutableStateOf<List<String>>(emptyList()) }
    var years by remember { mutableStateOf<List<String>>(emptyList()) }

    var selectedBranch by remember { mutableStateOf("") }
    var selectedSemester by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("") }
    var selectedYear by remember { mutableStateOf("") }

    // Data maps
    var branchMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var semesterMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var subjectYearMap by remember { mutableStateOf<SubjectYearMap>(emptyMap()) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBgColor =
        if (isDark) Color(0xFF2C2C2C).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.96f)
    val textColor = if (isDark) Color.White else Color.Black
    val appBarColor = if (isDark) Color(0xFF1C1C1C) else Color(0xFFE5D1B5)
    val appBarTextColor = if (isDark) Color.White else Color.Black

    var showPdfViewer by remember { mutableStateOf(false) }
    var pdfUrlToView by remember { mutableStateOf("") }

    BackHandler(enabled = showPdfViewer) {
        showPdfViewer = false
    }

    // -------------------- LOAD BRANCH LIST DIRECTLY --------------------
    LaunchedEffect(Unit) {
        MobileAds.initialize(context)

        coroutineScope.launch {
            try {
                val json = fetchJsonFromUrl(
                    "https://raw.githubusercontent.com/adityarrsdce/babubhaiya/refs/heads/main/PYQ/Btech/Branch_list.json"
                )
                val data = Gson().fromJson(json, BranchList::class.java)
                branchMap = data.branches
                branches = data.branches.keys.toList()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // -------------------- PDF VIEWER --------------------
    if (showPdfViewer && pdfUrlToView.isNotEmpty()) {
        val viewerUrl =
            "https://docs.google.com/gview?embedded=true&url=${Uri.encode(pdfUrlToView)}"
        val webViewState = rememberWebViewState(viewerUrl)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("") },
                    navigationIcon = {
                        IconButton(onClick = { showPdfViewer = false }) {
                            Icon(
                                painterResource(id = R.drawable.arrow),
                                contentDescription = "Back",
                                tint = appBarTextColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = appBarColor)
                )
            }
        ) { padding ->
            WebView(
                state = webViewState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    } else {

        // -------------------- MAIN UI --------------------
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Previous Year Questions", color = appBarTextColor) },
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
                    painter = painterResource(
                        if (isDark) R.drawable.pyq_dark else R.drawable.pyq_light
                    ),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.Center),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Download PYQ", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor)

                        // -------------------- BRANCH --------------------
                        DropdownSelector("Select Branch", branches, selectedBranch) { branch ->
                            selectedBranch = branch
                            selectedSemester = ""
                            selectedSubject = ""
                            selectedYear = ""
                            semesters = emptyList()
                            subjects = emptyList()
                            years = emptyList()

                            coroutineScope.launch {
                                val json = fetchJsonFromUrl(branchMap[branch]!!)
                                val data = Gson().fromJson(json, SemesterList::class.java)
                                semesterMap = data.semesters
                                semesters = data.semesters.keys.toList()
                            }
                        }

                        if (semesters.isNotEmpty())
                            DropdownSelector("Select Semester", semesters, selectedSemester) { sem ->
                                selectedSemester = sem
                                selectedSubject = ""
                                selectedYear = ""
                                subjects = emptyList()
                                years = emptyList()

                                coroutineScope.launch {
                                    val json = fetchJsonFromUrl(semesterMap[sem]!!)
                                    subjectYearMap =
                                        Gson().fromJson(json, SubjectYearMap::class.java)
                                    subjects = subjectYearMap.keys.toList()
                                }
                            }

                        if (subjects.isNotEmpty())
                            DropdownSelector("Select Subject", subjects, selectedSubject) { sub ->
                                selectedSubject = sub
                                years = subjectYearMap[sub]?.keys?.toList() ?: emptyList()
                            }

                        if (years.isNotEmpty())
                            DropdownSelector("Select Year", years, selectedYear) {
                                selectedYear = it
                            }

                        val pdfUrl = subjectYearMap[selectedSubject]?.get(selectedYear) ?: ""

                        Button(
                            onClick = {
                                if (pdfUrl.isEmpty()) {
                                    Toast.makeText(context, "File not available", Toast.LENGTH_SHORT).show()
                                } else {
                                    pdfUrlToView = pdfUrl
                                    showPdfViewer = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View")
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------- DROPDOWN ----------------------------
@Composable
fun DropdownSelector(
    label: String,
    options: List<String>,
    selectedOption: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (selectedOption.isEmpty()) label else selectedOption)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, null)
        }

        DropdownMenu(expanded, { expanded = false }) {
            options.forEach {
                DropdownMenuItem(
                    text = { Text(it) },
                    onClick = {
                        onSelected(it)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ---------------------------- NETWORK ----------------------------
suspend fun fetchJsonFromUrl(url: String): String =
    withContext(Dispatchers.IO) { URL(url).readText() }
