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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.primaloptima.scribe.*
import com.primaloptima.scribe.data.Book
import com.primaloptima.scribe.data.Folder
import com.primaloptima.scribe.data.Note
import com.primaloptima.scribe.ui.components.FloatingWindowOverlay
import com.primaloptima.scribe.util.ExportHelper
import com.primaloptima.scribe.view.ScribeEditText
import com.primaloptima.scribe.viewmodel.BookViewModel
import com.primaloptima.scribe.viewmodel.EditorViewModel
import com.primaloptima.scribe.viewmodel.NoteListViewModel
import com.primaloptima.scribe.viewmodel.ShortcutsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
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

    val currentBookNotes by bookVm.notes.observeAsState(emptyList())
    val currentBookFolders by bookVm.folders.observeAsState(emptyList())
    val allNotes by noteListVm.notes.observeAsState(emptyList())
    val allFolders by noteListVm.folders.observeAsState(emptyList())
    val shortcuts by shortcutsVm.shortcuts.observeAsState(emptyList())

    val floatingWindows by editorVm.floatingWindows.observeAsState(emptyList())

    val pinnedTopNotes by editorVm.pinnedTopNotes.observeAsState(emptyList())
    val pinnedTopIndex by editorVm.pinnedTopIndex.observeAsState(0)
    val pinnedBottomNotes by editorVm.pinnedBottomNotes.observeAsState(emptyList())
    val pinnedBottomIndex by editorVm.pinnedBottomIndex.observeAsState(0)

    var rightDrawerTab by remember { mutableIntStateOf(0) } // 0: Pinned, 1: Outline
    var leftDrawerMode by remember { mutableStateOf("Current") } // "Current" or "Books"
    var leftSearchQuery by remember { mutableStateOf("") }

    var showFindBar by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }

    var showRenameDialog by remember { mutableStateOf(false) }
    var showCreateNoteDialog by remember { mutableStateOf(false) }
    var filePickerTargetSlot by remember { mutableStateOf<String?>(null) } // "top" or "bottom"

    var editorRef by remember { mutableStateOf<ScribeEditText?>(null) }
    val expandedTreeState = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(initialNoteId) {
        if (!initialNoteId.isNullOrEmpty()) {
            editorVm.loadNote(initialNoteId)
        } else if (currentBookNotes.isNotEmpty()) {
            editorVm.loadNote(currentBookNotes.first().id)
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
                ModalDrawerSheet(modifier = Modifier.width(320.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Vault Explorer", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Row {
                                SingleChoiceSegmentedButtonRow {
                                    SegmentedButton(
                                        selected = leftDrawerMode == "Current",
                                        onClick = { leftDrawerMode = "Current" },
                                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                                    ) { Text("Current", fontSize = 10.sp) }
                                    SegmentedButton(
                                        selected = leftDrawerMode == "Books",
                                        onClick = { leftDrawerMode = "Books" },
                                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                                    ) { Text("Books", fontSize = 10.sp) }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Search Bar
                        OutlinedTextField(
                            value = leftSearchQuery,
                            onValueChange = { leftSearchQuery = it },
                            placeholder = { Text("Search files & folders...") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (leftSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { leftSearchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Tree / Search list
                        val displayNotes = if (leftDrawerMode == "Current") currentBookNotes else allNotes
                        val displayFolders = if (leftDrawerMode == "Current") currentBookFolders else allFolders

                        val filteredNotes = remember(displayNotes, leftSearchQuery) {
                            if (leftSearchQuery.isBlank()) displayNotes
                            else displayNotes.filter {
                                it.name.contains(leftSearchQuery, ignoreCase = true) ||
                                it.content.contains(leftSearchQuery, ignoreCase = true) ||
                                it.folderPath.contains(leftSearchQuery, ignoreCase = true)
                            }
                        }

                        if (leftSearchQuery.isNotBlank()) {
                            // Search Mode
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredNotes, key = { it.id }) { note ->
                                    val isSelected = note.id == activeNote?.id
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                editorVm.loadNote(note.id)
                                                if (leftSearchQuery.isNotBlank()) {
                                                    findQuery = leftSearchQuery
                                                    showFindBar = true
                                                }
                                                scope.launch { leftDrawerState.close() }
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = buildHighlightedText(note.name, leftSearchQuery, MaterialTheme.colorScheme.primary),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Text(
                                                text = buildHighlightedText(note.folderPath, leftSearchQuery, MaterialTheme.colorScheme.primary),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                            val snippet = remember(note.content, leftSearchQuery) {
                                                getPreviewSnippet(note.content, leftSearchQuery)
                                            }
                                            if (snippet.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = buildHighlightedText(snippet, leftSearchQuery, MaterialTheme.colorScheme.primary),
                                                    fontSize = 12.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Tree Mode
                            val folderGrouped = remember(displayNotes, displayFolders) {
                                val map = mutableMapOf<String, MutableList<Note>>()
                                displayNotes.forEach { n ->
                                    val f = n.folderPath.ifBlank { "/" }
                                    map.getOrPut(f) { mutableListOf() }.add(n)
                                }
                                map
                            }

                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                folderGrouped.forEach { (folderPath, notesInFolder) ->
                                    val isExpanded = expandedTreeState[folderPath] ?: true
                                    item(key = "folder_$folderPath") {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { expandedTreeState[folderPath] = !isExpanded }
                                                .padding(vertical = 6.dp, horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = folderPath.substringAfterLast('/'),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "${notesInFolder.size}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }

                                    if (isExpanded) {
                                        items(notesInFolder, key = { "note_${it.id}" }) { note ->
                                            var showMenu by remember { mutableStateOf(false) }
                                            val isSelected = note.id == activeNote?.id

                                            Box(modifier = Modifier.padding(start = 24.dp, top = 2.dp, bottom = 2.dp)) {
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .combinedClickable(
                                                            onClick = {
                                                                editorVm.loadNote(note.id)
                                                                scope.launch { leftDrawerState.close() }
                                                            },
                                                            onLongClick = { showMenu = true }
                                                        ),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                                    ),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(8.dp)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                Icons.Outlined.Description,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(14.dp),
                                                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = note.name,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                fontSize = 13.sp,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        Text(
                                                            text = note.folderPath,
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.outline
                                                        )
                                                        val preview = remember(note.content) {
                                                            note.content.lineSequence().filter { it.isNotBlank() }.take(2).joinToString(" ")
                                                        }
                                                        if (preview.isNotBlank()) {
                                                            Text(
                                                                text = preview,
                                                                fontSize = 11.sp,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }

                                                DropdownMenu(
                                                    expanded = showMenu,
                                                    onDismissRequest = { showMenu = false }
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("Open in Floating Window") },
                                                        onClick = {
                                                            showMenu = false
                                                            editorVm.openFloatingWindow(note.id)
                                                            scope.launch { leftDrawerState.close() }
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Pin to Top Slot") },
                                                        onClick = {
                                                            showMenu = false
                                                            editorVm.addPinnedTop(note.id)
                                                            scope.launch { leftDrawerState.close() }
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Pin to Bottom Slot") },
                                                        onClick = {
                                                            showMenu = false
                                                            editorVm.addPinnedBottom(note.id)
                                                            scope.launch { leftDrawerState.close() }
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Delete") },
                                                        onClick = {
                                                            showMenu = false
                                                            bookVm.deleteNote(note.id)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { showCreateNoteDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Note", fontSize = 12.sp)
                            }
                            TextButton(onClick = { folderPickerLauncher.launch(null) }) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("External SAF", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ModalNavigationDrawer(
                    drawerState = rightDrawerState,
                    gesturesEnabled = true,
                    drawerContent = {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            ModalDrawerSheet(modifier = Modifier.width(320.dp)) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TabRow(selectedTabIndex = rightDrawerTab) {
                            Tab(
                                selected = rightDrawerTab == 0,
                                onClick = { rightDrawerTab = 0 },
                                text = { Text("Pinned Notes", fontWeight = FontWeight.Bold) }
                            )
                            Tab(
                                selected = rightDrawerTab == 1,
                                onClick = { rightDrawerTab = 1 },
                                text = { Text("Outline (${outline.size})", fontWeight = FontWeight.Bold) }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        if (rightDrawerTab == 0) {
                            // Split Screen Pinned Notes View (Top & Bottom)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            ) {
                                // Top Slot Half
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    val currentTopId = pinnedTopNotes.getOrNull(pinnedTopIndex)
                                    val currentTopNote = remember(currentTopId, allNotes) {
                                        allNotes.firstOrNull { it.id == currentTopId }
                                    }

                                    if (currentTopNote == null) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable { filePickerTargetSlot = "top" },
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Default.AddCircleOutline,
                                                contentDescription = "Pick a note to pin",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                "Pick a note to pin",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    } else {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            // Split Screen Pure Text View
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth()
                                                    .verticalScroll(rememberScrollState())
                                                    .padding(12.dp)
                                            ) {
                                                Column {
                                                    Text(
                                                        currentTopNote.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        currentTopNote.content.ifBlank { "(Empty note content)" },
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        // Top Right Actions Overlay
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                                    CircleShape
                                                )
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (pinnedTopNotes.size > 1) {
                                                IconButton(onClick = { editorVm.prevPinnedTop() }, modifier = Modifier.size(24.dp)) {
                                                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Prev", modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(onClick = { editorVm.nextPinnedTop() }, modifier = Modifier.size(24.dp)) {
                                                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next", modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            IconButton(onClick = { filePickerTargetSlot = "top" }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.SwapHoriz, contentDescription = "Switch Note", modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(onClick = { editorVm.loadNote(currentTopNote.id) }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit in Main", modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(onClick = { filePickerTargetSlot = "top" }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Add, contentDescription = "Add More Notes", modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(onClick = { editorVm.removePinnedTop(currentTopNote.id) }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Close, contentDescription = "Unpin", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                                // Bottom Slot Half
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    val currentBottomId = pinnedBottomNotes.getOrNull(pinnedBottomIndex)
                                    val currentBottomNote = remember(currentBottomId, allNotes) {
                                        allNotes.firstOrNull { it.id == currentBottomId }
                                    }

                                    if (currentBottomNote == null) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable { filePickerTargetSlot = "bottom" },
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Default.AddCircleOutline,
                                                contentDescription = "Pick a note to pin",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                "Pick a note to pin",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    } else {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            // Split Screen Pure Text View
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth()
                                                    .verticalScroll(rememberScrollState())
                                                    .padding(12.dp)
                                            ) {
                                                Column {
                                                    Text(
                                                        currentBottomNote.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        currentBottomNote.content.ifBlank { "(Empty note content)" },
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        // Top Right Actions Overlay
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                                    CircleShape
                                                )
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (pinnedBottomNotes.size > 1) {
                                                IconButton(onClick = { editorVm.prevPinnedBottom() }, modifier = Modifier.size(24.dp)) {
                                                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Prev", modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(onClick = { editorVm.nextPinnedBottom() }, modifier = Modifier.size(24.dp)) {
                                                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next", modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            IconButton(onClick = { filePickerTargetSlot = "bottom" }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.SwapHoriz, contentDescription = "Switch Note", modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(onClick = { editorVm.loadNote(currentBottomNote.id) }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit in Main", modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(onClick = { filePickerTargetSlot = "bottom" }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Add, contentDescription = "Add More Notes", modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(onClick = { editorVm.removePinnedBottom(currentBottomNote.id) }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Close, contentDescription = "Unpin", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Outline Tab
                            if (outline.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No headings yet. Use # Heading to structure your writing.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(outline) { entry ->
                                        Card(
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
                                                },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.primary
                                                    ) {
                                                        Text(
                                                            "H${entry.level}",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onPrimary,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = entry.text,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                val headingPreview = remember(activeNote?.content, entry.text) {
                                                    getOutlineHeadingPreview(activeNote?.content ?: "", entry.text)
                                                }
                                                if (headingPreview.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = headingPreview,
                                                        fontSize = 12.sp,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Scaffold(
                    topBar = {
                        if (!zenMode) {
                            TopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                                    actionIconContentColor = MaterialTheme.colorScheme.primary,
                                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                                ),
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
                                        Icon(Icons.Default.FormatListBulleted, contentDescription = "Outline & Pinned")
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

                            val currentThemeBg = MaterialTheme.colorScheme.background
                            val currentThemeTextColor = MaterialTheme.colorScheme.onBackground
                            val currentThemePrimary = MaterialTheme.colorScheme.primary
                            val hasBgImage = !activeTheme?.backgroundImageUri.isNullOrEmpty()

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
                                update = { scrollView ->
                                    val edit = editorRef
                                    if (edit != null) {
                                        val bgArgb = if (hasBgImage) android.graphics.Color.TRANSPARENT else currentThemeBg.toArgb()
                                        val textArgb = currentThemeTextColor.toArgb()
                                        val primaryArgb = currentThemePrimary.toArgb()

                                        scrollView.setBackgroundColor(bgArgb)
                                        edit.setBackgroundColor(bgArgb)
                                        edit.setTextColor(textArgb)

                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                            edit.textCursorDrawable?.setTint(primaryArgb)
                                        }
                                        activeTheme?.let { t ->
                                            val tf = com.primaloptima.scribe.util.ThemeManager.resolveTypeface(edit.context, t.fontFamily)
                                            edit.typeface = tf
                                            edit.textSize = t.fontSize.toFloat()
                                        }
                                    }

                                    activeNote?.let { note ->
                                        if (editorRef?.text?.toString() != note.content) {
                                            editorRef?.setText(note.content)
                                            if (findQuery.isNotBlank()) {
                                                val pos = note.content.indexOf(findQuery, ignoreCase = true)
                                                if (pos >= 0) {
                                                    editorRef?.setSelection(pos)
                                                }
                                            }
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
    }
}

        // Floating Windows Overlay
        FloatingWindowOverlay(
            floatingWindows = floatingWindows,
            notes = currentBookNotes,
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

    filePickerTargetSlot?.let { targetSlot ->
        FileExplorerOverlayDialog(
            allNotes = if (leftDrawerMode == "Current") currentBookNotes else allNotes,
            allFolders = if (leftDrawerMode == "Current") currentBookFolders else allFolders,
            onSelectNote = { note ->
                if (targetSlot == "top") {
                    editorVm.addPinnedTop(note.id)
                } else {
                    editorVm.addPinnedBottom(note.id)
                }
                filePickerTargetSlot = null
            },
            onDismiss = { filePickerTargetSlot = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileExplorerOverlayDialog(
    allNotes: List<Note>,
    allFolders: List<Folder>,
    onSelectNote: (Note) -> Unit,
    onDismiss: () -> Unit
) {
    val expandedPaths = remember { mutableStateMapOf<String, Boolean>() }
    val folderGrouped = remember(allNotes, allFolders) {
        val map = mutableMapOf<String, MutableList<Note>>()
        allNotes.forEach { n ->
            val f = n.folderPath.ifBlank { "/" }
            map.getOrPut(f) { mutableListOf() }.add(n)
        }
        map
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick a note to pin", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                folderGrouped.forEach { (folderPath, notesInFolder) ->
                    val isExpanded = expandedPaths[folderPath] ?: true
                    item(key = "f_$folderPath") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { expandedPaths[folderPath] = !isExpanded }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                folderPath.substringAfterLast('/'),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (isExpanded) {
                        items(notesInFolder, key = { "n_${it.id}" }) { note ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 20.dp, top = 2.dp, bottom = 2.dp)
                                    .clickable { onSelectNote(note) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(note.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    val preview = remember(note.content) {
                                        note.content.lineSequence().filter { it.isNotBlank() }.take(2).joinToString(" ")
                                    }
                                    if (preview.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            preview,
                                            fontSize = 11.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun getOutlineHeadingPreview(content: String, headingText: String): String {
    val pos = content.indexOf(headingText)
    if (pos < 0) return ""
    val after = content.substring(pos + headingText.length).trimStart()
    val lines = after.lineSequence().filter { it.isNotBlank() && !it.trimStart().startsWith("#") }.take(2).toList()
    return lines.joinToString(" ")
}

private fun getPreviewSnippet(content: String, query: String): String {
    if (query.isBlank()) return content.take(100)
    val idx = content.indexOf(query, ignoreCase = true)
    if (idx < 0) return content.take(100)
    val start = (idx - 30).coerceAtLeast(0)
    val end = (idx + query.length + 50).coerceAtMost(content.length)
    return (if (start > 0) "..." else "") + content.substring(start, end) + (if (end < content.length) "..." else "")
}

private fun buildHighlightedText(fullText: String, query: String, highlightColor: Color) = buildAnnotatedString {
    if (query.isBlank()) {
        append(fullText)
        return@buildAnnotatedString
    }
    var startIndex = 0
    while (true) {
        val matchIndex = fullText.indexOf(query, startIndex, ignoreCase = true)
        if (matchIndex < 0) {
            append(fullText.substring(startIndex))
            break
        }
        append(fullText.substring(startIndex, matchIndex))
        withStyle(style = SpanStyle(background = highlightColor.copy(alpha = 0.35f), fontWeight = FontWeight.Bold)) {
            append(fullText.substring(matchIndex, matchIndex + query.length))
        }
        startIndex = matchIndex + query.length
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
