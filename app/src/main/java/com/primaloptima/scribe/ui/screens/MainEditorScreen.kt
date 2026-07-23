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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.primaloptima.scribe.*
import com.primaloptima.scribe.data.Note
import com.primaloptima.scribe.ui.components.FloatingWindowOverlay
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
    val activeTheme by editorVm.theme.observeAsState()

    val bookNotes by bookVm.notes.observeAsState(emptyList())
    val shortcuts by shortcutsVm.shortcuts.observeAsState(emptyList())

    val floatingWindows by editorVm.floatingWindows.observeAsState(emptyList())
    val pinnedTopNoteId by editorVm.pinnedTopNoteId.observeAsState()
    val pinnedBottomNoteId by editorVm.pinnedBottomNoteId.observeAsState()

    var rightDrawerTab by remember { mutableIntStateOf(0) } // 0: Reference Slots, 1: Outline
    var leftDrawerFilterAll by remember { mutableStateOf(false) }

    var showFindBar by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }

    var showRenameDialog by remember { mutableStateOf(false) }
    var showCreateNoteDialog by remember { mutableStateOf(false) }
    var noteToSelectForPinSlot by remember { mutableStateOf<String?>(null) } // "top" or "bottom"

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

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = leftDrawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Vault Explorer", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showCreateNoteDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "New Note")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Filter Segment: Current Book vs Vault
                        Row(modifier = Modifier.fillMaxWidth()) {
                            FilterChip(
                                selected = !leftDrawerFilterAll,
                                onClick = { leftDrawerFilterAll = false },
                                label = { Text("Current Book") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilterChip(
                                selected = leftDrawerFilterAll,
                                onClick = { leftDrawerFilterAll = true },
                                label = { Text("All Notes") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        items(bookNotes, key = { it.id }) { note ->
                            val isSelected = note.id == activeNote?.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable {
                                        editorVm.loadNote(note.id)
                                        scope.launch { leftDrawerState.close() }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Description,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        note.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "${note.folderPath} • ${note.content.split("\\s+".toRegex()).count { it.isNotBlank() }} words",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                var showRowMenu by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(
                                        onClick = { showRowMenu = true },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    DropdownMenu(
                                        expanded = showRowMenu,
                                        onDismissRequest = { showRowMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Open in Floating Window") },
                                            onClick = {
                                                showRowMenu = false
                                                editorVm.openFloatingWindow(note.id)
                                                scope.launch { leftDrawerState.close() }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Pin to Top Reference") },
                                            onClick = {
                                                showRowMenu = false
                                                editorVm.setPinnedTop(note.id)
                                                scope.launch { leftDrawerState.close() }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Pin to Bottom Reference") },
                                            onClick = {
                                                showRowMenu = false
                                                editorVm.setPinnedBottom(note.id)
                                                scope.launch { leftDrawerState.close() }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()
                    TextButton(
                        onClick = { folderPickerLauncher.launch(null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect External SAF Folder")
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
                        TabRow(selectedTabIndex = rightDrawerTab) {
                            Tab(
                                selected = rightDrawerTab == 0,
                                onClick = { rightDrawerTab = 0 },
                                text = { Text("Reference") }
                            )
                            Tab(
                                selected = rightDrawerTab == 1,
                                onClick = { rightDrawerTab = 1 },
                                text = { Text("Outline (${outline.size})") }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        if (rightDrawerTab == 0) {
                            // Pinned Notes Reference Slots (Top & Bottom)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Slot 1: Top Reference
                                ReferenceSlotCard(
                                    slotTitle = "Top Reference Slot",
                                    pinnedNoteId = pinnedTopNoteId,
                                    allNotes = bookNotes,
                                    onSelectNoteClick = { noteToSelectForPinSlot = "top" },
                                    onUnpin = { editorVm.setPinnedTop(null) },
                                    onOpenInEditor = { id -> editorVm.loadNote(id) }
                                )

                                HorizontalDivider()

                                // Slot 2: Bottom Reference
                                ReferenceSlotCard(
                                    slotTitle = "Bottom Reference Slot",
                                    pinnedNoteId = pinnedBottomNoteId,
                                    allNotes = bookNotes,
                                    onSelectNoteClick = { noteToSelectForPinSlot = "bottom" },
                                    onUnpin = { editorVm.setPinnedBottom(null) },
                                    onOpenInEditor = { id -> editorVm.loadNote(id) }
                                )
                            }
                        } else {
                            // Outline Tab
                            if (outline.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No markdown headings found (# H1, ## H2)", color = MaterialTheme.colorScheme.outline)
                                }
                            } else {
                                LazyColumn(contentPadding = PaddingValues(12.dp)) {
                                    items(outline) { entry ->
                                        Row(
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
                                                .padding(start = (entry.level * 10).dp, top = 8.dp, bottom = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text("H${entry.level}", fontSize = 10.sp) },
                                                modifier = Modifier.height(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = entry.text,
                                                fontSize = 14.sp,
                                                fontWeight = if (entry.level == 1) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
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
                                        Icon(Icons.Default.FormatListBulleted, contentDescription = "Outline & Reference")
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
                                            text = { Text("Open as Floating Reference Window") },
                                            onClick = {
                                                showMenu = false
                                                activeNote?.let { n -> editorVm.openFloatingWindow(n.id) }
                                            }
                                        )
                                        HorizontalDivider()
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
                                        HorizontalDivider()
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
                                                    else -> editorRef?.insertTextAtCursor(shortcut.payload)
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
                        // Custom Theme Background Image
                        if (!activeTheme?.backgroundImageUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = activeTheme?.backgroundImageUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                alpha = activeTheme?.backgroundImageOpacity ?: 0.35f,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

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

        // Floating Windows Overlay
        FloatingWindowOverlay(
            floatingWindows = floatingWindows,
            notes = bookNotes,
            onCloseWindow = { id -> editorVm.closeFloatingWindow(id) },
            onToggleCollapse = { id -> editorVm.toggleCollapseFloatingWindow(id) },
            onMoveWindow = { id, x, y -> editorVm.moveFloatingWindow(id, x, y) }
        )
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

    noteToSelectForPinSlot?.let { slot ->
        AlertDialog(
            onDismissRequest = { noteToSelectForPinSlot = null },
            title = { Text("Select Reference Note for $slot slot") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(bookNotes, key = { it.id }) { note ->
                        TextButton(
                            onClick = {
                                if (slot == "top") editorVm.setPinnedTop(note.id)
                                else editorVm.setPinnedBottom(note.id)
                                noteToSelectForPinSlot = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(note.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { noteToSelectForPinSlot = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ReferenceSlotCard(
    slotTitle: String,
    pinnedNoteId: String?,
    allNotes: List<Note>,
    onSelectNoteClick: () -> Unit,
    onUnpin: () -> Unit,
    onOpenInEditor: (String) -> Unit
) {
    val pinnedNote = remember(pinnedNoteId, allNotes) {
        allNotes.firstOrNull { it.id == pinnedNoteId }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(slotTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                if (pinnedNote != null) {
                    IconButton(onClick = onUnpin, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Unpin", modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (pinnedNote == null) {
                OutlinedButton(
                    onClick = onSelectNoteClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pin Note to Slot", fontSize = 12.sp)
                }
            } else {
                Text(pinnedNote.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .heightIn(max = 100.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        pinnedNote.content.ifBlank { "Empty note" },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { onOpenInEditor(pinnedNote.id) }) {
                        Text("Open in Editor", fontSize = 12.sp)
                    }
                }
            }
        }
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
