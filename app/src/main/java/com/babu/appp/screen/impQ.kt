package com.babu.appp.screen

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImpQScreen() {
    val coroutineScope = rememberCoroutineScope()

    var courses by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var branches by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var semesters by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var subjects by remember { mutableStateOf<Map<String, Map<String, String>>>(emptyMap()) }

    var selectedCourse by remember { mutableStateOf("") }
    var selectedBranch by remember { mutableStateOf("") }
    var selectedSemester by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf<String?>(null) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val appBarColor = if (isDark) Color(0xFF1C1C1C) else Color(0xFFE5D1B5)
    val appBarTextColor = if (isDark) Color.White else Color.Black

    // Load course list
    LaunchedEffect(Unit) {
        try {
            val json = fetchImpQJson("https://raw.githubusercontent.com/adityarrsdce/babubhaiya/refs/heads/main/Notes/notes_course_list.josn")
            val wrapper = Gson().fromJson(json, ImpQCourseWrapper::class.java)
            courses = wrapper.courses
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Important Questions",
                        color = appBarTextColor,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = appBarColor)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            ImpQDropdown("Select Course", courses.keys.toList(), selectedCourse) { course ->
                selectedCourse = course
                selectedBranch = ""
                selectedSemester = ""
                selectedSubject = null
                branches = emptyMap()
                semesters = emptyMap()
                subjects = emptyMap()

                coroutineScope.launch {
                    try {
                        val branchJson = fetchImpQJson(courses[course]!!)
                        val wrapper = Gson().fromJson(branchJson, ImpQBranchWrapper::class.java)
                        branches = wrapper.branches
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (branches.isNotEmpty()) {
                ImpQDropdown("Select Branch", branches.keys.toList(), selectedBranch) { branch ->
                    selectedBranch = branch
                    selectedSemester = ""
                    selectedSubject = null
                    semesters = emptyMap()
                    subjects = emptyMap()

                    coroutineScope.launch {
                        try {
                            val semJson = fetchImpQJson(branches[branch]!!)
                            val result = Gson().fromJson(semJson, ImpQSemesterWrapper::class.java)
                            semesters = result.semesters
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (semesters.isNotEmpty()) {
                ImpQDropdown("Select Semester", semesters.keys.toList(), selectedSemester) { sem ->
                    selectedSemester = sem
                    selectedSubject = null
                    subjects = emptyMap()

                    coroutineScope.launch {
                        try {
                            val subjectJson = fetchImpQJson(semesters[sem]!!)
                            val type = object : TypeToken<Map<String, Map<String, String>>>() {}.type
                            subjects = Gson().fromJson(subjectJson, type)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (subjects.isNotEmpty()) {
                ImpQDropdown("Select Subject", subjects.keys.toList(), selectedSubject ?: "") { subj ->
                    selectedSubject = subj
                }
            }

            Spacer(Modifier.height(16.dp))

            selectedSubject?.let { subjectName ->
                val detail = subjects[subjectName]
                detail?.let {
                    Text("📘 $subjectName", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    when (it["type"]) {
                        "pdf" -> ImpQWebViewPDF(it["url"] ?: "")
                        "text" -> LazyColumn(modifier = Modifier.padding(8.dp)) {
                            item { Text(it["text"] ?: "No content available.") }
                        }
                        else -> Text("❗ Type missing or invalid")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ✅ Banner Ad
            impBannerAd(adUnitId = "ca-app-pub-4302526630220985/1663343480")
        }
    }
}

@Composable
fun ImpQDropdown(label: String, items: List<String>, selectedItem: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val textColor = if (isDark) Color.White else Color.Black
    val menuBgColor = if (isDark) Color(0xFF2B2B2B) else Color.White

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = if (selectedItem.isEmpty()) label else selectedItem, color = textColor)
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = textColor)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth()
                .background(menuBgColor)
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item, color = textColor) },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    },
                    modifier = Modifier.background(menuBgColor)
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ImpQWebViewPDF(url: String) {
    AndroidView(factory = {
        WebView(it).apply {
            settings.javaScriptEnabled = true
            webViewClient = WebViewClient()
            loadUrl("https://drive.google.com/viewerng/viewer?embedded=true&url=$url")
        }
    }, modifier = Modifier
        .fillMaxWidth()
        .height(500.dp))
}

@Composable
fun impBannerAd(adUnitId: String) {
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

suspend fun fetchImpQJson(url: String): String = withContext(Dispatchers.IO) {
    URL(url).readText()
}

// Data Classes
data class ImpQCourseWrapper(val courses: Map<String, String>)
data class ImpQBranchWrapper(val branches: Map<String, String>)
data class ImpQSemesterWrapper(val semesters: Map<String, String>)
