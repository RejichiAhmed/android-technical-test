package fr.leboncoin.feature.albums.navigation

import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import fr.leboncoin.feature.albums.presentation.AlbumsViewModel
import fr.leboncoin.feature.albums.presentation.details.AlbumDetailRoot
import fr.leboncoin.feature.albums.presentation.liste.AlbumsListRoot
import org.koin.androidx.compose.koinViewModel

/**
 * Feature nav graph for the albums feature. Both destinations resolve the SAME
 * [AlbumsViewModel] instance — scoped to [AlbumsGraphRoute]'s back stack entry — so
 * the album list loaded on [AlbumsRoute] is directly available to [AlbumDetailRoute]
 * without a second network fetch.
 *
 * [onAlbumSelected] is a cross-feature callback (e.g. analytics tracking) invoked by
 * `:app` whenever an album is selected from the list.
 */
fun NavGraphBuilder.albumsGraph(
    navController: NavController,
    onAlbumSelected: (Int) -> Unit,
) {
    navigation<AlbumsGraphRoute>(startDestination = AlbumsRoute) {
        composable<AlbumsRoute> { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(AlbumsGraphRoute)
            }
            val viewModel: AlbumsViewModel = koinViewModel(viewModelStoreOwner = parentEntry)

            AlbumsListRoot(
                viewModel = viewModel,
                onNavigateToDetail = { albumId ->
                    onAlbumSelected(albumId)
                    navController.navigate(AlbumDetailRoute(albumId))
                },
            )
        }

        composable<AlbumDetailRoute> { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(AlbumsGraphRoute)
            }
            val viewModel: AlbumsViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
            val route: AlbumDetailRoute = backStackEntry.toRoute()

            AlbumDetailRoot(
                albumId = route.albumId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
