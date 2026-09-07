package fr.leboncoin.feature.albums.navigation

import kotlinx.serialization.Serializable

/**
 * Parent graph route wrapping [AlbumsRoute] and [AlbumDetailRoute] so both
 * destinations can share a single [fr.leboncoin.feature.albums.presentation.AlbumsViewModel]
 * instance scoped to this graph's back stack entry.
 */
@Serializable
object AlbumsGraphRoute

@Serializable
object AlbumsRoute

@Serializable
data class AlbumDetailRoute(
    val albumId: Int,
)
