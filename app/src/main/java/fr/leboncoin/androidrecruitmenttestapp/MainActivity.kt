package fr.leboncoin.androidrecruitmenttestapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.adevinta.spark.SparkTheme
import fr.leboncoin.androidrecruitmenttestapp.navigation.AlbumDetailRoute
import fr.leboncoin.androidrecruitmenttestapp.navigation.AlbumsRoute
import fr.leboncoin.androidrecruitmenttestapp.ui.AlbumsScreen
import fr.leboncoin.androidrecruitmenttestapp.utils.AnalyticsHelper
import fr.leboncoin.data.network.model.AlbumDto
import fr.leboncoin.data.repository.AlbumRepository
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val repository: AlbumRepository by inject()
    private val analyticsHelper: AnalyticsHelper by inject()

    private val viewModel: AlbumsViewModel by viewModels {
        AlbumsViewModel.Factory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        analyticsHelper.initialize(this)

        setContent {
            val navController = rememberNavController()
            val albums by viewModel.albums.collectAsStateWithLifecycle(initialValue = emptyList())

            SparkTheme {
                NavHost(
                    navController = navController,
                    startDestination = AlbumsRoute,
                ) {
                    composable<AlbumsRoute> {
                        AlbumsScreen(
                            viewModel = viewModel,
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
    }
}

@Composable
private fun AlbumDetailScreen(
    albumId: Int,
    albums: List<AlbumDto>,
    onBack: () -> Unit,
) {
    val album = albums.firstOrNull { it.id == albumId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        if (album == null) {
            Text("Album not found")
            Button(
                onClick = onBack,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Back")
            }
            return
        }

        Text(text = album.title)
        Text(text = "Album #${album.albumId}")
        Text(text = "Track #${album.id}")
        Button(
            onClick = onBack,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Back")
        }
    }
}
