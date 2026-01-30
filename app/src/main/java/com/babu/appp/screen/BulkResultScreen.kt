package com.babu.appp.screen

import com.babu.appp.R
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.babu.appp.util.convertSemesterToRoman
import kotlinx.coroutines.*
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.net.URL

// ---------------- DATA MODEL ----------------

data class StudentTextResult(
    val regNo: String,
    val name: String,
    val sgpa: String,
    val cgpa: String
)

data class SemesterConfig(
    val name: String,
    val session: String,
    val examHeld: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkResultScreen(navController: NavHostController) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundImage = if (isDark) R.drawable.pyq_dark else R.drawable.pyq_light

    var startReg by remember { mutableStateOf("") }
    var endReg by remember { mutableStateOf("") }
    var selectedSemester by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    var semesterConfigs by remember { mutableStateOf<Map<String, SemesterConfig>>(emptyMap()) }
    var bulkResults by remember { mutableStateOf<List<StudentTextResult>>(emptyList()) }

    var isLoading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }

    val semesters = listOf("1", "2", "3", "4", "5", "6", "7", "8")

    val jsonUrl =
        "https://raw.githubusercontent.com/adityarrsdce/babubhaiya/main/Resutl/result.json"

    // ---------------- LOAD SEMESTER CONFIG ----------------

    LaunchedEffect(Unit) {
        semesterConfigs = fetchSemesterConfigs(jsonUrl) ?: emptyMap()
        println("Loaded configs = $semesterConfigs")
    }

    // ---------------- BUILD BEU URL ----------------

    fun buildBeuUrl(
        config: SemesterConfig,
        romanSem: String,
        regNo: String
    ): String {

        val encodedName = URLEncoder.encode(config.name, "UTF-8")
        val encodedExamHeld = URLEncoder.encode(config.examHeld, "UTF-8")

        return "https://beu-bih.ac.in/result-three" +
                "?name=$encodedName" +
                "&semester=$romanSem" +
                "&session=${config.session}" +
                "&regNo=$regNo" +
                "&exam_held=$encodedExamHeld"
    }

    // ---------------- SINGLE FETCH ----------------

    suspend fun fetchSingleResult(
        config: SemesterConfig,
        romanSem: String,
        regNo: String
    ): StudentTextResult? = withContext(Dispatchers.IO) {

        try {
            val url = buildBeuUrl(config, romanSem, regNo)

            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .timeout(20000)
                .get()

            val rows = doc.select("table tr")

            if (rows.size < 6) return@withContext null

            val name = rows[3].select("td").getOrNull(1)?.text().orEmpty()
            val sgpa = rows.last().select("td").getOrNull(3)?.text().orEmpty()
            val cgpa = rows.last().select("td").getOrNull(4)?.text().orEmpty()

            if (name.isBlank()) return@withContext null

            StudentTextResult(
                regNo = regNo,
                name = name,
                sgpa = sgpa,
                cgpa = cgpa
            )

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ---------------- BULK FETCH ----------------

    fun buildBulkResults() {

        val start = startReg.toLongOrNull()
        val end = endReg.toLongOrNull()

        if (start == null || end == null || start > end) {
            Toast.makeText(context, "Enter valid Reg. No range", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedSemester.isBlank()) {
            Toast.makeText(context, "Select Semester first", Toast.LENGTH_SHORT).show()
            return
        }

        val total = (end - start + 1).toInt()
        if (total > 50) {
            Toast.makeText(context, "Max 50 results at once", Toast.LENGTH_LONG).show()
            return
        }

        val config = semesterConfigs[selectedSemester]
        if (config == null) {
            Toast.makeText(context, "Semester config not found in JSON", Toast.LENGTH_LONG).show()
            return
        }

        val romanSem = convertSemesterToRoman(selectedSemester)

        isLoading = true
        bulkResults = emptyList()
        progress = 0

        coroutineScope.launch {

            val list = mutableListOf<StudentTextResult>()
            var count = 0

            for (reg in start..end) {

                val result = fetchSingleResult(
                    config = config,
                    romanSem = romanSem,
                    regNo = reg.toString()
                )

                if (result != null) {
                    list.add(result)
                }

                count++
                progress = count

                delay(700)
            }

            bulkResults = list
            isLoading = false
        }
    }

    // ---------------- UI ----------------

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bulk Result") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) Color(0xFF1F1F1F) else Color(0xFFE5D1B5),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            Image(
                painter = painterResource(id = backgroundImage),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                OutlinedTextField(
                    value = startReg,
                    onValueChange = { startReg = it },
                    label = { Text("Start Reg. No") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = endReg,
                    onValueChange = { endReg = it },
                    label = { Text("End Reg. No") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedSemester.ifBlank { "Select Semester" },
                        onValueChange = {},
                        label = { Text("Semester") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        semesters.forEach { sem ->
                            DropdownMenuItem(
                                text = { Text("Semester $sem") },
                                onClick = {
                                    selectedSemester = sem
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { buildBulkResults() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fetch Bulk Result")
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Text("Fetching... $progress")
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(bulkResults) { item ->

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF2F2F2F) else Color(0xFFEAEAEA)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {

                                Text(
                                    text = "Reg No: ${item.regNo}",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text("Name: ${item.name}")
                                Text("SGPA: ${item.sgpa}")
                                Text("CGPA: ${item.cgpa}")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- FETCH SEMESTER CONFIG ----------------

suspend fun fetchSemesterConfigs(jsonUrl: String): Map<String, SemesterConfig>? {
    return withContext(Dispatchers.IO) {
        try {
            val jsonText = URL(jsonUrl).readText()
            val json = JSONObject(jsonText)

            val map = mutableMapOf<String, SemesterConfig>()

            json.keys().forEach { key ->
                val obj = json.getJSONObject(key)
                map[key] = SemesterConfig(
                    name = obj.getString("name"),
                    session = obj.getString("session"),
                    examHeld = obj.getString("exam_held")
                )
            }

            map
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
