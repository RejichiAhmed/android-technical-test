package fr.leboncoin.feature.favorites.navigation

import kotlinx.serialization.Serializable

/**
 * Parent graph route wrapping [FavoritesRoute] so the favorites screen
 * can use a dedicated [fr.leboncoin.feature.favorites.presentation.FavoritesViewModel]
 * instance scoped to this graph's back stack entry.
 */
@Serializable
object FavoritesTabRoute

@Serializable
object FavoritesRoute
