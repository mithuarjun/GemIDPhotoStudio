package com.gemid.photostudio.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.gemid.photostudio.ui.camera.CameraScreen
import com.gemid.photostudio.ui.screens.ExportScreen
import com.gemid.photostudio.ui.screens.HomeScreen
import com.gemid.photostudio.ui.screens.SheetLayoutScreen
import com.gemid.photostudio.ui.screens.StudioEditorScreen
import com.gemid.photostudio.ui.viewmodel.StudioViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues,
    viewModel: StudioViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val isFullScreen = navBackStackEntry?.destination?.route == Screen.Camera.route ||
            navBackStackEntry?.destination?.route == Screen.BackgroundBrush.route

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.padding(if (isFullScreen) PaddingValues(0.dp) else innerPadding)
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController, viewModel = viewModel)
        }
        composable(Screen.Camera.route) {
            CameraScreen(navController = navController, viewModel = viewModel)
        }
        composable(Screen.Studio.route) {
            StudioEditorScreen(navController = navController, viewModel = viewModel)
        }
        composable(Screen.BatchStudio.route) {
            com.gemid.photostudio.ui.screens.BatchStudioScreen(navController = navController, viewModel = viewModel)
        }
        composable(Screen.SheetLayout.route) {
            SheetLayoutScreen(navController = navController, viewModel = viewModel)
        }
        composable(Screen.Export.route) {
            ExportScreen(navController = navController, viewModel = viewModel)
        }
        composable(Screen.BackgroundBrush.route) {
            com.gemid.photostudio.ui.screens.BackgroundBrushScreen(navController = navController, viewModel = viewModel)
        }
        composable(Screen.Kiosk.route) {
            com.gemid.photostudio.ui.screens.KioskModeScreen(navController = navController, viewModel = viewModel)
        }
    }
}
