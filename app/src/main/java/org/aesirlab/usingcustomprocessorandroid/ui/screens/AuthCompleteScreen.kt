package org.aesirlab.usingcustomprocessorandroid.ui.screens


import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.runBlocking
import org.aesirlab.usingcustomprocessorandroid.shared.preliminaryAuth
import org.skCompiler.generatedModel.AuthTokenStore


private const val TAG = "AuthCompleteScreen"
@Composable
fun AuthCompleteScreen(
    tokenStore: AuthTokenStore,
    onFinishedAuth: () -> Unit,
) {
    val context = LocalContext.current
    val activity = (context as Activity).intent
    val intentData = activity.data
    val code = intentData?.getQueryParameter("code")


    runBlocking {
        preliminaryAuth(tokenStore, code)
    }
    onFinishedAuth()
}
