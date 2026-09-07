package fr.leboncoin.data.repository

import fr.leboncoin.data.local.AlbumDao
import fr.leboncoin.data.local.toDto
import fr.leboncoin.data.local.toEntity
import fr.leboncoin.data.network.api.AlbumApiService
import fr.leboncoin.data.network.model.AlbumDto
import fr.leboncoin.data.network.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlbumRepositoryImp(
    private val dao: AlbumDao,
    private val api: AlbumApiService,
) : AlbumRepository {

    override suspend fun observeAlbums(): Flow<List<AlbumDto>> =
        dao.getAll().map { entities -> entities.map { it.toDto() } }

    override suspend fun refreshAlbums(): Resource<Unit> = try {
        val remote = api.getAlbums()
        dao.upsertAll(remote.map { it.toEntity() })
        Resource.Success(Unit)
    } catch (e: Exception) {
        e.printStackTrace()
        Resource.Error(e.message ?: "Unable to refresh albums.")
    }
}
