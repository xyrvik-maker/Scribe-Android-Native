package com.primaloptima.scribe.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.primaloptima.scribe.BookActivity
import com.primaloptima.scribe.MainActivity
import com.primaloptima.scribe.ScribeApp
import com.primaloptima.scribe.data.Note
import com.primaloptima.scribe.util.CoverUtils
import com.primaloptima.scribe.viewmodel.BookViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookScreen(
    vm: BookViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val book by vm.book.observeAsState()
    val notes by vm.notes.observeAsState(emptyList())
    val folders by vm.folders.observeAsState(emptyList())
    val viewMode by vm.viewMode.observeAsState(BookViewModel.ViewMode.LIST)
    val sortMode by vm.sortMode.observeAsState(BookViewModel.SortMode.DATE_UPDATED)

    // Dialog states
    var showCreateNoteDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var noteToRename by remember { mutableStateOf<Note?>(null) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var selectedFolderPath by remember { mutableStateOf("/") }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val b = book ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val savedUri = CoverUtils.saveCoverImage(context.applicationContext, b.id, uri)
            val db = (context.applicationContext as ScribeApp).database
            db.bookDao().updateCover(b.id, savedUri, System.currentTimeMillis())
            vm.reload()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = book?.title ?: "Book Folders",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    label = { Text("Root (/)") },
                    selected = selectedFolderPath == "/",
                    onClick = {
                        selectedFolderPath = "/"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                folders.forEach { folder ->
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                        label = { Text(folder.path) },
                        selected = selectedFolderPath == folder.path,
                        onClick = {
                            selectedFolderPath = folder.path
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { showCreateFolderDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Folder")
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(book?.title ?: "Book", fontWeight = FontWeight.Bold)
                            if (selectedFolderPath != "/") {
                                Text("Folder: $selectedFolderPath", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Folder, contentDescription = "Folders")
                        }
                        IconButton(onClick = { vm.toggleViewMode() }) {
                            Icon(
                                if (viewMode == BookViewModel.ViewMode.LIST) Icons.Default.AccountTree else Icons.Default.List,
                                contentDescription = "Toggle View"
                            )
                        }
                        var showSortMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Change Book Cover") },
                                onClick = {
                                    showSortMenu = false
                                    coverPickerLauncher.launch("image/*")
                                }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Sort by Date Updated") },
                                onClick = {
                                    vm.setSortMode(BookViewModel.SortMode.DATE_UPDATED)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Date Created") },
                                onClick = {
                                    vm.setSortMode(BookViewModel.SortMode.DATE_CREATED)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Title (A-Z)") },
                                onClick = {
                                    vm.setSortMode(BookViewModel.SortMode.TITLE_AZ)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showCreateNoteDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "New Note")
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val filteredNotes = notes.filter {
                    if (selectedFolderPath == "/") true else it.folderPath == selectedFolderPath
                }

                if (filteredNotes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Description,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No notes in this folder", fontSize = 18.sp, color = MaterialTheme.colorScheme.outline)
                            Text("Tap + to create a note", fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else if (viewMode == BookViewModel.ViewMode.LIST) {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredNotes, key = { it.id }) { note ->
                            NoteListRow(
                                note = note,
                                onClick = {
                                    context.startActivity(
                                        Intent(context, MainActivity::class.java)
                                            .putExtra(MainActivity.EXTRA_NOTE_ID, note.id)
                                            .putExtra(MainActivity.EXTRA_BOOK_ID, vm.bookId)
                                    )
                                },
                                onOpenFloat = {
                                    context.startActivity(
                                        Intent(context, MainActivity::class.java)
                                            .putExtra(MainActivity.EXTRA_NOTE_ID, note.id)
                                            .putExtra(MainActivity.EXTRA_BOOK_ID, vm.bookId)
                                            .putExtra("openInFloat", true)
                                    )
                                },
                                onRename = { noteToRename = note },
                                onDuplicate = { vm.duplicateNote(note.id) },
                                onDelete = { noteToDelete = note }
                            )
                        }
                    }
                } else {
                    val treeItems = remember(notes, folders) { vm.buildTree() }
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(treeItems) { item ->
                            when (item) {
                                is BookViewModel.TreeItem.FolderItem -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = (item.depth * 16).dp, top = 8.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            item.folder.path,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                is BookViewModel.TreeItem.NoteItem -> {
                                    val note = item.note
                                    NoteListRow(
                                        note = note,
                                        modifier = Modifier.padding(start = (item.depth * 16).dp),
                                        onClick = {
                                            context.startActivity(
                                                Intent(context, MainActivity::class.java)
                                                    .putExtra(MainActivity.EXTRA_NOTE_ID, note.id)
                                                    .putExtra(MainActivity.EXTRA_BOOK_ID, vm.bookId)
                                            )
                                        },
                                        onOpenFloat = {
                                            context.startActivity(
                                                Intent(context, MainActivity::class.java)
                                                    .putExtra(MainActivity.EXTRA_NOTE_ID, note.id)
                                                    .putExtra(MainActivity.EXTRA_BOOK_ID, vm.bookId)
                                                    .putExtra("openInFloat", true)
                                            )
                                        },
                                        onRename = { noteToRename = note },
                                        onDuplicate = { vm.duplicateNote(note.id) },
                                        onDelete = { noteToDelete = note }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreateNoteDialog) {
        var noteTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateNoteDialog = false },
            title = { Text("New Note") },
            text = {
                OutlinedTextField(
                    value = noteTitle,
                    onValueChange = { noteTitle = it },
                    label = { Text("Note Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val t = noteTitle.trim()
                        if (t.isNotEmpty()) {
                            vm.createNote(t, selectedFolderPath) { id ->
                                showCreateNoteDialog = false
                                context.startActivity(
                                    Intent(context, MainActivity::class.java)
                                        .putExtra(MainActivity.EXTRA_NOTE_ID, id)
                                        .putExtra(MainActivity.EXTRA_BOOK_ID, vm.bookId)
                                )
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

    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name (e.g. Chapter 1)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val f = folderName.trim()
                        if (f.isNotEmpty()) {
                            val path = if (selectedFolderPath == "/") "/$f" else "$selectedFolderPath/$f"
                            vm.createFolder(path)
                            showCreateFolderDialog = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) { Text("Cancel") }
            }
        )
    }

    noteToRename?.let { note ->
        var renameText by remember { mutableStateOf(note.name) }
        AlertDialog(
            onDismissRequest = { noteToRename = null },
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
                            vm.renameNote(note.id, t)
                        }
                        noteToRename = null
                    }
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { noteToRename = null }) { Text("Cancel") }
            }
        )
    }

    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete Note?") },
            text = { Text("Are you sure you want to delete \"${note.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteNote(note.id)
                        noteToDelete = null
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun NoteListRow(
    note: Note,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onOpenFloat: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val wordCount = remember(note.content) {
        note.content.split("\\s+".toRegex()).count { it.isNotBlank() }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(note.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    text = "$wordCount words • ${note.folderPath}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Open") }, onClick = { showMenu = false; onClick() })
                    DropdownMenuItem(text = { Text("Open in Floating Window") }, onClick = { showMenu = false; onOpenFloat() })
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { showMenu = false; onRename() })
                    DropdownMenuItem(text = { Text("Duplicate") }, onClick = { showMenu = false; onDuplicate() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDelete() })
                }
            }
        }
    }
}
