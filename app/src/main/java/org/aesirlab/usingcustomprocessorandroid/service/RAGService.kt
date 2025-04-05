package org.aesirlab.usingcustomprocessorandroid.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import kotlinx.coroutines.runBlocking
import org.aesirlab.usingcustomprocessorandroid.rag.RagPipeline

private const val MSG_PROMPT = 1
private const val MSG_RESPONSE = 2
private const val TAG = "RAGService"
class RAGService: Service() {
    private var messenger: Messenger? = null
    private var ragPipeline: RagPipeline? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        if (ragPipeline == null) {
//            this.ragPipeline = RagPipeline(applicationContext)
//        }
        return startId
    }

    override fun onBind(intent: Intent?): IBinder? {
        if (messenger == null) {
            synchronized(RAGService::class) {
                messenger = Messenger(QueryHandler(ragPipeline))
            }
        }
        return messenger!!.binder
    }

}

private class QueryHandler(val ragPipeline: RagPipeline?): Handler(Looper.getMainLooper()) {
    override fun handleMessage(msg: Message) {
        when (msg.what) {
            MSG_PROMPT -> {
                val newMessage = Message.obtain(null, MSG_RESPONSE, 0, 0)
                val prompt = msg.data.getString("prompt")
                val b = Bundle()
                if (prompt != null) {
//                    runBlocking {
//                        ragPipeline.generateResponse(prompt) { response, _ ->
//                            b.putString("response", response.text)
//                        }
//                    }
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
            else -> super.handleMessage(msg)
        }
    }

    override fun dispatchMessage(msg: Message) {
        super.dispatchMessage(msg)
    }
}
