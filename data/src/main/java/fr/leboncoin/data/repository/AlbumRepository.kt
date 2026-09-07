package fr.leboncoin.data.repository

import fr.leboncoin.data.network.model.AlbumDto
import fr.leboncoin.data.network.util.Resource
import kotlinx.coroutines.flow.Flow

interface AlbumRepository {
    suspend fun observeAlbums(): Flow<List<AlbumDto>>
    suspend fun observeFavoriteTrackIds(): Flow<Set<Int>>
    suspend fun refreshAlbums(): Resource<Unit>
    suspend fun toggleFavorite(trackId: Int): Resource<Unit>
}