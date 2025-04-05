package org.aesirlab.usingcustomprocessorandroid

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.Messenger
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.aesirlab.usingcustomprocessorandroid.rag.RagPipeline
import org.aesirlab.usingcustomprocessorandroid.service.RAGService
import org.aesirlab.usingcustomprocessorandroid.ui.App
import org.aesirlab.usingcustomprocessorandroid.ui.theme.UsingCustomProcessorAndroidTheme
const val REDIRECT_URI = "org.aesirlab.customprocessor://app/callback"


class MainActivity : ComponentActivity() {
    private var mService: Messenger? = null
    private var bound = false
    private lateinit var ragPipeline: RagPipeline

//    private val mConnection = object: ServiceConnection {
//        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//            if (service != null) {
//                mService = Messenger(service)
//                bound = true
//            }
//        }
//
//        override fun onServiceDisconnected(name: ComponentName?) {
//            mService = null
//            bound = false
//        }
//    }

    override fun onStart() {
        super.onStart()
//        Intent(this, RAGService::class.java).also { intent ->
//            bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
//        }
    }

//    override fun onStop() {
//        super.onStop()
//        if (bound) {
//            unbindService(mConnection)
//            bound = false
//        }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ragPipeline = RagPipeline(application = application)
        setContent {
            UsingCustomProcessorAndroidTheme {
                App(ragPipeline)
            }
        }
    }
}
