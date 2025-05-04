package org.aesirlab.solidragapp.ui


import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.aesirlab.solidragapp.REDIRECT_URI
import org.aesirlab.solidragapp.model.AuthTokenStore
import org.aesirlab.solidragapp.ui.screens.AuthCompleteScreen
import org.aesirlab.solidragapp.ui.screens.RagServiceMainScreen
import org.aesirlab.solidragapp.ui.screens.StartAuthScreen
import org.aesirlab.solidragapp.ui.screens.UnfetchableWebIdScreen
import org.aesirlab.solidragapp.ui.screens.WebsocketConnectScreen

enum class Screens {
    AuthCompleteScreen,
    UnfetchableWebIdScreen,
    StartAuthScreen,
    WebsocketConnectScreen,
    RagServiceMainScreen
}
private const val TAG = "App"
@Composable
fun App() {
    val navController = rememberNavController()
    val applicationContext = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()
    val store = AuthTokenStore(applicationContext)

    Scaffold { innerPadding ->
        val context = LocalContext.current

        NavHost(
            navController = navController,
            startDestination = Screens.StartAuthScreen.name,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(route = Screens.StartAuthScreen.name) {
                val webId = runBlocking {
                    store.getWebId().first()
                }
                if (webId.isNotBlank()) {
                    navController.navigate(route = Screens.RagServiceMainScreen.name)
                }
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
                    navController.navigate(Screens.RagServiceMainScreen.name)
                }
            }
            composable(
                route = Screens.WebsocketConnectScreen.name
            ) {
                WebsocketConnectScreen()
            }
            composable(route = Screens.RagServiceMainScreen.name) {
                val accessToken = runBlocking { store.getAccessToken().first() }
                val signingJwk = runBlocking { store.getSigner().first() }
                RagServiceMainScreen(accessToken, signingJwk)
            }
        }
    }
}