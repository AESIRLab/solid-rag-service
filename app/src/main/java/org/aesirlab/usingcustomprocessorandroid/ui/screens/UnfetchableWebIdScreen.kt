package org.aesirlab.usingcustomprocessorandroid.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.openid.appauth.CodeVerifierUtil
import org.aesirlab.usingcustomprocessorandroid.R
import org.aesirlab.usingcustomprocessorandroid.REDIRECT_URI
import org.aesirlab.usingcustomprocessorandroid.shared.getUnsafeOkHttpClient
import org.json.JSONException
import org.json.JSONObject
import org.skCompiler.generatedAuth.buildAuthorizationUrl
import org.skCompiler.generatedAuth.buildConfigRequest
import org.skCompiler.generatedAuth.buildRegistrationJSONBody
import org.skCompiler.generatedAuth.buildRegistrationRequest
import org.skCompiler.generatedModel.AuthTokenStore

@Composable
fun UnfetchableWebIdScreen(
    tokenStore: AuthTokenStore,
    renderError: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement =  Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment =  Alignment.CenterHorizontally,
    ) {
        val appTitle = "Not sure yet Solid"
        var solidProvider by rememberSaveable {
            mutableStateOf("")
        }

        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "App logo"
        )
        Text(text=appTitle)
        TextField(
            value = solidProvider,
            onValueChange =  { solidProvider = it },
            label = { Text("Solid Provider") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
        val context = LocalContext.current
        StartButton(text = "Fetch OIDC Config") {
            val client = getUnsafeOkHttpClient()
            val redirectUris = listOf(REDIRECT_URI)
            CoroutineScope(Dispatchers.IO).launch {

                val configRequest = buildConfigRequest(solidProvider)
                val configResponse = client.newCall(configRequest).execute()

                val responseBody = configResponse.body!!.string()
                val configJSON = JSONObject(responseBody)

                val registrationEndpoint = configJSON.getString("registration_endpoint")
                val tokenEndpoint = configJSON.getString("token_endpoint")
                val authUrl = configJSON.getString("authorization_endpoint")

                tokenStore.setTokenUri(tokenEndpoint)

                val jsonBody = buildRegistrationJSONBody(appTitle, redirectUris)
                val registrationRequest = buildRegistrationRequest(registrationEndpoint, jsonBody)
                val registrationResponse = client.newCall(registrationRequest).execute()
                val registrationString = registrationResponse.body!!.string()

                val registrationJSON = JSONObject(registrationString)
                val clientId = registrationJSON.getString("client_id")
                var clientSecret: String? = null
                try {
                    clientSecret = registrationJSON.getString("client_secret")
                    tokenStore.setClientSecret(clientSecret)

                } catch (e: JSONException) {
                    Log.d("JSONException", "no client_secret returned")
                    renderError(e.message.toString())
                }

                tokenStore.setClientId(clientId)

                val codeVerifier = CodeVerifierUtil.generateRandomCodeVerifier()
                val codeVerifierChallenge = CodeVerifierUtil.deriveCodeVerifierChallenge(codeVerifier)

                tokenStore.setCodeVerifier(codeVerifier)

                val authorizationUrl = buildAuthorizationUrl(authUrl, clientId, codeVerifierChallenge, redirectUris[0], clientSecret)
                val sendUri = Uri.parse(authorizationUrl.toString())
                context.startActivity(Intent(Intent.ACTION_VIEW, sendUri))
            }
        }
    }
}