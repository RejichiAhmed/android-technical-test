package fr.leboncoin.feature.albums

import fr.leboncoin.data.network.api.AlbumApiService
import fr.leboncoin.data.network.model.AlbumDto
import fr.leboncoin.data.repository.AlbumRepositoryImp
import fr.leboncoin.feature.albums.presentation.AlbumsAction
import fr.leboncoin.feature.albums.presentation.AlbumsEvent
import fr.leboncoin.feature.albums.presentation.AlbumsViewModel
import fr.leboncoin.feature.albums.presentation.findAlbum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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

    private fun fakeRepository(albums: List<AlbumDto>): AlbumRepositoryImp {
        val fakeService = object : AlbumApiService {
            override suspend fun getAlbums(): List<AlbumDto> = albums
        }
        val scope = CoroutineScope(SupervisorJob())
        return AlbumRepositoryImp(scope, fakeService)
    }

    @Test
    fun onLoadAlbums_populatesStateWithNonEmptyList() = runTest {
        val repository = fakeRepository(
            listOf(AlbumDto(id = 1, albumId = 1, title = "t", url = "u", thumbnailUrl = "tu"))
        )
        val vm = AlbumsViewModel(repository)

        vm.onAction(AlbumsAction.OnLoadAlbums)

        val state = vm.state.first { !it.isLoading && it.albums.isNotEmpty() }
        assertTrue("Expected albums to be loaded", state.albums.isNotEmpty())
    }

    @Test
    fun onAlbumClick_emitsNavigateToDetailEvent() = runTest {
        val repository = fakeRepository(
            listOf(AlbumDto(id = 1, albumId = 1, title = "t", url = "u", thumbnailUrl = "tu"))
        )
        val vm = AlbumsViewModel(repository)

        vm.onAction(AlbumsAction.OnAlbumClick(albumId = 1))

        val event = vm.events.first()
        assertEquals(AlbumsEvent.NavigateToDetail(albumId = 1), event)
    }

    @Test
    fun findAlbum_returnsMatchingAlbum_fromSharedState() = runTest {
        val repository = fakeRepository(
            listOf(
                AlbumDto(id = 1, albumId = 1, title = "First", url = "u1", thumbnailUrl = "tu1"),
                AlbumDto(id = 2, albumId = 1, title = "Second", url = "u2", thumbnailUrl = "tu2"),
            )
        )
        val vm = AlbumsViewModel(repository)

        vm.onAction(AlbumsAction.OnLoadAlbums)
        val state = vm.state.first { it.albums.isNotEmpty() }

        assertEquals("Second", state.findAlbum(albumId = 2)?.title)
        assertNotNull(state.findAlbum(albumId = 2))
        assertNull(state.findAlbum(albumId = 99))
    }
}
