package org.aesirlab.usingcustomprocessorandroid.ui

import android.app.Application
import org.aesirlab.model.ItemDatabase
import org.aesirlab.model.ItemRepository

class SolidMobileItemApplication: Application() {
    init {
        appInstance = this
    }

    companion object {
        lateinit var appInstance: SolidMobileItemApplication
        const val BASE_URL = "http://soliditemapp.aesirlab.io"
    }

    val database by lazy { ItemDatabase.getDatabase(appInstance, BASE_URL) }
    val repository by lazy { ItemRepository(database.ItemDao()) }
}