package org.aesirlab.model

import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.rdf.model.ModelFactory
import com.hp.hpl.jena.rdf.model.ResourceFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.aesirlab.usingcustomprocessorandroid.model.Utilities
import org.aesirlab.usingcustomprocessorandroid.model.Utilities.Companion.resourceToItem
import java.io.File
import java.util.Random

public class ItemDaoImpl(
//  public val model: Model,
  private val baseUri: String,
  private val baseDir: File,
  private var webId: String?,
) : ItemDao {
  private var model: Model
  private var saveFilePath: String

  private fun generateRandomString(length: Int): String {
    val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    val randomString = StringBuilder()
    val random = Random()

    for (i in 0 until length) {
      val index = random.nextInt(characters.length)
      randomString.append(characters[index])
    }

    return randomString.toString()
  }

  init {
    val saveFilePath: String = if (webId != null) {
      webId!!.replace("https://", "").replace("http://",
        "").split("/").drop(1).joinToString(separator="_").replace("#", "")
    } else {
      generateRandomString(32)
    }
    this.saveFilePath = saveFilePath
    val model: Model = if (webId != null) {
      val file = File(baseUri, saveFilePath)

      if (file.exists()) {
        val inStream = file.inputStream()
        ModelFactory.createDefaultModel().read(inStream, null)
      } else {
        val model = ModelFactory.createDefaultModel()
        model.setNsPrefix("acp", Utilities.NS_ACP)
        model.setNsPrefix("acl", Utilities.NS_ACL)
        model.setNsPrefix("ldp", Utilities.NS_LDP)
        model.setNsPrefix("skos", Utilities.NS_SKOS)
        model.setNsPrefix("ti", Utilities.NS_Item)
        model
      }
    } else {
      val model = ModelFactory.createDefaultModel()
      model.setNsPrefix("acp", Utilities.NS_ACP)
      model.setNsPrefix("acl", Utilities.NS_ACL)
      model.setNsPrefix("ldp", Utilities.NS_LDP)
      model.setNsPrefix("skos", Utilities.NS_SKOS)
      model.setNsPrefix("ti", Utilities.NS_Item)
      model
    }
    this.model = model
  }

  public val modelLiveData: MutableStateFlow<List<Item>> =
      MutableStateFlow(getAllItems())

  override fun getAllItemsAsFlow(): Flow<List<Item>> = modelLiveData

  public override fun getAllItems(): List<Item> {
    val itemList = mutableListOf<Item>()
    val res = model.listResourcesWithProperty(model.createProperty(Utilities.NS_Item + "name"))
    while (res.hasNext()) {
      val nextResource = res.nextResource()
      itemList.add(resourceToItem(nextResource))
    }
    return itemList
  }

  override suspend fun insert(item: Item) {
    val id = java.util.UUID.randomUUID().toString()
    val mThingUri = model.createResource("$baseUri#$id")
    val mname = model.createProperty(Utilities.NS_Item + "name")
    val nameLiteral = ResourceFactory.createTypedLiteral(item.name)
    mThingUri.addLiteral(mname, nameLiteral)
    val mamount = model.createProperty(Utilities.NS_Item + "amount")
    val amountLiteral = ResourceFactory.createTypedLiteral(item.amount)
    mThingUri.addLiteral(mamount, amountLiteral)
    val file = File(baseDir, saveFilePath)
    val os = file.outputStream()
    model.write(os, null, null)
    modelLiveData.value = getAllItems()
  }

  override suspend fun delete(uri: String) {
    val resource = ModelFactory.createDefaultModel().createResource("$baseUri#$uri")
    model.removeAll(resource, null ,null)
    val file = File(baseDir, saveFilePath)
    val os = file.outputStream()
    model.write(os, null, null)
    modelLiveData.value = getAllItems()
  }

  override suspend fun update(item: Item) {
    val resource = ResourceFactory.createResource("$baseUri#${item.id}")
    if (model.containsResource(resource)) {
      val resourceInModel = model.getResource(resource.uri)
      val mname = model.createProperty(Utilities.NS_Item + "name")
      resourceInModel.removeAll(mname)
      val nameLiteral = ResourceFactory.createTypedLiteral(item.name)
      resourceInModel.addProperty(mname, nameLiteral)
      val mamount = model.createProperty(Utilities.NS_Item + "amount")
      resourceInModel.removeAll(mamount)
      val amountLiteral = ResourceFactory.createTypedLiteral(item.amount)
      resourceInModel.addProperty(mamount, amountLiteral)
      val file = File(baseDir, saveFilePath)
      val os = file.outputStream()
      model.write(os, null, null)
      modelLiveData.value = getAllItems()
    } else {
      throw Error("item with ${item.id} not found.")
    }
  }

  override fun getItemByIdAsFlow(id: String): Flow<Item> {
    val toSearch = ResourceFactory.createResource("$baseUri#$id")
    if (model.containsResource(toSearch)) {
      return flowOf(resourceToItem(model.getResource(toSearch.uri)))
    }
    return flowOf()
  }

  override fun updateWebId(webId: String) {
    this.webId = webId
    this.saveFilePath = webId.replace("https://", "").replace("http://",
        "").split("/").drop(1).joinToString(separator="_").replace("#", "")
    val file = File(baseUri, saveFilePath)
    this.model = if (file.exists()) {
      val inStream = file.inputStream()
      ModelFactory.createDefaultModel().read(inStream, null)
    } else {
      model = ModelFactory.createDefaultModel()
      model.setNsPrefix("acp", Utilities.NS_ACP)
      model.setNsPrefix("acl", Utilities.NS_ACL)
      model.setNsPrefix("ldp", Utilities.NS_LDP)
      model.setNsPrefix("skos", Utilities.NS_SKOS)
      model.setNsPrefix("ti", Utilities.NS_Item)
      model
    }
  }
}
