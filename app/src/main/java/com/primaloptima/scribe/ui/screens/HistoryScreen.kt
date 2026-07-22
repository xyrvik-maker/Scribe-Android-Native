package com.primaloptima.scribe.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.primaloptima.scribe.ScribeApp
import com.primaloptima.scribe.util.HistoryManager
import com.primaloptima.scribe.util.MarkdownUtil
import com.primaloptima.scribe.util.model.HistorySnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as ScribeApp
    val historyManager = remember { HistoryManager(app.prefs) }

    val noteId = remember { app.prefs.activeNoteId ?: "" }
    val snapshots = remember(noteId) { historyManager.getSnapshots(noteId) }

    var snapshotToPreview by remember { mutableStateOf<HistorySnapshot?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Version History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (snapshots.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No version snapshots recorded yet.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(snapshots) { snap ->
                    val dateStr = remember(snap.savedAt) {
                        SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()).format(Date(snap.savedAt))
                    }
                    val words = remember(snap.content) { MarkdownUtil.countWords(snap.content) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { snapshotToPreview = snap },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(dateStr, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("$words words", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    snap.content.take(80),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    snapshotToPreview?.let { snap ->
        AlertDialog(
            onDismissRequest = { snapshotToPreview = null },
            title = { Text("Restore Snapshot?") },
            text = {
                Column {
                    Text(
                        snap.content.take(400) + if (snap.content.length > 400) "…" else "",
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val db = app.database
                            db.noteDao().updateContent(noteId, snap.content, System.currentTimeMillis())
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Version restored", Toast.LENGTH_SHORT).show()
                                snapshotToPreview = null
                                onBack()
                            }
                        }
                    }
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { snapshotToPreview = null }) { Text("Cancel") }
            }
        )
    }
}
