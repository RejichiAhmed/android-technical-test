# Bottom Navigation Bar Implementation Plan

## Plan: Bottom Navigation Bar with Albums & Favorites Tabs

**TL;DR**: Refactor AppScreen.kt to use Material 3 `NavigationBar` with two main destinations: existing Albums feature (with nested navigation) and new Favorites screen. Favorites screen will display favorited tracks using existing `AlbumItem` component and shared favorite state from the repository. Create `:feature:favorites` module following the MVI pattern, leverage existing persistence layer, and keep AlbumsViewModel state accessible across tabs.

---

## Phase 1: Architecture & Design (Foundation)

### 1.1 Navigation Structure Planning
- **Main Navigation Routes**:
  - Create 2 top-level routes: `AlbumsTabRoute` and `FavoritesTabRoute`
  - Keep `AlbumsGraphRoute` nested under `AlbumsTabRoute` (preserves albumsGraph structure)
  - Add `FavoritesRoute` as primary route under `FavoritesTabRoute`
  
- **Bottom Navigation State Management**:
  - Add `currentTab: NavDestination` to `AppScreenViewModel` to track selected tab
  - Implement callback `onTabSelected: (NavDestination) -> Unit` to update tab state
  - Preserve tab state during navigation back-stack changes

### 1.2 Bottom Navigation Bar Components
- **Material 3 NavigationBar** with:
  - Two `NavigationBarItem` entries (Albums, Favorites)
  - Appropriate Material Icons (e.g., `Icons.Default.Album`, `Icons.Default.Favorite`)
  - Labels: "Albums" and "Favorites"
  - `selected` state bound to `currentTab`
  - `onClick` handler triggering `onTabSelected`

### 1.3 Design Decisions
- **Tab Persistence**: When user switches away from Albums tab, the back stack is preserved. Switching back restarts from the list (not detail).
- **Shared State**: `AlbumsViewModel` remains a singleton scoped to albums tab; favorites screen accesses repository directly for read-only favorite IDs and album data.
- **Favorites as New Module**: Create `:feature:favorites` to maintain feature isolation and clean DI.

---

## Phase 2: Create Favorites Feature Module

### 2.1 Module Setup
**File**: `feature/favorites/build.gradle.kts`
- Create new Gradle module `:feature:favorites`
- Dependencies: `:data`, Compose, Material 3, Koin
- Update `settings.gradle.kts` to include `:feature:favorites`

### 2.2 Navigation Routes
**Files**:
- `feature/favorites/src/.../navigation/FavoritesRoutes.kt` — Define `FavoritesTabRoute` (parent) and `FavoritesRoute` (destination)
- `feature/favorites/src/.../navigation/FavoritesNavGraph.kt` — Build graph with single destination

**Key Details**:
- Use `@Serializable` for type-safe routes
- `FavoritesTabRoute` wraps `FavoritesRoute` for consistency

### 2.3 ViewModel & State (MVI Pattern)
**Files**:
- `feature/favorites/src/.../presentation/FavoritesViewModel.kt`
- `feature/favorites/src/.../presentation/FavoritesState.kt`
- `feature/favorites/src/.../presentation/FavoritesAction.kt`
- `feature/favorites/src/.../presentation/FavoritesEvent.kt`

**State Structure**:
```
FavoritesState(
  albums: List<AlbumUi> = emptyList(),  // Favorites-filtered tracks
  isLoading: Boolean = false,
  error: String? = null,
  favoriteTrackIds: Set<Int> = emptySet(),
)

sealed interface FavoritesAction {
  data object OnLoadFavorites : FavoritesAction
  data class OnFavoriteToggle(val trackId: Int) : FavoritesAction
  data object OnRetryClick : FavoritesAction
}

sealed interface FavoritesEvent {
  // Currently minimal; extend if favorites screen needs navigation
}
```

**ViewModel Logic**:
- Observe repository''s combined `albums` + `favoriteTrackIds` flows
- Filter to show only tracks where `id ∈ favoriteTrackIds`
- Implement `onAction(FavoritesAction)` to handle favorite toggle (delegate to repo)
- Display empty state when `albums.isEmpty()`

### 2.4 UI Components
**Files**:
- `feature/favorites/src/.../ui/FavoritesScreen.kt` — Main composable
- Reuse `AlbumItem.kt` for each favorite track

**FavoritesScreen Content**:
- LazyColumn displaying `AlbumItem` for each favorite
- Empty state: "No favorites yet. Add some from Albums tab."
- Header: Material TopAppBar with "Favorites" title
- Retry button on error (similar to albums screen)

### 2.5 Dependency Injection
**File**: `feature/favorites/src/.../di/FavoritesModule.kt`
- Register `FavoritesViewModel` as factory (scoped to graph or unscoped)
- Import `repository` from `:data` module

---

## Phase 3: Navigation Graph Refactoring

### 3.1 Update AppScreen.kt
**File**: `app/src/main/java/fr/leboncoin/androidrecruitmenttestapp/ui/AppScreen.kt`

**Refactored Structure**:
```
Scaffold(
  bottomBar = {
    BottomNavigationBar(
      selectedTab = viewModel.currentTab,
      onTabSelected = { viewModel.onTabSelected(it) }
    )
  }
) {
  NavHost(
    startDestination = AlbumsTabRoute,
    navController = navController,
  ) {
    // Albums tab graph (existing albumsGraph nested inside)
    navigation<AlbumsTabRoute>(startDestination = AlbumsGraphRoute) {
      albumsGraph(navController, onAlbumSelected = ...)
    }
    
    // Favorites tab graph (new)
    navigation<FavoritesTabRoute>(startDestination = FavoritesRoute) {
      favoritesGraph(navController)
    }
  }
}
```

### 3.2 Bottom Navigation Bar Composable
**File**: `app/src/.../ui/components/BottomNavigationBar.kt` (new)

**Inputs**:
- `selectedTab: NavDestination`
- `onTabSelected: (NavDestination) -> Unit`
- `modifier: Modifier = Modifier`

**Implementation**:
```kotlin
NavigationBar {
  NavigationBarItem(
    icon = { Icon(Icons.Default.Album, "Albums") },
    label = { Text("Albums") },
    selected = selectedTab == AlbumsTabRoute,
    onClick = { onTabSelected(AlbumsTabRoute) },
  )
  
  NavigationBarItem(
    icon = { Icon(Icons.Default.Favorite, "Favorites") },
    label = { Text("Favorites") },
    selected = selectedTab == FavoritesTabRoute,
    onClick = { onTabSelected(FavoritesTabRoute) },
  )
}
```

### 3.3 Import Favorites Routes & Graph
- Add import for `FavoritesTabRoute` and `favoritesGraph` in `AppScreen.kt`
- Ensure `:app` module depends on `:feature:favorites` in `build.gradle.kts`

---

## Phase 4: State Management & Tab Navigation

### 4.1 Extend AppScreenViewModel
**File**: `app/src/main/java/fr/leboncoin/androidrecruitmenttestapp/viewmodel/AppScreenViewModel.kt`

**Add**:
- `_selectedTab: MutableStateFlow<NavDestination>` tracking current tab
- `selectedTab: StateFlow<NavDestination>` public accessor
- `onTabSelected(destination: NavDestination)` action handler that:
  - Updates `_selectedTab` immediately
  - Clears back stack for non-active tabs (optional: keep it for tab preservation)
  - Logs analytics event via `AnalyticsHelper`

### 4.2 Back Stack Management
- When user taps Albums tab while on Favorites:
  - Navigate to `AlbumsTabRoute` (if not already there)
  - Back stack restarts from top of albums list (UX: expected behavior)
  
- **Alternative** (if tab-state preservation desired):
  - Use separate NavControllers per tab (advanced, defer to Phase 5 refinement)

### 4.3 Shared Repository Access
- `FavoritesViewModel` reads repository directly (no special coupling)
- `AlbumsViewModel` continues to manage albums list for Albums tab
- No inter-feature communication; state is local to each tab''s ViewModel

---

## Phase 5: Integration & Testing

### 5.1 End-to-End Navigation Flow
**Test Scenarios**:
1. **Tab Switching**:
   - Launch app → Albums tab active
   - Tap Favorites → Favorites screen shows
   - Tap Albums → Back to albums list (not detail)
   - Tap Favorites → Shows previously loaded favorites (preserves state)

2. **Favorite Toggle Across Tabs**:
   - Albums tab: Mark track as favorite → Favorites tab immediately reflects it
   - Favorites tab: Unmark track → Disappears from list
   - Switch to Albums → Toggle on track that was just unfavorited → Reappears in Favorites

3. **Empty States**:
   - Favorites tab with no favorites → Shows "No favorites yet" message
   - Add favorite from Albums → Favorites tab updates

### 5.2 Integration Checklist
- [ ] `AppScreen.kt` imports `FavoritesTabRoute` and `favoritesGraph` successfully
- [ ] `Scaffold` wraps `NavHost` with `bottomBar` parameter
- [ ] Bottom nav bar appears on all routes
- [ ] Tab selection updates UI and navigates correctly
- [ ] `AnalyticsHelper` tracks tab switches
- [ ] No import cycles or missing dependencies

### 5.3 Offline & Persistence Validation
- **Scenario**: Close app while on Favorites tab → Reopen
  - Expected: App restarts with Albums as default tab (safe fallback)
  - OR: App remembers last selected tab (requires SavedStateHandle in AppScreenViewModel)
  
- **Scenario**: Toggle favorite offline → Go online → Back to app
  - Expected: Toggle change is persisted (handled by existing repository logic)
  - Favorites list updates automatically on both tabs

### 5.4 Testing Coverage
- **Unit Tests**:
  - `FavoritesViewModelTest`: State emission, actions, favorite filtering
  - `AppScreenViewModelTest`: Tab selection, state updates
  - Existing `AlbumsViewModelTest` to verify no regression

- **Integration Tests**:
  - Navigation between tabs (NavControllerTest)
  - Favorite sync across tabs (repository state consistency)

- **UI Tests** (Compose Testing):
  - Bottom nav bar visible and clickable
  - FavoritesScreen displays favorites correctly
  - Empty state shown when no favorites
  - AlbumItem reusability (same component on both tabs)

---

## Phase 6: Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                      AppScreen (Scaffold)                     │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │          BottomNavigationBar (NavigationBar)            │ │
│  │  [ Albums Tab ]  [ Favorites Tab ]                     │ │
│  └─────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                    NavHost                              │ │
│  │  ┌──────────────────────────────────────────────────┐  │ │
│  │  │  AlbumsTabRoute (Navigation)                     │  │ │
│  │  │  ├─ AlbumsGraphRoute (NestedGraph)               │  │ │
│  │  │  │  ├─ AlbumsRoute (AlbumsList)                  │  │ │
│  │  │  │  └─ AlbumDetailRoute (Detail)                 │  │ │
│  │  │  └─ AlbumsViewModel (shared)                     │  │ │
│  │  └──────────────────────────────────────────────────┘  │ │
│  │  ┌──────────────────────────────────────────────────┐  │ │
│  │  │  FavoritesTabRoute (Navigation)                  │  │ │
│  │  │  ├─ FavoritesRoute (FavoritesScreen)             │  │ │
│  │  │  └─ FavoritesViewModel                           │  │ │
│  │  └──────────────────────────────────────────────────┘  │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              ↓
                ┌─────────────────────────────┐
                │  AlbumRepository            │
                │  ├─ observeAlbums()         │
                │  ├─ observeFavoriteTrackIds│
                │  └─ toggleFavorite()        │
                └─────────────────────────────┘
                              ↓
                ┌─────────────────────────────┐
                │  AlbumDao & FavoriteAlbumEntity
                │  (Room Database Persistence)│
                └─────────────────────────────┘
```

---

## File Structure Summary

### New Files to Create

```
feature/favorites/
├── build.gradle.kts
├── src/main/java/fr/leboncoin/feature/favorites/
│   ├── navigation/
│   │   ├── FavoritesRoutes.kt        [NEW]
│   │   └── FavoritesNavGraph.kt      [NEW]
│   ├── presentation/
│   │   ├── FavoritesViewModel.kt     [NEW]
│   │   ├── FavoritesState.kt         [NEW]
│   │   ├── FavoritesAction.kt        [NEW]
│   │   ├── FavoritesEvent.kt         [NEW]
│   │   └── ObserveAsEvents.kt        [COPY from albums]
│   ├── ui/
│   │   └── FavoritesScreen.kt        [NEW]
│   ├── di/
│   │   └── FavoritesModule.kt        [NEW]

app/src/main/java/fr/leboncoin/androidrecruitmenttestapp/
├── ui/
│   ├── AppScreen.kt                  [REFACTOR]
│   └── components/
│       └── BottomNavigationBar.kt    [NEW]
├── viewmodel/
│   └── AppScreenViewModel.kt         [REFACTOR]
```

### Modified Files

1. **AppScreen.kt** — Add Scaffold with bottom nav, refactor NavHost structure
2. **AppScreenViewModel.kt** — Add tab tracking state
3. **app/build.gradle.kts** — Add `:feature:favorites` dependency
4. **settings.gradle.kts** — Add `:feature:favorites` module inclusion

---

## Acceptance Criteria

### Phase 1 ✅
- [ ] Navigation structure documented with routes and graph hierarchy
- [ ] Bottom bar design spec (Material 3 NavigationBar, tabs, icons) finalized
- [ ] State management approach agreed (app-level tab tracking)

### Phase 2 ✅
- [ ] `:feature:favorites` module created and builds successfully
- [ ] `FavoritesViewModel`, `FavoritesState`, actions, events defined
- [ ] `FavoritesScreen` composable displays favorite tracks
- [ ] Empty state shown when no favorites
- [ ] Koin DI wiring complete

### Phase 3 ✅
- [ ] `AppScreen.kt` refactored with Scaffold + bottom nav
- [ ] `BottomNavigationBar` composable functional and Material 3 compliant
- [ ] Both tab routes nested correctly
- [ ] No compilation errors

### Phase 4 ✅
- [ ] `AppScreenViewModel.kt` tracks selected tab
- [ ] Tab switching updates UI correctly
- [ ] Favorite toggle syncs between Albums and Favorites screens in real-time
- [ ] Analytics logged on tab selection

### Phase 5 ✅
- [ ] Manual testing: Tab switching, favorite toggle across tabs
- [ ] Manual testing: Offline scenario (toggle offline, go online)
- [ ] Manual testing: Empty state, retry flow
- [ ] Unit tests for `FavoritesViewModel` and tab navigation
- [ ] No regression in existing albums tests
- [ ] Integration tests verify state consistency across tabs

### Phase 6 ✅
- [ ] Documentation updated with new navigation structure
- [ ] Code review: Bottom nav implementation follows Material 3 guidelines
- [ ] Code review: Module structure consistent with `:feature:albums`
- [ ] Code review: No circular dependencies or unused imports

---

## Key Implementation Notes

1. **Reuse AlbumItem Component**: `FavoritesScreen` uses the same `AlbumItem.kt` component for consistency and reduced code duplication.

2. **Shared Repository**: Both `AlbumsViewModel` and `FavoritesViewModel` read from the same `AlbumRepository` instance. Favorite state is automatically in sync via Flow-based observation.

3. **No Back Stack Sharing**: Tabs maintain independent navigation back stacks. Switching to a tab restarts from its start destination (non-destructive, clear UX).

4. **Favorites Filtering**: `FavoritesViewModel` filters `repository.observeAlbums()` to show only tracks where `trackId ∈ favoriteTrackIds`. Update is automatic when favorite is toggled.

5. **Persistence Already Built**: Room database and `FavoriteAlbumEntity` are already implemented in `:data`. No additional persistence layer needed.

6. **Feature Module Isolation**: `:feature:favorites` has no direct dependency on `:feature:albums`. Both depend only on `:data`.

---

## Further Considerations

1. **Tab State Persistence** — Should the app remember the last selected tab across app restarts?
   - **Option A** (Recommended): Always start with Albums tab (safest, simplest)
   - **Option B**: Use SavedStateHandle to remember last tab (requires AppScreenViewModel enhancement)
   - **Option C**: Persist tab selection to preferences (over-engineering for now)

2. **Favorites Screen Navigation** — Should favorited tracks in Favorites tab be clickable to view detail?
   - **Option A** (Current plan): Favorites tab is read-only, detail view only from Albums
   - **Option B**: Add detail navigation from Favorites tab (requires nested graph similar to Albums)
   - **Option C**: Hybrid (show summary in Favorites, detail link to Albums tab detail view)

3. **Empty State Behavior** — When no favorites exist, should there be a call-to-action?
   - **Option A** (Current plan): Static "No favorites yet" message
   - **Option B**: Button linking to Albums tab to encourage favoriting
   - **Option C**: Dismissible info card with instructions

4. **Favorite Sync Animation** — Should favorite toggle show visual feedback when switching tabs?
   - **Option A**: None (list updates silently)
   - **Option B**: Fade/slide animation for added/removed items
   - **Option C**: Snackbar confirmation (non-intrusive feedback)

---

**Status**: Ready for review and feedback. Once approved, each phase can be executed independently.
