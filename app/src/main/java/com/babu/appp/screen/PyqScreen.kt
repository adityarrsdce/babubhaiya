package com.babu.appp.screen

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.ui.viewinterop.AndroidView
import com.babu.appp.R
import com.babu.appp.loading.LoadingAnimation
import com.google.android.gms.ads.MobileAds
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

// ---------------- DATA ----------------

data class BranchList(val branches: Map<String, String>)
data class SemesterList(val semesters: Map<String, String>)
typealias SubjectYearMap = Map<String, Map<String, String>>

// ---------------- SCREEN ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PyqScreen() {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var branches by remember { mutableStateOf<List<String>>(emptyList()) }
    var semesters by remember { mutableStateOf<List<String>>(emptyList()) }
    var subjects by remember { mutableStateOf<List<String>>(emptyList()) }
    var years by remember { mutableStateOf<List<String>>(emptyList()) }

    var selectedBranch by remember { mutableStateOf("") }
    var selectedSemester by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("") }
    var selectedYear by remember { mutableStateOf("") }

    var branchMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var semesterMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var subjectYearMap by remember { mutableStateOf<SubjectYearMap>(emptyMap()) }

    var showPdfViewer by remember { mutableStateOf(false) }
    var pdfUrlToView by remember { mutableStateOf("") }
    var pdfLoading by remember { mutableStateOf(true) }

    BackHandler(enabled = showPdfViewer) { showPdfViewer = false }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBgColor =
        if (isDark) Color(0xFF2C2C2C).copy(alpha = 0.95f)
        else Color.White.copy(alpha = 0.96f)

    val textColor = if (isDark) Color.White else Color.Black
    val appBarColor = if (isDark) Color(0xFF1C1C1C) else Color(0xFFE5D1B5)
    val appBarTextColor = if (isDark) Color.White else Color.Black

    // ---------- LOAD BRANCH ----------

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
                Toast.makeText(context, "Data uploading soon", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ---------- PDF VIEWER ----------

    if (showPdfViewer && pdfUrlToView.isNotEmpty()) {

        val viewerUrl =
            "https://docs.google.com/gview?embedded=true&url=${Uri.encode(pdfUrlToView)}"

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

            Box(Modifier.fillMaxSize()) {

                AndroidView(
                    factory = { ctx ->
                        android.webkit.WebView(ctx).apply {

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                    pdfLoading = false
                                }
                            }

                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false

                            isVerticalScrollBarEnabled = true
                            isHorizontalScrollBarEnabled = true

                            loadUrl(viewerUrl)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )

                if (pdfLoading) {
                    LoadingAnimation()
                }
            }
        }

    } else {

        // ---------- MAIN UI ----------

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
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text("Download PYQ",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        DropdownSelector("Select Branch", branches, selectedBranch) {
                            val url = branchMap[it] ?: return@DropdownSelector
                            selectedBranch = it
                            semesters = emptyList()
                            subjects = emptyList()
                            years = emptyList()

                            coroutineScope.launch {
                                val json = fetchJsonFromUrl(url)
                                val data = Gson().fromJson(json, SemesterList::class.java)
                                semesterMap = data.semesters
                                semesters = data.semesters.keys.toList()
                            }
                        }

                        if (semesters.isNotEmpty())
                            DropdownSelector("Select Semester", semesters, selectedSemester) {
                                val url = semesterMap[it] ?: return@DropdownSelector
                                selectedSemester = it
                                subjects = emptyList()
                                years = emptyList()

                                coroutineScope.launch {
                                    val json = fetchJsonFromUrl(url)
                                    subjectYearMap =
                                        Gson().fromJson(json, SubjectYearMap::class.java)
                                    subjects = subjectYearMap.keys.toList()
                                }
                            }

                        if (subjects.isNotEmpty())
                            DropdownSelector("Select Subject", subjects, selectedSubject) {
                                selectedSubject = it
                                years = subjectYearMap[it]?.keys?.toList() ?: emptyList()
                            }

                        if (years.isNotEmpty())
                            DropdownSelector("Select Year", years, selectedYear) {
                                selectedYear = it
                            }

                        val pdfUrl = subjectYearMap[selectedSubject]?.get(selectedYear) ?: ""

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Button(
                                onClick = {
                                    if (pdfUrl.isEmpty()) {
                                        Toast.makeText(context, "Data uploading soon", Toast.LENGTH_SHORT).show()
                                    } else {
                                        pdfUrlToView = pdfUrl
                                        showPdfViewer = true
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("View")
                            }

                            Button(
                                onClick = {
                                    if (pdfUrl.isEmpty()) {
                                        Toast.makeText(context, "Data uploading soon", Toast.LENGTH_SHORT).show()
                                    } else {
                                        downloadPdf(context, pdfUrl)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Download")
                            }
                        }

                    }
                    }
                }
            }
        }
    }

// ---------------- DROPDOWN ----------------

@Composable
fun DropdownSelector(
    label: String,
    options: List<String>,
    selectedOption: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {

        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(0.95f)
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

// ---------------- NETWORK ----------------

suspend fun fetchJsonFromUrl(url: String): String =
    withContext(Dispatchers.IO) { URL(url).readText() }

// ---------------- DOWNLOAD ----------------

fun downloadPdf(context: Context, url: String) {
    if (url.isEmpty()) return

    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle("PYQ Download")
        .setNotificationVisibility(
            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
        )
        .setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS,
            "PYQ_${System.currentTimeMillis()}.pdf"
        )

    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    dm.enqueue(request)

    Toast.makeText(context, "Downloading...", Toast.LENGTH_SHORT).show()
}
