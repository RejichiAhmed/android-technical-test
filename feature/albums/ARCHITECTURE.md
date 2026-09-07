# `:feature:albums` — Architecture & Design Choices

This document describes the MVI refactor applied to the albums feature module,
following the `android-presentation-mvi` skill, and the reasoning behind each
choice.

---

## Goals

1. Replace the legacy ad-hoc ViewModel (`MutableSharedFlow<List<AlbumDto>>`,
   no explicit intents, no events) with a proper **MVI** presentation layer.
2. **Share a single `AlbumsViewModel` instance** between the albums list
   screen and the album detail screen, so the detail screen never re-fetches
   data — it reads the album straight out of the list already loaded in
   shared state.
3. Remove all knowledge of `AlbumsViewModel` from the `:app` module. `:app`
   must only wire navigation and cross-cutting concerns (e.g. analytics), not
   resolve or hold feature ViewModels.

---

## MVI Structure

All three MVI pieces now live in one file:
`presentation/AlbumsViewModel.kt`.

### State

```kotlin
data class AlbumsState(
    val albums: List<AlbumUi> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedAlbumId: Int? = null,
    val availableCategories: List<Int> = emptyList(), // distinct albumIds, sorted ascending
    val selectedCategory: Int? = null,                // null = "All"
)

val AlbumsState.visibleAlbums: List<AlbumUi>
    get() = selectedCategory?.let { cat -> albums.filter { it.albumId == cat } } ?: albums
```

- `error` is a plain `String?` rather than `UiText`, because this project has
  no `core:presentation` module or string resources for errors yet.
  Introducing `UiText` now would be premature — the repository already
  returns exception messages, and there's nothing to localize. If/when the
  app grows string-resource-backed error messages, this is the natural spot
  to introduce `UiText` per the skill.
- `selectedAlbumId` tracks the last-clicked album purely for potential UI
  affordances (e.g. highlighting); the actual navigation argument is still
  passed via the type-safe route (`AlbumDetailRoute.albumId`).
- `availableCategories` / `selectedCategory` / `visibleAlbums` back the
  **category top bar** (see "Offline Persistence & Category Filtering"
  below) — `albumId` is treated as the "category" each item belongs to.

### Action (Intent)

```kotlin
sealed interface AlbumsAction {
    data object OnLoadAlbums : AlbumsAction
    data class OnAlbumClick(val albumId: Int) : AlbumsAction
    data object OnBackClick : AlbumsAction
    data object OnRetryClick : AlbumsAction
    data class OnCategorySelected(val albumId: Int?) : AlbumsAction // null = All
}
```

`OnLoadAlbums` and `OnRetryClick` both trigger `refreshAlbums()` (a network
sync into Room — see below) unconditionally; there is no longer an in-memory
idempotency guard, because **Room's `Flow` is the actual source of truth for
`albums`**, not the network call. Re-entering the list screen re-triggers a
refresh (cheap, and always safe since a failed refresh never clears
`albums` — see "Offline Persistence" below). `OnCategorySelected` sets the
active category filter (`null` = "All").

### Event (one-time side effects)

```kotlin
sealed interface AlbumsEvent {
    data class NavigateToDetail(val albumId: Int) : AlbumsEvent
    data object NavigateBack : AlbumsEvent
}
```

Delivered via a `Channel`, consumed once per emission (no replay), matching
the skill's guidance for one-time navigation/snackbar effects.

### UI Model

```kotlin
data class AlbumUi(
    val id: Int,
    val albumId: Int,
    val title: String,
    val thumbnailUrl: String,
    val albumLabel: String,   // "Album #<albumId>"
    val trackLabel: String,   // "Track #<id>"
)

fun AlbumDto.toAlbumUi(): AlbumUi
```

`AlbumDto` (network/data model) is mapped to `AlbumUi` (presentation model) so
the UI layer never depends on the raw DTO and pre-formatted display strings
(`albumLabel`, `trackLabel`) live outside the composables, per the skill's
"UI Model" section.

### Shared-state lookup helper

```kotlin
fun AlbumsState.findAlbum(albumId: Int): AlbumUi?
```

This is the mechanism that lets the detail screen avoid its own fetch: it
simply looks up the album by id inside the state the list screen already
populated.

---

## Sharing One ViewModel Across Two Screens

This is the key architectural decision: **the same `AlbumsViewModel`
instance must be visible to both `AlbumsRoute` and `AlbumDetailRoute`.**

Instead of hoisting the ViewModel resolution in `:app` (the old approach,
which leaked feature internals into the app module), the feature module now
owns a small **nested navigation graph**:

```kotlin
// feature/albums/navigation/AlbumsRoutes.kt
@Serializable object AlbumsGraphRoute   // parent graph route (no UI of its own)
@Serializable object AlbumsRoute
@Serializable data class AlbumDetailRoute(val albumId: Int)
```

```kotlin
// feature/albums/navigation/AlbumsNavGraph.kt
fun NavGraphBuilder.albumsGraph(
    navController: NavController,
    onAlbumSelected: (Int) -> Unit,
) {
    navigation<AlbumsGraphRoute>(startDestination = AlbumsRoute) {
        composable<AlbumsRoute> { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(AlbumsGraphRoute)
            }
            val viewModel: AlbumsViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
            AlbumsListRoot(viewModel = viewModel, onNavigateToDetail = { ... })
        }

        composable<AlbumDetailRoute> { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(AlbumsGraphRoute)
            }
            val viewModel: AlbumsViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
            AlbumDetailRoot(albumId = ..., viewModel = viewModel, onBack = { ... })
        }
    }
}
```

Both destinations resolve `AlbumsViewModel` with
`viewModelStoreOwner = navController.getBackStackEntry(AlbumsGraphRoute)` —
Jetpack Navigation's nested-graph back stack entry. Because both routes are
children of the same `AlbumsGraphRoute` navigation graph, they resolve to the
**exact same `ViewModelStore`**, and therefore the exact same `AlbumsViewModel`
instance, for as long as that graph is on the back stack. This is the
standard, idiomatic Navigation-Compose pattern for ViewModel sharing between
sibling destinations — no manual singleton, no hand-rolled scoping, no
hoisting required outside the feature module.

**Why not just hoist the ViewModel in `:app` (the previous approach)?**
Because that required `:app` to import `AlbumsViewModel` directly and collect
its state, coupling the app shell to feature internals. Wrapping the two
destinations in a feature-owned nested graph pushes that responsibility back
where it belongs: inside `:feature:albums`.

---

## Root / Screen Composable Split

Per the skill, each screen has a `Root` composable (owns the ViewModel,
observes events, forwards `state`/`onAction`) and a pure `Screen` composable
(receives only `state` + `onAction`, no ViewModel reference, previewable in
isolation).

- `presentation/liste/AlbumsScreen.kt` → `AlbumsListRoot` + `AlbumsListScreen`
- `presentation/details/AlbumDetailScreen.kt` → `AlbumDetailRoot` + `AlbumDetailScreen`

`AlbumDetailScreen` (pure) takes `state: AlbumsState` and the `albumId`, and
resolves the album via `state.findAlbum(albumId)` — no repository access, no
own loading state.

Since there's no shared `core:presentation` module yet, a small local
`ObserveAsEvents.kt` (lifecycle-aware `Channel`/`Flow` collector) was added
directly to `feature/albums/presentation/` rather than pulled from a
non-existent shared module.

---

## Offline Persistence & Category Filtering

Added after the initial MVI refactor, to satisfy: data available offline
(including after a full app restart), loading/error UI, and a category top
bar. Full phase-by-phase plan: `ALBUMS_PERSISTENCE_PLAN.md` (repo root).

### Room as the source of truth (`:data`)

- `data/local/AlbumEntity.kt` — `@Entity(tableName = "albums")`, `id` as
  `@PrimaryKey`; `AlbumDto.toEntity()` / `AlbumEntity.toDto()` mappers
  colocated in the same file.
- `data/local/AlbumDao.kt` — `getAll(): Flow<List<AlbumEntity>>`,
  `getById(id): AlbumEntity?`, `upsertAll(albums)` (`@Upsert`), `clearAll()`.
- `data/local/AppDatabase.kt` — `@Database(entities = [AlbumEntity::class], version = 1)`.
- `AlbumRepository` interface reshaped from a single `getAllAlbums()` suspend
  call to two responsibilities:
  ```kotlin
  interface AlbumRepository {
      fun observeAlbums(): Flow<List<AlbumDto>>
      suspend fun refreshAlbums(): Resource<Unit>
  }
  ```
- `OfflineFirstAlbumRepository` (replaces the old `AlbumRepositoryImp`) —
  `observeAlbums()` streams straight from `AlbumDao.getAll()` mapped to
  `AlbumDto`; `refreshAlbums()` fetches from `AlbumApiService`, upserts into
  Room on success, and **on failure returns `Resource.Error` without
  touching Room** — so previously cached data is never wiped by a failed
  network call. Named per the `android-data-layer` skill's convention for
  multi-source repositories (describes *how it behaves*, not `...Impl`).
- Registered in `DataModule.kt` as Koin singletons: `AppDatabase` (via
  `Room.databaseBuilder(androidContext(), ...)`), `AlbumDao`, and
  `single<AlbumRepository> { OfflineFirstAlbumRepository(get(), get()) }`.
  Required adding `implementation(libs.koin.android)` to
  `data/build.gradle.kts` (previously only `koin-core`) for `androidContext()`
  inside the module DSL.

### ViewModel: Room `Flow` + network refresh, decoupled

`AlbumsViewModel`'s `init` block starts a single
`viewModelScope.launch { repository.observeAlbums().collect { ... } }` that
owns `state.albums` and derives `state.availableCategories` (distinct, sorted
`albumId`s) on every emission — this fires immediately with whatever Room
already has cached, even fully offline or right after a process restart,
before any network call completes.

`refreshAlbums()` (triggered by `OnLoadAlbums`/`OnRetryClick`) only ever
touches `state.isLoading` and `state.error` — it never writes to
`state.albums` directly. That field is exclusively owned by the Room
collector above. This is what guarantees a failed refresh (offline, server
error, etc.) surfaces `state.error` **without erasing already-visible cached
albums**.

### Category filtering

`state.visibleAlbums` (extension property) filters `state.albums` by
`state.selectedCategory` (`albumId`), or returns everything when `null`
("All"). `AlbumsListScreen` renders `state.visibleAlbums`, never `state.albums`
directly.

### UI states (`AlbumsListScreen`)

Rendered in priority order inside the `Scaffold` body:
1. `isLoading && albums.isEmpty()` → full-screen `CircularProgressIndicator`.
2. `error != null && visibleAlbums.isEmpty()` → full-screen error + Spark
   `ButtonFilled` "Retry" (`OnRetryClick`).
3. Otherwise → an `ErrorBanner` on top (only if `error != null`, coexisting
   with the list below it so stale/cached data stays visible) + the
   `LazyColumn` over `visibleAlbums`, or an empty-category message.

`Scaffold`'s `topBar` hosts a horizontally scrollable row of Spark
`ChipTinted` chips — "All" plus one per `availableCategories` (label
`"Album #<id>"`, reusing the existing convention). Selection is indicated via
`ChipIntent.Main` (selected) vs `ChipIntent.Basic` (unselected), since Spark
1.4.0's `ChipTinted` has no dedicated `selected: Boolean` param. Tapping a
chip dispatches `OnCategorySelected`.

`AlbumDetailScreen` also defensively shows a loading indicator instead of
"Album not found" when `state.isLoading && state.findAlbum(albumId) == null`,
to avoid a false-negative flash on first entry before Room/network has
delivered data yet.

### Tests added

- `:data` — `OfflineFirstAlbumRepositoryTest` (fake `AlbumDao` + fake
  `AlbumApiService`, no Robolectric/instrumented DB needed since the DAO
  interface itself is faked): `observeAlbums()` reflects the fake DAO,
  `refreshAlbums()` success upserts, `refreshAlbums()` failure returns
  `Resource.Error` **and leaves the fake DAO's contents untouched**.
- `:feature:albums` — `AlbumsViewModelTest` rewritten against a fake
  `AlbumRepository`: state population from the repository's `Flow`,
  `isLoading` toggling around `refreshAlbums()`, error-without-cache-loss,
  category filtering (`visibleAlbums`), and `availableCategories` derivation.

---

## `:app` Module Changes

`app/.../ui/AppScreen.kt` no longer imports or resolves `AlbumsViewModel` at
all. It only embeds the feature's nav graph builder inside its own `NavHost`:

```kotlin
NavHost(navController = navController, startDestination = AlbumsGraphRoute) {
    albumsGraph(
        navController = navController,
        onAlbumSelected = { albumId -> analyticsHelper.trackSelection(albumId.toString()) },
    )
}
```

Analytics tracking (previously done inline in `:app`'s old `onItemSelected`
lambda) is preserved via the `onAlbumSelected` callback parameter — the
feature module stays analytics-agnostic and simply reports "this id was
selected" back up to whoever composes it.

DI registration (`AlbumsModule` Koin module: `viewModel { AlbumsViewModel(get()) }`)
still lives in `:feature:albums` and is still loaded by `PhotoApp.kt` —
that's correct and unchanged. What was removed is `:app`'s **direct
resolution/usage** of the ViewModel type via `koinViewModel<AlbumsViewModel>()`
inside `AppScreen.kt`.

---

## File Summary

| Action | File |
|---|---|
| Created | `presentation/AlbumsViewModel.kt` (State/Action/Event/ViewModel/UiModel) |
| Created | `presentation/ObserveAsEvents.kt` |
| Created | `navigation/AlbumsNavGraph.kt` (`albumsGraph()`) |
| Modified | `navigation/AlbumsRoutes.kt` (added `AlbumsGraphRoute`) |
| Modified | `presentation/liste/AlbumsScreen.kt` → `AlbumsListRoot` + `AlbumsListScreen` |
| Modified | `presentation/details/AlbumDetailScreen.kt` → `AlbumDetailRoot` + `AlbumDetailScreen` |
| Modified | `ui/AlbumItem.kt` (now takes `AlbumUi` instead of `AlbumDto`) |
| Modified | `di/AlbumsModule.kt` (import path update) |
| Modified | `app/.../ui/AppScreen.kt` (no more direct `AlbumsViewModel` usage) |
| Modified | `feature/albums/build.gradle.kts`, `gradle/libs.versions.toml` (added `kotlinx-coroutines-test`) |
| Modified | `src/test/.../AlbumsViewModelTest.kt` (rewritten for MVI API) |
| Deleted | `vm/AlbumsViewModel.kt` (legacy VM) |
| Deleted | `presentation/liste/AlbumsUiState.kt`, `presentation/liste/AlbumsEvent.kt` (dead stubs) |
| Deleted | `presentation/details/AlbumsDetailsUiState.kt`, `presentation/details/AlbumsDetailsEvent.kt` (dead stubs) |
| Created | `data/local/AlbumEntity.kt`, `data/local/AlbumDao.kt`, `data/local/AppDatabase.kt` |
| Created | `data/repository/OfflineFirstAlbumRepository.kt` (replaces `AlbumRepositoryImp.kt`, deleted) |
| Modified | `data/repository/AlbumRepository.kt` (`observeAlbums()` + `refreshAlbums()` interface shape) |
| Modified | `data/di/DataModule.kt` (Room DB/DAO singletons, repository rebinding) |
| Modified | `data/build.gradle.kts` (added `koin-android`, `kotlinx-coroutines-test`) |
| Modified | `presentation/liste/AlbumsScreen.kt` (category top bar, loading/error/empty states) |
| Modified | `presentation/details/AlbumDetailScreen.kt` (defensive loading state) |
| Created | `data/src/test/.../OfflineFirstAlbumRepositoryTest.kt` |
| Modified | `feature/albums/src/test/.../AlbumsViewModelTest.kt` (offline-first + category tests) |

---

## Validation

- `:data:compileDebugKotlin` / `:feature:albums:compileDebugKotlin` / `:app:compileDebugKotlin` — ✅
- `:data:test` — ✅ 3/3 passing (`OfflineFirstAlbumRepositoryTest`)
- `:feature:albums:test` — ✅ 7/7 passing (`AlbumsViewModelTest`)
- `:app:assembleDebug` — ✅ `BUILD SUCCESSFUL`, APK produced

## Follow-ups (not in scope here)

- Introduce `UiText`/`core:presentation` if/when localized error strings are
  needed.
- Consider `SavedStateHandle` if the detail screen ever needs to survive
  process death independently of the shared list state.
- Room schema export directory isn't configured yet (benign KSP warning) —
  set `exportSchema = false` or `room.schemaLocation` if schema history
  tracking becomes desirable.
- `ChipTinted` selection is currently approximated via `ChipIntent`
  (Main/Basic) since Spark 1.4.0 has no dedicated `selected` boolean param —
  revisit if Spark adds one, or if the visual distinction needs refinement.
