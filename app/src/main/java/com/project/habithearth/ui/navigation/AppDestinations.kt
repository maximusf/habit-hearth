package com.project.habithearth.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Map("map", "Map", Icons.Filled.Explore),
    Home("home", "Home", Icons.Filled.Home),
    Story("story","Story", Icons.Filled.Book),
//    TaskMaker("task_maker", "Tasks", Icons.Filled.Add),
    Profile("profile", "Profile", Icons.Filled.Person),
}
