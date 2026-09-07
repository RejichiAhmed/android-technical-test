package fr.leboncoin.feature.albums

import fr.leboncoin.data.network.api.AlbumApiService
import fr.leboncoin.data.network.model.AlbumDto
import fr.leboncoin.data.repository.AlbumRepository
import fr.leboncoin.feature.albums.viewmodel.AlbumsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumsViewModelTest {

    @Test
    fun loadsAlbums_emitsNonEmptyList() = runBlocking {
        val fakeService = object : AlbumApiService {
            override suspend fun getAlbums(): List<AlbumDto> = listOf(
                AlbumDto(id = 1, albumId = 1, title = "t", url = "u", thumbnailUrl = "tu")
            )
        }
        val scope = CoroutineScope(SupervisorJob())
        val repository = AlbumRepository(scope, fakeService)
        val vm = AlbumsViewModel(repository)

        vm.loadAlbums()

        val albums = vm.albums.first()
        assertTrue("Expected albums to be loaded", albums.isNotEmpty())
    }
}

