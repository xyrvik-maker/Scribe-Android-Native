# Scribe Android – Full Porting & Rebuild Plan
_Last updated: working document, updated as bugs are found/fixed_

---

## Architecture Overview

### Activity Stack (new)
```
HomeActivity  (LAUNCHER)  ← books grid/list
  └─ BookActivity          ← files/folders inside a book
       └─ MainActivity     ← editor (note open)
```

### Data Layer Changes (v1 → v2 migration)
- ADD: `books` table (id, title, cover_uri, created_at, updated_at, sort_order)
- ALTER: `notes` ADD `book_id TEXT DEFAULT 'default'`
- RECREATE: `folders` with composite PK `(book_id, path)`
- Existing data migrated into a "My Notes" default book

---

## Feature Inventory

### ✅ Already in Kotlin app
- ScribeEditText: smart pairs, skip-over, undo/redo, find/replace
- ThemeManager + 5 built-in themes + custom theme editor
- ShortcutBarView (keyboard accessory)
- Room DB: notes, folders, world_entries
- PrefsManager: all pref keys
- Writing stats (word count, streak, goals)
- Version history / snapshots
- SAF external folder sync
- HistoryActivity, SettingsActivity, SheetsActivity, ShortcutsActivity

### ❌ Missing / not ported from RN
- [x] Book/project top-level entity
- [x] HomeActivity (books homepage with shelve/list view modes)
- [x] BookActivity (inside-book file/folder view, tabs+cards or tree)
- [x] Left panel redesign: file tree + bottom bar (settings/theme/characters)
- [x] Right panel redesign: outline + reference pinned notes (keep existing structure)
- [x] Floating reference windows (FloatingWindow model exists, no UI)
- [ ] Full export (stubbed – markdown/HTML only real, PDF/EPUB/DOCX deferred)

---

## Bugs Found

### Critical (crash / build failure)
- [FIXED] Gradle wrapper JAR validation failure (CI broken)
- [FIXED] ClassCastException: MaterialButton cast to ImageButton (btn_replace_one/all)
- [TODO] NoteListViewModel.checkFirstLaunch must also seed a default Book row

### High (wrong behavior)
- [TODO] No back-navigation from editor to BookActivity (currently back = press again = exit)
- [TODO] Editor left panel still shows flat note list, not book file tree
- [TODO] Folder entity PK conflict when multiple books share same path string

### Medium
- [TODO] FloatingWindow model has no UI implementation
- [TODO] Export: PDF/EPUB/DOCX routes hit no-op stubs
- [TODO] Recovery window too small: autosave fires ~500ms, recovery check on same cycle

### Low
- [TODO] Outline extraction misses headings inside code blocks (MarkdownUtil)
- [TODO] Word count on NoteAdapter uses stripped markdown (OK, just FYI)

---

## Implementation Plan

### Batch 1 – Data Layer
1. `data/Book.kt` – entity
2. `data/BookDao.kt` – DAO
3. `data/Note.kt` – add book_id field
4. `data/NoteDao.kt` – add bookId-scoped queries
5. `data/AppDatabase.kt` – bump v2, add migration, add BookDao

### Batch 2 – ViewModels
6. `viewmodel/HomeViewModel.kt`
7. `viewmodel/BookViewModel.kt`
8. `viewmodel/NoteListViewModel.kt` – update for bookId scoping + seed default book

### Batch 3 – Activities
9.  `HomeActivity.kt` – books grid/list, FAB, search, sort menu
10. `BookActivity.kt` – file/folder view, top card, view mode, FAB
11. `MainActivity.kt` – redesign panels, add bookId routing, back nav

### Batch 4 – Adapters
12. `adapter/BookAdapter.kt` – grid + list dual mode
13. `adapter/FileTreeAdapter.kt` – tree/list file browser
14. `FloatingWindowManager.kt` – draggable overlay windows

### Batch 5 – Layouts (new)
15. `layout/activity_home.xml`
16. `layout/activity_book.xml`
17. `layout/item_book_grid.xml`
18. `layout/item_book_list.xml`
19. `layout/item_file_tree.xml`
20. `layout/item_file_card.xml`

### Batch 6 – Layouts (updated)
21. `layout/activity_main.xml` – new left panel (file tree + bottom bar)

### Batch 7 – Config
22. `AndroidManifest.xml` – register new activities, HomeActivity as launcher
23. `values/strings.xml` – new strings

### Batch 8 – Validate & Push
- Compile check (grep for obvious syntax issues)
- Verify Manifest references all new activities
- Git commit + push → CI builds APK

---

## Fix Log (append as fixes are made)

| # | File | Issue | Status |
|---|------|-------|--------|
| 1 | build.yml | Gradle wrapper JAR validation blocks CI | FIXED |
| 2 | MainActivity.kt:246 | ImageButton cast on MaterialButton | FIXED |
| 3 | AppDatabase.kt | No v2 migration for Book entity | FIXING |
| 4 | NoteListViewModel.kt | No default Book seed on first launch | FIXING |
| 5 | activity_main.xml | Left panel is flat note list, not file tree | FIXING |
| 6 | AndroidManifest.xml | HomeActivity not registered as launcher | FIXING |
