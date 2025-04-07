package org.aesirlab.usingcustomprocessorandroid.ui.screens

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.aesirlab.usingcustomprocessorandroid.rag.ChatViewModel
import org.aesirlab.usingcustomprocessorandroid.rag.MessageData
import org.aesirlab.usingcustomprocessorandroid.rag.MessageOwner
import org.aesirlab.usingcustomprocessorandroid.rag.RagPipeline
import org.aesirlab.usingcustomprocessorandroid.service.RagService
import org.aesirlab.usingcustomprocessorandroid.ui.SolidMobileItemApplication
import java.io.InputStream
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class RagServiceViewModel: ViewModel() {

    private val statistics = mutableStateOf("")
    private var _allMessages: MutableStateFlow<List<MessageData>> = MutableStateFlow(listOf())
    val allMessageData: StateFlow<List<MessageData>> get() = _allMessages

    fun resetState() {
        statistics.value = ""
        _allMessages.value = listOf()
    }

    fun appendMessage(role: MessageOwner, message: String) {
        val cList = _allMessages.value
        _allMessages.value = cList + MessageData(role, message)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
//                val application = (this[APPLICATION_KEY] as SolidMobileItemApplication)
                RagServiceViewModel()
            }
        }
    }

}