package fr.leboncoin.feature.albums.presentation

import fr.leboncoin.data.network.model.AlbumDto

data class AlbumsState(
    val albums: List<AlbumUi> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedAlbumId: Int? = null,
    val albumGroupCards: List<AlbumGroupCard> = emptyList(),
)

/**
 * Data class representing a UI model for an album/track.
 */
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

/**
 * Converts an AlbumDto to AlbumUi with favorite state.
 */
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

