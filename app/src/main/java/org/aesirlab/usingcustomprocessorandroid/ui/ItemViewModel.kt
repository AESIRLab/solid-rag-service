package org.aesirlab.usingcustomprocessorandroid.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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


    public suspend fun refreshAsync() {
        val newList = mutableListOf<Item>()
        Log.d(TAG, itemRemoteDataSource.remoteAccessible().toString())
        if (itemRemoteDataSource.remoteAccessible()) {
            newList += itemRemoteDataSource.fetchRemoteItemList()
            Log.d(TAG, "len of newList: ${newList.size}")
            repository.resetModel()
            repository.insertMany(newList)
        }

        repository.allItemsAsFlow().collect { list ->
            newList += list
            _allItems.value = newList.distinct()
        }

    }

    init {
        this.viewModelScope.launch {
            refreshAsync()
        }
    }

    fun remoteIsAvailable(): Boolean {
        return itemRemoteDataSource.remoteAccessible()
    }

    suspend fun setRemoteRepositoryData(
        accessToken: String,
        signingJwk: String,
        webId: String,
        expirationTime: Long,
    ) {
        itemRemoteDataSource.signingJwk = signingJwk
        itemRemoteDataSource.webId = webId
        itemRemoteDataSource.expirationTime = expirationTime
        itemRemoteDataSource.accessToken = accessToken
        refreshAsync()
    }

    suspend fun updateWebId(webId: String) {
        viewModelScope.launch {
            repository.insertWebId(webId)
            repository.allItemsAsFlow().collect { list ->
                _allItems.value = list
                itemRemoteDataSource.setLatestList(list)
            }
        }
    }

    suspend fun insert(item: Item) {
        viewModelScope.launch {
            repository.insert(item)
            // not sure if this is the right way to do it...
            repository.allItemsAsFlow().collect { list ->
                _allItems.value = list
                itemRemoteDataSource.setLatestList(list)
//                itemRemoteDataSource.updateRemoteItemList()
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
                itemRemoteDataSource.setLatestList(_allItems.value)
            }
//            itemRemoteDataSource.updateRemoteItemList()
        }
    }

    suspend fun updateRemote() {
        viewModelScope.launch {
            itemRemoteDataSource.updateRemoteItemList()
        }
    }

    suspend fun update(item: Item) {
        viewModelScope.launch {
            repository.update(item)
            repository.allItemsAsFlow().collect { list ->
                _allItems.value = list
            }
            itemRemoteDataSource.setLatestList(_allItems.value)
//            itemRemoteDataSource.updateRemoteItemList()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as SolidMobileItemApplication)
                val itemRepository = application.repository
                val itemRemoteDataSource = ItemRemoteDataSource(externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Default))
                ItemViewModel(itemRepository, itemRemoteDataSource)
            }
        }
    }
}
