package com.primaloptima.scribe.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.primaloptima.scribe.data.WorldEntry
import com.primaloptima.scribe.viewmodel.SheetsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetsScreen(
    vm: SheetsViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Characters, 1: Locations
    val characters by vm.characters.observeAsState(emptyList())
    val locations by vm.locations.observeAsState(emptyList())

    var showCreateDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<WorldEntry?>(null) }
    var entryToDelete by remember { mutableStateOf<WorldEntry?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("World Building Sheets", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Entry")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Characters (${characters.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Locations (${locations.size})") }
                )
            }

            val currentList = if (selectedTab == 0) characters else locations

            if (currentList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No ${if (selectedTab == 0) "characters" else "locations"} yet. Tap + to add.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(currentList, key = { it.id }) { entry ->
                        WorldEntryCard(
                            entry = entry,
                            onClick = { entryToEdit = entry },
                            onDuplicate = { vm.duplicateEntry(entry.id) },
                            onDelete = { entryToDelete = entry }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(if (selectedTab == 0) "New Character" else "New Location") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val type = if (selectedTab == 0) "character" else "location"
                        vm.createEntry(type, name) { created ->
                            showCreateDialog = false
                            entryToEdit = created
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    entryToEdit?.let { entry ->
        EditWorldEntryDialog(
            entry = entry,
            onDismiss = { entryToEdit = null },
            onSave = { updated ->
                vm.updateEntry(updated)
                entryToEdit = null
            }
        )
    }

    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Delete Entry?") },
            text = { Text("Are you sure you want to delete \"${entry.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteEntry(entry.id)
                        entryToDelete = null
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun WorldEntryCard(
    entry: WorldEntry,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (entry.type == "character") Icons.Default.Person else Icons.Default.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(entry.type.capitalize(), fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { showMenu = false; onClick() })
                    DropdownMenuItem(text = { Text("Duplicate") }, onClick = { showMenu = false; onDuplicate() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun EditWorldEntryDialog(
    entry: WorldEntry,
    onDismiss: () -> Unit,
    onSave: (WorldEntry) -> Unit
) {
    val gson = remember { Gson() }
    val fieldsType = object : TypeToken<List<SheetsViewModel.Companion.Field>>() {}.type
    val initialFields: List<SheetsViewModel.Companion.Field> = remember(entry) {
        try {
            gson.fromJson(entry.fieldsJson, fieldsType)
        } catch (_: Exception) {
            emptyList()
        }
    }

    var name by remember { mutableStateOf(entry.name) }
    var fields by remember { mutableStateOf(initialFields) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${entry.name}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                fields.forEachIndexed { index, field ->
                    OutlinedTextField(
                        value = field.value,
                        onValueChange = { newVal ->
                            val updatedList = fields.toMutableList()
                            updatedList[index] = field.copy(value = newVal)
                            fields = updatedList
                        },
                        label = { Text(field.label) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updatedJson = gson.toJson(fields)
                    onSave(entry.copy(name = name, fieldsJson = updatedJson))
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
