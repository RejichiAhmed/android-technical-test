package fr.leboncoin.data.repository

import fr.leboncoin.data.network.api.AlbumApiService
import fr.leboncoin.data.network.model.AlbumDto
import fr.leboncoin.data.network.util.Resource
import kotlinx.coroutines.CoroutineScope

class AlbumRepositoryImp(
    private val applicationScope: CoroutineScope,
    private val albumApiService: AlbumApiService,
):AlbumRepository {

    override suspend fun getAllAlbums(
    ): Resource<List<AlbumDto>> {

        return try {
            Resource.Success(
                data = albumApiService.getAlbums()
            )
        } catch(e: Exception) {
            e.printStackTrace()
            Resource.Error(e.message ?: "An unknown error occurred.")
        }

    }
}