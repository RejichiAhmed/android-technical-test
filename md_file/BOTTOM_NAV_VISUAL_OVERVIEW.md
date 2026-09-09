# Bottom Navigation Bar Implementation - Visual Overview

## 🎯 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        PhotoApp                                 │
│                   (Koin DI Setup)                               │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  Modules:                                                  │ │
│  │  • DataModule (repositories, network, db)                 │ │
│  │  • AppDependenciesProvider (AppScreenViewModel)           │ │
│  │  • AlbumsModule (AlbumsViewModel)                         │ │
│  │  • FavoritesModule (FavoritesViewModel) ← NEW              │ │
│  └────────────────────────────────────────────────────────────┘ │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────────┐
│                      MainActivity                               │
│                      (Compose Activity)                         │
│  AppScreen { ... }                                              │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────────┐
│                      Scaffold                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  BottomBar: BottomNavigationBar                           │ │
│  │  ┌──────────────────────────────────────────────────────┐ │ │
│  │  │  NavigationBar {                                    │ │ │
│  │  │    Albums (Home) │ Favorites (Star)                │ │ │
│  │  │  }                                                  │ │ │
│  │  └──────────────────────────────────────────────────────┘ │ │
│  │                                                            │ │
│  │  Content: NavHost                                         │ │
│  │  ┌──────────────────────────────────────────────────────┐ │ │
│  │  │ navigation<AlbumsTabRoute> {                        │ │ │
│  │  │   albumsGraph(...)  ← Existing nested graph         │ │ │
│  │  │ }                                                  │ │ │
│  │  │                                                    │ │ │
│  │  │ favoritesGraph(...)  ← New                          │ │ │
│  │  └──────────────────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────┬──────────────────────────────────────────┘
                      │
        ┌─────────────┴──────────────┐
        │                            │
        ↓                            ↓
  AlbumsTab                    FavoritesTab
  ┌──────────────────┐        ┌────────────────────┐
  │ AlbumsGraphRoute │        │ FavoritesTabRoute  │
  │  ┌────────────┐  │        │ ┌────────────────┐ │
  │  │ AlbumsRoute│  │        │ │ FavoritesRoute │ │
  │  │(List View) │  │        │ │ (Favorites)    │ │
  │  └─────┬──────┘  │        │ └────────────────┘ │
  │        │         │        └────────────────────┘
  │  ┌─────↓──────┐  │
  │  │ AlbumDetail│  │
  │  │(Detail Vie)│  │
  │  └────────────┘  │
  │                  │
  │ AlbumsViewModel  │        FavoritesViewModel
  │ (Shared scope)   │        (Per-tab scope)
  └──────────────────┘        └────────────────────┘
        │                            │
        └──────────┬─────────────────┘
                   │
                   ↓
          AlbumRepository
          (Shared Instance)
          • observeAlbums()
          • observeFavoriteTrackIds()
          • toggleFavorite()
          • refreshAlbums()
                   │
                   ↓
          Room Database
          Retrofit Network
          OkHttp Client
```

---

## 🎨 Screen Layout

### Albums Tab (Default)

```
┌─────────────────────────────────┐
│  Albums List (Top App Bar)      │
├─────────────────────────────────┤
│                                 │
│  ┌───────────────────────────┐  │
│  │ Album Item 1 (Card)      │  │
│  │ [Image] Album Title  [☆] │  │
│  └───────────────────────────┘  │
│                                 │
│  ┌───────────────────────────┐  │
│  │ Album Item 2 (Card)      │  │
│  │ [Image] Album Title  [★] │  │ ← Marked favorite
│  └───────────────────────────┘  │
│                                 │
│  ┌───────────────────────────┐  │
│  │ Album Item 3 (Card)      │  │
│  │ [Image] Album Title  [☆] │  │
│  └───────────────────────────┘  │
│                                 │
├─────────────────────────────────┤
│  [🏠 Albums]    [⭐ Favorites]   │ ← Bottom Nav
└─────────────────────────────────┘
```

### Favorites Tab (Empty State)

```
┌─────────────────────────────────┐
│  Favorites (Top App Bar)        │
├─────────────────────────────────┤
│                                 │
│                                 │
│       No favorites yet.          │
│    Add some from Albums tab.    │
│                                 │
│                                 │
│                                 │
├─────────────────────────────────┤
│  [🏠 Albums]    [⭐ Favorites]   │ ← Bottom Nav
└─────────────────────────────────┘
```

### Favorites Tab (With Data)

```
┌─────────────────────────────────┐
│  Favorites (Top App Bar)        │
├─────────────────────────────────┤
│                                 │
│  ┌───────────────────────────┐  │
│  │ Album Item 2 (Card)      │  │
│  │ [Image] Album Title  [★] │  │ ← Favorite
│  └───────────────────────────┘  │
│                                 │
│  ┌───────────────────────────┐  │
│  │ Album Item 5 (Card)      │  │
│  │ [Image] Album Title  [★] │  │ ← Favorite
│  └───────────────────────────┘  │
│                                 │
│  ┌───────────────────────────┐  │
│  │ Album Item 7 (Card)      │  │
│  │ [Image] Album Title  [★] │  │ ← Favorite
│  └───────────────────────────┘  │
│                                 │
├─────────────────────────────────┤
│  [🏠 Albums]    [⭐ Favorites]   │ ← Bottom Nav
└─────────────────────────────────┘
```

---

## 🔄 State Flow Diagram

```
                    User Interaction
                          │
                          ↓
                  ┌───────────────┐
                  │  Tab Selected  │
                  └───────┬───────┘
                          │
                ┌─────────┴─────────┐
                │                   │
                ↓                   ↓
        AlbumsTabRoute    FavoritesTabRoute
                │                   │
    ┌───────────┴──────────┐       ↓
    │                      │    Navigate to
    ↓                      │    FavoritesRoute
  Navigate to              │
  AlbumsGraphRoute         ↓
    │                FavoritesViewModel
    ↓                   (New ViewModel)
  AlbumsViewModel        • observe favorites
  (Existing ViewModel)   • filter by IDs
    │                    • emit state
    ├─ observe albums    • handle actions
    ├─ observe favorites
    ├─ handle favorite toggle
    └─ emit state
        │
        ├─ Album List displays
        │
        └─ Toggle Favorite
            │
            ↓
        toggleFavorite(trackId)
            │
            ├─ Update Room DB
            ├─ Emit new state
            ├─ AlbumsViewModel updates
            └─ FavoritesViewModel filters
                │
                ├─ List updates
                └─ Real-time sync across tabs
```

---

## 📱 Tab Switching Flow

```
Initial State: Albums Tab Active

    User taps Favorites tab
         │
         ↓
    AppScreenViewModel.onTabSelected(FavoritesTabRoute)
         │
         ├─ Update _selectedTab state
         ├─ Analytics.trackTabSelection("Favorites")
         └─ Trigger navigation
             │
             ↓
        NavController.navigate(FavoritesTabRoute)
             │
             ├─ popUpTo(startDestination) { saveState = true }
             ├─ launchSingleTop = true
             └─ restoreState = true
                 │
                 ↓
        Clear back stack for other tabs
        Start destination = FavoritesRoute
             │
             ↓
        BottomNavigationBar re-composes
        (selectedTab now = FavoritesTabRoute)
             │
             ├─ Favorites tab highlighted
             └─ FavoritesScreen displays
                 │
                 ↓
        FavoritesViewModel loads state
             │
             ├─ observe albums + favorite IDs
             ├─ filter to favorites
             └─ emit FavoritesState
```

---

## 🔄 Favorite Toggle Sync

```
User marks Album #5 as favorite in Albums tab:

    AlbumsScreen: Click ☆ icon
         │
         ↓
    AlbumsAction.OnFavoriteToggle(trackId = 5)
         │
         ↓
    AlbumsViewModel.onAction()
         │
         ↓
    toggleFavorite(trackId = 5)
         │
         ↓
    AlbumRepository.toggleFavorite(trackId = 5)
         │
         ├─ Update Room DB (FavoriteAlbumEntity)
         └─ Emit new favorite IDs set
             │
             ├─ AlbumsViewModel observes update
             │  └─ Recomputes albums + favorite state
             │     └─ Star fills for album #5
             │
             └─ FavoritesViewModel observes update
                └─ Recomputes filtered list
                   └─ Album #5 now appears in favorites
                      
    Result: Real-time sync across both tabs!
```

---

## 📊 State Management Hierarchy

```
AppScreenViewModel (App Level)
├── _selectedTab: MutableStateFlow<Any>
│   ├── Exposed as: selectedTab StateFlow
│   └── Used by: BottomNavigationBar
│
AppDependenciesProvider (Koin Module)
├── provides: AppScreenViewModel
│   └── depends on: AnalyticsHelper
│
FeatureLevel:

AlbumsViewModel (Scoped to AlbumsGraphRoute)
├── _state: AlbumsState
│   ├── albums: List<AlbumUi>
│   ├── isLoading: Boolean
│   ├── error: String?
│   ├── favoriteTrackIds: Set<Int>
│   ├── selectedAlbumId: Int?
│   ├── availableCategories: List<Int>
│   └── selectedCategory: Int?
├── _events: Channel<AlbumsEvent>
│
FavoritesViewModel (Scoped to FavoritesTabRoute)
├── _state: FavoritesState
│   ├── albums: List<AlbumUi> (filtered)
│   ├── isLoading: Boolean
│   ├── error: String?
│   └── favoriteTrackIds: Set<Int>
├── _events: Channel<FavoritesEvent>
│
DataLayer:

AlbumRepository (Singleton)
├── _albums: MutableStateFlow<List<AlbumDto>>
├── _favoriteTrackIds: MutableStateFlow<Set<Int>>
├── fun observeAlbums(): Flow<List<AlbumDto>>
├── fun observeFavoriteTrackIds(): Flow<Set<Int>>
├── suspend fun toggleFavorite(trackId: Int): Resource<Unit>
└── suspend fun refreshAlbums(): Resource<Unit>
```

---

## 🎯 Navigation Route Hierarchy

```
AppScreen (NavHost)
├── AlbumsTabRoute (parent graph)
│   └── AlbumsGraphRoute (nested parent graph)
│       ├── AlbumsRoute (screen: AlbumsList)
│       └── AlbumDetailRoute(albumId: Int) (screen: AlbumDetail)
│
└── FavoritesTabRoute (parent graph) ← NEW
    └── FavoritesRoute (screen: FavoritesScreen) ← NEW
```

---

## 🚀 Data Flow During Favorite Toggle

```
┌─────────────────────────────────────────────────────┐
│ User Interaction Layer (Compose UI)                 │
│  AlbumsScreen / FavoritesScreen                     │
└──────────────┬──────────────────────────────────────┘
               │ onFavoriteToggle(trackId)
               ↓
┌─────────────────────────────────────────────────────┐
│ Action Layer                                        │
│  AlbumsAction.OnFavoriteToggle(trackId)              │
│  FavoritesAction.OnFavoriteToggle(trackId)           │
└──────────────┬──────────────────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────────────────┐
│ ViewModel Layer                                     │
│  AlbumsViewModel.onAction(action)                    │
│  FavoritesViewModel.onAction(action)                 │
│  → toggleFavorite(trackId)                           │
└──────────────┬──────────────────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────────────────┐
│ Repository Layer                                    │
│  AlbumRepository.toggleFavorite(trackId)             │
└──────────────┬──────────────────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────────────────┐
│ Data Layer                                          │
│  Room Database (AlbumDao)                            │
│  + FavoriteAlbumEntity insert/delete                 │
└──────────────┬──────────────────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────────────────┐
│ State Update Emission                               │
│  observeFavoriteTrackIds() emits new Set<Int>       │
└──────────────┬──────────────────────────────────────┘
               │
         ┌─────┴─────┐
         │           │
         ↓           ↓
    AlbumsVM     FavoritesVM
    Updates      Updates
    State        State
         │           │
         ├─ Refilter │
         │   items   └─ Filter by new IDs
         │           
         ↓           ↓
    Recompose   Recompose
    Albums      Favorites
    Screen      Screen
         │           │
         └─────┬─────┘
               │
               ↓
        UI Updated
        Both Tabs Show
        Real-Time Sync
```

---

## ✅ Testing Scenarios Visual

```
Test 1: Tab Switching
┌──────────────────┐         ┌──────────────────┐
│   Albums Tab     │  Tap    │  Favorites Tab   │
│   (Active)       │─────→   │   (Active)       │
│   Albums List    │         │  Favorites List  │
└──────────────────┘         └──────────────────┘
         ↑ Tap                         ↓ Tap
         └─────────────────────────────┘

Test 2: Real-Time Sync
┌──────────────────┐              ┌──────────────────┐
│   Albums Tab     │              │  Favorites Tab   │
│   Album #5  [☆] │              │   (Empty)        │
│   Album #3  [☆] │─ tap star ──→│   Album #5  [★]  │
│   Album #1  [☆] │              │                  │
└──────────────────┘              └──────────────────┘
                                   Real-time
                                   appears!

Test 3: Multiple Favorites
┌──────────────────────────┐
│   Favorites Tab          │
│   ┌──────────────────┐   │
│   │ Album #5  [★]    │   │
│   ├──────────────────┤   │
│   │ Album #10 [★]    │   │
│   ├──────────────────┤   │
│   │ Album #15 [★]    │   │
│   └──────────────────┘   │
│   (Scrollable list)      │
└──────────────────────────┘
```

---

## 🔌 Dependency Injection Wiring

```
PhotoApp.onCreate()
    ↓
startKoin {
    modules(
        DataModule
        ├─ AlbumRepository (singleton)
        ├─ AlbumDao (singleton)
        ├─ AlbumApi (singleton)
        └─ AnalyticsHelper (singleton)
    
    AppDependenciesProvider
        └─ AppScreenViewModel (factory)
           └─ depends on: AnalyticsHelper
    
    AlbumsModule
        └─ AlbumsViewModel (viewModel)
           └─ depends on: AlbumRepository
    
    FavoritesModule ← NEW
        └─ FavoritesViewModel (viewModel)
           └─ depends on: AlbumRepository
    )
}
```

---

## 📱 Complete User Journey

```
1. App Launch
   ↓
   PhotoApp.onCreate() starts Koin
   ↓
   MainActivity launches
   ↓
   AppScreen composable loads
   ↓
   Albums tab active by default
   ↓
   AlbumsViewModel loads all albums
   ↓
   AlbumsScreen displays list

2. User marks favorite
   ↓
   Click star icon on album
   ↓
   AlbumsViewModel toggles favorite
   ↓
   Repository updates Room DB
   ↓
   FavoritesViewModel sees new data
   ↓
   AlbumsScreen updates (star fills)

3. User switches to Favorites tab
   ↓
   Click Favorites tab in bottom nav
   ↓
   AppScreenViewModel updates selectedTab
   ↓
   NavController navigates to FavoritesRoute
   ↓
   FavoritesScreen displays
   ↓
   Favorited album visible

4. User unmarks favorite from Favorites
   ↓
   Click star icon on favorite
   ↓
   FavoritesViewModel toggles favorite
   ↓
   Album disappears from Favorites
   ↓
   Switch to Albums tab
   ↓
   Album shows with empty star

5. Complete cycle verified! ✅
```

---

**This visual overview illustrates the complete architecture and flow of the Bottom Navigation Bar implementation.** 🎨
