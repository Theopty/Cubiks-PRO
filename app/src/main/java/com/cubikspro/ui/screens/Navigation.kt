package com.cubikspro.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cubikspro.ui.viewmodel.SolverViewModel

sealed class Screen(val route: String) {
    data object Camera : Screen("camera")
    data object Result : Screen("result")
    data object Settings : Screen("settings")
}

@Composable
fun AppNavigation(viewModel: SolverViewModel = viewModel()) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()

    NavHost(navController = navController, startDestination = Screen.Camera.route) {
        composable(Screen.Camera.route) {
            CameraScreen(
                onPhotoCaptured = { imageBytes ->
                    viewModel.solveFromImage(imageBytes)
                    navController.navigate(Screen.Result.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Result.route) {
            ResultScreen(
                uiState = uiState,
                onBackClick = {
                    viewModel.clearResult()
                    navController.popBackStack()
                },
                onRetryClick = {
                    uiState.capturedImageBytes?.let { bytes ->
                        viewModel.solveFromImage(bytes)
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                currentApiKey = apiKey,
                onApiKeySaved = { key -> viewModel.setApiKey(key) },
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
