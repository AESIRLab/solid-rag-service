package org.aesirlab.usingcustomprocessorandroid

import android.os.Bundle
import android.os.Messenger
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.aesirlab.usingcustomprocessorandroid.rag.RagPipeline
import org.aesirlab.usingcustomprocessorandroid.ui.App
import org.aesirlab.usingcustomprocessorandroid.ui.theme.UsingCustomProcessorAndroidTheme
const val REDIRECT_URI = "org.aesirlab.customprocessor://app/callback"


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UsingCustomProcessorAndroidTheme {
                App()
            }
        }
    }
}
