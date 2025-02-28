package org.aesirlab.usingcustomprocessorandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.zybooks.sksolidannotations.SolidAuthAnnotation
import org.aesirlab.usingcustomprocessorandroid.ui.App
import org.aesirlab.usingcustomprocessorandroid.ui.theme.UsingCustomProcessorAndroidTheme
const val REDIRECT_URI = "org.aesirlab.customprocessor://app/callback"

@SolidAuthAnnotation
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

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    UsingCustomProcessorAndroidTheme {
        Greeting("Android")
    }
}