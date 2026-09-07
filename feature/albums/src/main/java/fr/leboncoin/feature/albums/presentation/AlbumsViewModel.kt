package fr.leboncoin.feature.albums.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.leboncoin.data.network.model.AlbumDto
import fr.leboncoin.data.network.util.Resource
import fr.leboncoin.data.repository.AlbumRepositoryImp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun AlbumDto.toAlbumUi(): AlbumUi = AlbumUi(
    id = id,
    albumId = albumId,
    title = title,
    thumbnailUrl = thumbnailUrl,
    albumLabel = "Album #$albumId",
    trackLabel = "Track #$id",
)

/**
 * Returns the album matching [albumId] from the already-loaded state, or `null`
 * if it hasn't been loaded (yet) or doesn't exist. Used by the detail screen so it
 * never needs to fetch data on its own — it relies on the shared [AlbumsViewModel]
 * state populated by the list screen.
 */
fun AlbumsState.findAlbum(albumId: Int): AlbumUi? = albums.firstOrNull { it.id == albumId }

class AlbumsViewModel(
    private val repository: AlbumRepositoryImp,
) : ViewModel() {

    private val _state = MutableStateFlow(AlbumsState())
    val state = _state.asStateFlow()


    private val _events = Channel<AlbumsEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: AlbumsAction) {
        when (action) {
            AlbumsAction.OnLoadAlbums -> loadAlbums()
            AlbumsAction.OnRetryClick -> loadAlbums(forceReload = true)
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
        }
    }

    private fun loadAlbums(forceReload: Boolean = false) {
        val currentState = _state.value
        if (!forceReload && currentState.albums.isNotEmpty() && currentState.error == null) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when(val result = repository.getAllAlbums()) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            albums = result.data?.map { dto -> dto.toAlbumUi() } ?: emptyList(),
                            isLoading = false
                        )
                    }
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
