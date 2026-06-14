package se.mindphaser.skytte.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.exceptions.NoCredentialException
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import se.mindphaser.skytte.R
import se.mindphaser.skytte.auth.AuthManager

// Deep woodland green backdrop for the sign-in gate; warm off-white title for contrast.
private val SignInBackground = Color(0xFF22301A)
private val SignInTitle = Color(0xFFE9E5D8)

/** Full-screen sign-in gate shown when no user is signed in. */
@Composable
fun SignInScreen(authManager: AuthManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val signInFailed = stringResource(R.string.sign_in_failed)
    val signInNoAccount = stringResource(R.string.sign_in_no_account)
    var signingIn by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = SignInBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
        ) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = SignInTitle
            )
            Image(
                painter = painterResource(R.drawable.skytte_logo),
                contentDescription = null,
                modifier = Modifier.size(128.dp)
            )
            if (signingIn) {
                CircularProgressIndicator()
            } else {
                Button(onClick = {
                    signingIn = true
                    scope.launch {
                        authManager.signInWithGoogle(context).onFailure { e ->
                            val message = if (e is NoCredentialException) {
                                signInNoAccount
                            } else {
                                "$signInFailed: ${e.message ?: e::class.simpleName.orEmpty()}"
                            }
                            snackbarHostState.showSnackbar(message)
                        }
                        signingIn = false
                    }
                }) {
                    Text(stringResource(R.string.sign_in_google))
                }
            }
        }
    }
}
