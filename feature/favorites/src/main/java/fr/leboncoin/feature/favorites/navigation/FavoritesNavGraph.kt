package fr.leboncoin.feature.favorites.navigation

import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import fr.leboncoin.feature.favorites.presentation.FavoritesViewModel
import fr.leboncoin.feature.favorites.ui.FavoritesRoot
import org.koin.androidx.compose.koinViewModel

/**
 * Feature nav graph for the favorites feature. The [FavoritesRoute] destination resolves a
 * [FavoritesViewModel] instance scoped to [FavoritesTabRoute]'s back stack entry.
 */
fun NavGraphBuilder.favoritesGraph(
    navController: NavController,
) {
    navigation<FavoritesTabRoute>(startDestination = FavoritesRoute) {
        composable<FavoritesRoute> { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(FavoritesTabRoute)
            }
            val viewModel: FavoritesViewModel = koinViewModel(viewModelStoreOwner = parentEntry)

            FavoritesRoot(
                viewModel = viewModel,
            )
        }
    }
}
