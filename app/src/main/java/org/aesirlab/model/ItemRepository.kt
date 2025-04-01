package org.aesirlab.model

import androidx.`annotation`.WorkerThread
import kotlin.String
import kotlin.collections.List
import kotlinx.coroutines.flow.Flow

class ItemRepository(
  private val itemDao: ItemDao
) {
  fun allItemsAsFlow(): Flow<List<Item>> = itemDao.getAllItemsAsFlow()

  @WorkerThread
  suspend fun insertMany(itemList: List<Item>) {
    itemList.forEach {
      itemDao.insert(it)
    }
  }

  fun insertWebId(webId: String) {
    itemDao.updateWebId(webId)
  }

  @WorkerThread
  suspend fun update(item: Item) {
    itemDao.update(item)
  }

  @WorkerThread
  suspend fun insert(item: Item) {
    itemDao.insert(item)
  }

  @WorkerThread
  suspend fun deleteByUri(uri: String) {
    itemDao.delete(uri)
  }

  @WorkerThread
  suspend fun deleteAll() {
    itemDao.deleteAll()
  }

  fun resetModel() {
    itemDao.resetModel()
  }

  @WorkerThread
  suspend fun overwriteModelWithList(items: List<Item>) {
    itemDao.overwriteModelWithList(items)
  }
}
