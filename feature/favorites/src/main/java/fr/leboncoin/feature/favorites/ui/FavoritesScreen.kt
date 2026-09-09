package fr.leboncoin.feature.favorites.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.leboncoin.feature.albums.ui.AlbumItem
import fr.leboncoin.feature.favorites.presentation.AlbumUi
import fr.leboncoin.feature.favorites.presentation.FavoritesAction
import fr.leboncoin.feature.favorites.presentation.FavoritesState
import fr.leboncoin.feature.favorites.presentation.FavoritesViewModel

@Composable
fun FavoritesRoot(
    viewModel: FavoritesViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    FavoritesScreen(
        state = state,
        onAction = { viewModel.onAction(it) },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    state: FavoritesState,
    onAction: (FavoritesAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = state.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Button(
                            onClick = { onAction(FavoritesAction.OnRetryClick) },
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            state.albums.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No favorites yet. Add some from Albums tab.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.padding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        vertical = 12.dp,
                    ),
                ) {
                    items(state.albums, key = { it.id }) { album ->
                        AlbumItem(
                            album = album.toAlbumsAlbumUi(),
                            onItemSelected = { /* No detail navigation in favorites tab */ },
                            onFavoriteToggle = { onAction(FavoritesAction.OnFavoriteToggle(album.id)) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Converts favorites AlbumUi to albums AlbumUi for reuse of AlbumItem component.
 */
fun AlbumUi.toAlbumsAlbumUi(): fr.leboncoin.feature.albums.presentation.AlbumUi =
    fr.leboncoin.feature.albums.presentation.AlbumUi(
        id = id,
        albumId = albumId,
        title = title,
        url = url,
        thumbnailUrl = thumbnailUrl,
        albumLabel = albumLabel,
        trackLabel = trackLabel,
        isFavorite = isFavorite,
    )
