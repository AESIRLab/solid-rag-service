package org.aesirlab.solidragapp.ui.screens

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.aesirlab.solidragapp.rag.MessageData
import org.aesirlab.solidragapp.rag.MessageOwner

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