package fr.leboncoin.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_albums")
data class FavoriteAlbumEntity(
    @PrimaryKey val trackId: Int,
)
