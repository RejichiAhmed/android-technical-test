package fr.leboncoin.feature.favorites.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.leboncoin.data.network.util.Resource
import fr.leboncoin.data.repository.AlbumRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val repository: AlbumRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FavoritesState())
    val state = _state.asStateFlow()

    private val _events = Channel<FavoritesEvent>()
    val events = _events.receiveAsFlow()

    init {
        observeFavorites()
    }

    fun onAction(action: FavoritesAction) {
        when (action) {
            FavoritesAction.OnLoadFavorites -> refreshFavorites()
            FavoritesAction.OnRetryClick -> refreshFavorites()
            is FavoritesAction.OnFavoriteToggle -> toggleFavorite(action.trackId)
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            combine(
                repository.observeAlbums(),
                repository.observeFavoriteTrackIds(),
            ) { dtos, favoriteIds ->
                dtos to favoriteIds
            }.collect { (dtos, favoriteIds) ->
                // Filter albums to only show favorites
                val favorites = dtos
                    .filter { favoriteIds.contains(it.id) }
                    .map { dto ->
                        dto.toAlbumUi(isFavorite = true)
                    }
                _state.update {
                    it.copy(
                        albums = favorites,
                        favoriteTrackIds = favoriteIds,
                    )
                }
            }
        }
    }

    private fun toggleFavorite(trackId: Int) {
        viewModelScope.launch {
            when (val result = repository.toggleFavorite(trackId)) {
                is Resource.Success -> Unit
                is Resource.Error -> {
                    _state.update { it.copy(error = result.message ?: "Unable to update favorite state") }
                }
            }
        }
    }

    private fun refreshFavorites() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.refreshAlbums()) {
                is Resource.Success -> {
                    _state.update { it.copy(isLoading = false) }
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(isLoading = false, error = result.message ?: "Unable to load favorites")
                    }
                }
            }
        }
    }
}
