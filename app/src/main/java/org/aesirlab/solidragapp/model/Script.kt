package org.aesirlab.solidragapp.model

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
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import okhttp3.RequestBody
import java.util.Calendar
import java.util.UUID

val Context.dataStore: DataStore<Preferences> by preferencesDataStore("userData")

private fun generateCustomToken(method: String, uri: String, signingJwk: String): String {
    val parsedKey = ECKey.parse(JWK.parse(signingJwk).toJSONObject())
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