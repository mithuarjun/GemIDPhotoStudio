package com.gemid.photostudio.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Share
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Home : Screen("home", "Home", Icons.Rounded.Home)
    data object Studio : Screen("studio", "Studio", Icons.Rounded.PhotoCamera)
    data object SheetLayout : Screen("sheet_layout", "Sheets", Icons.Rounded.GridView)
    data object Export : Screen("export", "Export", Icons.Rounded.Share)
}

val bottomNavScreens = listOf(
    Screen.Home,
    Screen.Studio,
    Screen.SheetLayout,
    Screen.Export
)
