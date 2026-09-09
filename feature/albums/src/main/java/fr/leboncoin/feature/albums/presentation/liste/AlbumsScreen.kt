package fr.leboncoin.feature.albums.presentation.liste

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.leboncoin.feature.albums.presentation.AlbumsAction
import fr.leboncoin.feature.albums.presentation.AlbumsEvent
import fr.leboncoin.feature.albums.presentation.AlbumsState
import fr.leboncoin.feature.albums.presentation.AlbumsViewModel
import fr.leboncoin.feature.albums.presentation.AlbumUi
import fr.leboncoin.feature.albums.presentation.ObserveAsEvents
import fr.leboncoin.feature.albums.ui.AlbumGridCard
import org.koin.androidx.compose.koinViewModel
import com.adevinta.spark.components.buttons.ButtonFilled

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsListScreen(
    state: AlbumsState,
    onAction: (AlbumsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
    ) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                state.isLoading && state.albumGroupCards.isEmpty() -> FullScreenLoading(contentPadding)

                state.error != null && state.albumGroupCards.isEmpty() -> FullScreenError(
                    message = state.error,
                    onRetry = { onAction(AlbumsAction.OnRetryClick) },
                    contentPadding = contentPadding,
                )

                else -> Column(modifier = Modifier.fillMaxSize()) {
                    if (state.albumGroupCards.isEmpty() && !state.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("No albums")
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(12.dp),
                            modifier = Modifier.padding(contentPadding),
                        ) {
                            items(
                                items = state.albumGroupCards,
                                key = { card -> card.albumId }
                            ) { card ->
                                AlbumGridCard(
                                    card = card,
                                    onCardClick = {
                                        // Navigate using the first track ID in this album
                                        onAction(AlbumsAction.OnAlbumClick(card.firstTrackId))
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullScreenLoading(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun FullScreenError(
    message: String,
    onRetry: () -> Unit,
    contentPadding: PaddingValues,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message)
            ButtonFilled(
                onClick = onRetry,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
        )
        ButtonFilled(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Preview
@Composable
private fun AlbumsListScreenPreview() {
    AlbumsListScreen(state = AlbumsState(), onAction = {})
}

@Preview
@Composable
private fun AlbumsListScreenLoadingPreview() {
    AlbumsListScreen(
        state = AlbumsState(isLoading = true),
        onAction = {},
    )
}

@Preview
@Composable
private fun AlbumsListScreenErrorWithCachePreview() {
    AlbumsListScreen(
        state = AlbumsState(
            albums = listOf(
                AlbumUi(
                    id = 1,
                    albumId = 1,
                    title = "Cached album",
                    thumbnailUrl = "",
                    url = "",
                    albumLabel = "Album #1",
                    trackLabel = "Track #1",
                )
            ),
            error = "Network unavailable",
        ),
        onAction = {},
    )
}

@Preview
@Composable
private fun AlbumsListScreenErrorNoCachePreview() {
    AlbumsListScreen(
        state = AlbumsState(error = "Network unavailable"),
        onAction = {},
    )
}

@Preview
@Composable
private fun AlbumsListScreenPopulatedPreview() {
    AlbumsListScreen(
        state = AlbumsState(
            albums = (1..6).map { id ->
                AlbumUi(
                    id = id,
                    albumId = (id + 1) / 2, // 2 tracks per album
                    title = "Album title $id",
                    url = "",
                    thumbnailUrl = "",
                    albumLabel = "Album #${(id + 1) / 2}",
                    trackLabel = "Track #$id",
                )
            },
        ),
        onAction = {},
    )
}
