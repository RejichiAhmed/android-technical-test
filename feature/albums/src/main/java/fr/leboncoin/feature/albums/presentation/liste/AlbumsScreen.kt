package fr.leboncoin.feature.albums.presentation.liste

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adevinta.spark.ExperimentalSparkApi
import com.adevinta.spark.components.buttons.ButtonFilled
import com.adevinta.spark.components.chips.ChipIntent
import com.adevinta.spark.components.chips.ChipTinted
import com.adevinta.spark.components.scaffold.Scaffold
import fr.leboncoin.feature.albums.presentation.AlbumsAction
import fr.leboncoin.feature.albums.presentation.AlbumsEvent
import fr.leboncoin.feature.albums.presentation.AlbumsState
import fr.leboncoin.feature.albums.presentation.AlbumsViewModel
import fr.leboncoin.feature.albums.presentation.ObserveAsEvents
import fr.leboncoin.feature.albums.presentation.visibleAlbums
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

@OptIn(ExperimentalSparkApi::class)
@Composable
fun AlbumsListScreen(
    state: AlbumsState,
    onAction: (AlbumsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CategoryTopBar(
                availableCategories = state.availableCategories,
                selectedCategory = state.selectedCategory,
                onCategorySelected = { onAction(AlbumsAction.OnCategorySelected(it)) },
            )
        },
    ) { contentPadding ->
        when {
            state.isLoading && state.albums.isEmpty() -> FullScreenLoading(contentPadding)

            state.error != null && state.visibleAlbums.isEmpty() ->
                FullScreenError(
                    message = state.error,
                    onRetry = { onAction(AlbumsAction.OnRetryClick) },
                    contentPadding = contentPadding,
                )

            else -> Column(modifier = Modifier.fillMaxSize()) {
                if (state.error != null) {
                    ErrorBanner(
                        message = state.error,
                        onRetry = { onAction(AlbumsAction.OnRetryClick) },
                    )
                }

                if (state.visibleAlbums.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("No albums in this category")
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = contentPadding,
                    ) {
                        items(
                            items = state.visibleAlbums,
                            key = { album -> album.id }
                        ) { album ->
                            AlbumItem(
                                album = album,
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

@Composable
private fun CategoryTopBar(
    availableCategories: List<Int>,
    selectedCategory: Int?,
    onCategorySelected: (Int?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChipTinted(
            text = "All",
            intent = if (selectedCategory == null) ChipIntent.Main else ChipIntent.Basic,
            onClick = { onCategorySelected(null) },
        )
        availableCategories.forEach { albumId ->
            ChipTinted(
                text = "Album #$albumId",
                intent = if (selectedCategory == albumId) ChipIntent.Main else ChipIntent.Basic,
                onClick = { onCategorySelected(albumId) },
            )
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
                fr.leboncoin.feature.albums.presentation.AlbumUi(
                    id = 1,
                    albumId = 1,
                    title = "Cached album",
                    thumbnailUrl = "",
                    albumLabel = "Album #1",
                    trackLabel = "Track #1",
                )
            ),
            availableCategories = listOf(1),
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
            albums = (1..3).map { id ->
                fr.leboncoin.feature.albums.presentation.AlbumUi(
                    id = id,
                    albumId = id,
                    title = "Album title $id",
                    thumbnailUrl = "",
                    albumLabel = "Album #$id",
                    trackLabel = "Track #$id",
                )
            },
            availableCategories = listOf(1, 2, 3),
            selectedCategory = 2,
        ),
        onAction = {},
    )
}
