@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package se.mindphaser.skytte.ui.weapons

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import se.mindphaser.skytte.R
import se.mindphaser.skytte.data.Weapon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeaponsScreen(vm: WeaponsViewModel = viewModel(factory = WeaponsViewModel.Factory)) {
    val weapons by vm.weapons.collectAsState(initial = emptyList())
    var editing by remember { mutableStateOf<Weapon?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_weapons)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = Weapon(name = "") }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_weapon))
            }
        }
    ) { padding ->
        if (weapons.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) { Text(stringResource(R.string.empty_weapons)) }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = weapons, key = { it.id }) { w ->
                    Card(modifier = Modifier.clickable { editing = w }) {
                        Column(Modifier.padding(16.dp)) {
                            Text(w.name, style = MaterialTheme.typography.titleMedium)
                            val details = listOfNotNull(
                                w.caliber.takeIf { it.isNotBlank() },
                                w.notes.takeIf { it.isNotBlank() }
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

    editing?.let { weapon ->
        WeaponEditDialog(
            initial = weapon,
            onDismiss = { editing = null },
            onSave = { updated ->
                vm.save(updated)
                editing = null
            },
            onDelete = if (weapon.id != 0L) {
                {
                    vm.delete(weapon)
                    editing = null
                }
            } else null
        )
    }
}

@Composable
private fun WeaponEditDialog(
    initial: Weapon,
    onDismiss: () -> Unit,
    onSave: (Weapon) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(initial.name) }
    var caliber by remember { mutableStateOf(initial.caliber) }
    var notes by remember { mutableStateOf(initial.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial.id == 0L) stringResource(R.string.add_weapon)
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
                    onSave(initial.copy(name = name.trim(), caliber = caliber.trim(), notes = notes.trim()))
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
