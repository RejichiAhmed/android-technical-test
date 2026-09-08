package fr.leboncoin.feature.favorites.presentation

import fr.leboncoin.data.network.model.AlbumDto

data class FavoritesState(
    val albums: List<AlbumUi> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val favoriteTrackIds: Set<Int> = emptySet(),
)

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
