package fr.leboncoin.feature.albums.presentation.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import fr.leboncoin.feature.albums.presentation.AlbumsAction
import fr.leboncoin.feature.albums.presentation.AlbumsEvent
import fr.leboncoin.feature.albums.presentation.AlbumsState
import fr.leboncoin.feature.albums.presentation.AlbumsViewModel
import fr.leboncoin.feature.albums.presentation.AlbumUi
import fr.leboncoin.feature.albums.presentation.ObserveAsEvents
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    state: AlbumsState,
    albumId: Int,
    onAction: (AlbumsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Compute album and tracks locally from state
    val selectedAlbum = state.albums.firstOrNull { it.id == albumId }
    val tracksInAlbum = state.albums.filter { it.albumId == selectedAlbum?.albumId ?: -1 }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { onAction(AlbumsAction.OnBackClick) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { contentPadding ->
        when {
            selectedAlbum == null && state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            selectedAlbum == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Album not found")
                        Button(
                            onClick = { onAction(AlbumsAction.OnBackClick) },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Back")
                        }
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // Album header with thumbnail, title, and favorite toggle
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Column {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(selectedAlbum.url)
                                    .httpHeaders(
                                        NetworkHeaders.Builder()
                                            .add("User-Agent", "LeboncoinApp/1.0")
                                            .build()
                                    )
                                    .crossfade(true)
                                    .build(),
                                contentDescription = selectedAlbum.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                                contentScale = ContentScale.Crop,
                            )

                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedAlbum.albumLabel,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text = "${tracksInAlbum.size} track${if (tracksInAlbum.size != 1) "s" else ""}",
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // List of tracks in this album
                    if (tracksInAlbum.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("No tracks in this album")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        ) {
                            items(
                                items = tracksInAlbum,
                                key = { track -> track.id }
                            ) { track ->
                                AlbumItem(
                                    album = track,
                                    onItemSelected = { onAction(AlbumsAction.OnAlbumClick(it.id)) },
                                    onFavoriteToggle = { onAction(AlbumsAction.OnFavoriteToggle(it.id)) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AlbumDetailScreenPreview() {
    AlbumDetailScreen(state = AlbumsState(), albumId = 1, onAction = {})
}

@Preview
@Composable
private fun AlbumDetailScreenWithTracksPreview() {
    AlbumDetailScreen(
        state = AlbumsState(
            albums = listOf(
                AlbumUi(
                    id = 1,
                    albumId = 1,
                    title = "Track 1",
                    url = "",
                    thumbnailUrl = "",
                    albumLabel = "Album #1",
                    trackLabel = "Track #1",
                ),
                AlbumUi(
                    id = 2,
                    albumId = 1,
                    title = "Track 2",
                    thumbnailUrl = "",
                    url = "",
                    albumLabel = "Album #1",
                    trackLabel = "Track #2",
                ),
                AlbumUi(
                    id = 3,
                    albumId = 1,
                    title = "Track 3",
                    thumbnailUrl = "",
                    url = "",
                    albumLabel = "Album #1",
                    trackLabel = "Track #3",
                ),
            ),
        ),
        albumId = 1,
        onAction = {},
    )
}
