package com.babu.appp.screen

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/* ===================== CACHE ===================== */

private const val PREF_SYLLABUS = "syllabus_cache"

/* ===================== DATA ===================== */

data class BranchWrapper(val branches: Map<String, String>)
data class SemesterWrapper(val semesters: Map<String, String>)
typealias SubjectMap = Map<String, Map<String, String>>

/* ===================== SCREEN ===================== */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyllabusScreen() {

    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREF_SYLLABUS, Context.MODE_PRIVATE)

    var branches by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var semesters by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var subjects by remember { mutableStateOf<SubjectMap>(emptyMap()) }

    var selectedBranch by remember { mutableStateOf("") }
    var selectedSemester by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf<String?>(null) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val appBarColor = if (isDark) Color(0xFF1C1C1C) else Color(0xFFE5D1B5)
    val appBarTextColor = if (isDark) Color.White else Color.Black

    /* ---------------- LOAD BRANCHES ---------------- */

    LaunchedEffect(Unit) {
        val json = smartFetch(
            context,
            "branches",
            "https://raw.githubusercontent.com/adityarrsdce/babubhaiya/refs/heads/main/syllabus/btech/branch_list.json"
        )
        branches = Gson().fromJson(json, BranchWrapper::class.java).branches
    }

    /* ---------------- LOAD SEMESTERS ---------------- */

    LaunchedEffect(selectedBranch) {
        if (selectedBranch.isNotEmpty()) {
            branches[selectedBranch]?.let { url ->
                val json = smartFetch(context, selectedBranch, url)
                semesters = Gson().fromJson(json, SemesterWrapper::class.java).semesters
            }
        }
    }

    /* ---------------- LOAD SUBJECTS ---------------- */

    LaunchedEffect(selectedSemester) {
        if (selectedSemester.isNotEmpty()) {
            semesters[selectedSemester]?.let { url ->
                val json = smartFetch(context, selectedSemester, url)
                val type = object : TypeToken<SubjectMap>() {}.type
                subjects = Gson().fromJson(json, type)
            }
        }
    }

    /* ---------------- UI ---------------- */

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Syllabus", color = appBarTextColor, fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = appBarColor)
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Dropdown("Select Branch", branches.keys.toList(), selectedBranch) {
                selectedBranch = it
                selectedSemester = ""
                selectedSubject = null
                semesters = emptyMap()
                subjects = emptyMap()
            }

            Spacer(Modifier.height(8.dp))

            if (semesters.isNotEmpty()) {
                Dropdown("Select Semester", semesters.keys.toList(), selectedSemester) {
                    selectedSemester = it
                    selectedSubject = null
                    subjects = emptyMap()
                }
            }

            Spacer(Modifier.height(8.dp))

            if (subjects.isNotEmpty()) {
                Dropdown("Select Subject", subjects.keys.toList(), selectedSubject ?: "") {
                    selectedSubject = it
                }
            }

            Spacer(Modifier.height(16.dp))

            selectedSubject?.let { subject ->
                val data = subjects[subject] ?: return@let
                val text = data["text"] ?: ""

                LazyColumn {
                    item {
                        Text(
                            text = subject,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Spacer(Modifier.height(8.dp))

                        SelectionContainer {
                            Text(
                                text = parseBoldText(text),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            BannerAd("ca-app-pub-4302526630220985/1663343480")
        }
    }
}

/* ===================== SMART FETCH ===================== */

suspend fun smartFetch(context: Context, key: String, url: String): String {
    val prefs = context.getSharedPreferences(PREF_SYLLABUS, Context.MODE_PRIVATE)
    val cached = prefs.getString(key, null)

    return if (isInternetAvailable(context)) {
        try {
            val fresh = fetchJson(url)
            prefs.edit().putString(key, fresh).apply()
            fresh
        } catch (e: Exception) {
            cached ?: ""
        }
    } else {
        cached ?: ""
    }
}

fun isInternetAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/* ===================== HELPERS ===================== */

suspend fun fetchJson(url: String): String =
    withContext(Dispatchers.IO) { URL(url).readText() }

fun parseBoldText(input: String): AnnotatedString =
    buildAnnotatedString {
        var i = 0
        while (i < input.length) {
            if (i + 1 < input.length && input[i] == '*' && input[i + 1] == '*') {
                i += 2
                val start = i
                while (i + 1 < input.length && !(input[i] == '*' && input[i + 1] == '*')) i++
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(input.substring(start, i))
                pop()
                i += 2
            } else append(input[i++])
        }
    }

/* ===================== DROPDOWN ===================== */

@Composable
fun Dropdown(
    label: String,
    items: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(0.95f),
            onClick = { expanded = true }
        ) {
            Text(if (selected.isEmpty()) label else selected)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, null)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            items.forEach {
                DropdownMenuItem(
                    text = { Text(it) },
                    onClick = {
                        onSelect(it)
                        expanded = false
                    }
                )
            }
        }
    }
}

/* ===================== BANNER ===================== */

@Composable
fun BannerAd(adUnitId: String) {
    AndroidView(
        factory = { ctx ->
            AdView(ctx).apply {
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
