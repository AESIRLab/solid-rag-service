package org.aesirlab.usingcustomprocessorandroid.ui

import android.app.Application
import org.skCompiler.generatedModel.ItemDatabase
import org.skCompiler.generatedModel.ItemRepository

class SolidMobileItemApplication: Application() {
    init {
        appInstance = this
    }

    companion object {
        lateinit var appInstance: SolidMobileItemApplication
        const val FILE_PATH = "SolidItemApplication"
        const val BASE_URL = "http://soliditemapp.aesirlab.io"
    }

    private val database by lazy { ItemDatabase.getDatabase(appInstance, BASE_URL, FILE_PATH) }
    val repository by lazy { ItemRepository(database.ItemDao())}
}