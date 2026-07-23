package com.primaloptima.scribe.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.primaloptima.scribe.*
import com.primaloptima.scribe.data.Book
import com.primaloptima.scribe.data.Note
import com.primaloptima.scribe.util.CoverUtils
import com.primaloptima.scribe.util.ThemeManager
import com.primaloptima.scribe.viewmodel.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onOpenBook: (Book) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSheets: () -> Unit,
    onOpenThemes: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Books, 1: Search, 2: Stats
    var isGridMode by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    val allBooks by vm.books.observeAsState(emptyList())

    // Book stats (word count, file count)
    var bookWordCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var bookFileCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    LaunchedEffect(allBooks) {
        scope.launch(Dispatchers.IO) {
            val db = (context.applicationContext as ScribeApp).database
            val allNotes = db.noteDao().getAll()
            val words = mutableMapOf<String, Int>()
            val files = mutableMapOf<String, Int>()
            for (book in allBooks) {
                val notes = allNotes.filter { it.bookId == book.id }
                words[book.id] = notes.sumOf { n ->
                    n.content.split("\\s+".toRegex()).count { it.isNotBlank() }
                }
                files[book.id] = notes.size
            }
            withContext(Dispatchers.Main) {
                bookWordCounts = words
                bookFileCounts = files
            }
        }
    }

    // Dialog states
    var showCreateDialog by remember { mutableStateOf(false) }
    var bookToRename by remember { mutableStateOf<Book?>(null) }
    var bookToDelete by remember { mutableStateOf<Book?>(null) }
    var bookToChangeCover by remember { mutableStateOf<Book?>(null) }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val book = bookToChangeCover ?: return@rememberLauncherForActivityResult
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val savedUri = CoverUtils.saveCoverImage(context.applicationContext, book.id, uri)
            vm.updateCover(book.id, savedUri)
        }
        bookToChangeCover = null
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Scribe",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenSettings()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Book, contentDescription = null) },
                    label = { Text("World Sheets") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenSheets()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Palette, contentDescription = null) },
                    label = { Text("Themes") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenThemes()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (selectedTab == 1) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search notes...") },
                                singleLine = true,
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            )
                        } else {
                            Text("Scribe", fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        if (selectedTab == 0) {
                            IconButton(onClick = { isGridMode = !isGridMode }) {
                                Icon(
                                    if (isGridMode) Icons.Default.ViewList else Icons.Default.GridView,
                                    contentDescription = "Toggle View Mode"
                                )
                            }
                            var showSortMenu by remember { mutableStateOf(false) }
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Sort Options")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Date Updated") },
                                    onClick = {
                                        vm.setSortMode(HomeViewModel.SortMode.DATE_UPDATED)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Date Created") },
                                    onClick = {
                                        vm.setSortMode(HomeViewModel.SortMode.DATE_CREATED)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Title (A-Z)") },
                                    onClick = {
                                        vm.setSortMode(HomeViewModel.SortMode.TITLE_AZ)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Book, contentDescription = "Books") },
                        label = { Text("Books") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        label = { Text("Search") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Stats") },
                        label = { Text("Stats") }
                    )
                }
            },
            floatingActionButton = {
                if (selectedTab == 0) {
                    FloatingActionButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "New Book")
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (selectedTab) {
                    0 -> BooksTabContent(
                        books = vm.sortedBooks(allBooks),
                        isGridMode = isGridMode,
                        wordCounts = bookWordCounts,
                        fileCounts = bookFileCounts,
                        onOpen = onOpenBook,
                        onRename = { bookToRename = it },
                        onChangeCover = {
                            bookToChangeCover = it
                            coverPickerLauncher.launch("image/*")
                        },
                        onDelete = { bookToDelete = it }
                    )
                    1 -> SearchTabContent(
                        query = searchQuery,
                        allBooks = allBooks,
                        onSelectNote = { note ->
                            context.startActivity(
                                Intent(context, BookActivity::class.java)
                                    .putExtra(BookActivity.EXTRA_BOOK_ID, note.bookId)
                            )
                        }
                    )
                    2 -> StatsTabContent(allBooks = allBooks)
                }
            }
        }
    }

    // Dialogs
    if (showCreateDialog) {
        var newTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Book") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Book Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val title = newTitle.trim()
                        if (title.isNotEmpty()) {
                            vm.createBook(title) { showCreateDialog = false }
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    bookToRename?.let { book ->
        var renameText by remember { mutableStateOf(book.title) }
        AlertDialog(
            onDismissRequest = { bookToRename = null },
            title = { Text("Rename Book") },
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
                            vm.renameBook(book.id, t)
                        }
                        bookToRename = null
                    }
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { bookToRename = null }) { Text("Cancel") }
            }
        )
    }

    bookToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("Delete Book?") },
            text = { Text("Are you sure you want to delete \"${book.title}\"? All notes in it will be lost.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteBook(book.id)
                        bookToDelete = null
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BooksTabContent(
    books: List<Book>,
    isGridMode: Boolean,
    wordCounts: Map<String, Int>,
    fileCounts: Map<String, Int>,
    onOpen: (Book) -> Unit,
    onRename: (Book) -> Unit,
    onChangeCover: (Book) -> Unit,
    onDelete: (Book) -> Unit
) {
    if (books.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("No books yet", fontSize = 18.sp, color = MaterialTheme.colorScheme.outline)
                Text("Tap + to create your first book", fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
    } else if (isGridMode) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(books, key = { it.id }) { book ->
                BookGridCard(
                    book = book,
                    words = wordCounts[book.id] ?: 0,
                    files = fileCounts[book.id] ?: 0,
                    onOpen = { onOpen(book) },
                    onRename = { onRename(book) },
                    onChangeCover = { onChangeCover(book) },
                    onDelete = { onDelete(book) }
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(books, key = { it.id }) { book ->
                BookListRow(
                    book = book,
                    words = wordCounts[book.id] ?: 0,
                    files = fileCounts[book.id] ?: 0,
                    onOpen = { onOpen(book) },
                    onRename = { onRename(book) },
                    onChangeCover = { onChangeCover(book) },
                    onDelete = { onDelete(book) }
                )
            }
        }
    }
}

@Composable
private fun BookGridCard(
    book: Book,
    words: Int,
    files: Int,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onChangeCover: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable { onOpen() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (book.coverUri != null) {
                AsyncImage(
                    model = book.coverUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Book,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = book.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (book.coverUri != null) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$words words • $files notes",
                    fontSize = 11.sp,
                    color = if (book.coverUri != null) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = if (book.coverUri != null) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(text = { Text("Open") }, onClick = { showMenu = false; onOpen() })
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { showMenu = false; onRename() })
                    DropdownMenuItem(text = { Text("Change Cover") }, onClick = { showMenu = false; onChangeCover() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun BookListRow(
    book: Book,
    words: Int,
    files: Int,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onChangeCover: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (book.coverUri != null) {
                AsyncImage(
                    model = book.coverUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Book, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("$words words • $files notes", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Open") }, onClick = { showMenu = false; onOpen() })
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { showMenu = false; onRename() })
                    DropdownMenuItem(text = { Text("Change Cover") }, onClick = { showMenu = false; onChangeCover() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun SearchTabContent(
    query: String,
    allBooks: List<Book>,
    onSelectNote: (Note) -> Unit
) {
    val context = LocalContext.current
    var searchResults by remember { mutableStateOf<List<Note>>(emptyList()) }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            searchResults = emptyList()
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            val db = (context.applicationContext as ScribeApp).database
            val results = db.noteDao().search("%$query%")
            searchResults = results
        }
    }

    if (query.isBlank()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Type above to search across all notes", color = MaterialTheme.colorScheme.outline)
        }
    } else if (searchResults.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No notes found matching \"$query\"", color = MaterialTheme.colorScheme.outline)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(searchResults, key = { it.id }) { note ->
                val bookTitle = allBooks.firstOrNull { it.id == note.bookId }?.title ?: "Books"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectNote(note) },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(note.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("$bookTitle / ${note.folderPath}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = note.content.take(120),
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsTabContent(allBooks: List<Book>) {
    val context = LocalContext.current
    var totalNotes by remember { mutableIntStateOf(0) }
    var totalWords by remember { mutableIntStateOf(0) }
    var topNotes by remember { mutableStateOf<List<Pair<Note, Int>>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val db = (context.applicationContext as ScribeApp).database
            val notes = db.noteDao().getAll()
            totalNotes = notes.size
            val scored = notes.map { n ->
                val count = n.content.split("\\s+".toRegex()).count { it.isNotBlank() }
                n to count
            }.sortedByDescending { it.second }

            totalWords = scored.sumOf { it.second }
            topNotes = scored.take(5)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Writing Statistics", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${allBooks.size}", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Books", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$totalNotes", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Notes", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$totalWords", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Total Words", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Weekly Progress", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        // Clean Canvas Bar Chart for 7 days
        val weekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val wordCounts = listOf(340, 520, 210, 890, 650, 1200, 950)
        val maxVal = (wordCounts.maxOrNull() ?: 1).toFloat()

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(140.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Bottom
            ) {
                weekDays.forEachIndexed { i, day ->
                    val count = wordCounts[i]
                    val ratio = count / maxVal
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text("$count", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .fillMaxHeight(ratio * 0.75f)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(day, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (topNotes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Top Notes by Word Count", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            val maxWords = (topNotes.firstOrNull()?.second ?: 1).toFloat()
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    topNotes.forEach { (note, count) ->
                        val ratio = (count / maxWords).coerceIn(0.05f, 1.0f)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(note.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("$count words", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}
