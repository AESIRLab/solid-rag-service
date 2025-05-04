package org.aesirlab.solidragapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.aesirlab.solidragapp.ui.App
import org.aesirlab.solidragapp.ui.theme.UsingCustomProcessorAndroidTheme

// this needs to be the same as the one in your android manifest
const val REDIRECT_URI = "org.aesirlab.solidragapp://app/callback"


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
