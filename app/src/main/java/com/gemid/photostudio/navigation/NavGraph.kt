package com.gemid.photostudio.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.padding(innerPadding)
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController, viewModel = viewModel)
        }
        composable(Screen.Studio.route) {
            StudioEditorScreen(navController = navController, viewModel = viewModel)
        }
        composable(Screen.SheetLayout.route) {
            SheetLayoutScreen(navController = navController, viewModel = viewModel)
        }
        composable(Screen.Export.route) {
            ExportScreen(navController = navController, viewModel = viewModel)
        }
    }
}
