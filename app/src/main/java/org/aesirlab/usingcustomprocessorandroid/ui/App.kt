package org.aesirlab.usingcustomprocessorandroid.ui

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.aesirlab.model.Item
import org.aesirlab.usingcustomprocessorandroid.REDIRECT_URI
import org.aesirlab.usingcustomprocessorandroid.ui.screens.AuthCompleteScreen
import org.aesirlab.usingcustomprocessorandroid.ui.screens.MainScreen
import org.aesirlab.usingcustomprocessorandroid.ui.screens.StartAuthScreen
import org.aesirlab.usingcustomprocessorandroid.ui.screens.UnfetchableWebIdScreen
import org.skCompiler.generatedModel.AuthTokenStore

enum class Screens {
    MainScreen,
    AuthCompleteScreen,
    UnfetchableWebIdScreen,
    StartAuthScreen
}

@Composable
fun App() {
    val navController = rememberNavController()
    val applicationContext = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()
    val store = AuthTokenStore(applicationContext)
    val repository = (LocalContext.current.applicationContext as SolidMobileItemApplication).repository
    val viewModel = ItemViewModel(repository)


    Scaffold { innerPadding ->
        val context = LocalContext.current
        val items by viewModel.allItems.collectAsState()
        NavHost(
            navController = navController,
            startDestination = Screens.StartAuthScreen.name,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(route = Screens.StartAuthScreen.name) {
                StartAuthScreen(
                    tokenStore = store,
                    onFailNavigation = {
                        coroutineScope.launch {
                            withContext(Dispatchers.Main) {
                                navController.navigate(Screens.UnfetchableWebIdScreen.name)
                            }
                        }
                    },
                    onInvalidInput = { msg ->
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, msg.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            composable(route = Screens.UnfetchableWebIdScreen.name) {
                UnfetchableWebIdScreen(tokenStore = store) { err ->
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            composable(
                route = Screens.AuthCompleteScreen.name,
                deepLinks = listOf(navDeepLink { uriPattern = REDIRECT_URI })) {

                AuthCompleteScreen(tokenStore = store) {
                    navController.navigate(Screens.MainScreen.name)
                }
            }
            composable(route = Screens.MainScreen.name) {
                MainScreen(items,
                    onAddClick = { thing ->
                        val newItem = Item("", thing)
                        coroutineScope.launch {
                            viewModel.insert(newItem)
                        }
                    },
                    onIncreaseClick = { item ->
                        item.amount += 1
                        coroutineScope.launch {
                            viewModel.update(item)
                        }
                    },
                    onDecreaseClick = { item ->
                        if (item.amount > 0) {
                            item.amount -= 1
                            coroutineScope.launch {
                                viewModel.update(item)
                            }
                        }
                    },
                    onDeleteClick = { item ->
                        coroutineScope.launch {
                            viewModel.delete(item)
                        }
                    }
                )
            }
        }
    }
}