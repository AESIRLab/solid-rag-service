package org.aesirlab.usingcustomprocessorandroid.rag

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.aesirlab.usingcustomprocessorandroid.ui.SolidMobileItemApplication
import java.io.InputStream
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/** Instantiates the View Model for the chat view. */
class ChatViewModel(private val ragPipeline: RagPipeline) : ViewModel() {
    internal val messages = emptyList<MessageData>().toMutableStateList()
    internal val statistics = mutableStateOf("")
    private val executorService = Executors.newSingleThreadExecutor()
    private val backgroundExecutor: Executor = Executors.newSingleThreadExecutor()

    fun memorizeChunks(data: InputStream) {
        ragPipeline.memorizeChunks(data)
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    fun requestResponse(prompt: String) {
        appendMessage(MessageOwner.User, prompt)
        executorService.submit { viewModelScope.launch { requestResponseFromModel(prompt) } }
    }

    fun resetState() {
        statistics.value = ""
        messages.clear()
        ragPipeline.forget()
    }

    private suspend fun requestResponseFromModel(prompt: String) =
        withContext(backgroundExecutor.asCoroutineDispatcher()) {
            ragPipeline.generateResponse(
                prompt,
            ) { response, _ -> updateLastMessage(MessageOwner.Model, response.text) }

        }

    private fun appendMessage(role: MessageOwner, message: String) {
        messages.add(MessageData(role, message))
    }

    private fun updateLastMessage(role: MessageOwner, message: String) {
        if (messages.isNotEmpty() && messages[messages.lastIndex].owner == role) {
            messages[messages.lastIndex] = MessageData(role, message)
        } else {
            appendMessage(role, message)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as SolidMobileItemApplication)
                val ragPipeline = RagPipeline(application = application)
                ChatViewModel(ragPipeline)
            }
        }
    }

}

enum class MessageOwner {
    User,
    Model,
}

data class MessageData(val owner: MessageOwner, val message: String)