package org.aesirlab.usingcustomprocessorandroid.service

import android.app.Service
import android.content.Intent
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.aesirlab.usingcustomprocessorandroid.rag.RagPipeline
import org.aesirlab.usingcustomprocessorandroid.ui.SolidMobileItemApplication
import java.util.concurrent.Executors

private const val MSG_PROMPT = 1
private const val MSG_RESPONSE = 2
private const val MSG_MEMORIZE = 3
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
            messenger = Messenger(QueryHandler(ragPipeline!!, scope))
        }
        return messenger.binder
    }

}

private class QueryHandler(val ragPipeline: RagPipeline, val scope: CoroutineScope): Handler(Looper.getMainLooper()) {
    override fun handleMessage(msg: Message) {
        when (msg.what) {
            MSG_PROMPT -> {
                val newMessage = Message.obtain(null, MSG_RESPONSE, 0, 0)
                val prompt = msg.data.getString("prompt")
                val b = Bundle()
                if (prompt != null) {
                    runBlocking {
//                        val mutableResponse = ""
                        val totalResponse = ragPipeline.generateResponse(prompt) { _, _ ->
//                            mutableResponse.plus(response)
                        }
                        Log.d(TAG, "totalResponse $totalResponse")
                        b.putString("response", totalResponse)
                    }
                } else {
                    b.putString("response", "you must provide a 'prompt' extra in your message")
                }
                newMessage.data = b
                try {
                    val replyTo = msg.replyTo
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
            else -> super.handleMessage(msg)
        }
    }

    override fun dispatchMessage(msg: Message) {
        super.dispatchMessage(msg)
    }
}
