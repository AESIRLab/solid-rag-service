package org.aesirlab.usingcustomprocessorandroid.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.aesirlab.model.Item
import org.aesirlab.model.ItemRemoteDataSource
import org.aesirlab.model.ItemRepository


private const val TAG = "ItemViewModel"
class ItemViewModel(
    private val repository: ItemRepository,
    private val itemRemoteDataSource: ItemRemoteDataSource
): ViewModel() {

    private var _allItems: MutableStateFlow<List<Item>> = MutableStateFlow(listOf())
    val allItems: StateFlow<List<Item>> get() = _allItems

    init {
        this.viewModelScope.launch {
            val newList = mutableListOf<Item>()
            if (itemRemoteDataSource.remoteAccessible()) {
                newList += itemRemoteDataSource.fetchRemoteItemList()
            }
            repository.allItemsAsFlow().collect { list ->
                newList += list
            }
            _allItems.value = newList.distinct()
        }
    }

    suspend fun setRemoteRepositoryData(
        accessToken: String,
        signingJwk: String,
        webId: String,
        expirationTime: Long,
    ) {
        viewModelScope.launch {
            itemRemoteDataSource.signingJwk = signingJwk
            itemRemoteDataSource.webId = webId
            itemRemoteDataSource.expirationTime = expirationTime
            itemRemoteDataSource.accessToken = accessToken
        }
    }

    suspend fun updateWebId(webId: String) {
        viewModelScope.launch {
            repository.insertWebId(webId)
            repository.allItemsAsFlow().collect { list ->
                _allItems.value = list
            }
        }
    }

    suspend fun insert(item: Item) {
        viewModelScope.launch {
            repository.insert(item)
            // not sure if this is the right way to do it...
            repository.allItemsAsFlow().collect { list ->
                _allItems.value = list
            }
        }
    }

    suspend fun insertMany(list: List<Item>) {
        viewModelScope.launch {
            repository.insertMany(list)
            repository.allItemsAsFlow().collect { list ->
                _allItems.value = list
            }
        }
    }

    suspend fun delete(item: Item) {
        viewModelScope.launch {
            repository.deleteByUri(item.id)
            repository.allItemsAsFlow().collect { list ->
                _allItems.value = list
            }
        }
    }

    suspend fun update(item: Item) {
        viewModelScope.launch {
            repository.update(item)
            repository.allItemsAsFlow().collect { list ->
                _allItems.value = list
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as SolidMobileItemApplication)
                val itemRepository = application.repository
                val itemRemoteDataSource = ItemRemoteDataSource(ioDispatcher = Dispatchers.IO)
                ItemViewModel(itemRepository, itemRemoteDataSource)
            }
        }
    }
}
