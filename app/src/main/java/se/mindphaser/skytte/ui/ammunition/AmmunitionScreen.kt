@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package se.mindphaser.skytte.ui.ammunition

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import se.mindphaser.skytte.R
import se.mindphaser.skytte.data.Ammunition
import se.mindphaser.skytte.ui.SkytteTopBar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmmunitionScreen(
    onOpenSettings: () -> Unit,
    vm: AmmunitionViewModel = viewModel(factory = AmmunitionViewModel.Factory)
) {
    val items by vm.items.collectAsState(initial = emptyList())
    var editing by remember { mutableStateOf<Ammunition?>(null) }

    Scaffold(
        topBar = {
            SkytteTopBar(
                title = stringResource(R.string.tab_ammunition),
                onOpenSettings = onOpenSettings
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = Ammunition(name = "") }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_ammunition))
            }
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) { Text(stringResource(R.string.empty_ammunition)) }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = items, key = { it.id }) { a ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { editing = a }) {
                        Column(Modifier.padding(16.dp)) {
                            Text(a.name, style = MaterialTheme.typography.titleMedium)
                            val priceText = a.costPerRound?.let {
                                stringResource(
                                    R.string.cost_per_round_unit,
                                    String.format(Locale.forLanguageTag("sv-SE"), "%.2f", it)
                                )
                            }
                            val details = listOfNotNull(
                                a.caliber.takeIf { it.isNotBlank() },
                                a.notes.takeIf { it.isNotBlank() },
                                priceText
                            ).joinToString(" · ")
                            if (details.isNotEmpty()) {
                                Text(details, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { ammo ->
        AmmunitionEditDialog(
            initial = ammo,
            onDismiss = { editing = null },
            onSave = {
                vm.save(it)
                editing = null
            },
            onDelete = if (ammo.id.isNotBlank()) {
                {
                    vm.delete(ammo)
                    editing = null
                }
            } else null
        )
    }
}

@Composable
private fun AmmunitionEditDialog(
    initial: Ammunition,
    onDismiss: () -> Unit,
    onSave: (Ammunition) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(initial.name) }
    var caliber by remember { mutableStateOf(initial.caliber) }
    var notes by remember { mutableStateOf(initial.notes) }
    var cost by remember {
        mutableStateOf(initial.costPerRound?.let { String.format(Locale.forLanguageTag("sv-SE"), "%.2f", it) } ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial.id.isBlank()) stringResource(R.string.add_ammunition)
                else stringResource(R.string.edit)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = caliber,
                    onValueChange = { caliber = it },
                    label = { Text(stringResource(R.string.caliber)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cost,
                    onValueChange = { new ->
                        // Allow digits and a single decimal separator (comma or dot).
                        val filtered = new.filter { it.isDigit() || it == ',' || it == '.' }
                        if (filtered.count { it == ',' || it == '.' } <= 1) cost = filtered
                    },
                    label = { Text(stringResource(R.string.cost_per_round)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        initial.copy(
                            name = name.trim(),
                            caliber = caliber.trim(),
                            notes = notes.trim(),
                            costPerRound = cost.replace(',', '.').toDoubleOrNull()
                        )
                    )
                },
                enabled = name.isNotBlank()
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        }
    )
}
