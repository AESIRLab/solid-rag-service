package org.aesirlab.usingcustomprocessorandroid.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.RequestBody.Companion.toRequestBody
import org.aesirlab.model.generateGetRequest
import org.aesirlab.model.generatePutRequest
import org.aesirlab.mylibrary.sharedfunctions.createUnsafeOkHttpClient
import org.aesirlab.usingcustomprocessorandroid.rag.RagPipeline

private val RESOURCE_URIS = arrayOf(
    "https://storage.inrupt.com/9e06bd80-2380-46e0-9eaa-19c9d2baebb1/appOne/sample_content_1.txt",
//    "https://storage.inrupt.com/9e06bd80-2380-46e0-9eaa-19c9d2baebb1/appTwo/sample_content_2.txt",
//    "https://storage.inrupt.com/9e06bd80-2380-46e0-9eaa-19c9d2baebb1/appThree/sample_content_3.txt"
)
private const val TAG = "RagMainScreen"
@Composable
fun RagMainScreen(
    ragPipeline: RagPipeline,
    accessToken: String,
    signingJwk: String
) {
    val prompt = remember {
        mutableStateOf("")
    }
    val appCtx = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()
    val responseField = remember {
        mutableStateOf("")
    }
    val client = createUnsafeOkHttpClient()

    LaunchedEffect(key1 = Unit) {
        val requests = RESOURCE_URIS.map { generateGetRequest(signingJwk, it, accessToken) }

        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                for (request in requests) {
                    val response = client.newCall(request).execute()
                    if (response.code in 200..<300) {
                        ragPipeline.memorizeChunks(response.body!!.byteStream())
                    } else {
                        ragPipeline.memorizeChunks("<chunk_splitter>I am 31 years old and work as a janitor.".byteInputStream())
                    }
                }
            }
        }
    }

    Column {
        Row {
            TextField(value = prompt.value, onValueChange = { prompt.value = it })
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            Button(onClick = {
                if (prompt.value.isEmpty()) {
                    Toast
                        .makeText(appCtx, "Item name must not be empty!", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    coroutineScope.launch {
                        responseField.value = "Waiting..."
                        Log.d(TAG, prompt.value)
                        ragPipeline.memorizeChunks(prompt.value.byteInputStream())
                        val totalRecall = ragPipeline.generateResponse(prompt.value) { response, _ ->
                            val responseText = response.text
                            responseField.value = responseText
                        }
                        Log.d(TAG, totalRecall)
                        prompt.value = ""
                    }
                }

            }) {
                Text("Send Prompt!")
            }
        }
        Text(text = responseField.value)
    }
}