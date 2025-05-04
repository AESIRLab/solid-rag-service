package org.aesirlab.solidragapp.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.RequestBody.Companion.toRequestBody
import org.aesirlab.mylibrary.sharedfunctions.createUnsafeOkHttpClient
import org.aesirlab.solidragapp.model.generateGetRequest
import org.aesirlab.solidragapp.model.generatePutRequest
import org.aesirlab.solidragapp.rag.ChatViewModel
import org.aesirlab.solidragapp.rag.MessageData
import org.aesirlab.solidragapp.rag.MessageOwner
import org.aesirlab.solidragapp.ui.RESOURCE_URIS
import org.aesirlab.solidragapp.ui.SAVED_CHAT_URI_BASE
import kotlin.math.sign


private const val TAG = "RagMainScreen"

@Composable
        /** Displays a standard divider. */
fun StandardDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 3.dp), thickness = 1.dp)
}

@Composable
fun SaveToPodButton(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = { onClick() },
        icon = { Icon(Icons.AutoMirrored.Filled.Send, "Send to Pod") },
        text = { Text(text = "Save Conversation to Pod") })
}

@ExperimentalMaterial3Api
@Composable
        /**
         * Displays a chat view with a top bar, a dropdown menu, a message list and a message sending box.
         */
fun RagMainScreen(
    accessToken: String,
    signingJwk: String,
) {
    val localFocusManager = LocalFocusManager.current
    val composableScope = rememberCoroutineScope()
    val appCtx = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()
    val client = createUnsafeOkHttpClient()
    val viewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.Factory
    )

    LaunchedEffect(key1 = Unit) {
        val requests = RESOURCE_URIS.map { generateGetRequest(signingJwk =  signingJwk, resourceUri = it,  accessToken = accessToken) }

        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                for (request in requests) {
                    val response = client.newCall(request).execute()
                    if (response.code in 200..<300) {
                        viewModel.memorizeChunks(response.body!!.byteStream())
                    } else {
                        viewModel.memorizeChunks("<chunk_splitter>I am 31 years old and work as a janitor.".byteInputStream())
                    }
                }
            }
        }
    }

    val messages by viewModel.allMessageData.collectAsState()
    Scaffold(

        floatingActionButton = {
            SaveToPodButton {
                val userMessages = messages.map { md -> md.owner == MessageOwner.User }
                val botMessages = messages.map { md -> md.owner == MessageOwner.Model }
                val userMessagesString = userMessages.joinToString(separator = "<chunk_splitter>", prefix = "<chunk_splitter>")
                val rBody = userMessagesString.toByteArray().toRequestBody(null, 0, userMessagesString.toByteArray().size)
                val putRequest = generatePutRequest(
                    signingJwk = signingJwk,
                    accessToken = accessToken,
                    resourceUri = "$SAVED_CHAT_URI_BASE${System.currentTimeMillis()}.txt",
                    rBody = rBody,
                    contentType = "text/plain")
                val call = client.newCall(putRequest)
                viewModel.resetState()
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        val response = call.execute()
                        if (response.code in 200..<300) {
                            Log.d(TAG, "successfully placed saved user messages at $SAVED_CHAT_URI_BASE${System.currentTimeMillis()}.txt")
                        } else {
                            Log.d(TAG, "failed with ${response.code}")
                        }
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Start,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "On-device RAG pipeline", color = MaterialTheme.colorScheme.onPrimary)
                },
                colors =
                TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
            )
        },
        modifier =
        Modifier.pointerInput(Unit) { detectTapGestures(onTap = { localFocusManager.clearFocus() }) },
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 8.dp)) {
            val lazyColumnListState = rememberLazyListState()

            Text(
                text = viewModel.statistics.value,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = 5.dp),
            )

            LazyColumn(
                modifier = Modifier.weight(9f),
                verticalArrangement = Arrangement.spacedBy(5.dp, alignment = Alignment.Bottom),
                state = lazyColumnListState,
            ) {
                composableScope.launch { lazyColumnListState.animateScrollToItem(messages.size) }
                items(items = messages) { message -> MessageView(messageData = message) }
            }
            StandardDivider()
            SendMessageView(calledFunc = { text ->
                if (text.isEmpty()) {
                    Toast
                        .makeText(appCtx, "Item name must not be empty!", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    viewModel.requestResponse(text)
                }
            })
        }
    }
}

@Composable
        /** Displays a single message. */
fun MessageView(messageData: MessageData) {
    SelectionContainer {
        val fromModel: Boolean = messageData.owner == MessageOwner.Model
        val color =
            if (fromModel) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            }
        val backgroundColor =
            if (fromModel) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        val textAlign = if (fromModel) TextAlign.Left else TextAlign.Right
        val horizontalArrangement = if (fromModel) Arrangement.Start else Arrangement.End

        Row(horizontalArrangement = horizontalArrangement, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = messageData.message,
                textAlign = textAlign,
                color = color,
                style = TextStyle(fontSize = 18.sp),
                modifier =
                Modifier
                    .wrapContentWidth()
                    .background(color = backgroundColor, shape = RoundedCornerShape(5.dp))
                    .padding(3.dp)
                    .widthIn(max = 300.dp),
            )
        }
    }
}

@ExperimentalMaterial3Api
@Composable
        /** Displays a message sending box. */
fun SendMessageView(
    calledFunc: (String) -> Unit
) {
    val localFocusManager = LocalFocusManager.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(IntrinsicSize.Max)
            .fillMaxWidth()
            .padding(bottom = 5.dp),
    ) {
        val coroutineScope = rememberCoroutineScope()
        var text by rememberSaveable { mutableStateOf("") }
        OutlinedTextField(
            value = text,
            textStyle = TextStyle(fontSize = 18.sp),
            onValueChange = { text = it },
            label = { Text(text = "Query:", style = TextStyle(fontSize = 18.sp)) },
            modifier = Modifier.weight(9f),
        )
        IconButton(
            onClick = {
                localFocusManager.clearFocus()
                coroutineScope.launch {
                    calledFunc(text)
                    text = ""
                }
            },
            modifier = Modifier
                .aspectRatio(1f)
                .weight(1f),
            enabled = (text != ""),
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Commit message")
        }
    }
}