package org.aesirlab.solidragapp.ui.screens

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.aesirlab.solidragapp.rag.MessageOwner
import org.aesirlab.solidragapp.service.RagService
import org.unifiedpush.android.connector.UnifiedPush.getAckDistributor
import org.unifiedpush.android.connector.UnifiedPush.getDistributors
import org.unifiedpush.android.connector.UnifiedPush.registerApp
import org.unifiedpush.android.connector.UnifiedPush.saveDistributor
import org.unifiedpush.android.connector.UnifiedPush.unregisterApp

private const val FILENAME = "food_calendar.txt"
private const val TAG = "RagServiceMainScreen"
@Composable
fun RagServiceMainScreen(
    accessToken: String,
    signingJwk: String
) {
    val mBound = remember {
        mutableStateOf(false)
    }
    val appCtx = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()
    var mService: Messenger? = remember {
        null
    }
    val mConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                mService = Messenger(service)
                mBound.value = true
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        val data = appCtx.assets.open(FILENAME)
                        val chunks = data.bufferedReader().use { it.readText() }
                        val newMessage = Message.obtain(null, 3, 0, 0)
                        val b = Bundle()
                        b.putString("chunks", chunks)
                        newMessage.data = b
                        mService!!.send(newMessage)
                    }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                mBound.value = false
            }
        }
    }

    val viewModel: RagServiceViewModel = viewModel(
        factory = RagServiceViewModel.Factory
    )
    val messages by viewModel.allMessageData.collectAsState()
    val mHandler = object: Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                2 -> {
                    val responseFromRag = msg.data.getString("response")
                    Log.d(TAG, responseFromRag!!)
                    viewModel.appendMessage(MessageOwner.Model, responseFromRag)
                }
            }
        }
    }
    val receiveMessenger = Messenger(mHandler)

    val composableScope = rememberCoroutineScope()

//    val client = createUnsafeOkHttpClient()

    val prompt = rememberSaveable {
        mutableStateOf("")
    }

    val registered = rememberSaveable {
        mutableStateOf(false)
    }
    val userDistrib = rememberSaveable {
        mutableStateOf(getAckDistributor(appCtx))
    }
    val internalReceiver: BroadcastReceiver? = null
    val expanded = rememberSaveable {
        mutableStateOf(true)
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_PAUSE) {
//        Log.d(TAG, "ONPAUSE paused")
        Log.d(TAG, "ONPAUSE $registered")
        if (registered.value) {
            unregisterApp(appCtx, instance = "test-instance")
        }
        internalReceiver?.let {
            appCtx.unregisterReceiver(it)
        }
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        if (!registered.value && appCtx != null) {

            if (userDistrib.value == null) {
                Toast.makeText(appCtx, "nothing picked", Toast.LENGTH_SHORT).show()
            } else {
                getAckDistributor(appCtx)?.let {
                        dist -> saveDistributor(appCtx, dist) }
                    .also { registerApp(appCtx) }
                registered.value = registered.value.not()
            }

        }
    }

    Scaffold {

        Column(modifier = Modifier.padding(it)) {
            Box {
                Column {
                    if (!mBound.value) {
                        Button(onClick = {
                            Intent(appCtx, RagService::class.java).also { intent ->
                                appCtx.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
                            }
                        }) {
                            Text("Initialize connection to Rag Service")
                        }
                    } else {
                        Button(onClick = {
                            appCtx.unbindService(mConnection)
                            mBound.value = false
                        }) {
                            Text("Unbind connection to Rag Service")
                        }
                    }
                    if (userDistrib.value == null) {
                        LaunchedEffect(Unit) {
                            delay(250)
                        }
                        val distributors = getDistributors(appCtx)
                        Log.d(TAG, distributors.toString())
                        DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
                            distributors.forEach { distributor ->
                                DropdownMenuItem(text = { Text(distributor) }, onClick = {
                                    userDistrib.value = distributor
                                    saveDistributor(appCtx, distributor)
                                    Log.d(TAG, userDistrib.value.toString())
                                })
                            }
                        }
                    }
                    if (!registered.value) {
                        Button(onClick = {
                            if (userDistrib.value == null) {
                                Toast.makeText(appCtx, "no value picked!", Toast.LENGTH_SHORT).show()
                            } else {
                                getAckDistributor(appCtx)?.let {
                                    dist -> saveDistributor(appCtx, dist) }
                                    .also { registerApp(appCtx) }

                                val broadcastIntent = Intent()
                                broadcastIntent.`package` = "registration"
                                broadcastIntent.action = "BEGIN"
                                appCtx.sendBroadcast(broadcastIntent)
                                registered.value = registered.value.not()
                            }
                        }) {
                            Text("Begin broadcasting")
                        }
                    } else {
                        Button(onClick = {
                            if (userDistrib.value == null) {
                                Toast.makeText(appCtx, "no value picked!", Toast.LENGTH_SHORT).show()
                            } else {
                                unregisterApp(appCtx)
                                registered.value = registered.value.not()
                            }
                        }) {
                            Text("Unregister broadcasting")
                        }
                    }

                    if (mService != null) {
                        OutlinedTextField(
                            value = prompt.value,
                            onValueChange = { input -> prompt.value = input },
                            enabled = mBound.value,
                            label = { Text(text = "Query:", style = TextStyle(fontSize = 18.sp)) }
                        )
                        if (prompt.value != "") {
                            Button(onClick = {
                                viewModel.appendMessage(MessageOwner.User, prompt.value)
                                runBlocking {

                                    withContext(Dispatchers.IO) {
                                        val newMessage = Message.obtain(null, 1, 0, 0)
                                        val b = Bundle()
                                        b.putString("accessToken", accessToken)
                                        b.putString("signingJwk", signingJwk)
                                        b.putString("prompt", prompt.value)
                                        newMessage.data = b
                                        newMessage.replyTo = receiveMessenger
                                        mService!!.send(newMessage)
                                    }
                                }
                            }) {
                                Text("Send prompt to service!")
                            }
                        }

                    } else {
                        Text(text = "Connect to the service to start prompting the LLM!")
                    }
                }

            }
            val lazyColumnListState = rememberLazyListState()
            LazyColumn(
                state = lazyColumnListState,
                modifier = Modifier
                    .weight(1f)
            ) {
                composableScope.launch { lazyColumnListState.animateScrollToItem(messages.size) }
                items(items = messages) { message ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(text = message.owner.name)
                        Text(text = message.message)
                    }
                }
            }
        }
    }
}
