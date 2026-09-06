package fr.leboncoin.androidrecruitmenttestapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.adevinta.spark.SparkTheme
import com.adevinta.spark.components.image.Illustration
import fr.leboncoin.androidrecruitmenttestapp.utils.AnalyticsHelper
import org.koin.android.ext.android.inject

class DetailsActivity : ComponentActivity() {

    private val analyticsHelper: AnalyticsHelper by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        analyticsHelper.initialize(this)
        analyticsHelper.trackScreenView("Details")

        setContent {
            SparkTheme {
                Illustration(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(id = R.drawable.work_in_progress),
                    contentDescription = null,
                    contentScale = ContentScale.Inside,
                )
            }
        }
    }
}

