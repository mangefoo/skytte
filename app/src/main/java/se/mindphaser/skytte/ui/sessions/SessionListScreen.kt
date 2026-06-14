@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package se.mindphaser.skytte.ui.sessions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import se.mindphaser.skytte.R
import se.mindphaser.skytte.data.SessionWithRefs
import se.mindphaser.skytte.data.totalCost
import se.mindphaser.skytte.ui.SkytteTopBar
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("sv-SE"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onOpenSettings: () -> Unit,
    vm: SessionListViewModel = viewModel(factory = SessionListViewModel.Factory)
) {
    val sessions by vm.sessions.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            SkytteTopBar(
                title = stringResource(R.string.tab_sessions),
                onOpenSettings = onOpenSettings
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_session))
            }
        }
    ) { padding ->
        if (sessions.isEmpty()) {
            EmptyState(padding, stringResource(R.string.empty_sessions))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = sessions, key = { it.session.id }) { item ->
                    SessionCard(item, onClick = { onOpen(item.session.id) })
                }
            }
        }
    }
}

@Composable
private fun SessionCard(item: SessionWithRefs, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.session.location.ifBlank { "–" },
                style = MaterialTheme.typography.titleMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = item.session.date.format(dateFormatter),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (item.session.shootingType.isNotBlank()) {
                    Text(
                        text = item.session.shootingType,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            val weaponName = item.weapon?.name
            val ammoName = item.ammunition?.name
            val details = buildString {
                if (weaponName != null) append(weaponName)
                if (ammoName != null) {
                    if (isNotEmpty()) append(" · ")
                    append(ammoName)
                }
                if (item.session.ammoCount > 0) {
                    if (isNotEmpty()) append(" · ")
                    append("${item.session.ammoCount} skott")
                }
                val cost = item.totalCost()
                if (cost > 0) {
                    if (isNotEmpty()) append(" · ")
                    append(String.format(Locale.forLanguageTag("sv-SE"), "%.2f kr", cost))
                }
            }
            if (details.isNotEmpty()) {
                Text(details, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EmptyState(padding: PaddingValues, text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
