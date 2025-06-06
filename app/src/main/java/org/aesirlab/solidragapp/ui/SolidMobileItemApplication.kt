package org.aesirlab.solidragapp.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hp.hpl.jena.query.QueryExecutionFactory
import com.hp.hpl.jena.query.QueryFactory
import com.hp.hpl.jena.rdf.model.ModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.aesirlab.solidragapp.model.AuthTokenStore
import org.aesirlab.solidragapp.model.createUnsafeOkHttpClient
import org.aesirlab.solidragapp.model.generatePutRequest
import org.aesirlab.solidragapp.model.getStorage
import org.json.JSONObject
import java.util.UUID

val SAVE_RESOURCE_POD_URI = "https://ec2-18-119-19-244.us-east-2.compute.amazonaws.com/zach/profile/"
//val RESOURCE_URIS = arrayOf(
//    "https://storage.inrupt.com/9e06bd80-2380-46e0-9eaa-19c9d2baebb1/appOne/sample_content_1.txt",
//    "https://storage.inrupt.com/9e06bd80-2380-46e0-9eaa-19c9d2baebb1/appTwo/sample_content_2.txt",
//    "https://storage.inrupt.com/9e06bd80-2380-46e0-9eaa-19c9d2baebb1/appThree/sample_content_3.txt"
//)
//val SAVED_CHAT_URI_BASE = "https://storage.inrupt.com/9e06bd80-2380-46e0-9eaa-19c9d2baebb1/savedChats/"
class SolidMobileItemApplication: Application() {
    init {
        appInstance = this
    }

    companion object {
        lateinit var appInstance: SolidMobileItemApplication
    }
}

fun Context.broadcastMessageInfo(
    queryId: String,
    response: String
) {
    val broadcastIntent = Intent()
    broadcastIntent.`package` = this.packageName
    broadcastIntent.action = "MESSAGE"
    broadcastIntent.putExtra("queryId", queryId)
    broadcastIntent.putExtra("response", response)
    this.sendBroadcast(broadcastIntent)
}

private const val UP_CHANGE_URI = "https://ec2-18-119-19-244.us-east-2.compute.amazonaws.com/zach/up_notifications/"
fun Context.updateRegistrationInfo(endpoint: String) {
    val broadcastIntent = Intent()
    broadcastIntent.`package` = this.packageName
    broadcastIntent.action = "UPDATE"
    this.sendBroadcast(broadcastIntent)
    val tokenStore = AuthTokenStore(this.applicationContext)
    val accessToken = runBlocking { tokenStore.getAccessToken().first() }
    val signingJwk = runBlocking { tokenStore.getSigner().first() }
    signingJwk.ifEmpty { return }
    val endpointChangeUri = "$UP_CHANGE_URI${UUID.randomUUID()}.json"
    val json = JSONObject()
    json.put("endpoint", endpoint)
    val body = json.toString().toRequestBody("application/json".toMediaType())
    val request = generatePutRequest(accessToken, endpointChangeUri, body, signingJwk, "application/json")
    runBlocking {
        withContext(Dispatchers.IO) {
            val response = createUnsafeOkHttpClient().newCall(request).execute()
            val responseBody = response.body!!.string()
            Log.d("context updatereginfo", responseBody)
        }
    }
}

