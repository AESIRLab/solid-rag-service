package org.aesirlab.model

import kotlin.String
import kotlin.collections.List
import kotlinx.coroutines.flow.Flow

public interface ItemDao {
  public fun getAllItemsAsFlow(): Flow<List<Item>>

  public fun getAllItems(): List<Item>

  public suspend fun update(item: Item)

  public suspend fun insert(item: Item)

  public suspend fun delete(uri: String)

  public fun getItemByIdAsFlow(id: String): Flow<Item>

  public fun updateWebId(webId: String)

  public suspend fun deleteAll()

  fun resetModel()

  suspend fun overwriteModelWithList(items: List<Item>)
}
