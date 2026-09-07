package fr.leboncoin.feature.albums.presentation

data class AlbumsState(
    val albums: List<AlbumUi> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedAlbumId: Int? = null,
)


data class AlbumUi(
    val id: Int,
    val albumId: Int,
    val title: String,
    val thumbnailUrl: String,
    val albumLabel: String,
    val trackLabel: String,
)
