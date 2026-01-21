package com.babu.appp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.babu.appp.Navigation.AppNavigation
import com.babu.appp.ui.theme.ApppTheme
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private var mInterstitialAd: InterstitialAd? = null
    private val TAG = "MainActivity"

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "Notification permission not granted. You might miss important updates.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        firebaseAnalytics = Firebase.analytics
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        createNotificationChannel()

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "FCM Token: ${task.result}")
            } else {
                Log.e(TAG, "Fetching FCM token failed", task.exception)
            }
        }

        MobileAds.initialize(this)
        loadInterstitialAd()

        // ✅ FINAL THEME SETUP (AUTO + MANUAL BOTH FIXED)
        setContent {

            // 1️⃣ Read system theme
            val systemDark = isSystemInDarkTheme()

            // 2️⃣ Remember theme state, saved across recompositions
            var isDarkMode by rememberSaveable { mutableStateOf(systemDark) }

            // 3️⃣ If system theme changes AND user never toggled manually,
            //    then sync again with system
            LaunchedEffect(systemDark) {
                isDarkMode = systemDark
            }

            ApppTheme(darkTheme = isDarkMode) {

                AppNavigation(
                    isDarkMode = isDarkMode,
                    onThemeToggle = {
                        // Manual override
                        isDarkMode = !isDarkMode
                    }
                )
            }
        }
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            this,
            "ca-app-pub-4302526630220985/2830135242",
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded.")
                    mInterstitialAd = ad
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Interstitial ad failed to load: ${adError.message}")
                    mInterstitialAd = null
                }
            }
        )
    }

    fun showInterstitialAdIfReady() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(this)
        } else {
            Log.d(TAG, "Interstitial ad not ready.")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "babu_bhaiya_channel"
            val channelName = "PYQ Notifications"
            val channelDescription = "Important updates and alerts from the PYQ app"

            val soundUri = Uri.parse("android.resource://${packageName}/${R.raw.pikachu}")
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = channelDescription
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                enableLights(true)
                lightColor = android.graphics.Color.RED
            }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
