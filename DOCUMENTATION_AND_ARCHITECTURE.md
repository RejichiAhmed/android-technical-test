# Architecture & Technical Choices

This document summarizes the architecture, design patterns, libraries, and the
reasoning behind every major technical decision made while building this app.
It is the deliverable requested by the assignment: *"A document summarizing the
architecture choices, patterns and libraries applied"* + *"Justification of
choices made"*.

---

## 1. Project Snapshot

| | |
|---|---|
| **Language** | Kotlin (2.2.10) |
| **UI Toolkit** | Jetpack Compose (Material 3) |
| **Min SDK / Target SDK** | 24 / 36 (compile SDK 37) |
| **Modules** | `:app`, `:data`, `:feature:albums`, `:feature:favorites` |
| **Architecture** | Multi-module, Clean-ish layering + MVI presentation |
| **DI** | Koin |
| **Persistence** | Room (offline-first) |
| **Networking** | Retrofit + kotlinx.serialization |
| **Async** | Kotlin Coroutines + Flow |
| **Tests** | JUnit4 + kotlinx-coroutines-test (21 unit tests, 0 failures) |

---

## 2. Multi-Module Structure

```
Android RecruitmentTest App
├── :app                      → thin composition-root / shell
│   ├── MainActivity, PhotoApp (Application, Koin bootstrap)
│   ├── AppScreen + AppScreenViewModel (bottom-nav host, tab state)
│   └── di/AppDependenciesProvider
│
├── :data                     → single source of truth for all data
│   ├── network/   (Retrofit AlbumApiService, AlbumDto, Resource<T>)
│   ├── local/      (Room: AppDatabase, AlbumEntity, FavoriteAlbumEntity, AlbumDao)
│   ├── repository/ (AlbumRepository interface + AlbumRepositoryImp — offline-first)
│   └── di/DataModule (Koin: Retrofit, OkHttp, Room, Repository)
│
├── :feature:albums            → album list + album detail screens
│   ├── presentation/ (AlbumsViewModel, AlbumsState, AlbumsAction, AlbumsEvent)
│   ├── presentation/liste/    (AlbumsScreen — grid UI)
│   ├── presentation/details/  (AlbumDetailScreen — track list UI)
│   ├── navigation/    (AlbumsNavGraph, type-safe routes)
│   └── di/AlbumsModule
│
└── :feature:favorites          → favorite tracks screen
    ├── presentation/ (FavoritesViewModel, FavoritesState, FavoritesAction)
    ├── ui/FavoritesScreen
    ├── navigation/FavoritesNavGraph
    └── di/FavoritesModule
```

### Why multi-module?

- **Separation of concerns / ownership boundaries.** Each feature module owns its
  UI, state machine, and navigation graph. `:app` never imports a ViewModel or a
  screen composable directly — it only wires navigation graphs together.
- **Faster, more parallel builds.** Gradle can build/cache `:data`,
  `:feature:albums`, `:feature:favorites` independently and in parallel; changing
  the favorites UI doesn't force recompilation of the albums feature.
- **Enforced dependency direction.** `:feature:albums` and `:feature:favorites`
  both depend on `:data`, never on each other's presentation layer (favorites
  only reuses the `AlbumUi` model, not the albums ViewModel/UI), and `:app`
  depends on everything — this keeps the dependency graph a DAG and prevents
  accidental spaghetti coupling.
- **Testability.** Each module has its own `src/test`, and Gradle lets us run
  `:feature:albums:test` or `:feature:favorites:test` in isolation for fast
  feedback loops.
- **Matches the assignment's explicit grading criteria** ("L'aspect
  multi-modulaire").

---

## 3. Layered / Clean-ish Architecture

```
UI (Compose)  →  ViewModel (MVI state machine)  →  Repository (interface)  →  Room / Retrofit
```

- **Presentation layer** (`feature:*`) never talks to Retrofit or Room directly;
  it only depends on the `AlbumRepository` **interface** exposed by `:data`.
  This means the data source (network vs. cache vs. future GraphQL backend)
  can change without touching a single Composable or ViewModel.
- **Data layer** (`:data`) owns all I/O: DTOs (`AlbumDto`), Room entities
  (`AlbumEntity`, `FavoriteAlbumEntity`), and the repository implementation
  that merges both into a single offline-first data source.
- **Domain concepts stay tiny** on purpose: given the scope of the assignment
  (a single list + detail screen + favorites), a fully separate `:domain`
  module with dedicated use-case classes was judged as unnecessary
  over-engineering. The repository interface already gives us the same
  testability and inversion-of-control benefit without extra boilerplate.

---

## 4. Presentation Pattern: MVI (Model-View-Intent)

Both feature modules follow the same strict MVI contract:

```kotlin
data class AlbumsState(...)         // What the UI renders — pure data, no logic
sealed interface AlbumsAction        // What the user/UI can trigger
sealed interface AlbumsEvent         // One-shot side effects (navigation, snackbars)

class AlbumsViewModel(
    private val repository: AlbumRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AlbumsState())
    val state = _state.asStateFlow()          // observed by Compose

    private val _events = Channel<AlbumsEvent>()
    val events = _events.receiveAsFlow()       // consumed once (LaunchedEffect)

    fun onAction(action: AlbumsAction) { ... } // single entry point
}
```

**Why MVI over plain MVVM with ad-hoc `LiveData`/callbacks:**

- **Single entry point (`onAction`)** makes every user interaction traceable and
  unit-testable without touching the UI layer.
- **Unidirectional data flow** eliminates a whole class of bugs where two
  different code paths mutate the same mutable state inconsistently.
- **Events vs. State separation** cleanly distinguishes *"what should be drawn"*
  (State, replayable) from *"something that should happen once"* (Event —
  navigation, one-off side effects) — avoiding the classic "navigate twice on
  rotation" bug that plain `StateFlow`-only designs are prone to.
- **State holds no business logic**, only display attributes
  (`albums`, `albumGroupCards`, `isLoading`, `error`, `favoriteTrackIds`);
  all transformation/filtering/grouping logic lives in the ViewModel. This was
  a deliberate refactor (see git history) to keep `AlbumsState` a dumb,
  easily-asserted-on data holder in tests.
- Two feature modules share the exact same pattern, so a developer moving from
  `:feature:albums` to `:feature:favorites` has zero ramp-up cost.

### Shared ViewModel across list & detail (single fetch, no duplicate network calls)

`AlbumsNavGraph` scopes **one** `AlbumsViewModel` instance to the *nav graph's*
back stack entry (`navController.getBackStackEntry(AlbumsGraphRoute)`), not to
each individual screen:

```kotlin
composable<AlbumDetailRoute> { backStackEntry ->
    val parentEntry = remember(backStackEntry) {
        navController.getBackStackEntry(AlbumsGraphRoute)
    }
    val viewModel: AlbumsViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
    ...
}
```

This means clicking an album to open its detail screen reuses the already
loaded list in memory — no second network round-trip, no re-fetch, no
flicker — and both screens automatically stay in sync (e.g. toggling a
favorite in the detail screen is instantly reflected if the user navigates
back to the list).

---

## 5. Offline-First Persistence (Prerequisite)

`AlbumRepositoryImp` implements a **network-first, cache-always** strategy:

```kotlin
override suspend fun observeAlbums(): Flow<List<AlbumDto>> =
    dao.getAll().map { it.map { entity -> entity.toDto() } }   // Room is the single source of truth for the UI

override suspend fun refreshAlbums(): Resource<Unit> = try {
    val remote = api.getAlbums()
    dao.upsertAll(remote.map { it.toEntity() })                // success → persist
    Resource.Success(Unit)
} catch (e: Exception) {
    Resource.Error(e.message ?: "Unable to refresh albums.")   // failure → keep old cache, surface error
}
```

- **The UI always reads from Room**, never directly from the network response.
  This guarantees the exact same code path is used whether data came from the
  network a second ago or from a previous app session — the "available offline,
  even after restart" prerequisite is satisfied by construction, not by a
  special-cased "if offline" branch.
- **A failed refresh never wipes the cache.** If the network call throws, we
  surface `Resource.Error` (shown as a retry banner in the UI) but the
  previously cached albums remain visible — validated by
  `refreshAlbums_failure_returnsErrorAndDoesNotWipeCache` in
  `OfflineFirstAlbumRepositoryTest`.
- **Upsert instead of clear+insert** avoids a visible "flash of empty list"
  on every refresh and avoids losing favorite associations (favorites are
  keyed by `trackId`, stored in a separate table, so they survive album
  refreshes entirely independently).

---

## 6. Favorites Feature — Persisted by `trackId`, Not `albumId`

Initial implementation persisted the "favorite" flag on the *album*
(`albumId`). This was identified as a bug during review (a JSON feed where
several tracks share the same `albumId` would incorrectly favorite/unfavorite
sibling tracks together) and fixed by introducing a dedicated join-style table:

```kotlin
@Entity(tableName = "favorite_albums")
data class FavoriteAlbumEntity(@PrimaryKey val trackId: Int)
```

```kotlin
override suspend fun toggleFavorite(trackId: Int): Resource<Unit> = try {
    if (dao.isFavorite(trackId)) dao.removeFavorite(trackId)
    else dao.insertFavorite(FavoriteAlbumEntity(trackId))
    Resource.Success(Unit)
} catch (e: Exception) {
    Resource.Error(e.message ?: "Unable to update favorite state.")
}
```

- **A separate table (not a boolean column on `AlbumEntity`)** decouples the
  favorite state's lifecycle from the album cache's lifecycle. Album data can
  be safely refreshed/upserted/even cleared without ever touching favorite
  state, and vice-versa.
- **`Flow<Set<Int>>` (`observeFavoriteTrackIds`)** is combined with the albums
  flow in both `AlbumsViewModel` and `FavoritesViewModel` via
  `Flow.combine`, so a favorite toggled in one tab is reflected in the other
  tab in real time with zero extra plumbing.
- Favorites persist across app restarts because they live in the same Room
  database file as albums (verified manually and by
  `toggleFavorite_updatesAndRestoresFavoriteIds`).

---

## 7. Navigation — Type-Safe, Nested Graphs, Bottom Navigation

- **`androidx.navigation.compose` with `@Serializable` route objects**
  (`AlbumsRoute`, `AlbumDetailRoute(albumId: Int)`, `FavoritesTabRoute`, ...)
  instead of raw string routes — this removes an entire class of runtime
  crashes from malformed route strings/args and gives compile-time-checked
  argument passing (`backStackEntry.toRoute<AlbumDetailRoute>()`).
- **Each feature module exposes its own `NavGraphBuilder` extension**
  (`fun NavGraphBuilder.albumsGraph(...)`, `fun NavGraphBuilder.favoritesGraph(...)`)
  so `:app`'s `AppScreen` only composes graphs together, never owns feature
  internals.
- **Bottom navigation** (`AppScreen` + `BottomNavigationBar`) hosts two tabs
  (Albums / Favorites) using the standard `popUpTo(startDestination) { saveState
  = true } / restoreState = true` pattern, so switching tabs preserves each
  tab's scroll position and back stack instead of restarting it.

---

## 8. Configuration Change Management (Rejection Criterion)

The assignment explicitly rejects submissions that don't handle configuration
changes (e.g. rotation). This app handles it entirely through **standard
Android Architecture Components guarantees**, deliberately avoiding any manual
`onSaveInstanceState`/`configChanges` hacks:

- **All state lives in `ViewModel`s** (`AlbumsViewModel`, `FavoritesViewModel`,
  `AppScreenViewModel`), which are retained across configuration changes by the
  Android framework itself. No screen keeps `mutableStateOf` for anything that
  matters (list content, loading/error, selected album, favorites) — it's all
  sourced from `StateFlow` in the ViewModel.
- **Koin (`koinViewModel(viewModelStoreOwner = ...)`) resolves the *same*
  `ViewModelStoreOwner`-scoped instance** after a rotation, so `state.value`
  is exactly what it was before the configuration change — no data reload, no
  network re-fetch, no flicker.
- **No `android:configChanges` overrides** in `AndroidManifest.xml`: the app
  deliberately lets the system recreate the Activity/Compose tree on rotation
  (the "correct"/recommended approach) rather than suppressing recreation,
  which is an anti-pattern that breaks resource qualifiers (`values-land`,
  etc.).
- **Manual verification:** rotating the device on the Albums grid, the Album
  detail screen, and the Favorites list preserves scroll position (LazyGrid/
  LazyColumn built-in `rememberLazyGridState`/`rememberLazyListState` survive
  recomposition), loaded data, and any in-flight loading/error state.

---

## 9. Dependency Injection — Koin

**Koin** (not Hilt/Dagger) was chosen deliberately:

| Criterion | Koin | Hilt |
|---|---|---|
| Setup boilerplate | Minimal — plain Kotlin DSL (`module { ... }`) | Requires annotation processing, `@HiltAndroidApp`, `@AndroidEntryPoint` on every consumer |
| Build speed | No KAPT/KSP codegen for DI itself | Adds an annotation-processing pass |
| Compose integration | First-class `koinViewModel()` composable, incl. custom `viewModelStoreOwner` for nav-graph scoping | Possible but more ceremony for graph-scoped ViewModels |
| Learning curve | Very low — reads like a service locator with compile-time-ish safety via `get<T>()` | Steeper (KSP/Dagger component graph mental model) |
| Fit for this project's size | ✅ 4 modules, handful of singletons | Overkill |

Each module owns its own Koin module (`DataModule`, `AlbumsModule`,
`FavoritesModule`, `AppDependenciesProvider`) and `PhotoApp.onCreate()` loads
them all — mirroring the multi-module boundary 1:1 in the DI graph.

---

## 10. Library Choices & Justification

| Library | Why chosen |
|---|---|
| **Jetpack Compose + Material 3** | Modern declarative UI, less boilerplate than XML/View system, first-class support for state-driven UI which pairs naturally with MVI. Material 3 gives a professional, consistent design system (TopAppBar, FilterChip, NavigationBar, Card) out of the box, replacing the initial ad-hoc Spark usage for core screens once Material 3 was requested. |
| **Room** | Official, Compose/Coroutines-friendly persistence solution; `Flow`-returning DAOs give reactive, offline-first data for free; annotation-based schema is simple to reason about and test (see `FakeAlbumDao` pattern). |
| **Retrofit + kotlinx.serialization** | Retrofit is still the most mature/well-documented HTTP client on Android. kotlinx.serialization (vs. Gson/Moshi) was chosen to stay 100% Kotlin-first, avoid reflection-based (Gson) performance costs, and reuse the same `Json` instance/config already needed by other Kotlin-first parts of the stack. |
| **Kotlin Coroutines + Flow** | Native to Kotlin, avoids RxJava's extra dependency and learning curve; `StateFlow`/`Flow.combine` map 1:1 onto MVI's reactive `State` model and Room's reactive queries. |
| **Koin** | See section 9. |
| **Coil 3** | Modern, Compose-first image loading (`AsyncImage`) with Coroutines-based API, smaller footprint than Glide, good OkHttp integration reusing the same client used by Retrofit. |
| **LeakCanary** (debug only) | Zero-cost automatic memory leak detection during development; caught nothing critical here but is cheap insurance for a ViewModel/Compose-heavy codebase where lifecycle mistakes are easy to introduce. |
| **JUnit4 + kotlinx-coroutines-test** | Standard, lightweight choice for pure Kotlin/ViewModel unit tests; `UnconfinedTestDispatcher` + `runTest` let us assert on `StateFlow` values synchronously without needing Robolectric/instrumented tests for logic that has no Android framework dependency. |

---

## 11. Bugs Found & Fixed

The assignment explicitly calls out hidden traps/bugs. Issues identified and
resolved during development:

1. **Favorites persisted on `albumId` instead of `trackId`** — see section 6.
   Fixed by introducing `FavoriteAlbumEntity` keyed on `trackId` and a
   dedicated `favorite_albums` table.
2. **`compileSdk` / toolchain mismatch** causing build failures on a fresh
   checkout — pinned `compileSdk`/`targetSdk`/AGP/Kotlin versions in the
   version catalog (`libs.versions.toml`) to a known-good combination.
3. **DI wiring issues** when Koin was first introduced (`fix di using koin as
   lib`) — resolved by centralizing all bindings per-module (`DataModule`,
   `AlbumsModule`, `FavoritesModule`) instead of ad-hoc `startKoin` calls
   scattered across the app.
4. **Business logic leaking into `AlbumsState`** (extension functions doing
   filtering/grouping directly on the state object) — refactored so the state
   is a pure display model and all derivations happen in the ViewModel,
   improving testability (see git history: *"Refactoring AlbumsState to
   remove business logic helpers"*).
5. **Single-Activity + Compose Navigation migration** — the app originally
   risked fragment/activity-per-screen sprawl; migrated to a single
   `MainActivity` hosting a Compose `NavHost`, simplifying lifecycle and
   configuration-change handling (section 8).

---

## 12. Testing Strategy

| Module | Test class | What's covered |
|---|---|---|
| `:data` | `OfflineFirstAlbumRepositoryTest` | Offline-first read path, successful refresh/upsert, failed refresh preserving cache, favorite toggle (add/remove) round-trip |
| `:feature:albums` | `AlbumsViewModelTest` | Initial load populating state from repository flow, navigation event emission on album click, favorite toggle updates state, album lookup, loading state transitions, error-with-cached-data behavior, `albumGroupCards` grouping computation |
| `:feature:favorites` | `FavoritesViewModelTest` | Initial empty state, filtering to only favorited tracks, reactive updates on favorite add/remove from elsewhere, toggle action integration, error propagation on toggle/refresh failure, loading state transitions |
| `:app` | `ExampleUnitTest` | Placeholder/default template test |

**Approach:** all ViewModel/Repository tests are pure JVM unit tests using
hand-written fakes (`FakeAlbumRepository`, `FakeAlbumDao`) rather than
Mockito/MockK — this keeps tests fast (no Android framework needed),
readable, and resilient to refactors since fakes model real reactive
behavior (`MutableStateFlow`) instead of brittle stubbed method calls.

**Result:** 21 unit tests, 100% passing, executed in ~35s alongside a full
multi-module build.

---

## 13. Performance Considerations

- **`LazyVerticalGrid` / `LazyColumn`** for album grid and track lists — only
  visible items are composed/measured.
- **Single shared `AlbumsViewModel`** across list/detail avoids duplicate
  network calls (section 4).
- **Room as single source of truth** avoids redundant network calls on every
  screen visit; `refreshAlbums()` is only triggered explicitly (initial load /
  retry), not on every recomposition.
- **`Set<Int>` for favorite IDs** gives O(1) `contains()` checks when mapping
  albums → `AlbumUi.isFavorite` instead of O(n) list scans.
- Full clean build (4 modules) completes in ~35s locally; see
  `md_file/PERFORMANCE_AND_TESTS_REPORT.md` for a detailed breakdown.

---

## 14. Known Trade-offs / Things Deliberately Left Simple

- **`fallbackToDestructiveMigration()`** is used on the Room database. This is
  acceptable for this assignment's scope (no shipped versions to migrate from
  in production) but would be replaced with explicit `Migration` objects for a
  real production release.
- **No `:domain` module** — repository interfaces already provide the
  inversion-of-control and testability benefit a use-case layer would add,
  and adding one would be over-engineering for a 2-screen, 2-feature app.
- **No paging library** — the technical-test JSON feed is small and fully
  loaded in memory; `Paging3` would be the natural next step for a
  much larger catalog (see `md_file/NEXT_STEPS.md`).

---

## 15. Where To Find More

The `md_file/` directory contains detailed, phase-by-phase execution logs for
each feature (offline persistence, favorites, Material 3 migration, bottom
navigation, grid UI, etc.) written *during* development as working documents.
This file is the single, curated, up-to-date summary intended for reviewers;
the rest of `md_file/` is kept as historical/implementation detail for anyone
who wants to see the step-by-step reasoning behind a specific change.
