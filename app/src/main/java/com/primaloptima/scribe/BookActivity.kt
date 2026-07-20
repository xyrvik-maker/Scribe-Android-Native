package com.primaloptima.scribe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.primaloptima.scribe.adapter.FileCardAdapter
import com.primaloptima.scribe.adapter.FileTreeAdapter
import com.primaloptima.scribe.data.Folder
import com.primaloptima.scribe.data.Note
import com.primaloptima.scribe.viewmodel.BookViewModel

class BookActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BOOK_ID    = "bookId"
        const val EXTRA_BOOK_TITLE = "bookTitle"
    }

    private val vm: BookViewModel by viewModels()

    private lateinit var btnBack: ImageButton
    private lateinit var tvBookTitle: TextView
    private lateinit var btnViewMode: ImageButton
    private lateinit var btnOverflow: ImageButton
    private lateinit var imgCover: ImageView
    private lateinit var tvCardTitle: TextView
    private lateinit var tvCardSubtitle: TextView
    private lateinit var tvCardDate: TextView
    private lateinit var scrollFolderTabs: HorizontalScrollView
    private lateinit var folderTabs: LinearLayout
    private lateinit var rvFiles: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var fab: FloatingActionButton

    private lateinit var fileCardAdapter: FileCardAdapter
    private lateinit var fileTreeAdapter: FileTreeAdapter

    private var selectedFolderPath = "/"

    private val coverPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val uriStr = uri.toString()
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            (applicationContext as ScribeApp).database.bookDao()
                .updateCover(vm.bookId, uriStr, System.currentTimeMillis())
        }
        imgCover.load(uri) { crossfade(true) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book)

        val bookId    = intent.getStringExtra(EXTRA_BOOK_ID) ?: Note.DEFAULT_BOOK_ID
        val bookTitle = intent.getStringExtra(EXTRA_BOOK_TITLE) ?: ""

        bindViews()
        setupAdapters()
        vm.init(bookId)

        tvBookTitle.text = bookTitle
        tvCardTitle.text = bookTitle

        setupButtons()
        observeData()
    }

    private fun bindViews() {
        btnBack          = findViewById(R.id.btn_back)
        tvBookTitle      = findViewById(R.id.tv_book_title)
        btnViewMode      = findViewById(R.id.btn_view_mode)
        btnOverflow      = findViewById(R.id.btn_overflow)
        imgCover         = findViewById(R.id.img_cover)
        tvCardTitle      = findViewById(R.id.tv_card_title)
        tvCardSubtitle   = findViewById(R.id.tv_card_subtitle)
        tvCardDate       = findViewById(R.id.tv_card_date)
        scrollFolderTabs = findViewById(R.id.scroll_folder_tabs)
        folderTabs       = findViewById(R.id.folder_tabs)
        rvFiles          = findViewById(R.id.rv_files)
        emptyState       = findViewById(R.id.empty_state)
        fab              = findViewById(R.id.fab_new)
    }

    private fun setupAdapters() {
        fileCardAdapter = FileCardAdapter(
            onClick = { note -> openNote(note) },
            onLongClick = { note -> showNoteMenu(note) }
        )
        fileTreeAdapter = FileTreeAdapter(
            onNoteClick = { note -> openNote(note) },
            onFolderClick = { /* toggle expand in tree */ }
        )
        rvFiles.layoutManager = LinearLayoutManager(this)
        rvFiles.adapter = fileCardAdapter
    }

    private fun setupButtons() {
        btnBack.setOnClickListener { finish() }
        btnViewMode.setOnClickListener { vm.toggleViewMode() }
        btnOverflow.setOnClickListener { showOverflowMenu() }
        fab.setOnClickListener { showCreateMenu() }
    }

    private fun observeData() {
        vm.book.observe(this) { book ->
            book ?: return@observe
            tvCardTitle.text = book.title
            tvBookTitle.text = book.title
            if (book.coverUri != null) {
                imgCover.load(Uri.parse(book.coverUri)) { crossfade(true) }
            }
        }

        vm.notes.observe(this) { _ -> refreshFileView() }
        vm.folders.observe(this) { _ -> refreshFileView() }

        vm.viewMode.observe(this) { mode ->
            val isTree = mode == BookViewModel.ViewMode.TREE
            btnViewMode.setImageResource(if (isTree) R.drawable.ic_menu else R.drawable.ic_sidebar)
            scrollFolderTabs.visibility = if (isTree) View.GONE else View.VISIBLE
            refreshFileView()
        }
    }

    private fun refreshFileView() {
        val notes = vm.notes.value ?: emptyList()
        val mode = vm.viewMode.value ?: BookViewModel.ViewMode.LIST

        // Update info card subtitle
        tvCardSubtitle.text = resources.getQuantityString(
            R.plurals.note_count, notes.size, notes.size
        )

        if (mode == BookViewModel.ViewMode.TREE) {
            // Tree mode: show flat tree
            rvFiles.adapter = fileTreeAdapter
            val tree = vm.buildTree()
            fileTreeAdapter.update(tree)
            emptyState.visibility = if (tree.isEmpty()) View.VISIBLE else View.GONE
        } else {
            // List mode: folder tabs + cards
            rvFiles.adapter = fileCardAdapter
            rebuildFolderTabs()
            val visibleNotes = vm.notesInFolder(selectedFolderPath)
            fileCardAdapter.update(visibleNotes)
            emptyState.visibility = if (visibleNotes.isEmpty() && notes.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun rebuildFolderTabs() {
        folderTabs.removeAllViews()
        val folders = vm.folders.value ?: return

        // Root tab
        addFolderTab("/", "All", selectedFolderPath == "/")

        // Sub-folders (depth 1 from root)
        folders.filter { folder ->
            folder.path != "/" && !folder.path.removePrefix("/").contains("/")
        }.sortedBy { it.path }.forEach { folder ->
            val name = folder.path.trimStart('/')
            addFolderTab(folder.path, name, selectedFolderPath == folder.path)
        }
    }

    private fun addFolderTab(path: String, label: String, selected: Boolean) {
        val chip = Chip(this).apply {
            text = label
            isCheckable = false
            isChecked = selected
            isCheckedIconVisible = false
            isSelected = selected
            val dp8 = (8 * resources.displayMetrics.density).toInt()
            val dp4 = (4 * resources.displayMetrics.density).toInt()
            setPadding(dp8, dp4, dp8, dp4)
            val params = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            params.marginEnd = dp4
            layoutParams = params
            setOnClickListener {
                selectedFolderPath = path
                rebuildFolderTabs()
                fileCardAdapter.update(vm.notesInFolder(path))
            }
        }
        folderTabs.addView(chip)
    }

    // ── Menus ─────────────────────────────────────────────────────────────────

    private fun showCreateMenu() {
        val popup = PopupMenu(this, fab)
        popup.menu.apply {
            add(0, 0, 0, getString(R.string.new_text_file))
            add(0, 1, 1, getString(R.string.new_folder))
        }
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> showCreateNoteDialog()
                1 -> showCreateFolderDialog()
            }
            true
        }
        popup.show()
    }

    private fun showOverflowMenu() {
        val popup = PopupMenu(this, btnOverflow)
        popup.menu.apply {
            add(0, 0, 0, getString(R.string.sort_date_updated))
            add(0, 1, 1, getString(R.string.sort_date_created))
            add(0, 2, 2, getString(R.string.sort_title))
            add(0, 3, 3, getString(R.string.change_cover))
        }
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> vm.setSortMode(BookViewModel.SortMode.DATE_UPDATED)
                1 -> vm.setSortMode(BookViewModel.SortMode.DATE_CREATED)
                2 -> vm.setSortMode(BookViewModel.SortMode.TITLE_AZ)
                3 -> coverPickerLauncher.launch("image/*")
            }
            true
        }
        popup.show()
    }

    private fun showNoteMenu(note: Note) {
        val popup = PopupMenu(this, rvFiles)
        popup.menu.apply {
            add(0, 0, 0, getString(R.string.action_open))
            add(0, 1, 1, getString(R.string.action_rename))
            add(0, 2, 2, getString(R.string.action_delete))
        }
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> openNote(note)
                1 -> showRenameNoteDialog(note)
                2 -> confirmDeleteNote(note)
            }
            true
        }
        popup.show()
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    private fun showCreateNoteDialog() {
        val input = EditText(this).apply { hint = getString(R.string.hint_note_title); setSingleLine(true) }
        val container = FrameLayout(this).apply {
            val p = (20 * resources.displayMetrics.density).toInt()
            setPadding(p, p / 2, p, 0); addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.new_text_file))
            .setView(container)
            .setPositiveButton(getString(R.string.action_create)) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    vm.createNote(name, selectedFolderPath) { noteId ->
                        openNoteById(noteId)
                    }
                }
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun showCreateFolderDialog() {
        val input = EditText(this).apply { hint = getString(R.string.hint_folder_name); setSingleLine(true) }
        val container = FrameLayout(this).apply {
            val p = (20 * resources.displayMetrics.density).toInt()
            setPadding(p, p / 2, p, 0); addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.new_folder))
            .setView(container)
            .setPositiveButton(getString(R.string.action_create)) { _, _ ->
                val name = input.text.toString().trim().replace("/", "-")
                if (name.isNotEmpty()) {
                    val base = if (selectedFolderPath == "/") "" else selectedFolderPath
                    vm.createFolder("$base/$name")
                }
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun showRenameNoteDialog(note: Note) {
        val input = EditText(this).apply { setText(note.name); setSingleLine(true) }
        val container = FrameLayout(this).apply {
            val p = (20 * resources.displayMetrics.density).toInt()
            setPadding(p, p / 2, p, 0); addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.action_rename))
            .setView(container)
            .setPositiveButton(getString(R.string.action_rename)) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) vm.renameNote(note.id, name)
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun confirmDeleteNote(note: Note) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.action_delete))
            .setMessage(getString(R.string.delete_note_msg, note.name))
            .setPositiveButton(getString(R.string.action_delete)) { _, _ -> vm.deleteNote(note.id) }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private fun openNote(note: Note) = openNoteById(note.id)

    private fun openNoteById(noteId: String) {
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_NOTE_ID, noteId)
                .putExtra(MainActivity.EXTRA_BOOK_ID, vm.bookId)
        )
    }
}

