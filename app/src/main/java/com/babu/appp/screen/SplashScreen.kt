package com.babu.appp.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.babu.appp.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {

    // ⏳ Splash delay (change if needed)
    LaunchedEffect(Unit) {
        delay(2000)
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.pyq_icon), // your logo
            contentDescription = "App Logo",
            modifier = Modifier.size(140.dp)
        )
    }
}
