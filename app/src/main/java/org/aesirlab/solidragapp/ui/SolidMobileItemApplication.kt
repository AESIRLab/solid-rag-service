package org.aesirlab.solidragapp.ui

import android.app.Application
import android.content.Context
import android.content.Intent

val SAVE_RESOURCE_POD_URI = "https://ec2-18-119-19-244.us-east-2.compute.amazonaws.com/zach/profile/"
val RESOURCE_URIS = arrayOf(
    "https://storage.inrupt.com/9e06bd80-2380-46e0-9eaa-19c9d2baebb1/appOne/sample_content_1.txt",
//    "https://storage.inrupt.com/9e06bd80-2380-46e0-9eaa-19c9d2baebb1/appTwo/sample_content_2.txt",
//    "https://storage.inrupt.com/9e06bd80-2380-46e0-9eaa-19c9d2baebb1/appThree/sample_content_3.txt"
)
val SAVED_CHAT_URI_BASE = "https://storage.inrupt.com/9e06bd80-2380-46e0-9eaa-19c9d2baebb1/savedChats/"
class SolidMobileItemApplication: Application() {
    init {
        appInstance = this
    }

    companion object {
        lateinit var appInstance: SolidMobileItemApplication
    }
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