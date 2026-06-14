package se.mindphaser.skytte.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import se.mindphaser.skytte.BuildConfig
import se.mindphaser.skytte.R
import se.mindphaser.skytte.SkytteApp
import se.mindphaser.skytte.data.ImportResult
import se.mindphaser.skytte.data.exportBackup
import se.mindphaser.skytte.data.importBackup
import se.mindphaser.skytte.data.shareBackup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as SkytteApp
    val themePreferences = app.themePreferences
    val darkModePref by themePreferences.darkMode.collectAsState()
    val darkMode = darkModePref ?: isSystemInDarkTheme()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val shareTitle = stringResource(R.string.export_share)
    val exportFailed = stringResource(R.string.export_failed)

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val repos = app.currentRepositories ?: return@rememberLauncherForActivityResult
        scope.launch {
            val message = when (val result = importBackup(context, uri, repos)) {
                is ImportResult.Success -> context.getString(
                    R.string.import_success, result.weapons, result.ammunition, result.sessions
                )
                is ImportResult.Error -> context.getString(R.string.import_failed, result.message)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dark_mode),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = darkMode,
                    onCheckedChange = { themePreferences.setDarkMode(it) }
                )
            }

            SettingsActionRow(
                icon = Icons.Default.Upload,
                label = stringResource(R.string.export),
                onClick = {
                    val repos = app.currentRepositories ?: return@SettingsActionRow
                    scope.launch {
                        runCatching { shareBackup(context, exportBackup(context, repos), shareTitle) }
                            .onFailure { snackbarHostState.showSnackbar(exportFailed) }
                    }
                }
            )

            SettingsActionRow(
                icon = Icons.Default.Download,
                label = stringResource(R.string.import_data),
                onClick = { importLauncher.launch(arrayOf("application/json")) }
            )

            SettingsActionRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                label = stringResource(R.string.sign_out),
                onClick = { app.authManager.signOut() }
            )

            Text(
                text = stringResource(
                    R.string.app_version,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.BUILD_DATE
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
