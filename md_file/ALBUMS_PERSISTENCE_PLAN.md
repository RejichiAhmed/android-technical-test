# Execution Plan — Offline Persistence, Loading/Error UI, Category Top Bar

Target module: `:feature:albums` (presentation/nav) + `:data` (persistence).
No changes expected in `:app` beyond nothing (feature stays self-contained).

## Scope (from requirements)

1. **Offline persistence** — album data must be available offline, including
   after a full app restart (not just in-memory cache).
2. **Loading & error UI** — the list (and ideally detail) screen must show a
   loading indicator while fetching and a clear error state with retry, using
   the `isLoading`/`error` fields already present in `AlbumsState` but
   currently unused by `AlbumsListScreen`.
3. **Category top bar** — add a top bar to the albums list that lets the user
   filter the list by **album category**, i.e. by `albumId` (each `albumId`
   groups several photo entries — this is the "album" the item belongs to;
   the current UI already renders this as the `"Album #<albumId>"` chip).
   Selecting a category filters the visible list to that `albumId`; an "All"
   option shows everything.

---

## Current State (baseline, verified in code)

- `:data` already declares Room dependencies (`room-runtime`, `room-ktx`,
  `room-compiler` via KSP) in `data/build.gradle.kts`, but **no `@Database`,
  entity, or DAO exists yet** — Room is unused today.
- `AlbumRepository` (interface) / `AlbumRepositoryImp` (impl) already return
  `Resource<List<AlbumDto>>` (`Resource.Success` / `Resource.Error`), calling
  `AlbumApiService.getAlbums()` directly — **no local cache, no offline
  fallback**. Data is lost on process death / restart / no network.
- `AlbumsViewModel` (`feature/albums/presentation/AlbumsViewModel.kt`) already
  has `AlbumsState.isLoading` and `AlbumsState.error`, updates them correctly
  in `loadAlbums()`, but `AlbumsListScreen` **ignores both** — it always
  renders the `LazyColumn`, even while loading or on error.
- `AlbumDto` has no explicit "category" field — `albumId` is the grouping key
  (multiple entries share the same `albumId`).
- No top bar exists on the list screen (`Scaffold` has no `topBar` param set).

---

## Phase 1 — Design Decisions (no code)

- **Persistence strategy**: Offline-first, Room as local cache / source of
  truth for the list. On `OnLoadAlbums`: try network fetch → on success,
  replace Room cache with fresh data, emit from Room; on failure, fall back to
  whatever is already cached in Room (if any) and surface the error alongside
  the stale data so the UI can show both an error banner and existing content
  (rather than a blank error screen when offline data exists).
- **Data flow direction**: Repository exposes `Flow<List<AlbumEntity>>` (or a
  suspend "get once" — decide during implementation based on what keeps the
  ViewModel simplest; a `Flow` from Room is preferred per the
  `android-data-layer` skill's "Room as single source of truth" guidance, but
  a single suspend re-check is acceptable given this feature has no live
  local mutations yet — **decision: use `Flow<List<AlbumEntity>>` from Room**,
  since it also naturally supports future favorites/local edits without
  another refactor).
- **Category definition**: `albumId` is the category. UI needs a distinct,
  sorted list of `albumId`s to populate the top bar (e.g. chips or a
  dropdown) plus an "All" pseudo-category.
- **Error surfacing**: keep `error` as a plain `String?` in `AlbumsState`
  (per existing convention documented in `feature/albums/ARCHITECTURE.md` —
  no `UiText`/string-resources module in this project yet).
- **No new module needed** — Room lives in `:data` (already has the Room
  dependencies), MVI/UI changes live in `:feature:albums`.

**Validation**: this phase produces no code; a quick summary confirmation is
enough before Phase 2 starts.

---

## Phase 2 — Room Persistence Layer (`:data`)

1. **`AlbumEntity`** (`data/src/main/java/fr/leboncoin/data/local/AlbumEntity.kt`)
   — `@Entity(tableName = "albums")`, fields mirror `AlbumDto`
   (`id` as `@PrimaryKey`, `albumId`, `title`, `url`, `thumbnailUrl`).
2. **`AlbumDao`** (`data/src/main/java/fr/leboncoin/data/local/AlbumDao.kt`)
   — `@Query("SELECT * FROM albums") fun getAll(): Flow<List<AlbumEntity>>`,
   `@Query("SELECT * FROM albums WHERE id = :id") suspend fun getById(id: Int): AlbumEntity?`,
   `@Upsert suspend fun upsertAll(albums: List<AlbumEntity>)`,
   `@Query("DELETE FROM albums") suspend fun clearAll()`.
3. **`AppDatabase`** (`data/src/main/java/fr/leboncoin/data/local/AppDatabase.kt`)
   — `@Database(entities = [AlbumEntity::class], version = 1)`, abstract
   `albumDao(): AlbumDao`.
4. **Mappers** (extension functions, colocated with entity per skill
   convention): `fun AlbumDto.toEntity(): AlbumEntity`,
   `fun AlbumEntity.toDto(): AlbumDto` (keep DTO as the type crossing into
   presentation for now — no separate domain model layer exists in this
   project, consistent with current architecture; **do not** introduce a
   domain module for this task, it's out of scope).
5. **`Resource`**: no structural change needed; success/error already models
   the two outcomes the repository reports upward.
6. **`AlbumRepository` interface update**:
   ```kotlin
   interface AlbumRepository {
       fun observeAlbums(): Flow<List<AlbumDto>>
       suspend fun refreshAlbums(): Resource<Unit>
   }
   ```
   - `observeAlbums()` — Room `Flow`, mapped to `AlbumDto`, always available
     offline (empty list until first successful fetch, or previously cached
     data across restarts).
   - `refreshAlbums()` — triggers the network fetch + Room upsert; returns
     `Resource.Success(Unit)` or `Resource.Error(message)` so the ViewModel
     can surface network failures **without** wiping already-cached/rendered
     data.
7. **`AlbumRepositoryImp` (rename candidate: `OfflineFirstAlbumRepository`
   per the `android-data-layer` skill's naming convention for multi-source
   repositories)** — constructor takes `AlbumDao` + `AlbumApiService` (+
   existing `CoroutineScope` only if still needed; re-evaluate — likely
   removable once Room's `Flow` handles reactivity and `refreshAlbums()` is a
   plain suspend fun called from the ViewModel's own `viewModelScope`).
   ```kotlin
   class OfflineFirstAlbumRepository(
       private val dao: AlbumDao,
       private val api: AlbumApiService,
   ) : AlbumRepository {
       override fun observeAlbums(): Flow<List<AlbumDto>> =
           dao.getAll().map { entities -> entities.map { it.toDto() } }

       override suspend fun refreshAlbums(): Resource<Unit> = try {
           val remote = api.getAlbums()
           dao.upsertAll(remote.map { it.toEntity() })
           Resource.Success(Unit)
       } catch (e: Exception) {
           Resource.Error(e.message ?: "Unable to refresh albums.")
       }
   }
   ```
8. **DI (`DataModule.kt`)** — add:
   ```kotlin
   single {
       Room.databaseBuilder(androidContext(), AppDatabase::class.java, "albums.db").build()
   }
   single { get<AppDatabase>().albumDao() }
   single<AlbumRepository> { OfflineFirstAlbumRepository(get(), get()) }
   ```
   (requires `androidContext()` — `DataModule` currently has no Android
   context access; verify Koin module already runs in an Android-context
   scope via `startKoin { androidContext(...) }` in `PhotoApp` — it does, so
   `androidContext()` is available inside any module function.)

**Validation**: `:data:compileDebugKotlin`, `:data:test` (add DAO/repository
unit tests — see Phase 6). No consumers updated yet, so `:feature:albums`
will fail to compile until Phase 3 — that's expected and resolved next.

---

## Phase 3 — ViewModel Updates (`:feature:albums`)

1. **`AlbumsState`** — add:
   ```kotlin
   data class AlbumsState(
       val albums: List<AlbumUi> = emptyList(),
       val isLoading: Boolean = false,
       val error: String? = null,
       val selectedAlbumId: Int? = null,
       val availableCategories: List<Int> = emptyList(), // distinct albumIds, sorted
       val selectedCategory: Int? = null,                // null = "All"
   )
   ```
   Add a derived helper (either a plain function or computed in the
   ViewModel before emitting state) `val AlbumsState.visibleAlbums: List<AlbumUi>`
   that filters `albums` by `selectedCategory` when non-null.
2. **`AlbumsAction`** — add:
   ```kotlin
   data class OnCategorySelected(val albumId: Int?) : AlbumsAction // null = All
   ```
3. **`AlbumsViewModel`**:
   - Replace the current one-shot `repository.getAllAlbums()` suspend call
     with two concerns:
     - Collect `repository.observeAlbums()` in a `viewModelScope.launch`
       (started once, e.g. guarded by an `init` block or a `hasStarted` flag
       consistent with the existing idempotent-load guard), mapping each
       emission to `AlbumUi` and recomputing `availableCategories` from the
       distinct `albumId`s.
     - `OnLoadAlbums` / `OnRetryClick` now call `repository.refreshAlbums()`
       to trigger the network sync; on `Resource.Error`, set `state.error`
       **without** clearing `state.albums` (so cached/offline data stays
       visible under an error banner). On `Resource.Success`, clear
       `state.error` (fresh Room emission arrives via the collector above).
     - `OnCategorySelected` — simple `_state.update { it.copy(selectedCategory = albumId) }`.
   - Loading flag: `isLoading = true` while `refreshAlbums()` is in flight,
     `false` once it completes (success or error) — independent of the Room
     collector, which can update `albums`/`availableCategories` at any time.
4. **`AlbumDetailScreen` / `AlbumDetailRoot`**: no change needed to its data
   flow (`state.findAlbum(albumId)` still works against the shared state's
   `albums` list) — but it should also render `state.isLoading` /
   `state.error` for consistency if the detail screen is opened before the
   list finished its first load (edge case worth handling defensively).

**Validation**: `:feature:albums:compileDebugKotlin`. Existing
`AlbumsViewModelTest` will need updates (see Phase 6) before this compiles as
a full module including tests.

---

## Phase 4 — UI: Loading, Error, Category Top Bar (`:feature:albums`)

1. **`AlbumsListScreen`** (`presentation/liste/AlbumsScreen.kt`):
   - Add `topBar` to `Scaffold`: a simple `TopAppBar`/`ScrollableTabRow` (or
     Spark's equivalent component — check Spark's component set for a
     tabs/chips component consistent with the rest of the UI, e.g.
     `ChipTinted`/segmented control already used elsewhere) listing "All" +
     each `availableCategories` entry (`"Album #<id>"` label, reusing the
     existing label convention), calling
     `onAction(AlbumsAction.OnCategorySelected(id))` on selection, and
     visually indicating the currently `selectedCategory`.
   - Render three body states inside the `Scaffold` content, in priority
     order:
     1. `state.isLoading && state.albums.isEmpty()` → centered loading
        indicator (first-load spinner, full screen).
     2. `state.error != null` → error message + retry button
        (`onAction(AlbumsAction.OnRetryClick)`), shown **above** the list if
        `state.albums.isNotEmpty()` (offline/stale data still visible below
        the error banner), or full-screen if the list is empty.
     3. Otherwise → existing `LazyColumn` using the category-filtered list
        (`state.visibleAlbums`), with an empty-state message if the filtered
        result is empty (e.g. "No albums in this category").
   - Keep `AlbumsListScreen` a pure `state`/`onAction` composable (no
     ViewModel reference), per the MVI skill — all of the above is driven
     purely by `AlbumsState`.
2. **`AlbumDetailScreen`**: add the same minimal loading/error affordance if
   `state.isLoading` and the album isn't found yet, to avoid a flash of
   "Album not found" while the very first load is still in flight.
3. **Preview functions**: update/add `@Preview` variants for
   loading / error / empty-category / populated states so each renders
   independently (per skill: Screen composables must be previewable).

**Validation**: visual check via Compose Preview (loading, error, category
filter, empty-filtered-category states) + manual run (see Phase 6/7).

---

## Phase 5 — Category "Top Bar" Component Decision

Two options, pick the simplest that fits Spark's design system already in
use (`SparkTheme`, `ChipTinted`, `Card`, `ButtonFilled` observed in the
codebase):

- **Option A (recommended)**: horizontal scrollable row of `ChipTinted`/
  selectable chip components inside the `Scaffold`'s `topBar` slot — reuses
  an existing Spark component already imported elsewhere in this module, so
  no new dependency and consistent look.
- **Option B**: Material3 `ScrollableTabRow` with `Tab` per category — more
  "top bar" like, but introduces a Material3 component not otherwise used
  directly by this Spark-themed app.

**Decision**: implement Option A unless Spark exposes a dedicated tab/segment
component discovered during implementation that fits better — use judgement
at implementation time, but stay consistent with Spark, not raw Material3.

---

## Phase 6 — Tests

1. **`:data` — `AlbumDao` / Room test** (`data/src/test/...` if Robolectric
   is available, otherwise `androidTest` in-memory Room DB): insert, upsert,
   query-all, clear.
2. **`:data` — `OfflineFirstAlbumRepository` unit test**: fake `AlbumDao`
   (in-memory list) + fake `AlbumApiService`:
   - `observeAlbums()` emits what's in the fake DAO.
   - `refreshAlbums()` success path upserts into DAO.
   - `refreshAlbums()` failure path returns `Resource.Error` and does not
     wipe existing DAO contents.
3. **`:feature:albums` — `AlbumsViewModel` test updates**:
   - Loading flag toggles true→false around `refreshAlbums()`.
   - Error surfaces in state on repository failure **while retaining**
     previously-emitted albums (simulate Room already having cached data).
   - `OnCategorySelected` filters `visibleAlbums` correctly; `null` shows all.
   - `availableCategories` is the distinct, sorted set of `albumId`s.

**Validation**: `./gradlew :data:test :feature:albums:test` — all green.

---

## Phase 7 — Full Validation

1. `./gradlew :data:compileDebugKotlin :feature:albums:compileDebugKotlin :app:compileDebugKotlin`
2. `./gradlew :data:test :feature:albums:test`
3. `./gradlew :app:assembleDebug`
4. Manual/device check:
   - Fresh install, online → list loads, top bar categories populate.
   - Select a category → list filters correctly.
   - Force-stop the app, relaunch with network disabled (airplane mode) →
     previously loaded albums still display from Room (this is the core
     "available offline, even after restart" requirement).
   - Trigger a network error with no cache yet (e.g. clear app data + airplane
     mode) → error state with retry shown; tapping retry re-attempts once
     network is restored.

---

## File Summary (planned)

| Module | Action | File |
|---|---|---|
| `:data` | Create | `local/AlbumEntity.kt` |
| `:data` | Create | `local/AlbumDao.kt` |
| `:data` | Create | `local/AppDatabase.kt` |
| `:data` | Create | mapper extensions (`AlbumEntity.toDto()` / `AlbumDto.toEntity()`, colocated with entity or DAO) |
| `:data` | Modify | `repository/AlbumRepository.kt` (new interface shape: `observeAlbums()` + `refreshAlbums()`) |
| `:data` | Modify/Rename | `repository/AlbumRepositoryImp.kt` → `OfflineFirstAlbumRepository.kt` |
| `:data` | Modify | `di/DataModule.kt` (Room DB/DAO singletons, repository binding) |
| `:feature:albums` | Modify | `presentation/AlbumsViewModel.kt` (`AlbumsState`/`AlbumsAction` additions, Room-flow collection, category filtering) |
| `:feature:albums` | Modify | `presentation/liste/AlbumsScreen.kt` (top bar, loading/error/empty rendering) |
| `:feature:albums` | Modify | `presentation/details/AlbumDetailScreen.kt` (defensive loading/error rendering) |
| `:feature:albums` | Modify | `src/test/.../AlbumsViewModelTest.kt` (updated + new cases) |
| `:data` | Create | new test file(s) for DAO / repository |

---

## Risks

- **Room + KSP first-time setup** in this project may surface Kotlin/KSP
  version friction — mitigate by compiling `:data` in isolation right after
  Phase 2 before touching consumers.
- **Flow lifecycle in ViewModel**: must collect Room's `Flow` with
  `viewModelScope` (not a leaked/global scope) — verify no duplicate
  collectors are started across configuration changes (guard with the same
  idempotency pattern already used for `loadAlbums()`).
- **Offline error UX**: must not regress the "always show something useful"
  behavior — error must augment, not replace, existing cached data.
