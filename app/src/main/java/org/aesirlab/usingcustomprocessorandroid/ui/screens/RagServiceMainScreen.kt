package org.aesirlab.usingcustomprocessorandroid.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.aesirlab.usingcustomprocessorandroid.rag.RagPipeline
import org.aesirlab.usingcustomprocessorandroid.ui.SolidMobileItemApplication
import java.io.OutputStreamWriter
import kotlin.system.exitProcess


private const val TAG = "RagServiceMainScreen"
@Composable
fun RagServiceMainScreen(
) {
    val coroutineScope = rememberCoroutineScope()
    val applicationContext = LocalContext.current.applicationContext
    val ragPipeline = RagPipeline(applicationContext as SolidMobileItemApplication)
//    val files = listOf("sentence_split_2pages.txt", "University_of_Edinburgh.txt", "Emma_Darwin.txt", "Geological_Society_of_London.txt", "John_Stevens_Henslow.txt")
    val files = listOf("sentence_split_6pages.txt")
    val startIndexTime = System.currentTimeMillis()
    for (file in files) {
        val inStream = applicationContext.assets.open(file)
        ragPipeline.memorizeChunks(inStream)
    }
    val totalIndexTime = (System.currentTimeMillis() - startIndexTime)
    val queriesFileName = "six_page_questions_queries.txt"
    val queries = applicationContext.assets.open(queriesFileName).bufferedReader().use { reader ->
        reader.readLines()
    }
    val collectedResponse = remember { mutableStateOf("") }
    val outputStreamWriter = OutputStreamWriter(
        applicationContext.openFileOutput(
            "android_vector_sentence_split_6pagequestions.csv",
            Context.MODE_PRIVATE
        )
    )
    Scaffold {
        Button(onClick = {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    queries.forEach { query ->
                        val startTime = System.currentTimeMillis()
                        ragPipeline.generateResponse(query) { response, done ->
                            if (done) {
                                Log.d(TAG, "finished!")
                                outputStreamWriter.write("$query||${(System.currentTimeMillis() - startTime) / 1000}||${collectedResponse.value}||||\n")
                                collectedResponse.value = ""
                            } else {
                                collectedResponse.value = response.text
                            }
                        }
                    }
                    outputStreamWriter.close()
                    Log.d(TAG, "finished all queries! index took $totalIndexTime milliseconds!")
                    exitProcess(0)
                }
            }
        }, modifier = Modifier.padding(it)) {
            Text("Execute queries!")
        }
    }
}
