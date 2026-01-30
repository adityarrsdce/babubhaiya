package com.babu.appp

// Android system imports
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

// Activity + Compose setup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

// App navigation + theme
import com.babu.appp.Navigation.AppNavigation
import com.babu.appp.ui.theme.ApppTheme

// ✅ Play Store Update Manager
import com.babu.appp.update.UpdateManager

// Google Ads
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

// Firebase Analytics + Messaging
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    // Firebase analytics instance.
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    // Interstitial ad object
    private var mInterstitialAd: InterstitialAd? = null

    private val TAG = "MainActivity"

    // ---------------- Notification Permission Launcher ----------------
    // Android 13+ me notification permission required hoti hai
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

        // Enable full edge-to-edge UI
        enableEdgeToEdge()

        // ✅ Play Store update check on app launch
        UpdateManager(this).checkForUpdate()

        // Firebase analytics initialization
        firebaseAnalytics = Firebase.analytics
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)

        // Request notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Create custom notification channel
        createNotificationChannel()

        // Get Firebase Cloud Messaging token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "FCM Token: ${task.result}")
            } else {
                Log.e(TAG, "Fetching FCM token failed", task.exception)
            }
        }

        // Initialize Google Ads SDK
        MobileAds.initialize(this)

        // Load interstitial ad
        loadInterstitialAd()

        // ---------------- Compose UI ----------------
        setContent {

            // Detect system theme
            val systemDark = isSystemInDarkTheme()

            // Save theme preference across app lifecycle
            var isDarkMode by rememberSaveable { mutableStateOf(systemDark) }

            // Auto-sync with system theme changes
            LaunchedEffect(systemDark) {
                isDarkMode = systemDark
            }

            // Apply app theme
            ApppTheme(darkTheme = isDarkMode) {

                // App navigation entry point
                AppNavigation(
                    isDarkMode = isDarkMode,
                    onThemeToggle = {
                        // Manual theme toggle
                        isDarkMode = !isDarkMode
                    }
                )
            }
        }
    }

    // ---------------- Load Interstitial Ad ----------------
    private fun loadInterstitialAd() {

        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            this,
            "ca-app-pub-4302526630220985/2830135242",
            adRequest,
            object : InterstitialAdLoadCallback() {

                // Called when ad successfully loads
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded.")
                    mInterstitialAd = ad
                }

                // Called when ad fails
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Interstitial ad failed to load: ${adError.message}")
                    mInterstitialAd = null
                }
            }
        )
    }

    // Show ad if ready
    fun showInterstitialAdIfReady() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(this)
        } else {
            Log.d(TAG, "Interstitial ad not ready.")
        }
    }

    // ---------------- Notification Channel ----------------
    // Custom sound + vibration notification setup
    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channelId = "babu_bhaiya_channel"
            val channelName = "PYQ Notifications"
            val channelDescription = "Important updates and alerts from the PYQ app"

            // Custom notification sound
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
