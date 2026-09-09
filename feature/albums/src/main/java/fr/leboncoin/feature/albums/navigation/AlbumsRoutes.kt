package fr.leboncoin.feature.albums.navigation

import kotlinx.serialization.Serializable

/**
 * Parent tab route wrapping [AlbumsGraphRoute] to allow tab-level navigation
 * in the app when multiple tabs are present (e.g., Albums and Favorites tabs).
 */
@Serializable
object AlbumsTabRoute

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
