package fr.leboncoin.feature.albums

import fr.leboncoin.data.network.model.AlbumDto
import fr.leboncoin.data.network.util.Resource
import fr.leboncoin.data.repository.AlbumRepository
import fr.leboncoin.feature.albums.presentation.AlbumsAction
import fr.leboncoin.feature.albums.presentation.AlbumsEvent
import fr.leboncoin.feature.albums.presentation.AlbumsViewModel
import fr.leboncoin.feature.albums.presentation.toAlbumUi
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
class AlbumsViewModelTest {

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
    fun onLoadAlbums_populatesStateFromRepositoryFlow() = runTest {
        val repository = FakeAlbumRepository(initialFavoriteAlbumIds = setOf(2))
        val vm = AlbumsViewModel(repository)
        assertTrue("Expected empty albums before any emission", vm.state.value.albums.isEmpty())

        vm.onAction(AlbumsAction.OnLoadAlbums)
        repository.emit(listOf(albumDto(1), albumDto(2)))

        val state = vm.state.first { it.albums.isNotEmpty() }
        assertEquals(
            listOf(
                albumDto(1).toAlbumUi(isFavorite = false),
                albumDto(2).toAlbumUi(isFavorite = true),
            ),
            state.albums,
        )
    }

    @Test
    fun onAlbumClick_emitsNavigateToDetailEvent() = runTest {
        val repository = FakeAlbumRepository(listOf(albumDto(1)))
        val vm = AlbumsViewModel(repository)

        vm.onAction(AlbumsAction.OnAlbumClick(albumId = 1))

        val event = vm.events.first()
        assertEquals(AlbumsEvent.NavigateToDetail(albumId = 1), event)
    }

    @Test
    fun onFavoriteToggle_updatesFavoriteState() = runTest {
        val repository = FakeAlbumRepository(
            initialAlbums = listOf(albumDto(1), albumDto(2)),
            initialFavoriteAlbumIds = setOf(1),
        )
        val vm = AlbumsViewModel(repository)
        vm.state.first { it.albums.isNotEmpty() }

        vm.onAction(AlbumsAction.OnFavoriteToggle(trackId = 2))

        val state = vm.state.value
        assertTrue(state.albums.first { it.id == 2 }.isFavorite)
    }

    @Test
    fun findAlbum_returnsMatchingAlbum_fromSharedState() = runTest {
        val repository = FakeAlbumRepository(
            listOf(
                albumDto(1).copy(title = "First"),
                albumDto(2).copy(title = "Second"),
            )
        )
        val vm = AlbumsViewModel(repository)

        val state = vm.state.first { it.albums.isNotEmpty() }

        // Verify album can be found in albums list using local computation
        assertEquals("Second", state.albums.firstOrNull { it.id == 2 }?.title)
        assertNotNull(state.albums.firstOrNull { it.id == 2 })
        assertNull(state.albums.firstOrNull { it.id == 99 })
    }

    @Test
    fun refreshAlbums_togglesIsLoading_trueThenFalse() = runTest {
        val repository = FakeAlbumRepository()
        val gate = CompletableDeferred<Unit>()
        repository.onRefresh = {
            gate.await()
            Resource.Success(Unit)
        }
        val vm = AlbumsViewModel(repository)

        vm.onAction(AlbumsAction.OnLoadAlbums)
        assertTrue("Expected isLoading to be true while refresh is in flight", vm.state.value.isLoading)

        gate.complete(Unit)
        val state = vm.state.first { !it.isLoading }
        assertFalse(state.isLoading)
    }

    @Test
    fun refreshAlbums_failure_setsErrorButRetainsCachedAlbums() = runTest {
        val cached = listOf(albumDto(1), albumDto(2))
        val repository = FakeAlbumRepository(initialAlbums = cached)
        repository.onRefresh = { Resource.Error("Unable to refresh albums.") }
        val vm = AlbumsViewModel(repository)

        vm.onAction(AlbumsAction.OnRetryClick)

        val state = vm.state.value
        assertEquals("Unable to refresh albums.", state.error)
        assertFalse(state.isLoading)
        assertEquals(cached.map { it.toAlbumUi() }, state.albums)
    }

    @Test
    fun albumGroupCards_computed_onAlbumsUpdate() = runTest {
        val albums = listOf(
            albumDto(1, albumId = 1),
            albumDto(2, albumId = 2),
            albumDto(3, albumId = 1),
        )
        val repository = FakeAlbumRepository(initialAlbums = albums)
        val vm = AlbumsViewModel(repository)
        
        val state = vm.state.first { it.albums.isNotEmpty() }
        
        // Verify albumGroupCards are computed in the state
        assertEquals(2, state.albumGroupCards.size)
        assertEquals(1, state.albumGroupCards[0].albumId)
        assertEquals(2, state.albumGroupCards[1].albumId)
        assertEquals(2, state.albumGroupCards[0].trackCount)
        assertEquals(1, state.albumGroupCards[1].trackCount)
    }
}
