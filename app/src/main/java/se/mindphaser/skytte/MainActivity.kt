package se.mindphaser.skytte

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import se.mindphaser.skytte.ui.SkytteAppRoot
import se.mindphaser.skytte.ui.auth.NotAuthorizedScreen
import se.mindphaser.skytte.ui.auth.SignInScreen
import se.mindphaser.skytte.ui.theme.SkytteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as SkytteApp
            val darkModePref by app.themePreferences.darkMode.collectAsState()
            val darkTheme = darkModePref ?: isSystemInDarkTheme()
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                )
                onDispose {}
            }
            SkytteTheme(darkTheme = darkTheme) {
                // Auth gate: the app is only shown once a user is signed in and their repositories
                // are ready. Signed out → sign-in screen; signing in → brief loading spinner.
                val uid by app.authManager.uid.collectAsState()
                val accessDenied by app.accessDenied.collectAsState()
                val repositories by app.repositories.collectAsState()
                when {
                    repositories != null -> SkytteAppRoot()
                    uid != null && accessDenied -> NotAuthorizedScreen(onSignOut = { app.authManager.signOut() })
                    uid != null -> Surface(modifier = Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    else -> SignInScreen(app.authManager)
                }
            }
        }
    }
}
