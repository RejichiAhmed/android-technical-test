package fr.leboncoin.feature.albums.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.leboncoin.data.network.model.AlbumDto
import fr.leboncoin.data.network.util.Resource
import fr.leboncoin.data.repository.AlbumRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
            AlbumsAction.OnBackClick -> {
                viewModelScope.launch {
                    _events.send(AlbumsEvent.NavigateBack)
                }
            }
            is AlbumsAction.OnCategorySelected -> {
                _state.update { it.copy(selectedCategory = action.albumId) }
            }
        }
    }

    private fun observeAlbums() {
        viewModelScope.launch {
            repository.observeAlbums().collect { dtos ->
                _state.update {
                    it.copy(
                        albums = dtos.map { dto -> dto.toAlbumUi() },
                        availableCategories = dtos.map { dto -> dto.albumId }.distinct().sorted(),
                    )
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
}
