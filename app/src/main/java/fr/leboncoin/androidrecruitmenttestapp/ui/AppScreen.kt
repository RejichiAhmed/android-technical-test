package fr.leboncoin.androidrecruitmenttestapp.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.adevinta.spark.SparkTheme
import fr.leboncoin.androidrecruitmenttestapp.utils.AnalyticsHelper
import fr.leboncoin.androidrecruitmenttestapp.viewmodel.AppScreenViewModel
import fr.leboncoin.feature.albums.navigation.AlbumsGraphRoute
import fr.leboncoin.feature.albums.navigation.albumsGraph
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppScreen(
    analyticsHelper: AnalyticsHelper,
    viewModel: AppScreenViewModel = koinViewModel(),
) {
    val navController = rememberNavController()

    SparkTheme {
        NavHost(
            navController = navController,
            startDestination = AlbumsGraphRoute,
        ) {
            albumsGraph(
                navController = navController,
                onAlbumSelected = { albumId -> analyticsHelper.trackSelection(albumId.toString()) },
            )
        }
    }
}
