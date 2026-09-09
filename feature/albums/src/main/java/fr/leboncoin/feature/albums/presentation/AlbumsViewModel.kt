package fr.leboncoin.feature.albums.presentation

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

class AlbumsViewModel(
    private val repository: AlbumRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AlbumsState())
    val state = _state.asStateFlow()

    private val _events = Channel<AlbumsEvent>()
    val events = _events.receiveAsFlow()

    init {
        observeAlbums()
    }

    fun onAction(action: AlbumsAction) {
        when (action) {
            AlbumsAction.OnLoadAlbums -> refreshAlbums()
            AlbumsAction.OnRetryClick -> refreshAlbums()
            is AlbumsAction.OnAlbumClick -> {
                _state.update { it.copy(selectedAlbumId = action.albumId) }
                viewModelScope.launch {
                    _events.send(AlbumsEvent.NavigateToDetail(action.albumId))
                }
            }
            is AlbumsAction.OnFavoriteToggle -> toggleFavorite(action.trackId)
            AlbumsAction.OnBackClick -> {
                viewModelScope.launch {
                    _events.send(AlbumsEvent.NavigateBack)
                }
            }
        }
    }

    private fun observeAlbums() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            combine(
                repository.observeAlbums(),
                repository.observeFavoriteTrackIds(),
            ) { dtos, favoriteIds ->
                dtos to favoriteIds
            }.collect { (dtos, favoriteIds) ->
                val albums = dtos.map { dto ->
                    dto.toAlbumUi(isFavorite = favoriteIds.contains(dto.id))
                }
                computeAlbumGroupCards(albums).let {  list ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            albums = albums,
                            albumGroupCards = list,
                        )
                    }
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

    private fun refreshAlbums() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.refreshAlbums()) {
                is Resource.Success -> {
                    _state.update { it.copy(isLoading = false) }
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(isLoading = false, error = result.message ?: "Unable to load albums")
                    }
                }
            }
        }
    }

    /**
     * Groups albums by albumId and creates grid cards.
     */
    private fun computeAlbumGroupCards(albums: List<AlbumUi>): List<AlbumGroupCard> {
        val grouped = albums.groupBy { it.albumId }
        return grouped
            .map { (albumId, tracks) ->
                AlbumGroupCard(
                    albumId = albumId,
                    albumLabel = "Album #$albumId",
                    thumbnailUrl = tracks.firstOrNull()?.thumbnailUrl ?: "",
                    trackCount = tracks.size,
                    firstTrackId = tracks.firstOrNull()?.id ?: 0,
                )
            }
            .sortedBy { it.albumId }
    }
}
