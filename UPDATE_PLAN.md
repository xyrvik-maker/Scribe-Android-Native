# Scribe Android — UI/UX Overhaul & Feature Update Plan

_Working document. Any AI agent picking this up mid-flight: read `PORTING_PLAN.md`
first for architecture, then this file for the active workstream. Update the
"Progress" column as you go and commit after each section._

---

## Goals (from user, 2026-07-21)

Raise Scribe's visual quality to the level of top writing apps
(Ulysses / Bear / iA Writer / Scrivener). Themes must be beautiful,
consistent across the whole app, never break readability, and support an
optional background image with opacity — when the user picks a background
image, the app's palette derives a soft gradient from that image.

Feature additions center on: dual-mode homepage (grid + list) with rich
book cards, a Books/Notes/Statistics tri-tab on the home screen, per-book
statistics, a redesigned right panel (Pinned + Outline) in the editor,
richer search with highlighted matches, a redesigned left panel (file
tree with previews and Books/Current scope), and a redesigned
Characters/Locations screen with a working image picker.

---

## Section index

| #  | Area                                | Status  |
|----|-------------------------------------|---------|
| 1  | Theme system overhaul + BG image    | pending |
| 2  | Home: grid + list book cards        | pending |
| 3  | Home tri-tab: Books / Notes / Stats | pending |
| 3A | Statistics tab (global)             | pending |
| 3B | Statistics tab (per-book)           | pending |
| 4  | Editor right panel: Pinned + Outline| pending |
| 5  | Outline smooth-scroll + preview     | pending |
| 6  | Search with themed highlights       | pending |
| 7  | Editor left panel: rich file tree   | pending |
| 8  | Characters/Locations card view      | pending |
| 9  | Notes quick-notes folder            | pending |

Order chosen for implementation (rationale in each section header):
**1 → 7 → 2 → 3/9 → 3A → 3B → 4 → 5 → 6 → 8**.
Theming first because every later screen consumes the tokens; left-panel
next because it exercises the token surface at density; Home cards next
because they set the visual grammar for cards used elsewhere.

---

## 1 — Theme system overhaul (blocking prerequisite)

### Problems today
- Theme colors only reach a subset of views (activity backgrounds and the
  editor). Toolbars, dialogs, popups, adapter items and system chrome
  keep AppCompat defaults, so dark themes leave icons and text unreadable.
- No accessible-contrast guarantee. Custom themes can save a background
  and foreground that are indistinguishable.
- No background-image support; theme model has no image field or opacity.
- Text/icon tinting is done ad-hoc in each activity; adapters miss it.

### Design
1. **Token surface (single source of truth).** Extend `AppTheme` with:
   `background`, `surface`, `surfaceElevated`, `foreground`,
   `foregroundMuted`, `foregroundSubtle`, `primary`, `primaryOn`,
   `accent`, `accentOn`, `border`, `divider`, `success`, `warning`,
   `danger`, `overlayScrim`, `bgImageUri?`, `bgImageOpacity` (0–100),
   `bgImageBlur` (0–24 px).
   Backward-compat: missing fields derived from the old
   `background`/`foreground`/`accent` triple via HSL nudges.
2. **Runtime application.**
   - `ThemeManager.apply(activity)` walks the decor tree once per resume
     and applies token tints to every `Toolbar`, `AppBarLayout`,
     `MaterialButton`, `TextInputLayout`, `RecyclerView` scrollbar edge
     effect, `NavigationView`, and any view tagged `theme:role=...`.
   - Adapters receive the current theme via a lightweight
     `ThemeAware` interface + `onThemeChanged()` broadcast; a single
     `ThemeChangeBus` (a `MutableSharedFlow<AppTheme>` in `ScribeApp`)
     re-emits on every `setActiveTheme`. All adapters and open activities
     subscribe.
   - Dialogs: register a `ThemedAlertDialogBuilder` wrapper so every
     dialog picks up tokens (title/message/positive/negative/neutral).
3. **Contrast guardrail.** On save (custom theme) and on activation, run
   a WCAG contrast check on `foreground↔background` and
   `primaryOn↔primary`. If < 4.5 for body / < 3.0 for large text, offer
   an auto-fix (lift/darken foreground L\* until it passes) with a
   preview. Never silently accept an unreadable theme.
4. **Background image.**
   - New picker in Theme Edit ("Background image"): SAF `OpenDocument`
     with persistable permission, plus a small library of built-in
     textures (paper, linen, dark slate, warm parchment).
   - Store URI + opacity + blur in `AppTheme`. Renderer draws the image
     into a full-decor `ImageView` behind content, tinted with
     `overlayScrim` at `(1 - opacity)` so text stays legible.
   - **Palette derivation.** When a user picks an image, extract a
     Palette (androidx.palette) and propose a derived theme: dominant →
     `primary`, muted → `surface`, vibrant → `accent`, dark-muted →
     `foreground`. Show a "Use derived palette" toggle in the picker.
     Subtle vertical gradient (surface → background) is composited on
     top for depth.
5. **Built-in themes redesigned.** Replace the current five with a
   curated set of eight, each tested for both light and dark parity:
   Paper (warm off-white), Ink (near-black on cream), Slate (cool dark),
   Midnight (deep indigo), Sepia (writerly warm brown), Forest (muted
   green dark), Ocean (deep navy), Vellum (parchment cream). Each ships
   with a matching optional texture.
6. **Editor typography.** Serif default remains, but each theme carries
   a suggested body font (Downloadable Fonts) and line-height token.

### Files touched
- `util/model/Models.kt` (extend `AppTheme`)
- `util/DefaultThemes.kt` (new palettes)
- `util/ThemeManager.kt` (walker + bus + contrast + palette derivation)
- `util/PrefsManager.kt` (migration for new fields)
- `ThemeEditActivity.kt` + `activity_theme_edit.xml` (BG image + opacity
  + blur sliders, derived-palette toggle, contrast warning strip)
- Every activity's `onResume`: call `themeManager.apply(this)` once and
  subscribe to `ThemeChangeBus`.
- Every adapter: implement `ThemeAware`; bind view holders through
  tokens instead of hardcoded colors.
- `res/values/themes.xml` — reduce hardcoded colors, expose
  `?attr/colorSurface` etc. so Material widgets tint correctly.

### Acceptance
- All eight built-ins pass WCAG AA on primary body text.
- Toggling theme instantly restyles every open surface without a
  restart.
- Picking a background image derives a palette, applies gradient scrim,
  and body text remains AA against the resulting `foreground`.
- Custom theme with intentionally poor contrast triggers auto-fix modal.

---

## 2 — Home: dual-mode book cards (grid + list)

### List mode (per uploaded reference)
Row = 72–88 dp tall. Left: 40 dp cover thumbnail with rounded 8 dp
corners. Right: title (single line, ellipsize end) + subtitle strip
showing four metrics separated by middle-dot: `📁 N folders`, `📄 N
files`, `📝 N,NNN words`, `⏱ updated 2h`. On long-press: reorder handle
appears. Tap → BookActivity.

### Grid mode (per uploaded reference)
Two columns on phones (three on ≥600dp). Card = 3:4 aspect. Cover fills
the top ~70%, bottom ~30% is a translucent strip with title + word count
+ last-updated relative time. Overflow menu (three-dot) in the top-right
of the strip. When no cover, generate a gradient cover from the theme's
`primary`+`accent` with title initials in `primaryOn`.

### Files touched
- `adapter/BookAdapter.kt` (grid + list view types share the same data,
  swap layouts based on `HomeViewModel.viewMode`)
- `item_book_grid.xml`, `item_book_list.xml` (rewrite)
- `HomeActivity.kt` (view-mode toggle in top bar, span count logic,
  diff util for smooth animation between modes)
- `viewmodel/HomeViewModel.kt` (persist view mode in Prefs; expose
  aggregated stats per book: folder count, file count, word count,
  updatedAt)
- `data/BookDao.kt` (`@Transaction` query returning `BookWithStats`)

### Acceptance
- Switching mode animates without flicker.
- Stats reflect real-time DB state (folder/file/word counts).
- Both layouts respect current theme tokens end-to-end.

---

## 3 — Home tri-tab: Books / Notes / Statistics

### Design
Bottom navigation (or segmented control at top — decide during
implementation; leaning BottomNavigationView because thumb reach on
phones). Three destinations:
- **Books** — existing HomeActivity content.
- **Notes** — flat, folder-like collection of quick notes (see §9).
- **Statistics** — see §3A.

### Files touched
- Turn `HomeActivity` into a container with three fragments
  (`BooksFragment`, `NotesFragment`, `StatisticsFragment`) OR three
  `ViewGroup`s toggled via `TabLayout`. Fragments preferred to keep
  each scroll state isolated.
- New: `BooksFragment`, `NotesFragment`, `StatisticsFragment`.
- `activity_home.xml`: hosts the container + bottom nav.

### Acceptance
- Bottom nav preserves stack per tab.
- Deep-link intents route to the intended tab.

## 3A — Statistics tab (global)

Two top tabs: **Statistics** and **Breakdown** (final name; "By File"
was placeholder — "Breakdown" reads better and matches the expandable
Files/Folders/Books selector).

### Statistics tab
- 7-day chart of words written per day. Prefer MPAndroidChart (already
  a common dep) or a lightweight custom drawable if we want zero deps.
  Bars with rounded tops, gradient fill from `primary` → `accent`,
  x-axis day labels, y-axis auto-scaled. Tapping a bar shows exact word
  count in a tooltip.
- Below chart: two cards side by side.
  - **Books** — total book count, bold number, subtitle "books".
  - **Files** — total file count across all books, bold number,
    subtitle "files".
- Below cards: three secondary tiles — current streak (days),
  today's word count vs. daily goal (progress ring), all-time words.

### Breakdown tab
- Header row: segmented pill selector — Files · Folders · Books.
- List, descending by word count:
  - Title (bold), subtitle showing location path
    (`Book / Folder / …`).
  - A horizontal progress bar under the title where fill % =
    `entry.words / maxEntryWords`. Tinted `accent`.
  - Trailing: word count number.
- Empty state per selection.

### Data
- New: `WritingStats.aggregateForRange(days=7)` returns a `List<DayStat>`.
- New: `WritingStats.rankBy(scope: Files|Folders|Books)` returns
  `List<RankEntry>`.
- Backed by `notes.wordCount` + `folders` + `books`, computed on IO
  dispatcher, cached in ViewModel.

### Files touched
- `viewmodel/StatisticsViewModel.kt` (new)
- `fragment_statistics.xml`, `fragment_breakdown.xml`
- `adapter/RankEntryAdapter.kt`
- `util/WritingStats.kt` (extend)

## 3B — Statistics tab (per-book)

Inside `BookActivity` bottom bar, two options: **Write** and
**Statistics**. Statistics view here is the same components as §3A but
scoped to the current `bookId`. Reuse the same fragments with a bookId
argument.

---

## 4 — Editor right panel: Pinned + Outline

### Pinned tab (per uploaded reference)
Two vertically stacked sections (Top / Bottom). Each section:
- Header row: pinned-note title + icons (edit, unpin ✕, add ✚).
- Body: rendered preview of that note (first ~200 chars, markdown
  rendered) OR when multiple notes are pinned to that slot,
  swipe/arrow between them (chevron ‹ › shows current index like `2/4`).
- Adding: tap ✚ → file-explorer overlay in collapsible tree mode
  (books → folders → files). Selecting a file pins it.
- Editing: tap edit icon → opens that note in a floating window (reuses
  `FloatingWindowManager`) so main note stays open.
- Unpinning: tap ✕ removes it from that section.

### Outline tab (see §5)

### Files touched
- `activity_main.xml` (right drawer content)
- New: `fragment_pinned.xml`, `PinnedSectionView`, adapter for the
  two-section layout.
- `data/PinnedNote.kt` + DAO (bookId, section: TOP|BOTTOM, noteId,
  order).
- `viewmodel/EditorViewModel.kt` (pinnedTop/pinnedBottom flows).
- New: `FileTreePickerFragment` (reused by search + pinning).

### Acceptance
- Pinned notes survive editor restart.
- Swipe arrows cycle without losing state.
- Opening a pinned note in float doesn't close the current editor.

---

## 5 — Outline: bold headings + preview + smooth scroll

### Design
Each entry:
- Heading text in bold, size scaled by heading level (H1 largest).
- Two-line preview below (first non-heading paragraph after the
  heading), muted foreground color.
- Left indent by heading level for hierarchy.
- Tap → smooth-scroll the editor to that heading's line
  (`ObjectAnimator` on scrollY, 300 ms, decelerate). Highlight the
  heading briefly (200 ms accent tint fade) so the eye lands.

### Files touched
- `adapter/OutlineAdapter.kt` (rewrite)
- `util/MarkdownUtil.kt` (return heading + preview + line offset)
- `MainActivity.kt` (smooth scroll helper on ScrollView; if editor is
  inside a NestedScrollView, use `smoothScrollTo`; otherwise animate
  scrollY of the ScribeEditText's parent).

### Acceptance
- Tapping any outline entry smoothly scrolls to that heading in ≤400 ms.
- Preview truncates gracefully; no jitter on rapid taps.

---

## 6 — Search with themed highlights

### Design
Homepage search field. As user types (debounced 120 ms):
- Query files (notes) and folders across all books.
- Result row: title + snippet where each match run is highlighted with
  a token color:
  - `theme.accent` background at 30% alpha for match text.
  - Foreground stays readable (contrast-checked).
- Also tint the match runs in the title.
- Show scope label (`Book / Folder / …`).
- Tap → open that note or folder.

### Files touched
- `HomeActivity` search bar; new `SearchViewModel`.
- New: `SearchResultAdapter` — spannable with themed highlights.
- New DAO queries: full-text-ish `LIKE` matching for MVP; if perf
  suffers, migrate to FTS4 virtual table in `AppDatabase` v3.

### Acceptance
- Highlight color updates immediately when theme changes.
- Zero-match state shows friendly empty view.

---

## 7 — Editor left panel: rich file tree

### Design
- Top bar with segmented toggle: **Books** (all books, expandable tree)
  vs **Current** (folders/files inside the current book only).
- Each item:
  - Small icon (folder / note / book) tinted `foregroundMuted`.
  - Title in bold `foreground`.
  - Location line below (`Book / Folder`), tiny, `foregroundSubtle`.
  - Snippet line (~40 chars) of the note's body, italic, `subtle`.
- Item padding 12 dp vertical, 4 dp between items — dense enough for
  scanning, spaced enough to breathe.
- Long-press for context menu (rename/delete/move).

### Files touched
- `adapter/FileTreeAdapter.kt` (rewrite)
- `item_file_tree.xml` (rewrite)
- `MainActivity.kt` (left drawer top bar with segmented toggle)
- `viewmodel/EditorViewModel.kt` (expose `treeScope: Books|Current`).

### Acceptance
- Toggling Books/Current re-renders in <100 ms.
- Long note bodies don't cause row height to jump.

---

## 8 — Characters & Locations: cards + working image picker

### Problems today
- Image picker in `WorldEntryAdapter` returns URI but re-open loses it
  (persistable permission not taken) and thumbnail crops badly.
- List items are text-only; hard to scan.

### Design
Card grid (1 col phone, 2 col tablet). Each card:
- Left: square image, 96 dp, rounded 12 dp corners, `centerCrop`.
- Right: name (bold, size 18), then key-value chips for age / role /
  location / faction (whichever apply).
- Long-press to delete; tap to edit.
- Image picker uses `ActivityResultContracts.OpenDocument` with
  `takePersistableUriPermission`. On load, decode with `ImageDecoder`
  and cache a downsized version in app-private storage keyed by URI
  hash to avoid re-decoding on scroll.

### Files touched
- `SheetsActivity.kt`, `activity_sheets.xml`
- `adapter/WorldEntryAdapter.kt`, `item_world_entry.xml`
- New: `util/ImageCache.kt`
- `data/WorldEntry.kt` (already has image field — verify persistable
  perm flow).

### Acceptance
- Picked image survives app restart.
- Cards render without visible re-decode on scroll.

---

## 9 — Notes (quick-notes folder)

Third home tab. A flat list of "quick notes" that are NOT scoped to a
book — used for jots, todos, snippets. Uses existing `notes` table with
`bookId = "__notes__"` sentinel and no `folderPath`. FAB creates a new
quick note that opens directly in the editor.

### Files touched
- `NotesFragment` (new)
- `NoteListViewModel` (expose `quickNotesFlow`)
- `MainActivity` (route back to home Notes tab when opened from there).

---

## Cross-cutting engineering notes

- **DB migrations.** Any new column adds v3 migration; keep them
  additive. Pinned-notes table = v3.
- **ProGuard.** Any new Palette / chart lib gets keep-rules.
- **CI.** After each section pushes to `lovable/updates`, wait on the
  build.yml workflow run for that SHA; on failure, read logs, fix, push.
- **Testing.** Where feasible add a unit test for pure utility
  additions (`WritingStats.aggregateForRange`, contrast checker, palette
  derivation).
- **Commit hygiene.** One commit per section. Commit the UPDATE_PLAN.md
  update alongside the section's code so future agents see progress.

---

## Progress log (append; newest first)

| Date       | Section | Commit  | Notes                                    |
|------------|---------|---------|------------------------------------------|
| 2026-07-21 | plan    | (this)  | Initial plan committed on lovable/updates|
