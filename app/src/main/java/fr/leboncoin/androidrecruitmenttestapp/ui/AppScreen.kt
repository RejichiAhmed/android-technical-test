package fr.leboncoin.androidrecruitmenttestapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.adevinta.spark.SparkTheme
import fr.leboncoin.androidrecruitmenttestapp.utils.AnalyticsHelper
import fr.leboncoin.androidrecruitmenttestapp.viewmodel.AppScreenViewModel
import fr.leboncoin.feature.albums.navigation.AlbumDetailRoute
import fr.leboncoin.feature.albums.navigation.AlbumsRoute
import fr.leboncoin.feature.albums.ui.AlbumDetailScreen
import fr.leboncoin.feature.albums.ui.AlbumsScreen
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import android.app.Activity

@Composable
fun AppScreen(
    analyticsHelper: AnalyticsHelper,
    viewModel: AppScreenViewModel = koinViewModel(),
) {
    val navController = rememberNavController()

    // Get AlbumsViewModel from the feature module (via Koin)
    val albumsViewModel: fr.leboncoin.feature.albums.viewmodel.AlbumsViewModel = koinViewModel()
    val albums by albumsViewModel.albums.collectAsStateWithLifecycle(initialValue = emptyList())

    SparkTheme {
        NavHost(
            navController = navController,
            startDestination = AlbumsRoute,
        ) {
            composable<AlbumsRoute> {
                AlbumsScreen(
                    viewModel = albumsViewModel,
                    onItemSelected = {
                        analyticsHelper.trackSelection(it.id.toString())
                        navController.navigate(AlbumDetailRoute(it.id))
                    }
                )
            }

            composable<AlbumDetailRoute> { backStackEntry ->
                val route: AlbumDetailRoute = backStackEntry.toRoute()
                AlbumDetailScreen(
                    albumId = route.albumId,
                    albums = albums,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
