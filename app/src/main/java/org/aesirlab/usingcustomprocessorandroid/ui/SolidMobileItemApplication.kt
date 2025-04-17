package org.aesirlab.usingcustomprocessorandroid.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import org.aesirlab.model.ItemDatabase
import org.aesirlab.model.ItemRemoteDataSource
import org.aesirlab.model.ItemRepository

class SolidMobileItemApplication: Application() {
    init {
        appInstance = this
    }

    companion object {
        lateinit var appInstance: SolidMobileItemApplication
        const val BASE_URL = "http://soliditemapp.aesirlab.io"
    }


    private val database by lazy { ItemDatabase.getDatabase(appInstance, BASE_URL) }
    val repository by lazy { ItemRepository(database.ItemDao(), ) }
}

fun Context.broadcastMessageInfo(
    queryId: Int,
    response: String
) {
    val broadcastIntent = Intent()
    broadcastIntent.`package` = this.packageName
    broadcastIntent.action = "MESSAGE"
    broadcastIntent.putExtra("queryId", queryId)
    broadcastIntent.putExtra("response", response)
    this.sendBroadcast(broadcastIntent)
}