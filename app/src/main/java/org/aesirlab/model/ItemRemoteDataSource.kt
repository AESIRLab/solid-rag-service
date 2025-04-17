package org.aesirlab.model

import android.util.Log
import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.rdf.model.ModelFactory
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.aesirlab.model.Utilities.Companion.ABSOLUTE_URI
import org.aesirlab.model.Utilities.Companion.resourceToItem
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Calendar
import java.util.UUID

private const val TAG = "ItemRemoteDataSource"
class ItemRemoteDataSource(
    var webId: String? = null,
    var accessToken: String? = null,
    var expirationTime: Long? = null,
    var signingJwk: String? = null,
    private val externalScope: CoroutineScope
) {
    private val latestListMutex = Mutex()
    private var latestList: List<Item> = emptyList()

    private fun setLatestList(items: List<Item>) {
        latestList = items
    }

    suspend fun updateRemoteItemList(items: List<Item>) {
        setLatestList(items)
        if (webId != null && accessToken != null && accessTokenIsValid()) {
            val client = OkHttpClient()
            externalScope.launch {
                val storageUri = getStorage(webId!!)
                val model: Model = ModelFactory.createDefaultModel()
                val resourceUri = "${storageUri}$ABSOLUTE_URI"
                model.setNsPrefix("acp", Utilities.NS_ACP)
                model.setNsPrefix("acl", Utilities.NS_ACL)
                model.setNsPrefix("ldp", Utilities.NS_LDP)
                model.setNsPrefix("skos", Utilities.NS_SKOS)
                model.setNsPrefix("ci", Utilities.NS_Item)
                val ciName = model.createProperty(Utilities.NS_Item + "name")
                val ciAmount = model.createProperty(Utilities.NS_Item + "amount")
                latestList.forEach { ci ->
                    val id = ci.id
                    val mThingUri = model.createResource("$resourceUri#${id}")
                    mThingUri.addLiteral(ciName, ci.name)
                    mThingUri.addLiteral(ciAmount, ci.amount)
                }
                val bOutputStream = ByteArrayOutputStream()
                model.write(bOutputStream, "TURTLE", null)
                val rBody = bOutputStream.toByteArray().toRequestBody(null, 0, bOutputStream.size())
                val putRequest = generatePutRequest(signingJwk!!, accessToken!!, resourceUri, rBody)
                val putResponse = client.newCall(putRequest).execute()
                if (putResponse.code !in 200..299) {
                    throw Error("failed to update remote repository because: ${putResponse.code} ${putResponse.message}")
                } else {
                    Log.d(TAG, "successfully updated report repository at $resourceUri with code ${putResponse.code}")
                }
            }
        }
    }

    private fun accessTokenIsValid(): Boolean {
        return !(expirationTime == null || expirationTime!! < System.currentTimeMillis())
    }

    fun remoteAccessible(): Boolean {
        expirationTime?.let { Log.d(TAG, "${System.currentTimeMillis()} and expiring time ${expirationTime!!}") }
        return (accessToken != null &&
                webId != null &&
                expirationTime != null &&
                expirationTime!! > System.currentTimeMillis() &&
                signingJwk != null)
    }

    suspend fun fetchRemoteItemList(): List<Item> {
        return if (webId != null && accessToken != null && accessTokenIsValid()) {
            externalScope.async {
                val storageUri = getStorage(webId!!)
                val getRequest = generateGetRequest(signingJwk!!, "${storageUri}$ABSOLUTE_URI", accessToken!!)
                val client = OkHttpClient.Builder().build()
                val response = client.newCall(getRequest).execute()
                if (response.code in 200..299) {
                    val model: Model = ModelFactory.createDefaultModel()
                    model.setNsPrefix("acp", Utilities.NS_ACP)
                    model.setNsPrefix("acl", Utilities.NS_ACL)
                    model.setNsPrefix("ldp", Utilities.NS_LDP)
                    model.setNsPrefix("skos", Utilities.NS_SKOS)
                    model.setNsPrefix("ci", Utilities.NS_Item)
                    val ciName = model.createProperty(Utilities.NS_Item + "name")
                    val ciAmount = model.createProperty(Utilities.NS_Item + "amount")

                    val body = response.body!!.string().byteInputStream()
                    model.read(body, null, "TURTLE")
                    val res = model.listResourcesWithProperty(ciName)
                    val itemList = mutableListOf<Item>()
                    while (res.hasNext()) {
                        val nextResource = res.nextResource()
                        itemList.add(resourceToItem(nextResource))
                    }
                    latestListMutex.withLock {
                        latestList = itemList
                    }
                } else {
                    Log.d(TAG, "${response.code} ${response.body!!.string()}")
                }
                return@async this@ItemRemoteDataSource.latestList
            }.await()
        } else {
            return latestListMutex.withLock { this.latestList }
        }
    }
}


private fun generateCustomToken(signingJwk: String, method: String, uri: String): String {
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

fun generateGetRequest(signingJwk: String, resourceUri: String, accessToken: String, contentType: String = "text/turtle"): Request =
    Request.Builder().url(resourceUri).addHeader("DPoP", generateCustomToken(signingJwk, "GET",
        resourceUri)).addHeader("Authorization", "DPoP $accessToken").addHeader("Content-Type",
        contentType).addHeader("Link",
        "<http://www.w3.org/ns/ldp#Resource>;rel=\"type\"").method("GET", null).build()

fun generatePutRequest(signingJwk: String, accessToken: String, resourceUri: String, rBody: RequestBody, contentType: String = "text/turtle"): Request {
    return Request.Builder().url(resourceUri).addHeader("DPoP", generateCustomToken(signingJwk, "PUT",
        resourceUri)).addHeader("Authorization", "DPoP $accessToken").addHeader("content-type",
        contentType).addHeader("Link",
        "<http://www.w3.org/ns/ldp#Resource>;rel=\"type\"").method("PUT", rBody).build()
}

fun generatePostRequest(signingJwk: String, accessToken: String, resourceUri: String, rBody: RequestBody, contentType: String = "text/turtle"): Request {
    return Request.Builder().url(resourceUri).addHeader("DPoP", generateCustomToken(signingJwk, "POST",
        resourceUri)).addHeader("Authorization", "DPoP $accessToken").addHeader("content-type",
        contentType).addHeader("Link",
        "<http://www.w3.org/ns/ldp#Resource>;rel=\"type\"").method("PUT", rBody).build()
}

