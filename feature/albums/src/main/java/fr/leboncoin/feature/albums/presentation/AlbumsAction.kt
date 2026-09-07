package fr.leboncoin.feature.albums.presentation

sealed interface AlbumsAction {
    data object OnLoadAlbums : AlbumsAction
    data class OnAlbumClick(val albumId: Int) : AlbumsAction
    data object OnBackClick : AlbumsAction
    data object OnRetryClick : AlbumsAction
}