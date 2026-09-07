package fr.leboncoin.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AlbumEntity::class, FavoriteAlbumEntity::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
}
