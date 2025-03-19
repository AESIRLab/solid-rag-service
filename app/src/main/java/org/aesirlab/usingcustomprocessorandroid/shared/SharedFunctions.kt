package org.aesirlab.usingcustomprocessorandroid.shared

import android.util.Log
import com.nimbusds.jwt.SignedJWT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import net.openid.appauth.GrantTypeValues
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import org.skCompiler.generatedAuth.generateAuthString
import org.skCompiler.generatedModel.AuthTokenStore
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

suspend fun okHttpRequest(url: String): Response = withContext(Dispatchers.IO) {
    val client = getUnsafeOkHttpClient()
    val request = Request.Builder().url(url).build()
    val response = client.newCall(request).execute()
    return@withContext response
}

suspend fun preliminaryAuth(tokenStore: AuthTokenStore, code: String?)  {
    val clientId = tokenStore.getClientId().first()
    val rClientSecret = tokenStore.getClientSecret().first()
    val tokenUrl = tokenStore.getTokenUri().first()
    val codeVerifier = tokenStore.getCodeVerifier().first()
    val redirectUri = tokenStore.getRedirectUri().first()

    var clientSecret: String? = null
    if (rClientSecret != "") {
        clientSecret = rClientSecret
    }

    val authString = generateAuthString("POST", tokenUrl)

    val response = tokenRequest(
        clientId,
        clientSecret,
        tokenUrl,
        code!!,
        codeVerifier,
        redirectUri,
        authString
    )

    val json = JSONObject(response)
    val accessToken = json.getString("access_token")

    val idToken: String
    try {
        idToken = json.getString("id_token")
        tokenStore.setIdToken(idToken)

        try {
            val jwtObject = SignedJWT.parse(idToken)
            val body = jwtObject.payload
            val jsonBody = JSONObject(body.toJSONObject())
            val webId = jsonBody.getString("webid")
            tokenStore.setWebId(webId)
        } catch (e: Exception) {
            e.message?.let { Log.e("error", it) }
        }
    } catch (e: Exception) {
        e.message?.let { Log.e("error", it) }
    }

    val refreshToken: String
    try {
        refreshToken = json.getString("refresh_token")
        tokenStore.setRefreshToken(refreshToken)
    } catch (e: Exception){
        e.message?.let { Log.d("error", it) }
    }

    tokenStore.setAccessToken(accessToken)
}

fun getUnsafeOkHttpClient(): OkHttpClient {
    val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        }

        override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
    })

    // Install the all-trusting trust manager
    val sslContext = SSLContext.getInstance("SSL")
    sslContext.init(null, trustAllCerts, java.security.SecureRandom())
    // Create an ssl socket factory with our all-trusting manager
    val sslSocketFactory = sslContext.socketFactory

    return OkHttpClient.Builder()
        .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true }.build()
}

private suspend fun tokenRequest(
    clientId: String,
    clientSecret: String?,
    tokenUrl: String,
    code: String,
    codeVerifier: String,
    redirectUri: String,
    authString: String
): String = withContext(Dispatchers.IO){
    val client = getUnsafeOkHttpClient()

    val bodyBuilder = FormBody.Builder()
        .add("grant_type", GrantTypeValues.AUTHORIZATION_CODE)
        .add("code_verifier", codeVerifier)
        .add("code", code)
        .add("redirect_uri", redirectUri)
        .add("client_id", clientId)

    if (clientSecret != null) {
        bodyBuilder.add("client_secret", clientSecret)
    }
    val body = bodyBuilder.build()

    val tokenRequest = Request.Builder()
        .url(tokenUrl)
        .addHeader("Accept", "*/*")
        .addHeader("DPoP", authString)
        .addHeader("Content-Type", "application/x-www-form-urlencoded")
        .post(body)
        .build()
    val response = client.newCall(tokenRequest).execute()
    val responseBody = response.body!!.string()
    Log.d("response body", JSONObject(responseBody).toString())

    return@withContext responseBody
}