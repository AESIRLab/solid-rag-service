package org.aesirlab.solidragapp.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.aesirlab.solidragapp.model.createUnsafeOkHttpClient
import org.aesirlab.solidragapp.model.generatePutRequest
import org.aesirlab.solidragapp.rag.RagPipeline
import org.aesirlab.solidragapp.ui.SAVE_RESOURCE_POD_URI
import org.aesirlab.solidragapp.ui.SolidMobileItemApplication
import org.json.JSONObject
import java.util.concurrent.Executors

const val MSG_PROMPT = 1
const val MSG_RESPONSE = 2
const val MSG_MEMORIZE = 3
const val MSG_UP_RESPONSE = 4
private const val TAG = "RAGService"
class RagService: Service() {
    private lateinit var messenger: Messenger
    private var ragPipeline: RagPipeline? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ragPipeline == null) {
            this.ragPipeline = RagPipeline(applicationContext as SolidMobileItemApplication)
        }
        return startId
    }

    override fun onBind(intent: Intent?): IBinder? {
        synchronized(RagService::class) {
            if (ragPipeline == null) {
                this.ragPipeline = RagPipeline(applicationContext as SolidMobileItemApplication)
            }
            messenger = Messenger(QueryHandler(applicationContext, ragPipeline!!, scope))
        }
        return messenger.binder
    }

}

@SuppressLint("UnspecifiedRegisterReceiverFlag")
private class QueryHandler(val context: Context, val ragPipeline: RagPipeline, val scope: CoroutineScope): Handler(Looper.getMainLooper()) {
    // this is a map of the query ids based on the messenger which sent a query
    // this is to receive responses from unifiedpush and dispatch them properly
    private var queryIdCounter = 314
    private val queryIdCounterLock = Mutex()
    private val queryIdsToMessenger = mutableMapOf<Int, Messenger>()
    private val checkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent!!.action) {
                "MESSAGE" -> {
                    // build a message
                    val newMessage = Message.obtain(null, MSG_RESPONSE, 0, 0)
                    // create a new bundle
                    val b = Bundle()
                    // add the generated response from the bundle
                    val generatedText = intent.getStringExtra("response")!!
                    b.putString("response", generatedText)
                    newMessage.data = b
                    // get the query id
                    val queryId = intent.getIntExtra("queryId", -1)
                    // this SHOULD be in here
                    val replyTo = queryIdsToMessenger[queryId]!!
                    try {
                        replyTo.send(newMessage)
                    } catch (e: RemoteException) {
                        Log.e(TAG, "failed to send reply")
                    }
                }
            }
        }
    }

    init {
        Log.d(TAG, "service initializer")
        val intentFilter = IntentFilter().apply {
            addAction("UPDATE")
            addAction("MESSAGE")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(checkReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
            Log.d(TAG, "registered receiver!")
        } else {
            context.registerReceiver(checkReceiver, intentFilter)
            Log.d(TAG, "registered receiver!")
        }
    }

    // TODO: i need to check if wifi/http is available and if it is then i pick to generate a response
    override fun handleMessage(msg: Message) {
        when (msg.what) {
            MSG_PROMPT -> {
                // get the handler we are replying to
                val replyTo = msg.replyTo
                val queryId = queryIdCounter
                // lock the counter and update the map
                synchronized(queryIdCounterLock) {
                    queryIdsToMessenger[queryIdCounter] = replyTo
                    queryIdCounter += 1
                }
                // create a response message
                val newMessage = Message.obtain(null, MSG_RESPONSE, 0, 0)
                // get the prompt
                val prompt = msg.data.getString("prompt")
                // get the tokens for generating the put request
                val accessToken = msg.data.getString("accessToken")
                val signingJwk = msg.data.getString("signingJwk")
                val b = Bundle()
                val client = createUnsafeOkHttpClient()
                val jsonBody = JSONObject()
                jsonBody.put("query", prompt)
                jsonBody.put("query_id", queryId)
                val rBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
                val resourceUri = "${SAVE_RESOURCE_POD_URI}test$queryId.json"
                if (prompt == null) {
                    b.putString("response", "you must provide a 'prompt' extra in your message")
                } else {
                    runBlocking {
                        withContext(Dispatchers.IO) {

                            val request = generatePutRequest(
                                signingJwk = signingJwk!!,
                                accessToken = accessToken!!,
                                resourceUri = resourceUri,
                                rBody = rBody,
                                contentType = "application/json"
                            )
                            val response = client.newCall(request).execute()
                            Log.d(TAG, response.code.toString())
                            Log.d(TAG, response.body!!.string())
                            if (response.code !in 200..<300) {
                                val totalResponse = ragPipeline.generateResponse(prompt) { _, _ ->
//                            mutableResponse.plus(response)
                                }
                                Log.d(TAG, "totalResponse $totalResponse")
                                b.putString("response", totalResponse)
                            } else {
                                b.putString("response", "sent to remote processing, expect a response to your prompt soon!")
                            }
                        }
                    }
                }
                newMessage.data = b
                try {
                    replyTo.send(newMessage)
                } catch (e: RemoteException) {
                    Log.e(TAG, "failed to send reply")
                }
            }
            MSG_MEMORIZE -> {
                val chunks = msg.data.getString("chunks")
                if (chunks != null) {
                    val executor = Executors.newSingleThreadExecutor()
                    executor.submit { scope.launch { ragPipeline.memorizeChunks(chunks.byteInputStream()) } }
                }
            }
            MSG_UP_RESPONSE -> {
                // build a message
                val newMessage = Message.obtain(null, MSG_RESPONSE, 0, 0)
                // create a new bundle
                val b = Bundle()
                // add the generated response from the bundle
                val generatedText = msg.data.getString("generated_text")
                b.putString("response", generatedText)
                newMessage.data = b
                // get the query id
                val queryId = msg.data.getInt("query_id")
                // this SHOULD be in here
                val replyTo = queryIdsToMessenger[queryId]!!
                try {
                    replyTo.send(newMessage)
                } catch (e: RemoteException) {
                    Log.e(TAG, "failed to send reply")
                }
            }
            else -> super.handleMessage(msg)
        }
    }

    override fun dispatchMessage(msg: Message) {
        super.dispatchMessage(msg)
    }
}
