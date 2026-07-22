package com.primaloptima.scribe.ui.screens

import android.content.Intent
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.primaloptima.scribe.*
import com.primaloptima.scribe.data.Note
import com.primaloptima.scribe.util.ExportHelper
import com.primaloptima.scribe.view.ScribeEditText
import com.primaloptima.scribe.viewmodel.BookViewModel
import com.primaloptima.scribe.viewmodel.EditorViewModel
import com.primaloptima.scribe.viewmodel.NoteListViewModel
import com.primaloptima.scribe.viewmodel.ShortcutsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainEditorScreen(
    editorVm: EditorViewModel,
    bookVm: BookViewModel,
    noteListVm: NoteListViewModel,
    shortcutsVm: ShortcutsViewModel,
    initialNoteId: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val leftDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val rightDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val activeNote by editorVm.activeNote.observeAsState()
    val wordCount by editorVm.wordCount.observeAsState(0)
    val charCount by editorVm.charCount.observeAsState(0)
    val outline by editorVm.outline.observeAsState(emptyList())
    val zenMode by editorVm.zenMode.observeAsState(false)
    val recoveryAvailable by editorVm.recoveryAvailable.observeAsState(false)

    val bookNotes by bookVm.notes.observeAsState(emptyList())
    val shortcuts by shortcutsVm.shortcuts.observeAsState(emptyList())

    var showFindBar by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }

    var showRenameDialog by remember { mutableStateOf(false) }
    var showCreateNoteDialog by remember { mutableStateOf(false) }

    var editorRef by remember { mutableStateOf<ScribeEditText?>(null) }

    LaunchedEffect(initialNoteId) {
        if (!initialNoteId.isNullOrEmpty()) {
            editorVm.loadNote(initialNoteId)
        } else if (bookNotes.isNotEmpty()) {
            editorVm.loadNote(bookNotes.first().id)
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val name = uri.lastPathSegment?.substringAfterLast(':') ?: "External Folder"
        noteListVm.connectExternalFolder(uri, name)
    }

    ModalNavigationDrawer(
        drawerState = leftDrawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Vault Files", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showCreateNoteDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "New Note")
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    items(bookNotes, key = { it.id }) { note ->
                        val isSelected = note.id == activeNote?.id
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Outlined.Description, contentDescription = null) },
                            label = { Text(note.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            selected = isSelected,
                            onClick = {
                                editorVm.loadNote(note.id)
                                scope.launch { leftDrawerState.close() }
                            },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                Divider()
                TextButton(
                    onClick = { folderPickerLauncher.launch(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect External Folder")
                }
            }
        }
    ) {
        ModalNavigationDrawer(
            drawerState = rightDrawerState,
            gesturesEnabled = true,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Document Outline",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    if (outline.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No headings found in document", color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(12.dp)) {
                            items(outline) { entry ->
                                Text(
                                    text = entry.text,
                                    fontSize = 14.sp,
                                    fontWeight = if (entry.level == 1) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            editorRef?.let { ed ->
                                                val pos = ed.text?.indexOf(entry.text) ?: -1
                                                if (pos >= 0) {
                                                    ed.setSelection(pos)
                                                }
                                            }
                                            scope.launch { rightDrawerState.close() }
                                        }
                                        .padding(start = (entry.level * 12).dp, top = 8.dp, bottom = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    if (!zenMode) {
                        TopAppBar(
                            title = {
                                Column(
                                    modifier = Modifier.clickable {
                                        if (activeNote != null) showRenameDialog = true
                                    }
                                ) {
                                    Text(
                                        activeNote?.name ?: "Scribe Editor",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "$wordCount words • $charCount chars",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { leftDrawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Drawer")
                                }
                            },
                            actions = {
                                IconButton(onClick = { showFindBar = !showFindBar }) {
                                    Icon(Icons.Default.Search, contentDescription = "Find")
                                }
                                IconButton(onClick = { scope.launch { rightDrawerState.open() } }) {
                                    Icon(Icons.Default.FormatListBulleted, contentDescription = "Outline")
                                }
                                IconButton(onClick = { editorVm.toggleZen() }) {
                                    Icon(Icons.Default.Fullscreen, contentDescription = "Zen Mode")
                                }

                                var showMenu by remember { mutableStateOf(false) }
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                                }
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Export as TXT") },
                                        onClick = {
                                            showMenu = false
                                            activeNote?.let { n -> ExportHelper.shareNote(context, n, "txt") }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Export as Markdown") },
                                        onClick = {
                                            showMenu = false
                                            activeNote?.let { n -> ExportHelper.shareNote(context, n, "md") }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Export as HTML") },
                                        onClick = {
                                            showMenu = false
                                            activeNote?.let { n -> ExportHelper.shareNote(context, n, "html") }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Export as PDF") },
                                        onClick = {
                                            showMenu = false
                                            activeNote?.let { n -> ExportHelper.shareNote(context, n, "pdf") }
                                        }
                                    )
                                    Divider()
                                    DropdownMenuItem(
                                        text = { Text("Version History") },
                                        onClick = {
                                            showMenu = false
                                            context.startActivity(Intent(context, HistoryActivity::class.java))
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Shortcuts") },
                                        onClick = {
                                            showMenu = false
                                            context.startActivity(Intent(context, ShortcutsActivity::class.java))
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("User Guide") },
                                        onClick = {
                                            showMenu = false
                                            context.startActivity(Intent(context, GuideActivity::class.java))
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Settings") },
                                        onClick = {
                                            showMenu = false
                                            context.startActivity(Intent(context, SettingsActivity::class.java))
                                        }
                                    )
                                }
                            }
                        )
                    }
                },
                bottomBar = {
                    if (!zenMode) {
                        Surface(
                            shadowElevation = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FormatButton(label = "B") { editorRef?.applyFormat("**", "**") }
                                FormatButton(label = "I") { editorRef?.applyFormat("*", "*") }
                                FormatButton(label = "H1") { editorRef?.applyLinePrefix("# ") }
                                FormatButton(label = "H2") { editorRef?.applyLinePrefix("## ") }
                                FormatButton(label = "Quote") { editorRef?.applyLinePrefix("> ") }
                                FormatButton(label = "List") { editorRef?.applyLinePrefix("- ") }
                                FormatButton(label = "Code") { editorRef?.applyFormat("`", "`") }

                                shortcuts.forEach { shortcut ->
                                    Button(
                                        onClick = {
                                            when (shortcut.kind) {
                                                "wrap" -> editorRef?.applyFormat(shortcut.payload, shortcut.closing ?: shortcut.payload)
                                                "pair" -> editorRef?.applyFormat(shortcut.payload, shortcut.closing ?: "")
                                                else   -> editorRef?.insertTextAtCursor(shortcut.payload)
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text(shortcut.label, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (recoveryAvailable) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Unsaved recovery backup found", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
                                    Row {
                                        TextButton(onClick = {
                                            val recovered = editorVm.getRecoveryContent()
                                            if (recovered != null) {
                                                editorRef?.setText(recovered)
                                                editorVm.onContentChanged(recovered)
                                            }
                                            editorVm.dismissRecovery()
                                        }) { Text("Restore") }
                                        TextButton(onClick = { editorVm.dismissRecovery() }) { Text("Dismiss") }
                                    }
                                }
                            }
                        }

                        if (showFindBar) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = findQuery,
                                        onValueChange = { findQuery = it },
                                        placeholder = { Text("Find") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    OutlinedTextField(
                                        value = replaceQuery,
                                        onValueChange = { replaceQuery = it },
                                        placeholder = { Text("Replace") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    )
                                    IconButton(onClick = {
                                        if (findQuery.isNotEmpty()) {
                                            val currentText = editorRef?.text?.toString() ?: ""
                                            val updated = currentText.replace(findQuery, replaceQuery)
                                            editorRef?.setText(updated)
                                            editorVm.onContentChanged(updated)
                                        }
                                    }) {
                                        Icon(Icons.Default.FindReplace, contentDescription = "Replace All")
                                    }
                                    IconButton(onClick = { showFindBar = false }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close")
                                    }
                                }
                            }
                        }

                        AndroidView(
                            factory = { ctx ->
                                ScrollView(ctx).apply {
                                    isFillViewport = true
                                    val edit = ScribeEditText(ctx).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        setPadding(32, 32, 32, 32)
                                        textSize = 18f
                                        addTextChangedListener(object : TextWatcher {
                                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                                            override fun afterTextChanged(s: Editable?) {
                                                editorVm.onContentChanged(s?.toString() ?: "")
                                            }
                                        })
                                    }
                                    editorRef = edit
                                    addView(edit)
                                }
                            },
                            update = { _ ->
                                activeNote?.let { note ->
                                    if (editorRef?.text?.toString() != note.content) {
                                        editorRef?.setText(note.content)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (zenMode) {
                        FloatingActionButton(
                            onClick = { editorVm.setZen(false) },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Zen")
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog && activeNote != null) {
        var renameText by remember { mutableStateOf(activeNote!!.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Note") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val t = renameText.trim()
                        if (t.isNotEmpty()) {
                            bookVm.renameNote(activeNote!!.id, t)
                        }
                        showRenameDialog = false
                    }
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showCreateNoteDialog) {
        var noteTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateNoteDialog = false },
            title = { Text("New Note") },
            text = {
                OutlinedTextField(
                    value = noteTitle,
                    onValueChange = { noteTitle = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val t = noteTitle.trim()
                        if (t.isNotEmpty()) {
                            bookVm.createNote(t) { id ->
                                showCreateNoteDialog = false
                                editorVm.loadNote(id)
                            }
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateNoteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FormatButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private fun ScribeEditText.applyFormat(prefix: String, suffix: String) {
    val start = selectionStart
    val end = selectionEnd
    val textStr = text?.toString() ?: ""
    if (start >= 0 && end >= start) {
        val selected = textStr.substring(start, end)
        val formatted = "$prefix$selected$suffix"
        text?.replace(start, end, formatted)
        setSelection(start + prefix.length, end + prefix.length)
    }
}

private fun ScribeEditText.applyLinePrefix(prefix: String) {
    val start = selectionStart
    if (start >= 0) {
        text?.insert(start, prefix)
    }
}

private fun ScribeEditText.insertTextAtCursor(str: String) {
    val start = selectionStart
    if (start >= 0) {
        text?.insert(start, str)
    }
}
