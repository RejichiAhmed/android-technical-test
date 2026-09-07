package fr.leboncoin.data.repository

import fr.leboncoin.data.network.api.AlbumApiService
import kotlinx.coroutines.CoroutineScope

class AlbumRepository(
    private val applicationScope: CoroutineScope,
    private val albumApiService: AlbumApiService,
) {
    
    suspend fun getAllAlbums() = albumApiService.getAlbums()
}