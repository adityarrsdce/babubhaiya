package com.babu.appp.Navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavigationBar_V1(
    navController: NavController,
    isDarkMode: Boolean   // 🔑 Global theme state from MainActivity
) {

    val currentDestination by navController.currentBackStackEntryAsState()
    val currentRoute = currentDestination?.destination?.route

    // 🎨 Colors strictly from isDarkMode (NOT system theme)
    val selectedColor = if (isDarkMode) Color(0xFFE5D1B5) else Color(0xFF000000)
    val unselectedColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)
    val containerColor = if (isDarkMode) Color(0xFF121212) else Color.White

    val items = listOf(
        Triple("home", "Study", Icons.Default.School),
        Triple("ranking", "Ranking", Icons.Default.BarChart),
        Triple("college", "Holidays", Icons.Default.HolidayVillage),
        Triple("events", "Calendar", Icons.Default.Event),
        Triple("about", "About", Icons.Default.Info)
    )

    NavigationBar(
        containerColor = containerColor,
        tonalElevation = 0.dp
    ) {

        items.forEach { (route, label, icon) ->

            val isSelected = currentRoute == route

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        navController.navigate(route) {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
                icon = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) selectedColor else unselectedColor
                        )

                        Text(
                            text = label,
                            fontSize = 10.sp,
                            color = if (isSelected) selectedColor else unselectedColor
                        )
                    }
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent, // ❌ No ripple background
                    selectedIconColor = selectedColor,
                    selectedTextColor = selectedColor,
                    unselectedIconColor = unselectedColor,
                    unselectedTextColor = unselectedColor
                )
            )
        }
    }
}
