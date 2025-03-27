package org.aesirlab.model

import androidx.`annotation`.WorkerThread
import kotlin.String
import kotlin.collections.List
import kotlinx.coroutines.flow.Flow

public class ItemRepository(
  private val itemDao: ItemDao
) {
  public fun allItemsAsFlow(): Flow<List<Item>> = itemDao.getAllItemsAsFlow()

//  public fun allItems(): List<Item> = itemDao.getAllItems()

  @WorkerThread
  public suspend fun insertMany(itemList: List<Item>) {
    itemList.forEach {
      itemDao.insert(it)
    }
  }

  fun resetModel() {
    itemDao.resetModel()
  }

  @WorkerThread suspend fun insertWebId(webId: String) {
    itemDao.updateWebId(webId)
  }

  @WorkerThread
  public suspend fun update(item: Item) {
    itemDao.update(item)
  }

  @WorkerThread
  public suspend fun insert(item: Item) {
    if (item.id == "") {
      item.id = java.util.UUID.randomUUID().toString()
    }
    itemDao.insert(item)
  }

  @WorkerThread
  public suspend fun deleteByUri(uri: String) {
    itemDao.delete(uri)
  }

  public fun getItemLiveData(uri: String): Flow<Item> = itemDao.getItemByIdAsFlow(uri)
}
