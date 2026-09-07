package fr.leboncoin.data.repository

import fr.leboncoin.data.local.AlbumDao
import fr.leboncoin.data.local.AlbumEntity
import fr.leboncoin.data.local.FavoriteAlbumEntity
import fr.leboncoin.data.local.toDto
import fr.leboncoin.data.network.api.AlbumApiService
import fr.leboncoin.data.network.model.AlbumDto
import fr.leboncoin.data.network.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * In-memory fake of [AlbumDao] backed by a [MutableStateFlow], so [getAll] behaves
 * like Room's reactive query without needing an actual (Robolectric/instrumented) database.
 */
private class FakeAlbumDao(
    seed: List<AlbumEntity> = emptyList(),
    favoriteIds: Set<Int> = emptySet(),
) : AlbumDao {

    private val entities = MutableStateFlow(seed)
    private val favorites = MutableStateFlow(favoriteIds)

    override fun getAll(): Flow<List<AlbumEntity>> = entities

    override suspend fun getById(id: Int): AlbumEntity? = entities.value.firstOrNull { it.id == id }

    override suspend fun upsertAll(albums: List<AlbumEntity>) {
        val current = entities.value.associateBy { it.id }.toMutableMap()
        albums.forEach { current[it.id] = it }
        entities.value = current.values.toList()
    }

    override suspend fun clearAll() {
        entities.value = emptyList()
    }

    override fun observeFavoriteAlbumIds(): Flow<List<Int>> = favorites.map { it.toList() }

    override suspend fun insertFavorite(favorite: FavoriteAlbumEntity) {
        favorites.value = favorites.value + favorite.albumId
    }

    override suspend fun removeFavorite(albumId: Int) {
        favorites.value = favorites.value - albumId
    }

    override suspend fun isFavorite(albumId: Int): Boolean = favorites.value.contains(albumId)
}

private fun fakeApiService(
    result: () -> List<AlbumDto> = { emptyList() },
): AlbumApiService = object : AlbumApiService {
    override suspend fun getAlbums(): List<AlbumDto> = result()
}

private fun albumEntity(id: Int, albumId: Int = id) = AlbumEntity(
    id = id,
    albumId = albumId,
    title = "title-$id",
    url = "url-$id",
    thumbnailUrl = "thumb-$id",
)

private fun albumDto(id: Int, albumId: Int = id) = AlbumDto(
    id = id,
    albumId = albumId,
    title = "title-$id",
    url = "url-$id",
    thumbnailUrl = "thumb-$id",
)

class OfflineFirstAlbumRepositoryTest {

    @Test
    fun observeAlbums_emitsWhateverIsInTheDao() = runTest {
        val dao = FakeAlbumDao(seed = listOf(albumEntity(1), albumEntity(2)))
        val repository = AlbumRepositoryImp(dao, fakeApiService())

        val result = repository.observeAlbums().first()

        assertEquals(listOf(albumEntity(1).toDto(), albumEntity(2).toDto()), result)
    }

    @Test
    fun refreshAlbums_success_upsertsIntoDao() = runTest {
        val dao = FakeAlbumDao()
        val remote = listOf(albumDto(1), albumDto(2))
        val repository = AlbumRepositoryImp(dao, fakeApiService { remote })

        val result = repository.refreshAlbums()

        assertTrue("Expected Resource.Success", result is Resource.Success)
        assertEquals(remote.toSet(), repository.observeAlbums().first().toSet())
    }

    @Test
    fun refreshAlbums_failure_returnsErrorAndDoesNotWipeCache() = runTest {
        val cached = listOf(albumEntity(1), albumEntity(2))
        val dao = FakeAlbumDao(seed = cached)
        val failingApi = object : AlbumApiService {
            override suspend fun getAlbums(): List<AlbumDto> = throw IllegalStateException("boom")
        }
        val repository = AlbumRepositoryImp(dao, failingApi)

        val result = repository.refreshAlbums()

        assertTrue("Expected Resource.Error", result is Resource.Error)
        assertNotNull((result as Resource.Error).message)
        assertEquals(cached.map { it.toDto() }, repository.observeAlbums().first())
    }

    @Test
    fun toggleFavorite_updatesAndRestoresFavoriteIds() = runTest {
        val dao = FakeAlbumDao(favoriteIds = setOf(1))
        val repository = AlbumRepositoryImp(dao, fakeApiService())

        val result = repository.toggleFavorite(2)

        assertTrue(result is Resource.Success)
        assertEquals(setOf(1, 2), repository.observeFavoriteAlbumIds().first())

        val toggledBack = repository.toggleFavorite(1)
        assertTrue(toggledBack is Resource.Success)
        assertEquals(setOf(2), repository.observeFavoriteAlbumIds().first())
    }
}
