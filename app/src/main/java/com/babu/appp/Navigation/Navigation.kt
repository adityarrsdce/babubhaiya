package com.babu.appp.Navigation

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.babu.appp.AnalyticsHelper.AnalyticsNavObserver
import com.babu.appp.screen.*
import com.babu.appp.screen.bottom_bar.AboutScreen

@Composable
fun AppNavigation(
    isDarkMode: Boolean,              // 👈 from MainActivity
    onThemeToggle: () -> Unit         // 👈 from MainActivity
) {

    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    AnalyticsNavObserver(navController)

    val showBottomBar = currentRoute in listOf(
        "home",
        "about",
        "ranking",
        "college",
        "events"
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar_V1(
                    navController = navController,
                    isDarkMode = isDarkMode   // 🔑 THIS LINE IS MUST
                )
            }
        }
    )
    { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = "home"
        ) {

            // 🏠 HOME (THEME ICON WORKS FROM HERE)
            composable("home") {
                HomeScreen(
                    navController = navController,
                    paddingValues = innerPadding,
                    isDarkMode = isDarkMode,          // 🔑 PASS DOWN
                    onThemeToggle = onThemeToggle    // 🔑 PASS DOWN
                )
            }

            composable("pyq") { PyqScreen() }

            composable("result") { ResultScreen(navController = navController) }

            composable("syllabus") { SyllabusScreen() }

            composable("about") { AboutScreen() }

            composable("ranking") { RankingScreen() }

            composable("college") { HolidayScreen() }

            composable("events") { CalendarScreen() }

            composable("OtherResultScreen/{courseName}") { backStackEntry ->
                val courseName = backStackEntry.arguments?.getString("courseName") ?: ""
                OtherResultScreen(courseName = courseName)
            }

            composable("comp_exam") {
                CompetitiveExamScreenUI(navController = navController)
            }

            composable("imp") {
                ImpQScreen()
            }

            composable("resume") {
                ResumeScreen()
            }

            composable("feedback") {
                FeedbackScreen(navController = navController)
            }

            composable("bulk_result") {
                BulkResultScreen(navController = navController)
            }

        }
    }
}