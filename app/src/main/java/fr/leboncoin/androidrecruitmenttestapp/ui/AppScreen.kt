package fr.leboncoin.androidrecruitmenttestapp.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.adevinta.spark.SparkTheme
import fr.leboncoin.androidrecruitmenttestapp.ui.components.BottomNavigationBar
import fr.leboncoin.androidrecruitmenttestapp.utils.AnalyticsHelper
import fr.leboncoin.androidrecruitmenttestapp.viewmodel.AppScreenViewModel
import fr.leboncoin.feature.albums.navigation.AlbumsGraphRoute
import fr.leboncoin.feature.albums.navigation.AlbumsTabRoute
import fr.leboncoin.feature.albums.navigation.albumsGraph
import fr.leboncoin.feature.favorites.navigation.FavoritesTabRoute
import fr.leboncoin.feature.favorites.navigation.favoritesGraph
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppScreen(
    analyticsHelper: AnalyticsHelper,
    viewModel: AppScreenViewModel = koinViewModel(),
) {
    val navController = rememberNavController()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()

    SparkTheme {
        Scaffold(
            bottomBar = {
                BottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = { destination ->
                        viewModel.onTabSelected(destination)
                        navController.navigate(destination) {
                            // Clear the back stack when switching tabs
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AlbumsTabRoute,
                modifier = Modifier.padding(innerPadding),
            ) {
                // Albums tab with nested graph
                navigation<AlbumsTabRoute>(startDestination = AlbumsGraphRoute) {
                    albumsGraph(
                        navController = navController,
                        onAlbumSelected = { albumId -> analyticsHelper.trackSelection(albumId.toString()) },
                    )
                }

                // Favorites tab
                favoritesGraph(
                    navController = navController,
                )
            }
        }
    }
}
