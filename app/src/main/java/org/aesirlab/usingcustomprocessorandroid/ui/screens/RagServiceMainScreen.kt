package org.aesirlab.usingcustomprocessorandroid.ui.screens

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.ViewModelFactoryDsl
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.aesirlab.model.generateGetRequest
import org.aesirlab.mylibrary.sharedfunctions.createUnsafeOkHttpClient
import org.aesirlab.usingcustomprocessorandroid.rag.MessageData
import org.aesirlab.usingcustomprocessorandroid.rag.MessageOwner
import org.aesirlab.usingcustomprocessorandroid.service.RagService
import org.aesirlab.usingcustomprocessorandroid.ui.ItemViewModel

private const val TAG = "RagServiceMainScreen"
@Composable
fun RagServiceMainScreen(
    accessToken: String,
    signingJwk: String
) {
    val mBound = remember {
        mutableStateOf(false)
    }
    var mService: Messenger? = remember {
        null
    }
    val mConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                mService = Messenger(service)
                mBound.value = true
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
    val appCtx = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()
    val client = createUnsafeOkHttpClient()

    val prompt = rememberSaveable {
        mutableStateOf("")
    }

    LaunchedEffect(key1 = Unit) {
        val requests = RESOURCE_URIS.map { generateGetRequest(signingJwk, it, accessToken) }

        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                if (mBound.value) {
                    for (request in requests) {
                        val response = client.newCall(request).execute()
                        if (response.code in 200..<300) {
//                            viewModel.memorizeChunks(response.body!!.byteStream())
                            val newMessage = Message.obtain(null, 3, 0, 0)
                            val b = Bundle()
                            b.putString("chunks", response.body!!.string())
                            newMessage.data = b
                            mService!!.send(newMessage)
                        } else {
//                            viewModel.memorizeChunks("<chunk_splitter>I am 31 years old and work as a janitor.".byteInputStream())
                            val newMessage = Message.obtain(null, 3, 0, 0)
                            val b = Bundle()
                            b.putString("chunks", "<chunk_splitter>I am 31 years old and work as a janitor.")
                            newMessage.data = b
                            mService!!.send(newMessage)
                        }
                    }
                }
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
