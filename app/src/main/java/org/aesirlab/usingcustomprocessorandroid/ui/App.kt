package org.aesirlab.usingcustomprocessorandroid.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.aesirlab.usingcustomprocessorandroid.ui.screens.RagServiceMainScreen

enum class Screens {
    RagServiceMainScreen
}
private const val TAG = "App"
@Composable
fun App() {
    val navController = rememberNavController()


    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screens.RagServiceMainScreen.name,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(route = Screens.RagServiceMainScreen.name) {
                RagServiceMainScreen()
            }
        }
    }
}