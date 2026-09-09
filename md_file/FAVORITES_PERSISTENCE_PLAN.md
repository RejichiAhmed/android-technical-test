# Favorites persistence plan

## Phase 1 — Data model and persistence layer
- Add a Room table for favorite albums in `:data`.
- Extend the DAO to expose favorites as a reactive `Flow` and provide toggle/remove operations.
- Update `AppDatabase` and DI wiring so the favorites table is available across process restarts.
- Keep the existing Room-backed albums cache as the source of truth and add favorites as a second local table.

## Phase 2 — Repository and ViewModel state
- Extend `AlbumRepository` with favorite observation and toggle methods.
- Merge album data + favorite IDs in `AlbumsViewModel` so each `AlbumUi` knows whether it is marked as favorite.
- Preserve the existing offline-first refresh flow without clearing favorites when network requests fail.

## Phase 3 — UI toggle and detail visibility
- Add a favorite toggle button to each album card in the list screen.
- Wire the action into the shared `AlbumsViewModel`.
- Optionally surface the same toggle on the detail screen so the favorite state remains visible after navigation.

## Phase 4 — Validation and regression tests
- Add repository and ViewModel tests covering favorite toggling, persistence restoration, and no-loss behavior.
- Run the focused `:data:test` and `:feature:albums:test` suites.
- Verify cached albums remain available offline while favorite state persists across app restarts.

## Acceptance criteria
- The app remembers favorites after a full app restart.
- Favorites are stored locally in Room, not only in memory.
- The list screen can toggle a favorite directly from each album item.
- Favorite state is restored when the shared ViewModel reads cached albums and favorite IDs.
- Tests cover both toggling and persistence/restore behavior.
