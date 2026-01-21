package com.babu.appp.screen

import com.babu.appp.R

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
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
import com.babu.appp.json.fetchJsonFromUrl
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class NotesCourseList(val notesCourses: Map<String, String>)

@Serializable
data class NotesData(val topics: Map<String, Map<String, Map<String, String>>>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen() {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()

    val backgroundPainter = if (isDark) painterResource(id = R.drawable.pyq_dark)
    else painterResource(id = R.drawable.pyq_light)

    val textColor = if (isDark) Color.White else Color.Black
    val dropdownBg = if (isDark) Color(0xFF2C2C2C) else Color.White
    val dropdownTextColor = if (isDark) Color.White else Color.Black
    val appBarColor = if (isDark) Color(0xFF1C1C1C) else Color(0xFFE5D1B5)
    val appBarTextColor = if (isDark) Color.White else Color.Black

    var notesCourseList by remember { mutableStateOf<NotesCourseList?>(null) }
    var notesData by remember { mutableStateOf<NotesData?>(null) }
    var notesLoading by remember { mutableStateOf(false) }
    var notesError by remember { mutableStateOf<String?>(null) }

    var selectedCourse by remember { mutableStateOf("") }
    var selectedBranch by remember { mutableStateOf("") }
    var selectedSemester by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("") }
    var selectedCourseUrl by remember { mutableStateOf("") }

    val notesCourseListUrl = "https://raw.githubusercontent.com/adityarrsdce/babubhaiya/refs/heads/main/Notes/notes_course_list.josn"

    var mInterstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }
    val adUnitId = "ca-app-pub-4302526630220985/6516043471"

    LaunchedEffect(Unit) {
        MobileAds.initialize(context)
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                mInterstitialAd = ad
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("AdLoad", "Ad failed to load: ${adError.message}")
                mInterstitialAd = null
            }
        })

        loadNotesCourseList(notesCourseListUrl, {
            notesCourseList = it
        }, {
            notesError = it
        })
    }

    val courses = notesCourseList?.notesCourses?.keys?.toList() ?: emptyList()
    val branches = notesData?.topics?.keys?.toList() ?: emptyList()
    val semesters = notesData?.topics?.notestCaseInsensitive(selectedBranch)?.keys?.toList() ?: emptyList()
    val subjects = notesData?.topics
        ?.notestCaseInsensitive(selectedBranch)
        ?.notestCaseInsensitive(selectedSemester)
        ?.keys?.toList() ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes", color = appBarTextColor) },
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
                contentDescription = "Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            when {
                notesLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                notesError != null -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(notesError!!, color = MaterialTheme.colorScheme.error)
                    Button(onClick = {
                        coroutineScope.launch {
                            loadNotesCourseList(notesCourseListUrl, {
                                notesCourseList = it
                                notesError = null
                            }, {
                                notesError = it
                            })
                        }
                    }) {
                        Text("Retry")
                    }
                }

                else -> {
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = dropdownBg),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Download Notes", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor)
                            Text("All Courses Supported", fontSize = 14.sp, color = textColor)

                            NotesDropdown("Select Course", courses, selectedCourse, textColor, dropdownBg, dropdownTextColor) {
                                selectedCourse = it
                                selectedCourseUrl = notesCourseList?.notesCourses?.notestCaseInsensitive(it) ?: ""
                                selectedBranch = ""
                                selectedSemester = ""
                                selectedSubject = ""

                                coroutineScope.launch {
                                    notesLoading = true
                                    loadNotesData(selectedCourseUrl, {
                                        notesData = it
                                        notesError = null
                                        notesLoading = false
                                    }, {
                                        notesError = it
                                        notesLoading = false
                                    })
                                }
                            }

                            if (selectedCourse.isNotEmpty()) NotesDropdown("Select Branch", branches, selectedBranch, textColor, dropdownBg, dropdownTextColor) {
                                selectedBranch = it
                                selectedSemester = ""
                                selectedSubject = ""
                            }

                            if (selectedBranch.isNotEmpty()) NotesDropdown("Select Semester", semesters, selectedSemester, textColor, dropdownBg, dropdownTextColor) {
                                selectedSemester = it
                                selectedSubject = ""
                            }

                            if (selectedSemester.isNotEmpty()) NotesDropdown("Select Subject", subjects, selectedSubject, textColor, dropdownBg, dropdownTextColor) {
                                selectedSubject = it
                            }

                            if (selectedSubject.isNotEmpty()) {
                                val pdfUrl = notesData?.topics
                                    ?.notestCaseInsensitive(selectedBranch)
                                    ?.notestCaseInsensitive(selectedSemester)
                                    ?.notestCaseInsensitive(selectedSubject)

                                Button(
                                    onClick = {
                                        if (!pdfUrl.isNullOrEmpty()) {
                                            val fileName = "${selectedSubject.replace(" ", "_")}_Notes.pdf"
                                            val startDownload = {
                                                val request = DownloadManager.Request(Uri.parse(pdfUrl)).apply {
                                                    setTitle(fileName)
                                                    setDescription("Downloading Notes...")
                                                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                                                    setAllowedOverMetered(true)
                                                    setAllowedOverRoaming(true)
                                                }
                                                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                                downloadManager.enqueue(request)
                                                Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
                                            }

                                            if (mInterstitialAd != null && activity != null) {
                                                mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                                                    override fun onAdDismissedFullScreenContent() {
                                                        startDownload()
                                                        mInterstitialAd = null
                                                    }

                                                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                                        startDownload()
                                                    }
                                                }
                                                mInterstitialAd?.show(activity)
                                            } else {
                                                startDownload()
                                            }
                                        } else {
                                            Toast.makeText(context, "File not available", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Download Notes")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotesDropdown(
    label: String,
    options: List<String>,
    selected: String,
    textColor: Color,
    backgroundColor: Color,
    itemTextColor: Color,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (selected.isEmpty()) label else selected, color = textColor)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = textColor)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
        ) {
            options.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item, color = itemTextColor) },
                    onClick = {
                        onSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

suspend fun loadNotesCourseList(
    url: String,
    onSuccess: (NotesCourseList) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val jsonString = withContext(Dispatchers.IO) { fetchJsonFromUrl(url) }
        val data = Json { ignoreUnknownKeys = true }.decodeFromString<NotesCourseList>(jsonString)
        onSuccess(data)
    } catch (e: Exception) {
        onError("Data not available")
    }
}

suspend fun loadNotesData(
    url: String,
    onSuccess: (NotesData) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val jsonString = withContext(Dispatchers.IO) { fetchJsonFromUrl(url) }
        val data = Json { ignoreUnknownKeys = true }.decodeFromString<NotesData>(jsonString)
        onSuccess(data)
    } catch (e: Exception) {
        onError("Data not available")
    }
}

fun <V> Map<String, V>.notestCaseInsensitive(key: String): V? {
    return this.entries.find { it.key.equals(key, ignoreCase = true) }?.value
}
