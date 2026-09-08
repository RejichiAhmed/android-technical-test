package fr.leboncoin.feature.albums.presentation

import fr.leboncoin.data.network.model.AlbumDto

data class AlbumsState(
    val albums: List<AlbumUi> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedAlbumId: Int? = null,
    val availableCategories: List<Int> = emptyList(), // distinct albumIds, sorted ascending
    val selectedCategory: Int? = null,                // null = "All"
    val favoriteTrackIds: Set<Int> = emptySet(),
)

val AlbumsState.visibleAlbums: List<AlbumUi>
    get() = selectedCategory?.let { cat -> albums.filter { it.albumId == cat } } ?: albums


data class AlbumUi(
    val id: Int,
    val albumId: Int,
    val title: String,
    val url: String,
    val thumbnailUrl: String,
    val albumLabel: String,
    val trackLabel: String,
    val isFavorite: Boolean = false,
)

fun AlbumDto.toAlbumUi(isFavorite: Boolean = false): AlbumUi = AlbumUi(
    id = id,
    albumId = albumId,
    title = title,
    url = url,
    thumbnailUrl = thumbnailUrl,
    albumLabel = "Album #$albumId",
    trackLabel = "Track #$id",
    isFavorite = isFavorite,
)

/**
 * Returns the album matching [albumId] from the already-loaded state, or `null`
 * if it hasn't been loaded (yet) or doesn't exist. Used by the detail screen so it
 * never needs to fetch data on its own — it relies on the shared [AlbumsViewModel]
 * state populated by the list screen.
 */
fun AlbumsState.findAlbum(albumId: Int): AlbumUi? = albums.firstOrNull { it.id == albumId }

/**
 * Groups visible albums by albumId (for grid display).
 * Each key is an album ID, and the value is a list of all tracks in that album.
 */
fun AlbumsState.albumsByGroup(): Map<Int, List<AlbumUi>> =
    visibleAlbums.groupBy { it.albumId }

/**
 * Returns a list of album group cards suitable for grid display.
 * Each card contains the album ID, label, thumbnail (from first track),
 * and track count for the album group.
 */
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

/**
 * Returns all tracks for a given album ID from the current state.
 * Filters based on the selected category (if any).
 */
fun AlbumsState.getTracksForAlbum(albumId: Int): List<AlbumUi> =
    visibleAlbums.filter { it.albumId == albumId }

/**
 * Data class representing a groupable album card for grid display.
 */
data class AlbumGroupCard(
    val albumId: Int,
    val albumLabel: String,
    val thumbnailUrl: String, // First track's thumbnail in this album
    val trackCount: Int,
    val firstTrackId: Int, // ID of the first track, used for navigation
)

