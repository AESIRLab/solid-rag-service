package org.aesirlab.usingcustomprocessorandroid.ui.screens


import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.aesirlab.mylibrary.sharedfunctions.buildTokenRequest
import org.aesirlab.mylibrary.sharedfunctions.createUnsafeOkHttpClient
import org.aesirlab.mylibrary.sharedfunctions.parseTokenResponseBody
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

    val informationHashMap = runBlocking {
        val hm = HashMap<String ,String>()
        hm["clientId"] = tokenStore.getClientId().first()
        hm["clientSecret"] = tokenStore.getClientSecret().first()
        hm["tokenUri"] = tokenStore.getTokenUri().first()
        hm["codeVerifier"] = tokenStore.getCodeVerifier().first()
        hm["redirectUri"] = tokenStore.getRedirectUri().first()
        hm
    }
    val tokenRequest = buildTokenRequest(
        informationHashMap["clientId"]!!,
        informationHashMap["tokenUri"]!!,
        informationHashMap["codeVerifier"]!!,
        informationHashMap["redirectUri"]!!,
        informationHashMap["clientSecret"]!!,
        code
    )
    val client = createUnsafeOkHttpClient()
    val tokenResponse = runBlocking {
        withContext(Dispatchers.IO) {
            client.newCall(tokenRequest).execute()
        }
    }
    val responseBody = tokenResponse.body!!.string()
    val tokensHashMap = parseTokenResponseBody(responseBody)
    runBlocking {
        tokenStore.apply {
            tokensHashMap["access_token"]?.let { setAccessToken(it) }
            tokensHashMap["id_token"]?.let { setWebId(it)}
            tokensHashMap["web_id"]?.let { setIdToken(it)}
            tokensHashMap["refresh_token"]?.let { setRefreshToken(it) }
        }
    }
    onFinishedAuth()
}
