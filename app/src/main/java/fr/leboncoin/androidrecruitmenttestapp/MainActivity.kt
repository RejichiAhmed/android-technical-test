package fr.leboncoin.androidrecruitmenttestapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.adevinta.spark.SparkTheme
import fr.leboncoin.androidrecruitmenttestapp.ui.AlbumsScreen
import fr.leboncoin.androidrecruitmenttestapp.utils.AnalyticsHelper
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
            SparkTheme {
                AlbumsScreen(
                    viewModel = viewModel,
                    onItemSelected = {
                        analyticsHelper.trackSelection(it.id.toString())
                        startActivity(Intent(this, DetailsActivity::class.java))
                    }
                )
            }
        }
    }
}