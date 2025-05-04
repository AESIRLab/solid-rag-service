package org.aesirlab.solidragapp.ui.screens


import android.app.Activity
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.aesirlab.solidragapp.model.AuthTokenStore
import org.aesirlab.solidragapp.model.buildTokenRequest
import org.aesirlab.solidragapp.model.createUnsafeOkHttpClient
import org.aesirlab.solidragapp.model.generateDPoPKey
import org.aesirlab.solidragapp.model.parseTokenResponseBody


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

    val ecKey = generateDPoPKey()
    val tokenRequest = buildTokenRequest(
        informationHashMap["clientId"]!!,
        informationHashMap["tokenUri"]!!,
        informationHashMap["codeVerifier"]!!,
        informationHashMap["redirectUri"]!!,
        signingJwk =  ecKey,
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
    for (entry in tokensHashMap) {
        Log.d(TAG, "${entry.key}: ${entry.value}")
    }
    runBlocking {
        tokenStore.apply {
            setSigner(ecKey.toJSONObject().toString())
            tokensHashMap["access_token"]?.let { setAccessToken(it) }
            tokensHashMap["id_token"]?.let { setIdToken(it) }
            tokensHashMap["web_id"]?.let { setWebId(it) }
            tokensHashMap["refresh_token"]?.let { setRefreshToken(it) }
        }
    }
    Log.d(TAG, ecKey.toJSONObject().toString())
    onFinishedAuth()
}
