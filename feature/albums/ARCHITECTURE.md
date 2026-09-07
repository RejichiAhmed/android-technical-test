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
)
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

### Action (Intent)

```kotlin
sealed interface AlbumsAction {
    data object OnLoadAlbums : AlbumsAction
    data class OnAlbumClick(val albumId: Int) : AlbumsAction
    data object OnBackClick : AlbumsAction
    data object OnRetryClick : AlbumsAction
}
```

`OnLoadAlbums` is idempotent — `loadAlbums()` skips re-fetching if albums are
already loaded and there's no error, so navigating back and forth between
list/detail (which re-enters the list composable) does not trigger redundant
network calls. `OnRetryClick` forces a reload, for a future retry-on-error UI.

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

---

## Validation

- `:feature:albums:compileDebugKotlin` — ✅
- `:feature:albums:test` — ✅ 3/3 passing
- `:app:compileDebugKotlin` — ✅
- `:app:assembleDebug` — ✅ `BUILD SUCCESSFUL`

## Follow-ups (not in scope here)

- Introduce `UiText`/`core:presentation` if/when localized error strings are
  needed.
- Add a retry UI affordance wired to `AlbumsAction.OnRetryClick` (action
  already exists, no UI triggers it yet).
- Consider `SavedStateHandle` if the detail screen ever needs to survive
  process death independently of the shared list state.
