package org.aesirlab.solidragapp.ui

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
import kotlinx.coroutines.flow.first
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

    suspend fun resetState() {
        viewModelScope.launch {
            repository.resetModel()
        }
    }

    suspend fun refreshAsync() {
        val currList = repository.allItemsAsFlow().first()
        for (item in currList) {
            println("currList: ${item.id}, ${item.name}, ${item.amount}")
        }
        val remoteList = mutableListOf<Item>()
        if (itemRemoteDataSource.remoteAccessible()) {
            remoteList += itemRemoteDataSource.fetchRemoteItemList()
            for (item in remoteList) {
                println("remoteList: ${item.id}, ${item.name}, ${item.amount}")
            }
        }
        val combinedList = remoteList + currList
        for (item in combinedList) {
            println("combinedList: ${item.id}, ${item.name}, ${item.amount}")
        }
        val newList = (remoteList + currList).distinctBy { it.id }
        for (item in newList) {
            println("newList: ${item.id}, ${item.name}, ${item.amount}")
        }
        repository.overwriteModelWithList(newList)
        _allItems.value = newList.sortedBy { it.id }
    }

    init {
        this.viewModelScope.launch {
            refreshAsync()
        }
    }

    fun remoteIsAvailable(): Boolean {
        return itemRemoteDataSource.remoteAccessible()
    }

    fun setRemoteRepositoryData(
        accessToken: String,
        signingJwk: String,
        webId: String,
        expirationTime: Long,
    ) {
        itemRemoteDataSource.signingJwk = signingJwk
        itemRemoteDataSource.webId = webId
        itemRemoteDataSource.expirationTime = expirationTime
        itemRemoteDataSource.accessToken = accessToken
    }

    suspend fun updateWebId(webId: String) {
        viewModelScope.launch {
            repository.insertWebId(webId)
            _allItems.value = repository.allItemsAsFlow().first().sortedBy { it.id }
        }
    }

    suspend fun insert(item: Item) {
        viewModelScope.launch {
            repository.insert(item)
            // not sure if this is the right way to do it...
            _allItems.value = repository.allItemsAsFlow().first().sortedBy { it.id }
        }
    }

    suspend fun insertMany(list: List<Item>) {
        viewModelScope.launch {
            repository.insertMany(list)
            _allItems.value = repository.allItemsAsFlow().first().sortedBy { it.id }
        }
    }

    suspend fun delete(item: Item) {
        viewModelScope.launch {
            repository.deleteByUri(item.id)
            _allItems.value = repository.allItemsAsFlow().first().sortedBy { it.id }
        }
    }

    suspend fun updateRemote(items: List<Item>) {
        viewModelScope.launch {
            itemRemoteDataSource.updateRemoteItemList(items)
        }
    }

    suspend fun update(item: Item, change: Int) {
        viewModelScope.launch {
            val currentList = _allItems.value
            val newList = currentList.map {
                if (it == item) {
                    val n = it.copy(id = it.id, name = it.name, amount = it.amount + change)
                    repository.update(n)
                    n
                } else {
                    it
                }
            }
            _allItems.value = newList.sortedBy { it.id }
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
