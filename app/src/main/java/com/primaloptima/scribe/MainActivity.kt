package com.primaloptima.scribe

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.primaloptima.scribe.adapter.FileTreeAdapter
import com.primaloptima.scribe.adapter.OutlineAdapter
import com.primaloptima.scribe.data.Note
import com.primaloptima.scribe.util.ExportHelper
import com.primaloptima.scribe.util.ThemeManager
import com.primaloptima.scribe.util.model.AppTheme
import com.primaloptima.scribe.util.model.ShortcutAction
import com.primaloptima.scribe.view.ScribeEditText
import com.primaloptima.scribe.view.ShortcutBarView
import com.primaloptima.scribe.viewmodel.BookViewModel
import com.primaloptima.scribe.viewmodel.EditorViewModel
import com.primaloptima.scribe.viewmodel.NoteListViewModel
import androidx.lifecycle.lifecycleScope
import com.primaloptima.scribe.util.model.PinnedItem
import com.primaloptima.scribe.viewmodel.ShortcutsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOTE_ID = "noteId"
        const val EXTRA_BOOK_ID = "bookId"
    }

    // ViewModels
    private val editorVm: EditorViewModel by viewModels()
    private val bookVm: BookViewModel by viewModels()
    private val noteListVm: NoteListViewModel by viewModels()
    private val shortcutsVm: ShortcutsViewModel by viewModels()

    // Core views
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var editorScrollView: ScrollView
    private lateinit var editor: ScribeEditText
    private lateinit var shortcutBar: ShortcutBarView
    private lateinit var topBar: LinearLayout
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var titleEditText: EditText
    private lateinit var findBar: LinearLayout
    private lateinit var findInput: EditText
    private lateinit var replaceInput: EditText
    private lateinit var tvMatchCount: TextView

    // Left drawer
    private lateinit var leftDrawer: LinearLayout
    private lateinit var rvFileTree: RecyclerView
    private lateinit var tvVaultName: TextView

    // Floating reference windows
    private lateinit var floatingWindowManager: FloatingWindowManager

    // Right drawer
    private lateinit var rightDrawer: LinearLayout
    private lateinit var rvOutline: RecyclerView
    private lateinit var pinnedTop: FrameLayout
    private lateinit var pinnedBottom: FrameLayout

    // Floating elements
    private lateinit var fabWordCount: FloatingActionButton
    private lateinit var tvWordCount: TextView
    private lateinit var recoveryBanner: LinearLayout

    // Zen mode
    private var zenMode = false

    // Find & Replace state
    private var findRegex = false
    private var findCaseSensitive = false

    // Adapters
    private lateinit var fileTreeAdapter: FileTreeAdapter
    private lateinit var outlineAdapter: OutlineAdapter

    // SAF folder picker
    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        val name = uri.lastPathSegment?.substringAfterLast(':') ?: "External Folder"
        noteListVm.connectExternalFolder(uri, name)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var typewriterRunnable: Runnable? = null

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        setupDrawers()
        setupEditor()
        setupShortcutBar()
        setupFindBar()
        setupFileTree()
        setupOutline()
        setupTopBar()
        setupRecoveryBanner()
        observeViewModels()
        handleBackPress()

        // Load book + note passed from BookActivity
        val bookId = intent.getStringExtra(EXTRA_BOOK_ID) ?: Note.DEFAULT_BOOK_ID
        bookVm.init(bookId)
        floatingWindowManager = FloatingWindowManager(this)
    }

    override fun onResume() {
        super.onResume()
        shortcutsVm.reload()
        editorVm.reloadTheme()
    }

    override fun onPause() {
        super.onPause()
        flushEditorContent()
    }

    // ── View binding ───────────────────────────────────────────────────────────

    private fun bindViews() {
        drawerLayout     = findViewById(R.id.drawer_layout)
        editorScrollView = findViewById(R.id.editor_scroll)
        editor           = findViewById(R.id.editor)
        shortcutBar      = findViewById(R.id.shortcut_bar)
        topBar           = findViewById(R.id.top_bar)
        tvTitle          = findViewById(R.id.tv_title)
        tvSubtitle       = findViewById(R.id.tv_subtitle)
        titleEditText    = findViewById(R.id.title_edit_text)
        findBar          = findViewById(R.id.find_bar)
        findInput        = findViewById(R.id.find_input)
        replaceInput     = findViewById(R.id.replace_input)
        tvMatchCount     = findViewById(R.id.tv_match_count)
        leftDrawer       = findViewById(R.id.left_drawer)
        rvFileTree       = findViewById(R.id.rv_file_tree)
        tvVaultName      = findViewById(R.id.tv_vault_name)
        rightDrawer      = findViewById(R.id.right_drawer)
        rvOutline        = findViewById(R.id.rv_outline)
        pinnedTop        = findViewById(R.id.pinned_top)
        pinnedBottom     = findViewById(R.id.pinned_bottom)
        recoveryBanner   = findViewById(R.id.recovery_banner)
        fabWordCount     = findViewById(R.id.fab_word_count)
        tvWordCount      = findViewById(R.id.tv_word_count)
    }

    // ── Drawers ────────────────────────────────────────────────────────────────

    private fun setupDrawers() {
        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerClosed(drawerView: View) {
                if (drawerView === leftDrawer) editor.requestFocus()
            }
            override fun onDrawerOpened(drawerView: View) {
                if (drawerView === rightDrawer) refreshPinnedSlots()
            }
        })
    }

    fun openLeftDrawer() = drawerLayout.openDrawer(GravityCompat.START)
    fun openRightDrawer() = drawerLayout.openDrawer(GravityCompat.END)

    // ── Editor ─────────────────────────────────────────────────────────────────

    private fun setupEditor() {
        editor.onTextChangedListener = { content ->
            editorVm.onContentChanged(content)
        }
        editor.onUndoRedoChanged = { canUndo, canRedo ->
            shortcutBar.setUndoEnabled(canUndo)
            shortcutBar.setRedoEnabled(canRedo)
        }
        editor.onCursorMovedListener = {
            if (editorVm.typewriterMode) applyTypewriterScroll()
        }

        // Double-tap to toggle zen mode
        editor.setOnClickListener(null)
        var lastTap = 0L
        editor.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastTap < 300) toggleZen()
            lastTap = now
        }

        // Word count badge
        fabWordCount.setOnClickListener { /* collapse/expand handled below */ }
        fabWordCount.visibility = View.GONE
    }

    private fun applyTypewriterScroll() {
        val layout = editor.layout ?: return
        val cursor = editor.selectionStart.coerceAtLeast(0)
        val line = layout.getLineForOffset(cursor)
        val lineTop = layout.getLineTop(line) + editor.top
        val visibleHeight = editorScrollView.height
        val targetY = lineTop - visibleHeight / 2
        editorScrollView.smoothScrollTo(0, maxOf(0, targetY))
    }

    // ── Shortcut bar ───────────────────────────────────────────────────────────

    private fun setupShortcutBar() {
        shortcutBar.listener = object : ShortcutBarView.Listener {
            override fun onUndoClick() { editor.undo() }
            override fun onRedoClick() { editor.redo() }
            override fun onShortcutClick(shortcut: ShortcutAction) {
                editor.applyShortcut(shortcut.kind, shortcut.payload, shortcut.closing)
            }
            override fun onAddShortcutClick() {
                startActivity(Intent(this@MainActivity, ShortcutsActivity::class.java))
            }
        }
    }

    // ── Find bar ───────────────────────────────────────────────────────────────

    private fun setupFindBar() {
        findBar.visibility = View.GONE
        findInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                editor.startFind(s?.toString() ?: "", findRegex, findCaseSensitive)
                updateMatchCount()
            }
        })
        findViewById<ImageButton>(R.id.btn_find_prev).setOnClickListener {
            editor.findPrev(); updateMatchCount()
        }
        findViewById<ImageButton>(R.id.btn_find_next).setOnClickListener {
            editor.findNext(); updateMatchCount()
        }
        findViewById<Button>(R.id.btn_replace_one).setOnClickListener {
            editor.replaceCurrentMatch(replaceInput.text.toString()); updateMatchCount()
        }
        findViewById<Button>(R.id.btn_replace_all).setOnClickListener {
            val count = editor.replaceAll(replaceInput.text.toString())
            Toast.makeText(this, "Replaced $count occurrence(s)", Toast.LENGTH_SHORT).show()
            updateMatchCount()
        }
        findViewById<ImageButton>(R.id.btn_find_close).setOnClickListener { hideFindBar() }
        findViewById<ImageButton>(R.id.btn_find_regex).setOnClickListener {
            findRegex = !findRegex
            it.alpha = if (findRegex) 1f else 0.4f
            editor.startFind(findInput.text.toString(), findRegex, findCaseSensitive)
            updateMatchCount()
        }
        findViewById<ImageButton>(R.id.btn_find_case).setOnClickListener {
            findCaseSensitive = !findCaseSensitive
            it.alpha = if (findCaseSensitive) 1f else 0.4f
            editor.startFind(findInput.text.toString(), findRegex, findCaseSensitive)
            updateMatchCount()
        }
    }

    fun showFindBar() {
        findBar.visibility = View.VISIBLE
        findInput.requestFocus()
    }

    private fun hideFindBar() {
        editor.clearFind()
        findBar.visibility = View.GONE
    }

    private fun updateMatchCount() {
        val total = editor.matchCount()
        val idx = editor.currentMatchIndex()
        tvMatchCount.text = if (total == 0) "No results" else "${idx + 1} / $total"
    }

    // ── Top bar ────────────────────────────────────────────────────────────────

    private fun setupTopBar() {
        findViewById<ImageButton>(R.id.btn_menu).setOnClickListener { openLeftDrawer() }
        findViewById<ImageButton>(R.id.btn_sidebar).setOnClickListener { openRightDrawer() }
        findViewById<ImageButton>(R.id.btn_find).setOnClickListener { showFindBar() }
        findViewById<ImageButton>(R.id.btn_more).setOnClickListener { showMoreMenu() }
        tvTitle.setOnClickListener { startTitleEdit() }
    }

    private fun startTitleEdit() {
        val note = editorVm.activeNote.value ?: return
        tvTitle.visibility = View.GONE
        tvSubtitle.visibility = View.GONE
        titleEditText.visibility = View.VISIBLE
        titleEditText.setText(note.name)
        titleEditText.selectAll()
        titleEditText.requestFocus()
        showKeyboard(titleEditText)
        titleEditText.setOnEditorActionListener { _, _, _ ->
            commitTitleEdit(); true
        }
        titleEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) commitTitleEdit()
        }
    }

    private fun commitTitleEdit() {
        val note = editorVm.activeNote.value ?: return
        val newName = titleEditText.text.toString().trim()
        if (newName.isNotEmpty() && newName != note.name) {
            noteListVm.renameNote(note.id, newName)
        }
        tvTitle.visibility = View.VISIBLE
        tvSubtitle.visibility = View.VISIBLE
        titleEditText.visibility = View.GONE
        hideKeyboard()
    }

    private fun showMoreMenu() {
        val note = editorVm.activeNote.value
        val items = mutableListOf("Typewriter mode", "Find & Replace", "Export", "Zen mode", "History",
            "Settings", "Themes")
        AlertDialog.Builder(this)
            .setItems(items.toTypedArray()) { _, which ->
                when (which) {
                    0 -> toggleTypewriterMode()
                    1 -> showFindBar()
                    2 -> showExportDialog(note)
                    3 -> { toggleZen() }
                    4 -> startActivity(Intent(this, HistoryActivity::class.java))
                    5 -> startActivity(Intent(this, SettingsActivity::class.java))
                    6 -> startActivity(Intent(this, ThemeListActivity::class.java))
                }
            }
            .show()
    }

    private fun toggleTypewriterMode() {
        val prefs = (application as ScribeApp).prefs
        prefs.typewriterMode = !prefs.typewriterMode
        Toast.makeText(this,
            if (prefs.typewriterMode) "Typewriter mode on" else "Typewriter mode off",
            Toast.LENGTH_SHORT).show()
    }

    // ── Zen mode ───────────────────────────────────────────────────────────────

    private fun toggleZen() {
        zenMode = !zenMode
        topBar.visibility = if (zenMode) View.GONE else View.VISIBLE
        shortcutBar.visibility = if (zenMode) View.GONE else View.VISIBLE
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val controller = window.insetsController
            if (zenMode) {
                controller?.hide(android.view.WindowInsets.Type.statusBars()
                        or android.view.WindowInsets.Type.navigationBars())
                controller?.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller?.show(android.view.WindowInsets.Type.statusBars()
                        or android.view.WindowInsets.Type.navigationBars())
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = if (zenMode)
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            else 0
        }
    }

    // ── File tree (left drawer) ────────────────────────────────────────────────

    private fun setupFileTree() {
        fileTreeAdapter = FileTreeAdapter(
            onNoteClick = { note -> openNote(note) },
            onFolderClick = { /* fold/unfold handled by adapter */ },
            onNoteLongClick = { note -> showNoteLongPressMenu(note) }
        )
        rvFileTree.layoutManager = LinearLayoutManager(this)
        rvFileTree.adapter = fileTreeAdapter

        // Bottom bar buttons
        findViewById<View>(R.id.btn_close_drawer).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        findViewById<View>(R.id.btn_drawer_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        findViewById<View>(R.id.btn_drawer_theme).setOnClickListener {
            startActivity(Intent(this, ThemeListActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        findViewById<View>(R.id.btn_drawer_characters).setOnClickListener {
            startActivity(Intent(this, SheetsActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun updateFileTree() {
        val tree = bookVm.buildTree()
        fileTreeAdapter.update(tree)
    }

    private fun openNote(note: Note) {
        (application as ScribeApp).prefs.activeNoteId = note.id
        editorVm.loadNote(note.id)
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun showNoteLongPressMenu(note: Note) {
        val prefs = (application as ScribeApp).prefs
        val pinned = prefs.getPinned()
        val isPinnedTop    = pinned.any { it.slot == "top"    && it.noteId == note.id }
        val isPinnedBottom = pinned.any { it.slot == "bottom" && it.noteId == note.id }

        val items = arrayOf(
            "Rename",
            "Move to…",
            "Duplicate",
            "Open in floating window",
            if (isPinnedTop)    "Unpin from panel (top)"    else "Pin to panel (top)",
            if (isPinnedBottom) "Unpin from panel (bottom)" else "Pin to panel (bottom)",
            "Export…",
            "Delete",
            "View History"
        )
        AlertDialog.Builder(this)
            .setTitle(note.name)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showRenameNoteDialog(note)
                    1 -> showMoveNoteDialog(note)
                    2 -> noteListVm.duplicateNote(note.id)
                    3 -> floatingWindowManager.openWindow(note)
                    4 -> togglePinNote(note.id, "top",    isPinnedTop)
                    5 -> togglePinNote(note.id, "bottom", isPinnedBottom)
                    6 -> showExportDialog(editorVm.activeNote.value?.let { if (it.id == note.id) it else null } ?: note)
                    7 -> confirmDeleteNote(note)
                    8 -> {
                        prefs.activeNoteId = note.id
                        startActivity(Intent(this, HistoryActivity::class.java))
                    }
                }
            }
            .show()
    }

    // ── Pin slots ──────────────────────────────────────────────────────────────

    private fun togglePinNote(noteId: String, slot: String, alreadyPinned: Boolean) {
        val prefs = (application as ScribeApp).prefs
        val pinned = prefs.getPinned().toMutableList()
        if (alreadyPinned) {
            pinned.removeAll { it.noteId == noteId && it.slot == slot }
        } else {
            pinned.removeAll { it.slot == slot }
            pinned.add(PinnedItem(slot = slot, noteId = noteId))
        }
        prefs.savePinned(pinned)
        refreshPinnedSlots()
    }

    private fun refreshPinnedSlots() {
        val pinned  = (application as ScribeApp).prefs.getPinned()
        val topId   = pinned.firstOrNull { it.slot == "top" }?.noteId
        val bottomId = pinned.firstOrNull { it.slot == "bottom" }?.noteId
        val divider = findViewById<View>(R.id.pinned_divider)

        populatePinnedSlot(pinnedTop,    topId)
        populatePinnedSlot(pinnedBottom, bottomId)
        divider.visibility = if (topId != null && bottomId != null) View.VISIBLE else View.GONE
    }

    private fun populatePinnedSlot(container: FrameLayout, noteId: String?) {
        container.removeAllViews()
        val lp = container.layoutParams as? LinearLayout.LayoutParams
        if (noteId == null) {
            container.visibility = View.GONE
            lp?.weight = 0f
            container.layoutParams = lp
            return
        }
        lifecycleScope.launch {
            val note = withContext(Dispatchers.IO) {
                (application as ScribeApp).database.noteDao().getById(noteId)
            } ?: return@launch
            container.addView(buildPinnedNoteView(note))
            container.visibility = View.VISIBLE
            lp?.weight = 1f
            container.layoutParams = lp
        }
    }

    private fun buildPinnedNoteView(note: Note): View {
        val d = { v: Int -> (v * resources.displayMetrics.density).toInt() }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            // Header: 📌 title + unpin button
            val header = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(d(12), d(8), d(4), d(4))
            }
            header.addView(TextView(context).apply {
                text = "\uD83D\uDCCC ${note.name}"
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            header.addView(ImageButton(context).apply {
                setImageResource(R.drawable.ic_close)
                setBackgroundResource(android.R.color.transparent)
                layoutParams = LinearLayout.LayoutParams(d(32), d(32))
                alpha = 0.4f
                setOnClickListener {
                    val prefs = (application as ScribeApp).prefs
                    prefs.savePinned(prefs.getPinned().filter { it.noteId != note.id })
                    refreshPinnedSlots()
                }
            })
            addView(header)
            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                setBackgroundColor(0x1A000000)
            })
            // Content scroll
            val scroll = ScrollView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
                isNestedScrollingEnabled = false
            }
            scroll.addView(TextView(context).apply {
                text = note.content.ifEmpty { "(empty)" }
                textSize = 12f
                setPadding(d(12), d(6), d(12), d(12))
                alpha = 0.8f
            })
            addView(scroll)
            setOnClickListener { openNote(note) }
        }
    }

    private fun showRenameNoteDialog(note: Note) {
        val input = EditText(this).also { it.setText(note.name); it.selectAll() }
        AlertDialog.Builder(this)
            .setTitle("Rename note")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) noteListVm.renameNote(note.id, name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMoveNoteDialog(note: Note) {
        val folders = noteListVm.folders.value
            ?.map { it.path }?.sorted() ?: listOf("/")
        AlertDialog.Builder(this)
            .setTitle("Move to folder")
            .setItems(folders.toTypedArray()) { _, which ->
                noteListVm.moveNote(note.id, folders[which])
            }
            .show()
    }

    private fun confirmDeleteNote(note: Note) {
        AlertDialog.Builder(this)
            .setTitle("Delete \"${note.name}\"?")
            .setMessage("This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                if (editorVm.activeNote.value?.id == note.id) {
                    // Clear the editor immediately and cancel pending autosave so it
                    // can't write the content back after the DB row is gone.
                    editor.setContentSilently("")
                    editorVm.clearActiveNote()
                }
                noteListVm.deleteNote(note.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRenameVaultDialog() {
        val prefs = (application as ScribeApp).prefs
        val input = EditText(this).also { it.setText(prefs.vaultName) }
        AlertDialog.Builder(this)
            .setTitle("Vault name")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) { prefs.vaultName = name; tvVaultName.text = name }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Outline (right drawer) ─────────────────────────────────────────────────

    private fun setupOutline() {
        outlineAdapter = OutlineAdapter { entry ->
            editor.jumpToLine(entry.lineIndex)
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        rvOutline.layoutManager = LinearLayoutManager(this)
        rvOutline.adapter = outlineAdapter
    }

    // ── Recovery banner ────────────────────────────────────────────────────────

    private fun setupRecoveryBanner() {
        recoveryBanner.visibility = View.GONE
        findViewById<View>(R.id.btn_recovery_restore).setOnClickListener {
            val content = editorVm.getRecoveryContent() ?: return@setOnClickListener
            editor.setContentSilently(content)
            editorVm.restoreSnapshot(content)
            editorVm.dismissRecovery()
            recoveryBanner.visibility = View.GONE
        }
        findViewById<View>(R.id.btn_recovery_dismiss).setOnClickListener {
            editorVm.dismissRecovery()
            recoveryBanner.visibility = View.GONE
        }
    }

    // ── Export ─────────────────────────────────────────────────────────────────

    private fun showExportDialog(note: Note?) {
        if (note == null) { Toast.makeText(this, "No note open", Toast.LENGTH_SHORT).show(); return }
        val formats = arrayOf("Plain Text (.txt)", "Markdown (.md)", "HTML (.html)",
            "PDF", "EPUB (.epub)", "Word (.docx)")
        val keys = arrayOf("txt", "md", "html", "pdf", "epub", "docx")
        AlertDialog.Builder(this)
            .setTitle("Export \"${note.name}\"")
            .setItems(formats) { _, which ->
                ExportHelper.shareNote(this, note, keys[which])
            }
            .show()
    }

    // ── Observe ViewModels ─────────────────────────────────────────────────────

    private fun observeViewModels() {
        // Active note → load content into editor
        editorVm.activeNote.observe(this) { note ->
            if (note == null) {
                editor.setContentSilently("")
                tvTitle.text = "Scribe"
                tvSubtitle.text = "no note open"
                return@observe
            }
            editor.setContentSilently(note.content)
            tvTitle.text = note.name
            val path = if (note.folderPath == "/") "" else "${note.folderPath}/"
            tvSubtitle.text = "$path${note.name}.${note.ext}"
        }

        // Theme
        editorVm.theme.observe(this) { theme -> applyTheme(theme) }

        // Outline
        editorVm.outline.observe(this) { entries -> outlineAdapter.submitList(entries) }

        // Word count
        editorVm.wordCount.observe(this) { count ->
            val rt = editorVm.readingTime.value ?: 0
            tvWordCount.text = "$count words · ${rt}min read"
        }

        // Goal
        editorVm.goalProgress.observe(this) { progress ->
            val reached = editorVm.goalReached.value ?: false
            val theme = editorVm.theme.value
            editor.setGoalProgress(progress, reached, theme?.colors?.accent ?: "#a8651e")
        }

        // Recovery
        editorVm.recoveryAvailable.observe(this) { available ->
            recoveryBanner.visibility = if (available) View.VISIBLE else View.GONE
        }

        // File tree — observes BookViewModel (book-scoped)
        bookVm.notes.observe(this) { notes ->
            updateFileTree()
            // Auto-open on first load: prefer intent noteId → prefs → first note
            if (editorVm.activeNote.value == null && notes.isNotEmpty()) {
                val prefId = (application as ScribeApp).prefs.activeNoteId
                val intentId = intent.getStringExtra(EXTRA_NOTE_ID)
                val targetId = intentId ?: prefId
                val note = notes.firstOrNull { it.id == targetId } ?: notes.first()
                editorVm.loadNote(note.id)
            }
        }
        bookVm.folders.observe(this) { _ -> updateFileTree() }

        bookVm.book.observe(this) { book ->
            tvVaultName.text = book?.title ?: getString(R.string.app_name)
        }

        // Shortcuts
        shortcutsVm.shortcuts.observe(this) { shortcuts ->
            val theme = editorVm.theme.value ?: return@observe
            shortcutBar.refreshShortcuts(shortcuts, theme)
        }
    }

    // ── Theme ──────────────────────────────────────────────────────────────────

    private fun applyTheme(theme: AppTheme) {
        val bg = ThemeManager.parseColor(theme.colors.background)
        val text = ThemeManager.parseColor(theme.colors.text)
        val toolbar = ThemeManager.parseColor(theme.colors.toolbar)
        val toolbarText = ThemeManager.parseColor(theme.colors.toolbarText)
        val accent = ThemeManager.parseColor(theme.colors.accent)
        val prefs = (application as ScribeApp).prefs

        // Root window
        window.decorView.setBackgroundColor(bg)
        window.statusBarColor = toolbar

        // Top bar
        topBar.setBackgroundColor(toolbar)
        tvTitle.setTextColor(toolbarText)
        tvSubtitle.setTextColor(ThemeManager.parseColor(theme.colors.mutedText))

        // Editor
        editor.setBackgroundColor(bg)
        editor.setTextColor(text)
        editor.setHintTextColor(ThemeManager.parseColor(theme.colors.mutedText))
        editor.setPadding(
            (theme.paddingHorizontal * resources.displayMetrics.density).toInt(),
            (theme.paddingVertical * resources.displayMetrics.density).toInt(),
            (theme.paddingHorizontal * resources.displayMetrics.density).toInt(),
            (theme.paddingVertical * resources.displayMetrics.density).toInt()
        )
        editor.textSize = theme.fontSize.toFloat()
        val typeface = ThemeManager.resolveTypeface(this, theme.fontFamily)
        editor.typeface = typeface
        editor.setLineSpacing(0f, ThemeManager.lineSpacingMultiplier(prefs.lineSpacing))
        editor.letterSpacing = theme.letterSpacing / (theme.fontSize * resources.displayMetrics.scaledDensity)

        // Shortcut bar
        val shortcuts = shortcutsVm.shortcuts.value ?: emptyList()
        shortcutBar.refreshShortcuts(shortcuts, theme)

        // Left / right drawers
        leftDrawer.setBackgroundColor(ThemeManager.parseColor(theme.colors.surface))
        rightDrawer.setBackgroundColor(ThemeManager.parseColor(theme.colors.surface))
    }

    // ── Keyboard helpers ───────────────────────────────────────────────────────

    private fun showKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editor.windowToken, 0)
    }

    // ── Back press ────────────────────────────────────────────────────────────

    private fun handleBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    zenMode -> toggleZen()
                    findBar.visibility == View.VISIBLE -> hideFindBar()
                    drawerLayout.isDrawerOpen(GravityCompat.START) ->
                        drawerLayout.closeDrawer(GravityCompat.START)
                    drawerLayout.isDrawerOpen(GravityCompat.END) ->
                        drawerLayout.closeDrawer(GravityCompat.END)
                    else -> finish()  // return to BookActivity
                }
            }
        })
    }

    // ── Flush on pause ─────────────────────────────────────────────────────────

    private fun flushEditorContent() {
        val content = editor.text?.toString() ?: return
        editorVm.flushContent(content)
    }
}
