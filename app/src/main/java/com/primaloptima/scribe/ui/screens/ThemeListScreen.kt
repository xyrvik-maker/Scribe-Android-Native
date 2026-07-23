package com.primaloptima.scribe.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.primaloptima.scribe.ThemeEditActivity
import com.primaloptima.scribe.ui.theme.parseComposeColor
import com.primaloptima.scribe.util.model.AppTheme
import com.primaloptima.scribe.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeListScreen(
    vm: ThemeViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val themes by vm.themes.observeAsState(emptyList())
    val activeTheme by vm.activeTheme.observeAsState()

    var themeToDelete by remember { mutableStateOf<AppTheme?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Themes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val active = activeTheme ?: return@FloatingActionButton
                    val newTheme = active.copy(
                        id = vm.generateId(),
                        name = "Custom Theme",
                        builtIn = false
                    )
                    vm.save(newTheme)
                    context.startActivity(
                        Intent(context, ThemeEditActivity::class.java)
                            .putExtra("theme_id", newTheme.id)
                    )
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Theme")
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(themes, key = { theme -> theme.id }) { theme ->
                val isSelected = theme.id == activeTheme?.id
                ThemeCard(
                    theme = theme,
                    isSelected = isSelected,
                    onSelect = {
                        vm.setActive(theme.id)
                        Toast.makeText(context, "${theme.name} applied", Toast.LENGTH_SHORT).show()
                    },
                    onEdit = {
                        context.startActivity(
                            Intent(context, ThemeEditActivity::class.java)
                                .putExtra("theme_id", theme.id)
                        )
                    },
                    onDuplicate = {
                        vm.duplicate(theme.id)
                        Toast.makeText(context, "Duplicated ${theme.name}", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = { themeToDelete = theme }
                )
            }
        }
    }

    themeToDelete?.let { theme ->
        AlertDialog(
            onDismissRequest = { themeToDelete = null },
            title = { Text("Delete \"${theme.name}\"?") },
            text = { Text("Are you sure you want to delete this theme?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.delete(theme.id)
                        themeToDelete = null
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { themeToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ThemeCard(
    theme: AppTheme,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val bgColor = parseComposeColor(theme.colors.background, Color.LightGray)
    val textColor = parseComposeColor(theme.colors.text, Color.Black)
    val accentColor = parseComposeColor(theme.colors.accent, Color.Blue)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .then(
                if (isSelected) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(12.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color Preview Swatch
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .border(1.dp, Color.Gray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("Aa", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(theme.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    if (theme.builtIn) "Built-in" else "Custom",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Active",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Apply") }, onClick = { showMenu = false; onSelect() })
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { showMenu = false; onEdit() })
                    DropdownMenuItem(text = { Text("Duplicate") }, onClick = { showMenu = false; onDuplicate() })
                    if (!theme.builtIn) {
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDelete() })
                    }
                }
            }
        }
    }
}
