# Scribe Android — UI/UX Overhaul & Feature Update Plan

_Working document. Any AI agent picking this up mid-flight: read
`PORTING_PLAN.md` first for architecture, then this file for the active
workstream. Update the "Progress log" at the bottom after each commit._

Reference images from user (2026-07-21): see `docs/reference/` after the
first section-2 commit lands. Corrections from the images are baked into
the section descriptions below.

---

## Bottom navigation (canonical)

Four tabs on the Home surface, in this order and with these exact labels:

```
  Book   |   Files   |   Notes   |   Statistic
```

- **Book** — books grid or list (current HomeActivity content).
- **Files** — flat file explorer across all books (a mirror of the
  editor's left panel but at the home level).
- **Notes** — quick-notes folder (§9).
- **Statistic** — global stats (§3A). Two top tabs inside: `Statistic`
  and `Distribution` (the ranking view; matches the label the user's
  reference screenshot uses).

Inside a book (`BookActivity`), the bottom bar is only:

```
   Write   |   Statistic
```

Scoped to that book. Same components as global Statistic, filtered.

---

## Goals (from user, 2026-07-21)

Raise Scribe's visual quality to the level of top writing apps (Ulysses,
Bear, iA Writer, Scrivener). Themes must be beautiful, consistent across
the whole app, never break readability, and support an optional
background image with opacity — when the user picks a background image,
the app's palette derives a soft gradient from that image.

Feature additions: dual-mode homepage (grid + list) with rich book
cards, a four-tab home (Book / Files / Notes / Statistic),
per-book statistics, a redesigned right panel (Pinned + Outline) in the
editor, richer search with two-color highlighted matches, a redesigned
left panel (file tree with previews and Books/Current scope), and a
redesigned Characters/Locations screen with a working image picker.

---

## Section index

| #  | Area                                | Status  |
|----|-------------------------------------|---------|
| 1  | Theme system overhaul + BG image    | in progress |
| 2  | Home: grid + list book cards        | pending |
| 3  | Home tabs: Book/Files/Notes/Statistic| pending |
| 3A | Statistic tab (global) + Distribution| pending |
| 3B | Statistic tab (per-book)            | pending |
| 4  | Editor right panel: Pinned + Outline| pending |
| 5  | Outline smooth-scroll + preview     | pending |
| 6  | Search with themed highlights       | pending |
| 7  | Editor left panel: rich file tree   | pending |
| 8  | Characters/Locations card view      | pending |
| 9  | Notes quick-notes folder            | pending |

Order of implementation: **1 → 7 → 2 → 3/9 → 3A → 3B → 4 → 5 → 6 → 8**.

---

## 1 — Theme system overhaul (blocking prerequisite)

### Problems today
- Theme colors only reach a subset of views. Toolbars, dialogs, popups,
  adapter items, and system chrome keep AppCompat defaults; dark themes
  leave icons and text unreadable.
- No accessible-contrast guarantee — a custom theme can save a
  background and foreground that are indistinguishable.
- No background-image support beyond the existing (unused) URI + opacity
  fields on `AppTheme`.
- Text/icon tinting is ad-hoc per activity; adapters miss it.

### Design
1. **Existing tokens kept.** `ThemeColors` already has 9 fields
   (background/surface/text/mutedText/accent/border/selection/toolbar/
   toolbarText). We keep them and derive additional roles at runtime
   inside `ThemeManager`:
   - `surfaceElevated` = lighten/darken surface by 6% L\*
   - `foregroundSubtle` = mix mutedText with background at 40%
   - `overlayScrim` = background at 82% alpha (used behind bg image)
   - `primaryOn` = auto-picked black/white against `accent`
2. **New `AppTheme` fields** (all optional; migration is additive):
   - `backgroundImageBlur: Int? = null` (0–24 px)
   - `searchHighlightTitle: String? = null` (default derived from accent
     hue-shifted 180°)
   - `searchHighlightBody: String? = null` (default = accent)
3. **`ThemeChangeBus`** — a `MutableSharedFlow<AppTheme>` in `ScribeApp`
   emitted by `ThemeManager.setActiveTheme()`. All open activities and
   adapters collect from it and re-tint without a restart.
4. **`BaseThemedActivity`** — every activity extends this. Its
   `onResume` walks the decor tree once and applies theme tokens to
   every `Toolbar`, `AppBarLayout`, `MaterialButton`, `TextInputLayout`,
   `TabLayout`, `NavigationView`, `MaterialCardView`, plus any view
   tagged `theme:role=…`. Also observes `ThemeChangeBus`.
5. **Contrast guardrail** (`ContrastUtil`). On save (custom theme) and
   on activation, check WCAG 4.5:1 for body / 3.0:1 for large text on
   `text↔background` and `toolbarText↔toolbar`. If it fails, offer an
   auto-fix (lift/darken foreground L\* until it passes) in a modal.
6. **Background image**.
   - Picker in Theme Edit ("Background image"): SAF `OpenDocument` with
     persistable permission, plus a small built-in library
     (paper / linen / slate / vellum textures) shipped as drawables.
   - Renderer: full-decor `ImageView` behind content, tinted with
     `overlayScrim` at `(1 - opacity)`, blurred `blur` px (API 31+
     `RenderEffect`; older APIs fall back to no blur but keep opacity).
   - **Palette derivation** with androidx.palette. When user picks an
     image, extract vibrant/muted/dark-muted swatches, propose a
     derived theme: dominant → `surface`, vibrant → `accent`,
     dark-muted → `text`, background computed to keep AA contrast on
     text. Toggle: "Use derived palette".
7. **Built-in themes redesigned.** Replace the current five with eight,
   each tested for both light and dark parity and WCAG AA on body text:
   Paper, Ink, Slate, Midnight, Sepia, Forest, Ocean, Vellum. Each ships
   with a suggested texture.

### Files touched (section 1)
- `util/model/Models.kt` — extend `AppTheme` (additive fields)
- `util/DefaultThemes.kt` — 8 curated palettes
- `util/ThemeManager.kt` — bus emission, derived tokens, contrast
- `util/ThemeBus.kt` — new
- `util/ContrastUtil.kt` — new
- `util/PaletteExtractor.kt` — new
- `util/ThemedActivity.kt` — new base class
- Every activity: change `AppCompatActivity` → `ThemedActivity`
- `ThemeEditActivity` + `activity_theme_edit.xml` — BG image + opacity
  + blur sliders, derived-palette toggle, contrast warning strip
- `app/build.gradle.kts` — add `androidx.palette:palette-ktx`

### Acceptance
- Eight built-ins all pass WCAG AA on body text.
- Toggling theme instantly restyles every open surface (no restart).
- Picking a background image derives a palette + gradient scrim; body
  text stays AA against derived `text`.
- Custom theme with poor contrast triggers auto-fix modal.

---

## 2 — Home: dual-mode book cards

### List mode (per reference image 4)
Row = ~120 dp tall. Left: portrait cover thumbnail (3:4), rounded 10 dp.
Right column:
- Title (single line, ellipsize end, size 22sp)
- Row 1: `Tt` icon + total word count (comma-formatted) · `📄` icon +
  file count
- Row 2: book intro (single line, `foregroundMuted`, "No book intro"
  fallback)
- FAB `+` bottom-right for new book.

### Grid mode (per reference image 3)
Three columns on phones. Card = 3:4 portrait. Full-bleed cover with a
gradient scrim from transparent at top to `background` at bottom. Below
each cover: title in `foreground` (single line, ellipsize end).
Placeholder cover for books with none: dark diagonal panel with a
feather glyph watermark (matches reference).

### Toolbar (both modes)
- Left: hamburger drawer.
- Center: "Full-text search" pill (opens §6 bottom sheet).
- Right icons: star (favorites), display-mode (list ↔ grid toggle),
  overflow.
- Below toolbar: "Recent edit: <last note title>" row with a right
  arrow to open it, plus the mode-toggle icon on the far right (shows
  the *other* mode's glyph — list icon while in grid, grid icon while
  in list).

### Files touched
- `adapter/BookAdapter.kt` (grid + list view types)
- `item_book_grid.xml`, `item_book_list.xml` (rewrite)
- `HomeActivity.kt` (mode toggle, span count logic, DiffUtil)
- `viewmodel/HomeViewModel.kt` (persist view mode; expose
  `BookWithStats` — folder count / file count / word count / updatedAt)
- `data/BookDao.kt` (`@Transaction` query)

### Acceptance
- Switching mode animates smoothly.
- Stats reflect real-time DB state.
- Both layouts respect current theme tokens end-to-end.

---

## 3 — Home: Book / Files / Notes / Statistic

Container-based `HomeActivity` with a `BottomNavigationView` and four
fragments: `BookFragment`, `FilesFragment`, `NotesFragment`,
`StatisticFragment`.

### Files tab (new)
Flat file/folder explorer across all books, matching the editor's left
panel §7. Selectable scope pill at top: **All books** vs **Recent**.

### Files touched
- `HomeActivity.kt` → container.
- New: `BookFragment`, `FilesFragment`, `NotesFragment`,
  `StatisticFragment`, `DistributionFragment`.
- `activity_home.xml` → container + bottom nav.

## 3A — Statistic tab (global)

Two top tabs (`TabLayout`): **Statistic** and **Distribution**.

### Statistic tab
- 7-day chart: word count per day. Rounded bars, gradient fill
  `primary → accent`, day labels on x-axis, auto y-scale. Tap bar →
  tooltip with exact count.
- Below chart, two bold cards side by side:
  - **Books** — total book count.
  - **Files** — total file count across all books.
- Below cards, three secondary tiles: current streak, today's words vs.
  daily goal (progress ring), all-time words.

### Distribution tab (per reference image 1)
- Header row: title "Count distribution" + a dropdown on the right
  reading `File ▾` with options `File / Folder / Book`.
- List, descending by word count:
  - Title in bold `foreground`. Long paths ellipsized on the left
    (`…ystem/000 WORLD/(world) WORLD`), matching the reference.
  - Word count right-aligned, comma-formatted.
  - Horizontal progress bar under the title, fill = `words /
    max(rowWords)`, tinted `accent` (green in Slate/Midnight themes).
- Empty state per selection.

### Data
- New: `WritingStats.aggregateForRange(days=7)` → `List<DayStat>`.
- New: `WritingStats.distribution(scope: File|Folder|Book)` →
  `List<RankEntry>`.
- Both cached in `StatisticViewModel`.

### Files touched
- `viewmodel/StatisticViewModel.kt` (new)
- `fragment_statistic.xml`, `fragment_distribution.xml`
- `adapter/RankEntryAdapter.kt`
- `util/WritingStats.kt` (extend)

## 3B — Statistic tab (per-book)

Inside `BookActivity` bottom bar: **Write** | **Statistic**. Same
components as §3A, scoped to the current `bookId`.

---

## 4 — Editor right panel: Pinned + Outline (per reference image 2)

### Pinned tab
Two vertically stacked sections divided by a subtle line.
Each empty section shows a bookmark icon + "No note pinned to
top/bottom" + a pill outlined button `+ Pick a note to pin`.
Once filled:
- Header row: pinned-note title + icons (edit ✎, unpin ✕, add ✚).
- Body: rendered preview of that note (~200 chars, markdown rendered).
- Multiple notes per slot: chevron ‹ › + `2/4` counter cycles between
  them.
- Tap ✚ → file-explorer overlay in collapsible tree mode
  (books → folders → files).
- Tap ✎ → opens that note in a floating window
  (`FloatingWindowManager`); main note stays open.

### Outline tab (see §5)

### Files touched
- `activity_main.xml` (right drawer)
- New: `fragment_pinned.xml`, `PinnedSectionView`, section adapter
- `data/PinnedNote.kt` + DAO (bookId, section: TOP|BOTTOM, noteId,
  order) — DB v3
- `viewmodel/EditorViewModel.kt` (pinnedTop/pinnedBottom flows)
- New: `FileTreePickerFragment` (reused by search + pinning)

---

## 5 — Outline: bold headings + preview + smooth scroll

Each entry:
- Heading text bold, size scaled by heading level.
- Two-line muted preview (first paragraph after heading).
- Left indent by heading level.
- Tap → `ObjectAnimator` on scrollY, 300 ms, decelerate. Highlight the
  target heading briefly (200 ms accent fade).

### Files touched
- `adapter/OutlineAdapter.kt` (rewrite)
- `util/MarkdownUtil.kt` (return heading + preview + line offset)
- `MainActivity.kt` (smooth scroll helper)

---

## 6 — Search with themed highlights (per reference image 5)

Bottom-sheet search invoked from home toolbar. As user types (debounced
120 ms), query notes + folders across all books.

Result row shows:
- Title path (`Book/Folder/Note`) in `foreground`.
  Match runs inside the title highlighted with
  **`searchHighlightTitle`** — a cyan/blue at 100% alpha in the
  reference.
- Multi-line snippet from body. Match runs highlighted with
  **`searchHighlightBody`** — a green (accent-derived) at 100% alpha.
- Date line below (updatedAt, `yyyy-MM-dd`).

Two colors intentionally: title matches must stand out against the
title's ellipsized path.

### Files touched
- `HomeActivity` search pill → bottom sheet.
- New: `SearchBottomSheet`, `SearchViewModel`, `SearchResultAdapter`
  (spannable highlights).
- DAO queries: `LIKE` MVP; upgrade to FTS4 in v4 if perf warrants.

---

## 7 — Editor left panel: rich file tree

### Design
- Top bar with segmented toggle: **Books** (all books, expandable tree)
  vs **Current** (folders/files inside the current book only).
- Each item:
  - Small icon (folder / note / book) tinted `foregroundMuted`.
  - Title bold `foreground`.
  - Location line (`Book/Folder`), tiny, `foregroundSubtle`.
  - Snippet line (~40 chars) of note body, italic, subtle.
- Item padding 12 dp vertical, 4 dp between items.
- Long-press → context menu (rename/delete/move).

### Files touched
- `adapter/FileTreeAdapter.kt` (rewrite)
- `item_file_tree.xml` (rewrite)
- `MainActivity.kt` (left drawer top bar segmented toggle)
- `viewmodel/EditorViewModel.kt` (`treeScope: Books|Current`)

---

## 8 — Characters & Locations: cards + working image picker

Card grid (1 col phone, 2 col tablet). Each card:
- Left: 96 dp square image, rounded 12 dp, `centerCrop`.
- Right: name (bold 18sp) + key-value chips (age/role/location/faction).
- Long-press to delete; tap to edit.
- Image picker uses `ActivityResultContracts.OpenDocument` with
  `takePersistableUriPermission`; on load, decode with `ImageDecoder`
  and cache a downsized version in app-private storage keyed by URI
  hash to avoid re-decoding on scroll.

### Files touched
- `SheetsActivity.kt`, `activity_sheets.xml`
- `adapter/WorldEntryAdapter.kt`, `item_world_entry.xml`
- New: `util/ImageCache.kt`

---

## 9 — Notes (quick-notes folder)

Third home tab. Flat list of "quick notes" NOT scoped to a book — jots,
todos, snippets. Uses existing `notes` table with `bookId = "__notes__"`
sentinel and no `folderPath`. FAB creates a new quick note that opens
directly in the editor.

### Files touched
- `NotesFragment` (new)
- `NoteListViewModel` (`quickNotesFlow`)

---

## Cross-cutting engineering notes

- **DB migrations.** Any new column adds a numbered migration; keep
  them additive. `pinned_notes` table = v3.
- **ProGuard.** Any new palette / chart lib gets keep-rules.
- **CI.** After each section pushes to `lovable/updates`, wait on the
  build.yml workflow run for that SHA; on failure, read logs and fix.
- **Commit hygiene.** One commit per section (or per meaningful
  sub-step). Update UPDATE_PLAN.md progress log alongside the code so
  future agents see progress.

---

## Progress log (append; newest first)

| Date       | Section | Commit  | Notes                                    |
|------------|---------|---------|------------------------------------------|
| 2026-07-21 | plan    | 5dcd291 | Initial plan committed on lovable/updates|
| 2026-07-21 | plan    | (this)  | Updated with corrections from reference images (4 tabs, "Distribution" name, dual search-highlight colors) |
