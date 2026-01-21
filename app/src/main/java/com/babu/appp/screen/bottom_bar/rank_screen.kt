package com.babu.appp.screen

import com.babu.appp.R
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.babu.appp.json.fetchJsonFromUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ---------------- CACHE CONFIG ----------------
private const val RANK_PREF = "rank_cache"
private const val RANK_JSON_KEY = "rank_json"
private const val RANK_TIME_KEY = "rank_time"
private const val CACHE_VALIDITY = 24 * 60 * 60 * 1000L
private const val DEFAULT_COURSE = "BTECH"

// ---------------- DATA ----------------
@Serializable
data class CollegeRankData(
    val rank: Int,
    val image: String? = null,
    val website: String? = null
)

data class CollegeRanking(
    val name: String,
    val rank: Int,
    val image: String?,
    val website: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen() {

    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val backgroundPainter =
        if (isDark) painterResource(R.drawable.pyq_dark)
        else painterResource(R.drawable.pyq_light)

    val topBarColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFE5D1B5)
    val textColor = if (isDark) Color.White else Color.Black

    var rankings by remember { mutableStateOf<List<CollegeRanking>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val courseListUrl =
        "https://raw.githubusercontent.com/adityarrsdce/babubhaiya/refs/heads/main/Rank/course_list.json"

    LaunchedEffect(Unit) {

        val prefs = context.getSharedPreferences(RANK_PREF, Context.MODE_PRIVATE)
        val cachedJson = prefs.getString(RANK_JSON_KEY, null)
        val cachedTime = prefs.getLong(RANK_TIME_KEY, 0L)
        val now = System.currentTimeMillis()

        try {
            val jsonString = when {
                cachedJson != null && now - cachedTime < CACHE_VALIDITY -> cachedJson
                else -> {
                    val courseMapJson = withContext(Dispatchers.IO) {
                        fetchJsonFromUrl(courseListUrl)
                    }

                    val courseMap: Map<String, String> =
                        Json.decodeFromString(courseMapJson)

                    val courseUrl = courseMap[DEFAULT_COURSE]
                        ?: throw Exception("Course not found")

                    val freshJson = withContext(Dispatchers.IO) {
                        fetchJsonFromUrl(courseUrl)
                    }

                    prefs.edit()
                        .putString(RANK_JSON_KEY, freshJson)
                        .putLong(RANK_TIME_KEY, now)
                        .apply()

                    freshJson
                }
            }

            val data: Map<String, CollegeRankData> =
                Json.decodeFromString(jsonString)

            rankings = data.entries
                .sortedBy { it.value.rank }
                .map {
                    CollegeRanking(
                        name = it.key,
                        rank = it.value.rank,
                        image = it.value.image,
                        website = it.value.website
                    )
                }

            error = null

        } catch (e: Exception) {
            error = "Failed to load ranking data"
        }

        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("College Rank", color = textColor) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarColor)
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
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> CircularProgressIndicator()
                    error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                    rankings.isNotEmpty() -> LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(rankings) { college ->
                            RankingCard(college)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RankingCard(college: CollegeRanking) {

    val uriHandler = LocalUriHandler.current   // ✅ SAFE PLACE

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !college.website.isNullOrBlank()) {
                college.website?.let {
                    runCatching { uriHandler.openUri(it) }
                }
            },
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            college.image?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "#${college.rank}  ${college.name}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = DEFAULT_COURSE,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
