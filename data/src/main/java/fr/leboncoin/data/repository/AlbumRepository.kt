package fr.leboncoin.data.repository

import fr.leboncoin.data.network.model.AlbumDto
import fr.leboncoin.data.network.util.Resource


interface AlbumRepository {
    suspend fun getAllAlbums(): Resource<List<AlbumDto>>
}