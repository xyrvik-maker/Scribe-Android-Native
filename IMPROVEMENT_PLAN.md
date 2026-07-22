# Scribe Android – Master UI/UX & Feature Improvement Plan

## 1. Overview & Objectives
This document details the complete technical blueprint and execution roadmap for rebuilding and perfecting Scribe Android Native. The goal is to deliver a top-tier writing experience with unified dynamic themes, advanced statistics, dual-slot pinned note references, smooth outline navigation, high-contrast theme image pickers, and world-building card views.

---

## 2. Core Architectural Upgrades & Feature Modules

### Module 1: Universal Theme System & Custom Background Image Engine
- **Full App Coverage**: Update `ThemeManager` and base activity setup so theme background, text colors, card colors, input colors, status bar, and navigation bar colors apply consistently across all activities (`HomeActivity`, `BookActivity`, `MainActivity`, `SettingsActivity`, `ThemeEditActivity`, `SheetsActivity`, `ShortcutsActivity`, `HistoryActivity`).
- **Background Image Picker & Opacity Slider**:
  - Store `backgroundImageUri` and `bgOpacity` (0.0 to 1.0) in `PrefsManager` and `Theme` model.
  - Apply custom background image across activity root layouts and editor canvas with an adjustable dark/light overlay scrim.
  - Automatically enforce high-contrast text and icon colors when background images or dark themes are active.

### Module 2: Homepage Dual Views & Navigation (`HomeActivity`)
- **Dual View Modes**:
  - **Grid Mode**: Visually appealing book cards with cover image or theme gradient, title, word count tag, and folder/file counts.
  - **List Mode**: Detailed list items showing book cover thumbnail, title, date modified, folder count, file count, and total word count.
- **Homepage Bottom Navigation Bar**:
  - **Books**: Top-level book shelf and project manager.
  - **Notes**: Dedicated Quick Notes folder for standalone fast note-taking without cluttering book structures.
  - **Statistics**: App-wide analytical dashboard.

### Module 3: Global & Scoped Statistics System
- **Global Statistics (`HomeActivity` -> Stats Tab)**:
  - **Tab A: Statistics**:
    - 7-Day Word Count Bar Chart: Custom canvas/bar view rendering daily writing output for the past week.
    - Summary Cards: Bold count for **Book Count** and **File Count**, plus Total Words and Daily Average.
  - **Tab B: Contents Ranking**:
    - Filter tabs: **Files**, **Folders**, **Books**.
    - Descending list ordered by total word count.
    - Under title: Custom proportional progress bar reflecting word count ranking relative to max item.
    - Subtitle: Location breadcrumb path and word count.
- **Book-Scoped Statistics (`BookActivity`)**:
  - Bottom navigation bar inside `BookActivity` with two options: **Write** (File/Folder Explorer) and **Statistics** (scoped strictly to current book metrics).

### Module 4: Editor Right Swipe Panel - Pinned Notes & Smooth Outline
- **Tab 1: Pinned Notes (Dual Reference Slots)**:
  - Split layout with Top Slot and Bottom Slot for reference documents while writing.
  - Each slot header features: Edit Icon (open note in main editor), Close 'x' (unpin), Add '+' (open collapsible tree file explorer overlay to select note), and Left/Right pagination arrows when multiple notes are pinned in a slot.
- **Tab 2: Document Outline**:
  - Extracts Markdown headings (`#`, `##`, `###`, etc.).
  - Displays heading text in bold with a 2-3 line text preview snippet underneath.
  - Clicking heading triggers smooth scroll animation in `ScribeEditText` to exact line position.

### Module 5: Homepage Real-Time Highlighted Search
- Interactive search input on `HomeActivity`.
- As user types, searches note titles, paths, and text content across all books.
- Displays matching list items with query terms highlighted using theme-accented background colors.
- Subtitle shows location path, word count, and text snippet.

### Module 6: Left Swipe Panel File Explorer Redesign (`MainActivity`)
- Displays folders and files with folder/file icons, title, location path line, and 2-line snippet preview.
- Top-right toggle: **Books** (view all books and overall folder hierarchy) vs **Current** (filter to current book's folders and files).

### Module 7: World-Building Character & Location Cards (`SheetsActivity`)
- Rebuilt card layout: Left side avatar image thumbnail, right side metadata (Name, Category tag, Age, Description, Attributes).
- Reliable Image Picker: Pick image from gallery, copy file to internal app storage (`files/world_images/`), persist local file URI in `WorldEntry` Room entity, render cleanly with bitmap decoder.

---

## 3. Step-by-Step Implementation Roadmap

1. **Step 1**: Commit `IMPROVEMENT_PLAN.md` to git repository.
2. **Step 2**: Enhance Data Models & Database (`Theme`, `Book`, `Note`, `WorldEntry`, `NoteDao`, `BookDao`).
3. **Step 3**: Rebuild Theme System (`ThemeManager`, `ThemeEditActivity`, background image & opacity, text/icon contrast auto-tuning).
4. **Step 4**: Implement Homepage Dual Modes, Search, and Bottom Bar (`HomeActivity`, `BookAdapter`, `item_book_grid.xml`, `item_book_list.xml`).
5. **Step 5**: Build Statistics Views (7-day chart view, Book & File metrics cards, Content ranking tab, Book-scoped statistics).
6. **Step 6**: Implement Editor Right Panel (Dual-slot Pinned Reference Notes with tree file picker overlay, Smooth scrolling Outline with text preview).
7. **Step 7**: Update Editor Left Panel File Explorer (Icons, location breadcrumb, snippet preview, Books vs Current filter).
8. **Step 8**: Refactor Character & Location Section (`SheetsActivity`, image picker, local storage copy, card layout).
9. **Step 9**: Verify with Gradle build (`./gradlew assembleDebug`), commit to git, and push to GitHub remote.
