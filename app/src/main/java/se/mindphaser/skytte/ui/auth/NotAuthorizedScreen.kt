package se.mindphaser.skytte.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import se.mindphaser.skytte.R

/** Shown when a signed-in account is not on the allowlist. The Firestore rules are the real boundary. */
@Composable
fun NotAuthorizedScreen(onSignOut: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Text(
                stringResource(R.string.not_authorized_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.not_authorized_message),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            OutlinedButton(onClick = onSignOut) {
                Text(stringResource(R.string.sign_out))
            }
        }
    }
}
