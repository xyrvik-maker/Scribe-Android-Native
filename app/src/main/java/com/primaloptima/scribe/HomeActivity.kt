package com.primaloptima.scribe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.primaloptima.scribe.adapter.BookAdapter
import com.primaloptima.scribe.data.Book
import com.primaloptima.scribe.viewmodel.HomeViewModel

class HomeActivity : AppCompatActivity() {

    private val vm: HomeViewModel by viewModels()

    private lateinit var rvBooks: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var btnSearchClear: ImageButton
    private lateinit var btnViewMode: ImageButton
    private lateinit var btnOverflow: ImageButton
    private lateinit var fab: FloatingActionButton

    private lateinit var bookAdapter: BookAdapter

    private var isGridMode = true  // true = shelve (grid), false = list

    // Cover image picker
    private var pendingCoverBookId: String? = null
    private val coverPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        val bookId = pendingCoverBookId ?: return@registerForActivityResult
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        vm.updateCover(bookId, uri.toString())
        pendingCoverBookId = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        rvBooks        = findViewById(R.id.rv_books)
        emptyState     = findViewById(R.id.empty_state)
        etSearch       = findViewById(R.id.et_search)
        btnSearchClear = findViewById(R.id.btn_search_clear)
        btnViewMode    = findViewById(R.id.btn_view_mode)
        btnOverflow    = findViewById(R.id.btn_overflow)
        fab            = findViewById(R.id.fab_new_book)

        setupAdapter()
        setupSearch()
        setupButtons()
        observeBooks()
    }

    // ── Adapter & RecyclerView ────────────────────────────────────────────────

    private fun setupAdapter() {
        bookAdapter = BookAdapter(
            isGridMode = isGridMode,
            onClick = { book -> openBook(book) },
            onLongClick = { book, anchor -> showBookMenu(book, anchor) }
        )
        applyLayoutManager()
        rvBooks.adapter = bookAdapter
    }

    private fun applyLayoutManager() {
        rvBooks.layoutManager = if (isGridMode) {
            GridLayoutManager(this, 2)
        } else {
            LinearLayoutManager(this)
        }
    }

    // ── Observations ──────────────────────────────────────────────────────────

    private fun observeBooks() {
        vm.books.observe(this) { books ->
            val query = etSearch.text?.toString() ?: ""
            val display = if (query.isBlank()) vm.sortedBooks(books)
                          else vm.sortedBooks(books).filter {
                              it.title.contains(query, ignoreCase = true)
                          }
            bookAdapter.update(display, isGridMode)
            emptyState.visibility = if (display.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                btnSearchClear.visibility = if (query.isNotBlank()) View.VISIBLE else View.GONE
                vm.books.value?.let { all ->
                    val display = vm.sortedBooks(
                        if (query.isBlank()) all
                        else all.filter { it.title.contains(query, ignoreCase = true) }
                    )
                    bookAdapter.update(display, isGridMode)
                    emptyState.visibility = if (display.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        })
        btnSearchClear.setOnClickListener {
            etSearch.setText("")
        }
    }

    // ── Buttons ───────────────────────────────────────────────────────────────

    private fun setupButtons() {
        btnViewMode.setOnClickListener { toggleViewMode() }
        btnOverflow.setOnClickListener { showSortMenu() }
        fab.setOnClickListener { showCreateBookDialog() }
    }

    private fun toggleViewMode() {
        isGridMode = !isGridMode
        btnViewMode.setImageResource(
            if (isGridMode) R.drawable.ic_sidebar else R.drawable.ic_menu
        )
        applyLayoutManager()
        vm.books.value?.let { books ->
            bookAdapter.update(vm.sortedBooks(books), isGridMode)
        }
    }

    private fun showSortMenu() {
        val popup = PopupMenu(this, btnOverflow)
        popup.menu.apply {
            add(0, 0, 0, getString(R.string.sort_date_updated))
            add(0, 1, 1, getString(R.string.sort_date_created))
            add(0, 2, 2, getString(R.string.sort_title))
        }
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> vm.setSortMode(HomeViewModel.SortMode.DATE_UPDATED)
                1 -> vm.setSortMode(HomeViewModel.SortMode.DATE_CREATED)
                2 -> vm.setSortMode(HomeViewModel.SortMode.TITLE_AZ)
            }
            vm.books.value?.let { books ->
                bookAdapter.update(vm.sortedBooks(books), isGridMode)
            }
            true
        }
        popup.show()
    }

    // ── Book CRUD dialogs ─────────────────────────────────────────────────────

    private fun showCreateBookDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.hint_book_title)
            setSingleLine(true)
        }
        val container = FrameLayout(this).apply {
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
            addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.new_book))
            .setView(container)
            .setPositiveButton(getString(R.string.action_create)) { _, _ ->
                val title = input.text.toString().trim()
                if (title.isNotEmpty()) {
                    vm.createBook(title) { /* book created */ }
                }
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun showBookMenu(book: Book, anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.apply {
            add(0, 0, 0, getString(R.string.action_open))
            add(0, 1, 1, getString(R.string.action_rename))
            add(0, 2, 2, getString(R.string.change_cover))
            add(0, 3, 3, getString(R.string.action_delete))
        }
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> openBook(book)
                1 -> showRenameDialog(book)
                2 -> { pendingCoverBookId = book.id; coverPickerLauncher.launch("image/*") }
                3 -> confirmDeleteBook(book)
            }
            true
        }
        popup.show()
    }

    private fun showRenameDialog(book: Book) {
        val input = EditText(this).apply {
            setText(book.title)
            setSingleLine(true)
        }
        val container = FrameLayout(this).apply {
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
            addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.action_rename))
            .setView(container)
            .setPositiveButton(getString(R.string.action_rename)) { _, _ ->
                val t = input.text.toString().trim()
                if (t.isNotEmpty()) vm.renameBook(book.id, t)
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun confirmDeleteBook(book: Book) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_book_title))
            .setMessage(getString(R.string.delete_book_msg, book.title))
            .setPositiveButton(getString(R.string.action_delete)) { _, _ -> vm.deleteBook(book.id) }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private fun openBook(book: Book) {
        startActivity(
            Intent(this, BookActivity::class.java)
                .putExtra(BookActivity.EXTRA_BOOK_ID, book.id)
                .putExtra(BookActivity.EXTRA_BOOK_TITLE, book.title)
        )
    }
}
