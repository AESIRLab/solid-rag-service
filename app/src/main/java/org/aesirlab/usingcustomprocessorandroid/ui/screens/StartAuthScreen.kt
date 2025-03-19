package org.aesirlab.usingcustomprocessorandroid.ui.screens


import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import okhttp3.Response
import org.aesirlab.usingcustomprocessorandroid.R
import org.aesirlab.usingcustomprocessorandroid.REDIRECT_URI
import org.aesirlab.usingcustomprocessorandroid.shared.getUnsafeOkHttpClient
import org.aesirlab.usingcustomprocessorandroid.shared.okHttpRequest
import org.json.JSONException
import org.json.JSONObject
import org.skCompiler.generatedAuth.buildAuthorizationUrl
import org.skCompiler.generatedAuth.buildConfigRequest
import org.skCompiler.generatedAuth.buildRegistrationJSONBody
import org.skCompiler.generatedAuth.buildRegistrationRequest
import org.skCompiler.generatedAuth.getOidcProviderFromWebIdDoc
import org.skCompiler.generatedModel.AuthTokenStore

@Composable
fun StartButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text = text)
    }
}

@Composable
fun StartAuthScreen(
    tokenStore: AuthTokenStore,
    onFailNavigation: () -> Unit,
    onInvalidInput: (String?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement =  Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment =  Alignment.CenterHorizontally,
    ) {
        val appTitle = "Android Item Tracker Solid"
        var webId by rememberSaveable {
            mutableStateOf("")
        }

        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "App logo"
        )
        Text(text=appTitle)
        TextField(
            value = webId,
            onValueChange =  { webId = it },
            label = { Text("WebId") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
        val context = LocalContext.current
        StartButton(text = "Start Auth!") {

            val redirectUris = listOf(REDIRECT_URI)
            CoroutineScope(Dispatchers.IO).launch {

                val client = getUnsafeOkHttpClient()
                tokenStore.setWebId(webId)
                tokenStore.setRedirectUri(redirectUris[0])
                val response: Response?
                try {
                    response = okHttpRequest(webId)
                } catch (e: Exception) {
                    onInvalidInput(e.message)
                    return@launch
                }

                if (response.code < 200 || response.code > 299)  {
                    onFailNavigation()
                    return@launch
                }
                val data = response.body!!.string()
                val oidcProvider = getOidcProviderFromWebIdDoc(data)
                tokenStore.setOidcProvider(oidcProvider)

                val configRequest = buildConfigRequest(oidcProvider)
                val configResponse = client.newCall(configRequest).execute()
                // needs 4xx error checking
                val responseBody = configResponse.body!!.string()
                val configJSON = JSONObject(responseBody)

                val registrationEndpoint = configJSON.getString("registration_endpoint")
                val tokenEndpoint = configJSON.getString("token_endpoint")
                val authUrl = configJSON.getString("authorization_endpoint")

                tokenStore.setTokenUri(tokenEndpoint)

                val registrationBody = buildRegistrationJSONBody(appTitle, redirectUris)
                val registrationRequest = buildRegistrationRequest(registrationEndpoint, registrationBody)
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

