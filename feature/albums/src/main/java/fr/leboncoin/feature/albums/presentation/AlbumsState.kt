package fr.leboncoin.feature.albums.presentation

import fr.leboncoin.data.network.model.AlbumDto

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


data class AlbumUi(
    val id: Int,
    val albumId: Int,
    val title: String,
    val thumbnailUrl: String,
    val albumLabel: String,
    val trackLabel: String,
)

fun AlbumDto.toAlbumUi(): AlbumUi = AlbumUi(
    id = id,
    albumId = albumId,
    title = title,
    thumbnailUrl = thumbnailUrl,
    albumLabel = "Album #$albumId",
    trackLabel = "Track #$id",
)

/**
 * Returns the album matching [albumId] from the already-loaded state, or `null`
 * if it hasn't been loaded (yet) or doesn't exist. Used by the detail screen so it
 * never needs to fetch data on its own — it relies on the shared [AlbumsViewModel]
 * state populated by the list screen.
 */
fun AlbumsState.findAlbum(albumId: Int): AlbumUi? = albums.firstOrNull { it.id == albumId }

