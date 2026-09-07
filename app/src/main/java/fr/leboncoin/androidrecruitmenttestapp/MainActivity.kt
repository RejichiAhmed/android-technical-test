package fr.leboncoin.androidrecruitmenttestapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import fr.leboncoin.androidrecruitmenttestapp.ui.AppScreen
import fr.leboncoin.androidrecruitmenttestapp.utils.AnalyticsHelper
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val analyticsHelper: AnalyticsHelper by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        analyticsHelper.initialize(this)

        setContent {
            AppScreen(analyticsHelper = analyticsHelper)
        }
    }
}

