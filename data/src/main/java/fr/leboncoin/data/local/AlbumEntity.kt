package fr.leboncoin.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.leboncoin.data.network.model.AlbumDto

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val id: Int,
    val albumId: Int,
    val title: String,
    val url: String,
    val thumbnailUrl: String,
)

fun AlbumDto.toEntity(): AlbumEntity = AlbumEntity(
    id = id,
    albumId = albumId,
    title = title,
    url = url,
    thumbnailUrl = thumbnailUrl,
)

fun AlbumEntity.toDto(): AlbumDto = AlbumDto(
    id = id,
    albumId = albumId,
    title = title,
    url = url,
    thumbnailUrl = thumbnailUrl,
)
