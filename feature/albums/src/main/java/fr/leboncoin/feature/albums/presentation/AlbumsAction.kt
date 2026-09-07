package fr.leboncoin.feature.albums.presentation

sealed interface AlbumsAction {
    data object OnLoadAlbums : AlbumsAction
    data class OnAlbumClick(val albumId: Int) : AlbumsAction
    data class OnFavoriteToggle(val trackId: Int) : AlbumsAction
    data object OnBackClick : AlbumsAction
    data object OnRetryClick : AlbumsAction
    data class OnCategorySelected(val albumId: Int?) : AlbumsAction // null = All
}