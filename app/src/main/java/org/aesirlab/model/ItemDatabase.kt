package org.aesirlab.model

import android.`annotation`.SuppressLint
import android.content.Context
import java.io.File
import kotlin.String
import kotlin.jvm.Volatile

public class ItemDatabase(
  private val baseUri: String,
  private val baseDir: File,
//  private var model: Model,
  private var webId: String?
) {

  public fun ItemDao(): ItemDao = ItemDaoImpl(baseUri,baseDir, webId)

  public companion object {
    @SuppressLint
    @Volatile
    private var INSTANCE: ItemDatabase? = null

    public fun getDatabase(context: Context, baseUri: String, webId: String? = null): ItemDatabase {
      if (INSTANCE != null) {
        return INSTANCE!!
      } else {
        synchronized(this) {
        val instance = ItemDatabase(baseUri, context.filesDir, webId)
        INSTANCE = instance
        return instance
        }
        }
      }
    }
  }
