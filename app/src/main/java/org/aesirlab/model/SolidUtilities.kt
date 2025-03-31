package org.aesirlab.model

import android.content.Context
import com.hp.hpl.jena.query.QueryExecutionFactory
import com.hp.hpl.jena.query.QueryFactory
import com.hp.hpl.jena.rdf.model.ModelFactory
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.io.ByteArrayOutputStream
import java.util.Calendar
import java.util.UUID
import kotlin.Int
import kotlin.String
import kotlin.collections.List
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.aesirlab.usingcustomprocessorandroid.model.AuthTokenStore
import org.aesirlab.model.Utilities.Companion.ABSOLUTE_URI
import org.json.JSONObject
import java.util.Locale

public class SolidUtilities(
  context: Context,
) {
  private val tokenStore: AuthTokenStore = AuthTokenStore(context)

  public suspend fun updateSolidDataset(items: List<Item>): Int {
    val accessToken = runBlocking { tokenStore.getAccessToken().first() }
    val storageUri = runBlocking { val webId = tokenStore.getWebId().first(); getStorage(webId) }
    if (storageUri != "" && accessToken != "") {
      val client = OkHttpClient.Builder().build()
      val resourceUri = "${storageUri}${ABSOLUTE_URI}"
      val model = ModelFactory.createDefaultModel()
      model.setNsPrefix("acp", Utilities.NS_ACP)
      model.setNsPrefix("acl", Utilities.NS_ACL)
      model.setNsPrefix("ldp", Utilities.NS_LDP)
      model.setNsPrefix("skos", Utilities.NS_SKOS)
      model.setNsPrefix("ci", Utilities.NS_Item)
      val ciName = model.createProperty(Utilities.NS_Item + "name")
      val ciAmount = model.createProperty(Utilities.NS_Item + "amount")
      items.forEach { ci -> 
      val id = ci.id
      val mThingUri = model.createResource("$resourceUri#$id")
      mThingUri.addLiteral(ciName, ci.name)
      mThingUri.addLiteral(ciAmount, ci.amount)
      }
      val bOutputStream = ByteArrayOutputStream()
      model.write(bOutputStream, "TURTLE", null)
      val rBody = bOutputStream.toByteArray().toRequestBody(null, 0, bOutputStream.size())
      val putRequest = generatePutRequest(resourceUri, rBody)
      val putResponse = client.newCall(putRequest).execute()
      return putResponse.code
    } else {
      return 600
    }
  }

  private fun generateGetRequest(resourceUri: String, accessToken: String): Request =
      Request.Builder().url(resourceUri).addHeader("DPoP", generateCustomToken("GET",
      resourceUri)).addHeader("Authorization", "DPoP $accessToken").addHeader("Content-Type",
      "text/turtle").addHeader("Link",
      "<http://www.w3.org/ns/ldp#Resource>;rel=\"type\"").method("GET", null).build()

  private fun generateCustomToken(method: String, uri: String): String {
    val signingJwk = runBlocking { tokenStore.getSigner().first() }
    if (signingJwk == "") {
      throw Error("no signing jwk found")
    }
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

  private fun generatePutRequest(resourceUri: String, rBody: RequestBody): Request {
    val accessToken = runBlocking { tokenStore.getAccessToken().first() }
    return Request.Builder().url(resourceUri).addHeader("DPoP", generateCustomToken("PUT",
        resourceUri)).addHeader("Authorization", "DPoP $accessToken").addHeader("content-type",
        "text/turtle").addHeader("Link",
        "<http://www.w3.org/ns/ldp#Resource>;rel=\"type\"").method("PUT", rBody).build()
  }

  public suspend fun regenerateRefreshToken() {
    val tokenUri = runBlocking { tokenStore.getTokenUri().first() }
    val refreshToken = runBlocking { tokenStore.getRefreshToken().first() }
    val clientId = runBlocking { tokenStore.getClientId().first() }
    val accessToken = runBlocking { tokenStore.getAccessToken().first() }
    val clientSecret = runBlocking { tokenStore.getClientSecret().first() }
    val formBody = FormBody.Builder()
    .addEncoded("grant_type", "refresh_token")
    .addEncoded("refresh_token", refreshToken)
    .addEncoded("client_id", clientId)
    .addEncoded("client_secret", clientSecret)
    .addEncoded("scope", "openid+offline_access+webid")
    .build()
    val request = Request.Builder().url(tokenUri).addHeader("DPoP", generateCustomToken("POST",
        tokenUri)).addHeader("Authorization", "DPoP $accessToken").addHeader("Content-Type",
        "application/x-www-form-urlencoded").method("POST", formBody).build()
    val client = OkHttpClient()
    val response = client.newCall(request).execute()
    val body = response.body!!.string()
    if (response.code in 400..499) {
      throw Error("could not refresh token sad")
    } else {
      val jsonResponse = JSONObject(body)
      val newAccessToken = jsonResponse.getString("access_token")
      val newIdToken = jsonResponse.getString("id_token")
      val newRefreshToken = jsonResponse.getString("refresh_token")
      tokenStore.setIdToken(newIdToken)
      tokenStore.setRefreshToken(newRefreshToken)
      tokenStore.setAccessToken(newAccessToken)
    }
  }

  public suspend fun checkStorage(storageUri: String, accessToken: String): String {
    val client = OkHttpClient()
    val request = generateGetRequest("$storageUri$ABSOLUTE_URI", accessToken)
    val response = client.newCall(request).execute()
    if (response.code in 400..499) {
      return ""
    }
    val body = response.body!!.string()
    return body
  }
}

public suspend fun getStorage(webId: String): String {
  val client = OkHttpClient()
  val webIdRequest = Request.Builder().url(webId.lowercase(Locale.ROOT)).build()
  val webIdResponse = client.newCall(webIdRequest).execute()
  val responseString = webIdResponse.body!!.string()
  val byteArray = responseString.toByteArray()
  val inStream = String(byteArray).byteInputStream()
  val m = ModelFactory.createDefaultModel().read(inStream, null, "TURTLE")
  val queryString = "SELECT ?o\n" +
                  "WHERE\n" +
                  "{ ?s <http://www.w3.org/ns/pim/space#storage> ?o }"
  val q = QueryFactory.create(queryString)
  var storage = ""
  try {
  val qexec = QueryExecutionFactory.create(q, m)
  val results = qexec.execSelect()
  while (results.hasNext()) {
  val soln = results.nextSolution()
  storage = soln.getResource("o").toString()
  break
  }
  } catch (e: Exception) {
  }
  return storage
}
