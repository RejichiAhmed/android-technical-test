package fr.leboncoin.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {

    @Query("SELECT * FROM albums")
    fun getAll(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE id = :id")
    suspend fun getById(id: Int): AlbumEntity?

    @Upsert
    suspend fun upsertAll(albums: List<AlbumEntity>)

    @Query("DELETE FROM albums")
    suspend fun clearAll()

    @Query("SELECT albumId FROM favorite_albums")
    fun observeFavoriteAlbumIds(): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteAlbumEntity)

    @Query("DELETE FROM favorite_albums WHERE albumId = :albumId")
    suspend fun removeFavorite(albumId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_albums WHERE albumId = :albumId)")
    suspend fun isFavorite(albumId: Int): Boolean
}
