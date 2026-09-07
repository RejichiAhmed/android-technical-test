package fr.leboncoin.feature.albums

import fr.leboncoin.data.network.model.AlbumDto
import fr.leboncoin.data.network.util.Resource
import fr.leboncoin.data.repository.AlbumRepository
import fr.leboncoin.feature.albums.presentation.AlbumsAction
import fr.leboncoin.feature.albums.presentation.AlbumsEvent
import fr.leboncoin.feature.albums.presentation.AlbumsViewModel
import fr.leboncoin.feature.albums.presentation.findAlbum
import fr.leboncoin.feature.albums.presentation.toAlbumUi
import fr.leboncoin.feature.albums.presentation.visibleAlbums
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

/**
 * In-memory fake of [AlbumRepository]: [observeAlbums] is backed by a [MutableStateFlow]
 * (seed/[emit] to simulate whatever Room currently holds), and [refreshAlbums] behavior
 * is configurable via [onRefresh] so tests can simulate success/error/slow network calls.
 */
private class FakeAlbumRepository(
    initialAlbums: List<AlbumDto> = emptyList(),
) : AlbumRepository {

    private val albumsFlow = MutableStateFlow(initialAlbums)
    var onRefresh: suspend () -> Resource<Unit> = { Resource.Success(Unit) }

    override fun observeAlbums(): Flow<List<AlbumDto>> = albumsFlow

    override suspend fun refreshAlbums(): Resource<Unit> = onRefresh()

    fun emit(albums: List<AlbumDto>) {
        albumsFlow.value = albums
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
        val repository = FakeAlbumRepository()
        val vm = AlbumsViewModel(repository)
        assertTrue("Expected empty albums before any emission", vm.state.value.albums.isEmpty())

        vm.onAction(AlbumsAction.OnLoadAlbums)
        repository.emit(listOf(albumDto(1), albumDto(2)))

        val state = vm.state.first { it.albums.isNotEmpty() }
        assertEquals(
            listOf(albumDto(1).toAlbumUi(), albumDto(2).toAlbumUi()),
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
    fun findAlbum_returnsMatchingAlbum_fromSharedState() = runTest {
        val repository = FakeAlbumRepository(
            listOf(
                albumDto(1).copy(title = "First"),
                albumDto(2).copy(title = "Second"),
            )
        )
        val vm = AlbumsViewModel(repository)

        val state = vm.state.first { it.albums.isNotEmpty() }

        assertEquals("Second", state.findAlbum(albumId = 2)?.title)
        assertNotNull(state.findAlbum(albumId = 2))
        assertNull(state.findAlbum(albumId = 99))
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
    fun onCategorySelected_filtersVisibleAlbums() = runTest {
        val albums = listOf(
            albumDto(1, albumId = 1),
            albumDto(2, albumId = 2),
            albumDto(3, albumId = 1),
        )
        val repository = FakeAlbumRepository(initialAlbums = albums)
        val vm = AlbumsViewModel(repository)
        vm.state.first { it.albums.isNotEmpty() }

        vm.onAction(AlbumsAction.OnCategorySelected(albumId = 1))
        var state = vm.state.value
        assertEquals(1, state.selectedCategory)
        assertEquals(listOf(1, 3), state.visibleAlbums.map { it.id })

        vm.onAction(AlbumsAction.OnCategorySelected(albumId = null))
        state = vm.state.value
        assertNull(state.selectedCategory)
        assertEquals(3, state.visibleAlbums.size)
    }

    @Test
    fun availableCategories_isDistinctSortedAlbumIds() = runTest {
        val albums = listOf(
            albumDto(1, albumId = 3),
            albumDto(2, albumId = 1),
            albumDto(3, albumId = 3),
            albumDto(4, albumId = 2),
        )
        val repository = FakeAlbumRepository(initialAlbums = albums)
        val vm = AlbumsViewModel(repository)

        val state = vm.state.first { it.albums.isNotEmpty() }

        assertEquals(listOf(1, 2, 3), state.availableCategories)
    }
}
