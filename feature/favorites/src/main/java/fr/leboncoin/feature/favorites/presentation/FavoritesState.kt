package fr.leboncoin.feature.favorites.presentation

import fr.leboncoin.data.network.model.AlbumDto
import fr.leboncoin.feature.albums.presentation.AlbumUi

data class FavoritesState(
    val albums: List<AlbumUi> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val favoriteTrackIds: Set<Int> = emptySet(),
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



fun AlbumUi.toAlbumsAlbumUi(): AlbumUi = AlbumUi(
    id = id,
    albumId = albumId,
    title = title,
    url = url,
    thumbnailUrl = thumbnailUrl,
    albumLabel = albumLabel,
    trackLabel = trackLabel,
    isFavorite = isFavorite,
)
