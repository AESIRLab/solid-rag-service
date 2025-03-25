package org.aesirlab.usingcustomprocessorandroid.ui

import android.app.Application
import android.content.ClipData.Item
import org.skCompiler.generatedModel.ItemDao
import org.skCompiler.generatedModel.ItemDaoImpl
import org.skCompiler.generatedModel.ItemDatabase
import org.skCompiler.generatedModel.ItemRepository

class SolidMobileItemApplication: Application() {
    init {
        appInstance = this
    }

    companion object {
        lateinit var appInstance: SolidMobileItemApplication
        const val BASE_URL = "http://soliditemapp.aesirlab.io"
    }

    val database by lazy { ItemDatabase.getDatabase(appInstance, BASE_URL) }
    val repository by lazy { ItemRepository(database.ItemDao())}
}