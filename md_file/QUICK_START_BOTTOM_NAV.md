# Quick Start Guide - Bottom Navigation Bar Implementation

## Build and Install

### Build Debug APK
```bash
./gradlew assembleDebug
```

### Install on Device/Emulator
```bash
./gradlew installDebug
```

### Run Debug Build
```bash
./gradlew runDebug
```

---

## What's New

### Two Main Tabs
- **Albums Tab** (Home Icon) - View all albums with favorites management
- **Favorites Tab** (Star Icon) - View only favorited tracks

### Key Features
✅ Switch between Albums and Favorites with bottom navigation bar
✅ Real-time synchronization of favorite state across tabs
✅ Empty state message when no favorites exist
✅ Material 3 compliant UI components
✅ Analytics tracking for tab selection

---

## Manual Testing Checklist

### Basic Navigation
- [ ] Launch app - Should start on Albums tab
- [ ] Tap Favorites tab - Shows favorites screen (or empty state if no favorites)
- [ ] Tap Albums tab - Back to albums list
- [ ] Bottom navigation bar visible and clickable

### Favorites Management
- [ ] In Albums tab, tap star icon on any album to add favorite
- [ ] Switch to Favorites tab - New favorite should appear immediately
- [ ] In Favorites tab, tap star icon to remove from favorites
- [ ] Album removed immediately from Favorites list
- [ ] Switch back to Albums - Removed album should show as not favorited

### Empty States
- [ ] Remove all favorites - Favorites tab shows "No favorites yet. Add some from Albums tab."
- [ ] Add a favorite - Favorites tab updates and shows the album
- [ ] Remove it again - Empty state returns

### Error Handling
- [ ] (Requires offline/network error simulation) Error message displays with Retry button
- [ ] Tap Retry - Should attempt to refresh

### Detail Screen (Albums Only)
- [ ] In Albums tab, tap album item - Detail screen opens
- [ ] Back arrow works correctly
- [ ] Return to Albums list - Can switch to Favorites and back

### Tab State Persistence
- [ ] Add a favorite in Albums
- [ ] Navigate to detail screen
- [ ] Back - Still on Albums with favorite marked
- [ ] Switch to Favorites - Album appears
- [ ] Switch back to Albums - Still showing same view

---

## Architecture Overview

### Module Structure
```
:app (thin shell)
├─ AppScreen.kt (Scaffold with BottomNavigationBar + NavHost)
├─ AppScreenViewModel.kt (Tab state management)
├─ components/
│  └─ BottomNavigationBar.kt (Material 3 NavigationBar)
└─ DI setup

:feature:albums (Albums tab)
├─ navigation/
│  ├─ AlbumsTabRoute (tab-level)
│  ├─ AlbumsGraphRoute (feature graph)
│  └─ AlbumsRoute, AlbumDetailRoute (destinations)
├─ presentation/ (MVI pattern)
│  ├─ AlbumsViewModel
│  ├─ AlbumsState / Action / Event
│  └─ UI screens
└─ DI (AlbumsModule)

:feature:favorites (NEW - Favorites tab)
├─ navigation/
│  ├─ FavoritesTabRoute (tab-level)
│  ├─ FavoritesRoute (destination)
│  └─ FavoritesNavGraph
├─ presentation/ (MVI pattern)
│  ├─ FavoritesViewModel
│  ├─ FavoritesState / Action / Event
│  └─ FavoritesScreen (reuses AlbumItem from albums)
└─ DI (FavoritesModule)

:data (Shared repository)
└─ Repository with observeAlbums(), observeFavoriteTrackIds(), toggleFavorite()
```

### Data Flow
1. User taps Favorites tab
2. BottomNavigationBar calls `AppScreenViewModel.onTabSelected()`
3. `AppScreenViewModel` updates `selectedTab` StateFlow
4. AppScreen recomposes with new tab route
5. NavHost navigates to FavoritesTabRoute
6. FavoritesScreen observes FavoritesViewModel state
7. ViewModel combines albums and favorite IDs from repository
8. Only favorited albums displayed

---

## Key Implementation Details

### Tab Switching
- Clears back stack of non-active tab for clean state
- Preserves state when returning to a tab (first visit shows list, detail screen preserved)
- Analytics logged for each tab selection

### Favorites Synchronization
- Both tabs observe same `AlbumRepository` instance
- `observeFavoriteTrackIds()` and `observeAlbums()` Flows provide real-time updates
- When favorite toggled, repository updates Room database
- All observing ViewModels receive update automatically

### Component Reuse
- `AlbumItem` component from `:feature:albums` used in `:feature:favorites`
- Ensures consistent album display and interaction pattern
- Conversion function handles domain model mismatch

### Material 3 Compliance
- NavigationBar with ripple effects
- TopAppBar in Favorites screen
- Card-based album items
- Material icons with proper tinting
- Respects system theme colors

---

## Dependency Structure

```
:app
├─ :data ✓
├─ :feature:albums ✓
└─ :feature:favorites ✓

:feature:favorites
├─ :data ✓
└─ :feature:albums ✓ (for AlbumItem component)

:feature:albums
└─ :data ✓

No circular dependencies ✓
```

---

## Files to Review

### Key Implementation Files
1. **app/src/main/java/fr/leboncoin/androidrecruitmenttestapp/ui/AppScreen.kt** - Main app shell with tabs
2. **app/src/main/java/fr/leboncoin/androidrecruitmenttestapp/ui/components/BottomNavigationBar.kt** - Tab navigation UI
3. **feature/favorites/src/main/java/fr/leboncoin/feature/favorites/presentation/FavoritesViewModel.kt** - Core business logic
4. **feature/favorites/src/main/java/fr/leboncoin/feature/favorites/ui/FavoritesScreen.kt** - Favorites UI
5. **feature/albums/src/main/java/fr/leboncoin/feature/albums/navigation/AlbumsRoutes.kt** - Tab-level route added

### Gradle Configuration
1. **settings.gradle.kts** - Module inclusion
2. **app/build.gradle.kts** - Dependencies
3. **feature/favorites/build.gradle.kts** - Favorites module config
4. **PhotoApp.kt** - Koin module registration

---

## Troubleshooting

### Build Fails
```bash
# Clean and rebuild
./gradlew clean build
```

### Cannot Find Symbols
```bash
# Sync Gradle
./gradlew :app:dependencies
```

### Bottom Navigation Not Showing
- Verify `Scaffold` wraps `NavHost` in AppScreen.kt
- Check `bottomBar` parameter is set
- Ensure BottomNavigationBar composable is being called

### Favorites Not Updating Across Tabs
- Verify both ViewModels use `repository.observeFavoriteTrackIds()`
- Check Room database is being updated on toggle
- Inspect logcat for flow emissions

---

## Analytics

Tab switches are logged via `AnalyticsHelper.trackTabSelection()`. Check logcat for entries like:
```
Analytics: User selected tab - AlbumsTabRoute
Analytics: User selected tab - FavoritesTabRoute
```

Album selections from the album list are still logged via existing `trackSelection()` method.

---

## Performance Considerations

- RecycledViewPool (LazyColumn) reused efficiently
- ViewModels scoped to graph/tab for lifecycle management
- Repository provides single source of truth for albums data
- Favorites filtering done in ViewModel, not at DB level (acceptable for small datasets)

---

## Future Enhancements

1. **Favorites Detail Screen**: Allow detail view from Favorites tab
2. **Tab State Persistence**: Remember last selected tab across app restarts
3. **Favorites Sync Animation**: Visual feedback when favorite toggles
4. **Sort/Filter**: Sort favorites by date added, album, etc.
5. **Search**: Search across favorites
6. **Undo Toast**: Snackbar feedback when removing from favorites

---

## Support

For issues or questions about the implementation:
1. Check BOTTOM_NAV_IMPLEMENTATION_COMPLETE.md for detailed documentation
2. Review the code comments in implementation files
3. Check the BOTTOM_NAV_FAVORITES_PLAN.md for architectural decisions
4. Review similar patterns in existing `:feature:albums` module

---

**Implementation Date**: September 9, 2026
**Status**: ✅ Complete and Verified
**Build Output**: BUILD SUCCESSFUL
