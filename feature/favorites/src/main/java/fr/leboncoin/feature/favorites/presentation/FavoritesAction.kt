package fr.leboncoin.feature.favorites.presentation

sealed interface FavoritesAction {
    data object OnLoadFavorites : FavoritesAction
    data class OnFavoriteToggle(val trackId: Int) : FavoritesAction
    data object OnRetryClick : FavoritesAction
}
