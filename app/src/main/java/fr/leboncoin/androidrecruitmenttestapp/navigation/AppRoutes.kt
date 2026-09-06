package fr.leboncoin.androidrecruitmenttestapp.navigation

import kotlinx.serialization.Serializable

@Serializable
object AlbumsRoute

@Serializable
data class AlbumDetailRoute(
    val albumId: Int,
)
