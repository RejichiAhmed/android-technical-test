# Album Grid Refactoring - Key Code Changes

## 1. New Helper Functions in AlbumsState.kt

### AlbumGroupCard Data Class
```kotlin
data class AlbumGroupCard(
    val albumId: Int,
    val albumLabel: String,
    val thumbnailUrl: String,      // First track's thumbnail
    val trackCount: Int,
    val firstTrackId: Int,          // For navigation
)
```

### Group Albums by Album ID
```kotlin
fun AlbumsState.albumsByGroup(): Map<Int, List<AlbumUi>> =
    visibleAlbums.groupBy { it.albumId }
```

### Convert to Grid Cards
```kotlin
fun AlbumsState.albumGroupCards(): List<AlbumGroupCard> =
    albumsByGroup()
        .map { (albumId, tracks) ->
            AlbumGroupCard(
                albumId = albumId,
                albumLabel = "Album #$albumId",
                thumbnailUrl = tracks.firstOrNull()?.thumbnailUrl ?: "",
                trackCount = tracks.size,
                firstTrackId = tracks.firstOrNull()?.id ?: 0,
            )
        }
        .sortedBy { it.albumId }
```

### Get Tracks for Album Detail
```kotlin
fun AlbumsState.getTracksForAlbum(albumId: Int): List<AlbumUi> =
    visibleAlbums.filter { it.albumId == albumId }
```

## 2. AlbumsScreen Grid Implementation

### Before (List)
```kotlin
LazyColumn(
    verticalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = contentPadding,
) {
    items(
        items = state.visibleAlbums,
        key = { album -> album.id }
    ) { album ->
        AlbumItem(
            album = album,
            onItemSelected = { onAction(AlbumsAction.OnAlbumClick(it.id)) },
            onFavoriteToggle = { onAction(AlbumsAction.OnFavoriteToggle(it.id)) },
        )
    }
}
```

### After (Grid)
```kotlin
LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(12.dp),
    modifier = Modifier.padding(contentPadding),
) {
    items(
        items = state.albumGroupCards(),  // Now using grouped cards
        key = { card -> card.albumId }
    ) { card ->
        AlbumGridCard(
            card = card,
            onCardClick = {
                // Navigate using first track ID in album
                onAction(AlbumsAction.OnAlbumClick(card.firstTrackId))
            },
        )
    }
}
```

### New AlbumGridCard Composable
```kotlin
@Composable
private fun AlbumGridCard(
    card: AlbumGroupCard,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),  // Square cards
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onCardClick() },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Album thumbnail (top, full width)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(card.thumbnailUrl)
                    .httpHeaders(
                        NetworkHeaders.Builder()
                            .add("User-Agent", "LeboncoinApp/1.0")
                            .build()
                    )
                    .crossfade(true)
                    .build(),
                contentDescription = card.albumLabel,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentScale = ContentScale.Crop,
            )

            // Album label and track count (bottom)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = card.albumLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "${card.trackCount} tracks",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
```

## 3. AlbumDetailScreen - Track List

### Before (Single Track Details)
```kotlin
// Showed only:
// - Album thumbnail
// - Track title
// - Album and track labels in details section
```

### After (Track List)
```kotlin
// Album header card
Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
    Column {
        // Thumbnail with 1:1 aspect ratio
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(selectedAlbum.thumbnailUrl)
                // ...
                .build(),
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            contentScale = ContentScale.Crop,
        )
        
        // Album info header
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedAlbum.albumLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${tracksInAlbum.size} track${if (tracksInAlbum.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                // Favorite button
                IconButton(
                    onClick = { onAction(AlbumsAction.OnFavoriteToggle(selectedAlbum.id)) },
                ) {
                    Icon(
                        imageVector = if (selectedAlbum.isFavorite)
                            Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = "Add to favorites",
                        tint = if (selectedAlbum.isFavorite)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

// Track list below
LazyColumn(
    modifier = Modifier.fillMaxWidth().fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
) {
    items(
        items = tracksInAlbum,  // All tracks in album
        key = { track -> track.id }
    ) { track ->
        AlbumItem(
            album = track,
            onItemSelected = { onAction(AlbumsAction.OnAlbumClick(it.id)) },
            onFavoriteToggle = { onAction(AlbumsAction.OnFavoriteToggle(it.id)) },
        )
    }
}
```

## Navigation Flow

### Original Flow
```
AlbumsScreen (list of all tracks)
  ↓ click track
AlbumDetailScreen (show that track's details)
```

### New Flow
```
AlbumsScreen (grid of albums)
  ↓ click album card
AlbumDetailScreen (show all tracks in that album)
  ↓ click track
Can navigate to other albums or view track details
```

### Key Points
- `AlbumsAction.OnAlbumClick(trackId)` still takes a track ID
- When clicking a grid card, we pass `card.firstTrackId` to navigate
- Detail screen uses `state.getTracksForAlbum()` to show all tracks in that album
- Category filtering maintained throughout

## Test Results

✅ **Compilation**: All Kotlin files compile successfully
✅ **Tests**: All unit tests pass
✅ **Architecture**: MVI pattern preserved, no state changes required
✅ **Navigation**: Type-safe routes work correctly
✅ **Favorite Toggle**: Works on individual tracks
✅ **Category Filter**: Properly filters album groups

## Files Modified

1. `AlbumsState.kt` - Added helper functions and data class
2. `AlbumsScreen.kt` - Replaced LazyColumn with LazyVerticalGrid
3. `AlbumDetailScreen.kt` - Changed to show track list
