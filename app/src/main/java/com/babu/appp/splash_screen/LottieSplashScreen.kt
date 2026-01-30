package com.babu.appp.screen.splash_screen



import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.airbnb.lottie.compose.*

@Composable
fun SplashScreen(navController: NavHostController) {

    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("splash.json")
    )

    // Play animation once
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1
    )

    // When animation finishes → go to home
    LaunchedEffect(progress) {
        if (progress == 1f) {
            navController.navigate("home") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(250.dp)
        )
    }
}
