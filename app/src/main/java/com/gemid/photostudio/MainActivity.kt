package com.gemid.photostudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.gemid.photostudio.navigation.BottomNavBar
import com.gemid.photostudio.navigation.NavGraph
import com.gemid.photostudio.ui.theme.GemIDPhotoStudioTheme
import com.gemid.photostudio.ui.viewmodel.StudioViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GemIDPhotoStudioTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp(
    viewModel: StudioViewModel = viewModel()
) {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavBar(navController = navController)
        }
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            innerPadding = innerPadding,
            viewModel = viewModel
        )
    }
}
