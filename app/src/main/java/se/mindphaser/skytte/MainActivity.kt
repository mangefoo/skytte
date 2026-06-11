package se.mindphaser.skytte

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import se.mindphaser.skytte.ui.SkytteAppRoot
import se.mindphaser.skytte.ui.theme.SkytteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkytteTheme {
                SkytteAppRoot()
            }
        }
    }
}
