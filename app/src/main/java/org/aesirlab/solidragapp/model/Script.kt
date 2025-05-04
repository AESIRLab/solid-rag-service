package org.aesirlab.solidragapp.model

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import okhttp3.RequestBody
import java.util.UUID
import android.net.Uri
import android.util.Log
import com.hp.hpl.jena.query.QueryExecutionFactory
import com.hp.hpl.jena.query.QueryFactory
import com.hp.hpl.jena.rdf.model.ModelFactory
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import net.openid.appauth.GrantTypeValues
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Calendar
import java.util.UUID.randomUUID
import kotlin.String
import kotlin.collections.List
import org.json.JSONArray
import org.json.JSONObject
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

val Context.dataStore: DataStore<Preferences> by preferencesDataStore("userData")

public fun getOidcProviderFromWebIdDoc(webIdResponse: String): String {
    val stringAsByteArray = webIdResponse.toByteArray()
    val utf8String = String(stringAsByteArray, Charsets.UTF_8)
    val inStream = utf8String.byteInputStream()
    val m = ModelFactory.createDefaultModel().read(inStream, null, "TURTLE")
    val queryString = "SELECT ?o\n" +
            "WHERE\n" +
            "{ ?s <http://www.w3.org/ns/solid/terms#oidcIssuer> ?o }"
    val q = QueryFactory.create(queryString)
    var result = ""
    try {
        val qexec = QueryExecutionFactory.create(q, m)
        val results = qexec.execSelect()
        while (results.hasNext()) {
            val soln = results.nextSolution()
            result = soln.getResource("o").toString()
            break
        }
    } catch (e: Exception) {
    }
    return result
}

public fun buildAuthorizationUrl(
    authUrl: String,
    clientId: String,
    codeVerifierChallenge: String,
    redirectUri: String,
    clientSecret: String? = null,
): HttpUrl {
    val authUrl2 = Uri.parse(authUrl)
    val newAuthUriBuilder = Uri.Builder()
        .scheme(authUrl2.scheme)
        .authority(authUrl2.authority)
        .appendEncodedPath("authorization")
        .appendQueryParameter("response_type", "code")
        .appendQueryParameter("redirect_uri", redirectUri)
        .appendQueryParameter("scope", "offline_access openid webid")
        .appendQueryParameter("client_id", clientId)
        .appendQueryParameter("code_challenge_method", "S256")
        .appendQueryParameter("code_challenge", codeVerifierChallenge)
        .appendQueryParameter("prompt", "consent")

    if (clientSecret != null) {
        newAuthUriBuilder.appendQueryParameter("client_secret", clientSecret)
    }
    val newAuthUri = newAuthUriBuilder.build()
    val newUrl = newAuthUri.toString().toHttpUrl()
    return newUrl
}

public fun buildRegistrationJSONBody(clientName: String, redirectUris: List<String>): JSONObject {
    val jsonData = JSONObject()
    val redirectUrisJSON = JSONArray(redirectUris)

    jsonData.put("client_name", clientName)
    jsonData.put("redirect_uris", redirectUrisJSON)
    jsonData.put("application_type", "native")
    jsonData.put("token_endpoint_auth_method", "client_secret_post")
    val arr = JSONArray()
    arr.put("authorization_code")
    arr.put("refresh_token")
    jsonData.put("grant_types", arr)
    return jsonData
}

public fun buildRegistrationRequest(registrationUrl: String, jsonBody: JSONObject): Request {
    val jsonString = jsonBody.toString()
    val postBody = jsonString.toRequestBody("application/json".toMediaTypeOrNull())

    val request = Request.Builder()
        .url(registrationUrl)
        .addHeader("Content-Type", "application/json")
        .addHeader("Accept", "application/json")
        .post(postBody)
        .build()
    return request
}

public fun buildConfigRequest(configUrl: String): Request {
    val request = Request.Builder().url("$configUrl/.well-known/openid-configuration").build()
    return request
}

public fun generateDPoPKey(): ECKey {
    val ecJWK = ECKeyGenerator(Curve.P_256).generate()
    return ecJWK
}

public fun generateAuthString(method: String, tokenUri: String, ecJWK: ECKey = generateDPoPKey()): String {
    val keyId = randomUUID().toString()
    val ecPublicJWK = ecJWK.toPublicJWK()
    val signer = ECDSASigner(ecJWK)
    val body = JWTClaimsSet.Builder().claim("htu", tokenUri).claim("htm",
        method).issueTime(Calendar.getInstance().time).jwtID(randomUUID().toString()).claim("nonce",
        randomUUID().toString()).build()
    val header =
        JWSHeader.Builder(JWSAlgorithm.ES256).keyID(keyId).type(JOSEObjectType("dpop+jwt")).jwk(ecPublicJWK).build()
    val signedJWT = SignedJWT(header, body)
    signedJWT.sign(signer)
    return signedJWT.serialize()
}


fun buildTokenRequest(
    clientId: String,
    tokenUri: String,
    codeVerifier: String,
    redirectUri: String,
    signingJwk: ECKey = generateDPoPKey(),
    clientSecret: String? = null,
    code: String? = null
): Request {
    val authString = generateAuthString("POST", tokenUri, signingJwk)

    val bodyBuilder = FormBody.Builder().apply {
        add("grant_type", GrantTypeValues.AUTHORIZATION_CODE)
        add("code_verifier", codeVerifier)
        code?.let { add("code", code) }
        add("redirect_uri", redirectUri)
        add("client_id", clientId)
    }

    if (clientSecret != null) {
        bodyBuilder.add("client_secret", clientSecret)
    }
    val body = bodyBuilder.build()

    val tokenRequest = Request.Builder()
        .url(tokenUri)
        .addHeader("Accept", "*/*")
        .addHeader("DPoP", authString)
        .addHeader("Content-Type", "application/x-www-form-urlencoded")
        .post(body)
        .build()

    return tokenRequest
}

fun parseTokenResponseBody(responseBody: String): HashMap<String, String> {

    val dict = HashMap<String, String>()
    val json = JSONObject(responseBody)
    val accessToken = json.getString("access_token")
    dict["access_token"] = accessToken

    try {
        val idToken = json.getString("id_token")
        dict["id_token"] = idToken
        try {
            val jwtObject = SignedJWT.parse(idToken)
            val body = jwtObject.payload
            val jsonBody = JSONObject(body.toJSONObject())
            val webId = jsonBody.getString("webid")
            dict["web_id"] = webId
        } catch (e: Exception) {
            e.message?.let { Log.e("error", it) }
        }
    } catch (e: Exception) {
        e.message?.let { Log.e("error", it) }
    }

    try {
        val refreshToken = json.getString("refresh_token")
        dict["refresh_token"] = refreshToken
    } catch (e: Exception){
        e.message?.let { Log.d("error", it) }
    }

    return dict
}

fun createUnsafeOkHttpClient(): OkHttpClient {
    val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        @SuppressLint("TrustAllX509TrustManager")
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        }

        @SuppressLint("TrustAllX509TrustManager")
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

private fun generateCustomToken(method: String, uri: String, signingJwk: String): String {
    if (signingJwk == "") {
        throw Error("no signing jwk found")
    }
    val jsonFromStringJWK = JSONObject(signingJwk)
    val parsedKey = ECKey.parse(jsonFromStringJWK.toString(4))
    val ecPublicJWK = parsedKey.toPublicJWK()
    val signer = ECDSASigner(parsedKey)
    val body = JWTClaimsSet.Builder().claim("htu", uri).claim("htm",
        method).issueTime(Calendar.getInstance().time).jwtID(UUID.randomUUID().toString()).build()
    val header =
        JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType("dpop+jwt")).jwk(ecPublicJWK).build()
    val signedJWT = SignedJWT(header, body)
    signedJWT.sign(signer)
    return signedJWT.serialize()
}

//private fun generateCustomToken(method: String, uri: String, signingJwk: String): String {
//    val parsedKey = ECKey.parse(JWK.parse(signingJwk).toJSONObject())
//    val ecPublicJWK = parsedKey.toPublicJWK()
//    val signer = ECDSASigner(parsedKey)
//    val body = JWTClaimsSet.Builder().claim("htu", uri).claim("htm",
//        method).issueTime(Calendar.getInstance().time).jwtID(randomUUID().toString()).build()
//    val header =
//        JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType("dpop+jwt")).jwk(ecPublicJWK).build()
//    val signedJWT = SignedJWT(header, body)
//    signedJWT.sign(signer)
//    return signedJWT.serialize()
//}

fun generateGetRequest(resourceUri: String, accessToken: String, signingJwk: String): Request =
    Request.Builder().url(resourceUri).addHeader("DPoP", generateCustomToken("GET",
        resourceUri, signingJwk)).addHeader("Authorization", "DPoP $accessToken").addHeader("Content-Type",
        "text/turtle").addHeader("Link",
        "<http://www.w3.org/ns/ldp#Resource>;rel=\"type\"").method("GET", null).build()


fun generatePutRequest(accessToken: String, resourceUri: String, rBody: RequestBody, signingJwk: String, contentType: String = "text/turtle"): Request {
    return Request.Builder().url(resourceUri).addHeader("DPoP", generateCustomToken("PUT",
        resourceUri, signingJwk)).addHeader("Authorization", "DPoP $accessToken").addHeader("content-type",
        contentType).addHeader("Link",
        "<http://www.w3.org/ns/ldp#Resource>;rel=\"type\"").method("PUT", rBody).build()
}