# Scribe App - Comprehensive Improvement Plan

## Overview
This document outlines the detailed architecture and implementation plan to elevate Scribe into a premier, world-class writing app for novelists and authors.

---

## Task 1: Theme Engine & Universal UI Polish
- **Global Theme Application**:
  - Update `ScribeTheme.kt` and `ThemeManager.kt` so that `ScribeComposeTheme` supplies a complete Material3 `ColorScheme` (`primary`, `onPrimary`, `background`, `onBackground`, `surface`, `onSurface`, `surfaceVariant`, `onSurfaceVariant`, `outline`, etc.) derived from active `AppTheme`.
  - Ensure all screens (`HomeScreen`, `BookScreen`, `MainEditorScreen`, `SheetsScreen`, `ThemeListScreen`, `ThemeEditScreen`, `SettingsScreen`, `GuideScreen`, `HistoryScreen`, `ShortcutsScreen`) use `MaterialTheme.colorScheme` and respect custom font families, letter spacing, line height, background colors, and toolbar colors.
  - Fix dark theme contrast issues ensuring text and icons are 100% visible and sharp regardless of light/dark/custom theme.
- **Theme Image Picker & Custom Editor Background**:
  - Enhance `ThemeEditScreen` with custom background image selection, opacity slider (0%–100%), and color extraction or harmonious color palette pairing.
  - Apply custom background image rendering in the editor and throughout the app with subtle translucent gradient overlays for maximum legibility.

---

## Task 2: Homepage View Modes (Grid & List Layouts)
- **Grid View**: Book cover image / decorative feather book canvas + Book title below.
- **List View**: Book cover thumbnail + Title + Word count (`Tt <count>`) + File count (`📄 <count>`) + Book intro summary ("No book intro" if empty).
- Top bar toggle button to switch between Grid and List modes effortlessly.

---

## Task 3: Homepage Navigation & Advanced Statistics
- **Homepage Bottom Navigation Tabs**:
  1. **Book**: Displays book library in Grid/List mode with search & create action.
  2. **Files**: Multi-book file hierarchy explorer.
  3. **Notes**: Quick notes section. Pressing `+` FAB inside this tab auto-creates sequential quick notes (`Note_1`, `Note_2`, etc.) directly without prompting and opens the note.
  4. **Statistic**:
     - **Statistics Tab**: 7-day word count bar chart graph, plus side-by-side cards for **Book Counts** and **File Counts** in bold.
     - **Distribution / By File Tab**: Dropdown selector (`[Files, Folders, Books]`). Lists items sorted in descending order by word count with a location bar underneath and a horizontal progress indicator bar representing word count ranking.
- **Book-Specific Statistics (`BookScreen.kt`)**:
  - Bottom bar in `BookScreen` with **Write** and **Statistics** tabs.
  - Statistics tab inside book shows metrics, 7-day progress, and file distribution scoped *exclusively* to the current book.

---

## Task 4: Editor Right Swipe Panel (Pinned Notes & Markdown Outline)
- **Pinned Tab**:
  - Divided into Top and Bottom sections.
  - Empty state with `+ Pick a note to pin` button.
  - Clicking button opens a file picker overlay in collapsible tree mode (Books > Folders > Notes).
  - Section header controls: Switch/Swap icon, Edit icon (opens pinned note in editor), Unpin `X` icon, Add `+` icon, and `<` / `>` pagination arrows for cycling multiple pinned notes in a slot.
- **Outline Tab**:
  - Parses Markdown headings (`#`, `##`, `###`, etc.).
  - Displays headings in bold with line index, heading level badge, and multi-line preview text underneath.
  - Tapping a heading smoothly scrolls the editor to that exact heading position.

---

## Task 5: Homepage Full-Text Search with Highlighted Matches
- Real-time full-text search across all notes, folders, and books as user types.
- Highlights matching search text with theme-compatible highlight color background.
- Displays note title, book/folder location path, snippet context, and date updated.
- Tapping a search result opens the note directly in the editor.

---

## Task 6: Editor Left Swipe Panel (Enhanced File Explorer)
- Multi-level tree list of folders and files with distinct file icons, title, relative folder path line, and preview text lines.
- Top-right view mode selector: **Books** (explore across all books) vs **Current** (explore current book's folders/notes).

---

## Task 7: Character & World Sheet Enhancements (`SheetsScreen.kt`)
- Card-based UI for World Entries (Characters, Locations, Items, Factions, Lore).
- Left side: Avatar image thumbnail with interactive image picker to select character/location photo.
- Right side: Character/Location name, category badge, age/role, summary, and detail attributes.

---

## Task 8: Verification, Git Commits & Release Build
- Commit changes incrementally to git repository.
- Verify using `compile_applet` and Gradle release assemble to ensure 0 build errors.
