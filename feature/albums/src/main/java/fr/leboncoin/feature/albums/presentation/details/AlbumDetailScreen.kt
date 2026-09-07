package fr.leboncoin.feature.albums.presentation.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adevinta.spark.components.buttons.ButtonFilled
import fr.leboncoin.feature.albums.presentation.AlbumsAction
import fr.leboncoin.feature.albums.presentation.AlbumsEvent
import fr.leboncoin.feature.albums.presentation.AlbumsState
import fr.leboncoin.feature.albums.presentation.AlbumsViewModel
import fr.leboncoin.feature.albums.presentation.ObserveAsEvents
import fr.leboncoin.feature.albums.presentation.findAlbum
import org.koin.androidx.compose.koinViewModel

@Composable
fun AlbumDetailRoot(
    albumId: Int,
    onBack: () -> Unit,
    viewModel: AlbumsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            AlbumsEvent.NavigateBack -> onBack()
            is AlbumsEvent.NavigateToDetail -> Unit
        }
    }

    AlbumDetailScreen(
        state = state,
        albumId = albumId,
        onAction = viewModel::onAction,
    )
}

@Composable
fun AlbumDetailScreen(
    state: AlbumsState,
    albumId: Int,
    onAction: (AlbumsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val album = state.findAlbum(albumId)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        if (album == null && state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return
        }

        if (album == null) {
            Text("Album not found")
            ButtonFilled(
                onClick = { onAction(AlbumsAction.OnBackClick) },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Back")
            }
            return
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = album.title)
            IconButton(
                onClick = { onAction(AlbumsAction.OnFavoriteToggle(album.id)) },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(text = if (album.isFavorite) "★" else "☆")
            }
        }
        Text(text = album.albumLabel)
        Text(text = album.trackLabel)
        ButtonFilled(
            onClick = { onAction(AlbumsAction.OnBackClick) },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Back")
        }
    }
}

@Preview
@Composable
private fun AlbumDetailScreenPreview() {
    AlbumDetailScreen(state = AlbumsState(), albumId = 1, onAction = {})
}
