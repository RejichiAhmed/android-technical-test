package fr.leboncoin.feature.favorites

import fr.leboncoin.data.network.model.AlbumDto
import fr.leboncoin.data.network.util.Resource
import fr.leboncoin.data.repository.AlbumRepository
import fr.leboncoin.feature.favorites.presentation.FavoritesAction
import fr.leboncoin.feature.favorites.presentation.FavoritesViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeAlbumRepository(
    initialAlbums: List<AlbumDto> = emptyList(),
    initialFavoriteAlbumIds: Set<Int> = emptySet(),
) : AlbumRepository {

    private val albumsFlow = MutableStateFlow(initialAlbums)
    private val favoritesFlow = MutableStateFlow(initialFavoriteAlbumIds)
    var onRefresh: suspend () -> Resource<Unit> = { Resource.Success(Unit) }

    override suspend fun observeAlbums(): Flow<List<AlbumDto>> = albumsFlow

    override suspend fun observeFavoriteTrackIds(): Flow<Set<Int>> = favoritesFlow

    override suspend fun refreshAlbums(): Resource<Unit> = onRefresh()

    override suspend fun toggleFavorite(trackId: Int): Resource<Unit> {
        val next = if (favoritesFlow.value.contains(trackId)) {
            favoritesFlow.value - trackId
        } else {
            favoritesFlow.value + trackId
        }
        favoritesFlow.value = next
        return Resource.Success(Unit)
    }

    fun emit(albums: List<AlbumDto>) {
        albumsFlow.value = albums
    }

    fun emitFavorites(ids: Set<Int>) {
        favoritesFlow.value = ids
    }
}

private fun albumDto(id: Int, albumId: Int = id) = AlbumDto(
    id = id,
    albumId = albumId,
    title = "title-$id",
    url = "url-$id",
    thumbnailUrl = "thumb-$id",
)

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isEmptyAndNotLoading() = runTest {
        val repository = FakeAlbumRepository()
        val vm = FavoritesViewModel(repository)

        val state = vm.state.first { !it.isLoading }
        assertTrue("Expected no favorites initially", state.albums.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun observeFavorites_onlyIncludesFavoritedTracks() = runTest {
        val repository = FakeAlbumRepository(
            initialAlbums = listOf(albumDto(1), albumDto(2), albumDto(3)),
            initialFavoriteAlbumIds = setOf(1, 3),
        )
        val vm = FavoritesViewModel(repository)

        val state = vm.state.first { it.albums.isNotEmpty() }
        assertEquals(setOf(1, 3), state.albums.map { it.id }.toSet())
        assertTrue(state.albums.all { it.isFavorite })
    }

    @Test
    fun observeFavorites_reactsToNewFavoriteToggle() = runTest {
        val repository = FakeAlbumRepository(
            initialAlbums = listOf(albumDto(1), albumDto(2)),
            initialFavoriteAlbumIds = setOf(1),
        )
        val vm = FavoritesViewModel(repository)
        vm.state.first { it.albums.isNotEmpty() }

        repository.emitFavorites(setOf(1, 2))

        val state = vm.state.first { it.albums.size == 2 }
        assertEquals(setOf(1, 2), state.albums.map { it.id }.toSet())
    }

    @Test
    fun observeFavorites_reactsToUnfavorite_removesFromList() = runTest {
        val repository = FakeAlbumRepository(
            initialAlbums = listOf(albumDto(1), albumDto(2)),
            initialFavoriteAlbumIds = setOf(1, 2),
        )
        val vm = FavoritesViewModel(repository)
        vm.state.first { it.albums.size == 2 }

        repository.emitFavorites(setOf(1))

        val state = vm.state.first { it.albums.size == 1 }
        assertEquals(listOf(1), state.albums.map { it.id })
    }

    @Test
    fun onFavoriteToggle_delegatesToRepository_andUpdatesList() = runTest {
        val repository = FakeAlbumRepository(
            initialAlbums = listOf(albumDto(1), albumDto(2)),
            initialFavoriteAlbumIds = setOf(1),
        )
        val vm = FavoritesViewModel(repository)
        vm.state.first { it.albums.isNotEmpty() }

        vm.onAction(FavoritesAction.OnFavoriteToggle(trackId = 2))

        val state = vm.state.first { it.albums.size == 2 }
        assertEquals(setOf(1, 2), state.favoriteTrackIds)
        assertTrue(state.albums.all { it.isFavorite })
    }

    @Test
    fun onFavoriteToggle_removingLastFavorite_resultsInEmptyList() = runTest {
        val repository = FakeAlbumRepository(
            initialAlbums = listOf(albumDto(1)),
            initialFavoriteAlbumIds = setOf(1),
        )
        val vm = FavoritesViewModel(repository)
        vm.state.first { it.albums.isNotEmpty() }

        vm.onAction(FavoritesAction.OnFavoriteToggle(trackId = 1))

        val state = vm.state.first { it.albums.isEmpty() }
        assertTrue(state.albums.isEmpty())
        assertTrue(state.favoriteTrackIds.isEmpty())
    }

    @Test
    fun onFavoriteToggle_failure_setsError() = runTest {
        val repository = FakeAlbumRepository(
            initialAlbums = listOf(albumDto(1)),
            initialFavoriteAlbumIds = emptySet(),
        )
        val failingRepository = object : AlbumRepository by repository {
            override suspend fun toggleFavorite(trackId: Int): Resource<Unit> =
                Resource.Error("Unable to update favorite state")
        }
        val vm = FavoritesViewModel(failingRepository)
        vm.state.first { !it.isLoading }

        vm.onAction(FavoritesAction.OnFavoriteToggle(trackId = 1))

        val state = vm.state.first { it.error != null }
        assertEquals("Unable to update favorite state", state.error)
    }

    @Test
    fun onLoadFavorites_togglesIsLoading_trueThenFalse() = runTest {
        val repository = FakeAlbumRepository()
        val gate = CompletableDeferred<Unit>()
        repository.onRefresh = {
            gate.await()
            Resource.Success(Unit)
        }
        val vm = FavoritesViewModel(repository)
        vm.state.first { !it.isLoading }

        vm.onAction(FavoritesAction.OnLoadFavorites)
        assertTrue("Expected isLoading to be true while refresh is in flight", vm.state.value.isLoading)

        gate.complete(Unit)
        val state = vm.state.first { !it.isLoading }
        assertFalse(state.isLoading)
    }

    @Test
    fun onRetryClick_failure_setsErrorMessage() = runTest {
        val repository = FakeAlbumRepository()
        repository.onRefresh = { Resource.Error("Unable to load favorites") }
        val vm = FavoritesViewModel(repository)
        vm.state.first { !it.isLoading }

        vm.onAction(FavoritesAction.OnRetryClick)

        val state = vm.state.first { it.error != null }
        assertEquals("Unable to load favorites", state.error)
        assertFalse(state.isLoading)
    }
}
