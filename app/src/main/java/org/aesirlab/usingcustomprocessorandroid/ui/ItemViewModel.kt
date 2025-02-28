package org.aesirlab.usingcustomprocessorandroid.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.aesirlab.model.Item
import org.skCompiler.generatedModel.ItemRepository

private const val TAG = "ItemViewModel"
class ItemViewModel(private val repository: ItemRepository): ViewModel() {

    private val _allItems: MutableStateFlow<List<Item>> = MutableStateFlow(listOf())
    val allItems: StateFlow<List<Item>> get() = _allItems

    init {
        _allItems.value = repository.allItems()
    }

    suspend fun insert(item: Item) {
        viewModelScope.launch {
            repository.insert(item)
            // not sure if this is the right way to do it...
            _allItems.value = repository.allItems()
        }
    }

    suspend fun insertMany(list: List<Item>) {
        viewModelScope.launch {
            repository.insertMany(list)
            _allItems.value = repository.allItems()
        }
    }

    suspend fun delete(item: Item) {
        viewModelScope.launch {
            repository.deleteByUri(item.id)
            _allItems.value = repository.allItems()
        }
    }

    suspend fun update(item: Item) {
        viewModelScope.launch {
            repository.update(item)
        }
    }
}

class ItemViewModelFactory(private val repository: ItemRepository): ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(ItemViewModel::class.java)) {
            ItemViewModel(repository) as T
        } else {
            throw IllegalArgumentException("ViewModel cannot be created due to class instantiation failure")
        }
    }
}