package fr.leboncoin.feature.albums.presentation.liste

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adevinta.spark.components.scaffold.Scaffold
import fr.leboncoin.feature.albums.presentation.AlbumsAction
import fr.leboncoin.feature.albums.presentation.AlbumsEvent
import fr.leboncoin.feature.albums.presentation.AlbumsState
import fr.leboncoin.feature.albums.presentation.AlbumsViewModel
import fr.leboncoin.feature.albums.presentation.ObserveAsEvents
import fr.leboncoin.feature.albums.ui.AlbumItem
import org.koin.androidx.compose.koinViewModel

@Composable
fun AlbumsListRoot(
    onNavigateToDetail: (Int) -> Unit,
    viewModel: AlbumsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onAction(AlbumsAction.OnLoadAlbums)
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is AlbumsEvent.NavigateToDetail -> onNavigateToDetail(event.albumId)
            AlbumsEvent.NavigateBack -> Unit
        }
    }

    AlbumsListScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun AlbumsListScreen(
    state: AlbumsState,
    onAction: (AlbumsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = it,
        ) {
            items(
                items = state.albums,
                key = { album -> album.id }
            ) { album ->
                AlbumItem(
                    album = album,
                    onItemSelected = { onAction(AlbumsAction.OnAlbumClick(it.id)) },
                )
            }
        }
    }
}

@Preview
@Composable
private fun AlbumsListScreenPreview() {
    AlbumsListScreen(state = AlbumsState(), onAction = {})
}
