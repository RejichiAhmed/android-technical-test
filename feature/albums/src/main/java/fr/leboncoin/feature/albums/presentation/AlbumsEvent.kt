package fr.leboncoin.feature.albums.presentation

sealed interface AlbumsEvent {
    data class NavigateToDetail(val albumId: Int) : AlbumsEvent
    data object NavigateBack : AlbumsEvent
}
