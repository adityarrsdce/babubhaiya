package com.babu.appp.topbar



import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonTopBarWithBack(title: String, onBack: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val background = if (isDark) Color(0xFF1C1C1C) else Color(0xFFE5D1B5)
    val textColor = if (isDark) Color.White else Color.Black

    TopAppBar(
        title = { Text(text = title, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = background
        )
    )
}
@Composable
@Preview
fun CommonTopBarWithBackPreview() {
    CommonTopBarWithBack(title = "Sample Title", onBack = {})
}

