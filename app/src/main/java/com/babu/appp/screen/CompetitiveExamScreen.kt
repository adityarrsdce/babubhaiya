package com.babu.appp.screen

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.babu.appp.json.fetchJsonFromUrl
import com.babu.appp.R
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.FullScreenContentCallback
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ExamPdf(val title: String, val fileUrl: String, val uploadedAt: String)

@Serializable
data class ExamList(val exams: Map<String, String>)

fun downloadCompetitivePdf(context: Context, fileUrl: String, fileName: String) {
    try {
        val request = DownloadManager.Request(Uri.parse(fileUrl)).apply {
            setTitle(fileName)
            setDescription("Downloading PDF...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

        Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompetitiveExamScreenUI(navController: NavHostController) {
    val context = LocalContext.current
    val activity = context as? Activity
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val background: Painter = if (isDark) painterResource(R.drawable.pyq_dark) else painterResource(R.drawable.pyq_light)
    val commonThumbnail = painterResource(R.drawable.pdf_icon)

    val appBarColor = if (isDark) Color(0xFF1C1C1C) else Color(0xFFE5D1B5)
    val appBarTextColor = if (isDark) Color.White else Color.Black
    val titleTextColor = if (isDark) Color.White else Color.Black

    var examMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selectedExam by remember { mutableStateOf<String?>(null) }
    var pdfList by remember { mutableStateOf(listOf<ExamPdf>()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    var mInterstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }
    val adUnitId = "ca-app-pub-4302526630220985/2830135242"
    val bannerAdUnitId = "ca-app-pub-4302526630220985/1663343480"

    fun loadAd(callback: (() -> Unit)? = null) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                mInterstitialAd = ad
                callback?.invoke()
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                mInterstitialAd = null
                callback?.invoke()
            }
        })
    }

    LaunchedEffect(Unit) {
        val testDeviceIds = listOf("631C9C6AE7F9C35179C286BE45471BC1")
        val configuration = RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
        MobileAds.setRequestConfiguration(configuration)

        // ✅ Mute all AdMob ads (including video interstitials)
        MobileAds.setAppMuted(true)

        MobileAds.initialize(context)
        loadAd()

        isLoading = true
        try {
            val json = withContext(Dispatchers.IO) {
                fetchJsonFromUrl("https://raw.githubusercontent.com/adityarrsdce/babubhaiya/main/CompetitiveExam/Master_JSON.json")
            }
            val parsed = Json.decodeFromString<ExamList>(json)
            examMap = parsed.exams
        } catch (e: Exception) {
            errorMessage = "Failed to load exam list"
        } finally {
            isLoading = false
        }
    }

    fun loadExamJsonSafe(url: String) {
        isLoading = true
        errorMessage = null
        pdfList = emptyList()

        coroutineScope.launch {
            try {
                val json = withContext(Dispatchers.IO) { fetchJsonFromUrl(url) }
                pdfList = Json.decodeFromString(json)
            } catch (_: Exception) {
                errorMessage = "Failed to load exam data"
            } finally {
                isLoading = false
            }
        }
    }

    val filteredList = pdfList.sortedByDescending { it.uploadedAt }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Competitive Exams", color = appBarTextColor) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = appBarColor)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Image(
                painter = background,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.padding(16.dp)) {
                if (examMap.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = selectedExam ?: "Select Exam",
                            onValueChange = {},
                            label = { Text("Select Exam") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = if (isDark) Color.Black else Color.White,
                                unfocusedContainerColor = if (isDark) Color.Black else Color.White,
                                focusedTextColor = if (isDark) Color.White else Color.Black,
                                unfocusedTextColor = if (isDark) Color.White else Color.Black,
                                focusedLabelColor = if (isDark) Color.White else Color.Black,
                                unfocusedLabelColor = if (isDark) Color.White else Color.Black
                            )
                        )


                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            examMap.forEach { (examName, url) ->
                                DropdownMenuItem(
                                    text = { Text(examName) },
                                    onClick = {
                                        selectedExam = examName
                                        expanded = false
                                        loadExamJsonSafe(url)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                when {
                    isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                    errorMessage != null -> Text(errorMessage ?: "", color = Color.Red)

                    filteredList.isNotEmpty() -> {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            filteredList.forEach { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    border = BorderStroke(1.dp, Color.Black)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            painter = commonThumbnail,
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                        Text(
                                            text = item.title,
                                            color = titleTextColor,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 12.dp)
                                        )

                                        IconButton(onClick = {
                                            val fileName = item.title.replace(" ", "_") + ".pdf"
                                            val startDownload = {
                                                downloadCompetitivePdf(context, item.fileUrl, fileName)
                                                loadAd() // Preload next ad after current one
                                            }

                                            if (mInterstitialAd != null && activity != null) {
                                                mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                                                    override fun onAdDismissedFullScreenContent() {
                                                        startDownload()
                                                    }

                                                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                                        startDownload()
                                                    }
                                                }
                                                mInterstitialAd?.show(activity)
                                            } else {
                                                startDownload()
                                            }
                                        }) {
                                            Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.Red)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                ComBannerAd(adUnitId = bannerAdUnitId)
            }
        }
    }
}

@Composable
fun ComBannerAd(adUnitId: String) {
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
