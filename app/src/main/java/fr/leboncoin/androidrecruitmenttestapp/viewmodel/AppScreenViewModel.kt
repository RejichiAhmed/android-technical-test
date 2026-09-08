package fr.leboncoin.androidrecruitmenttestapp.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import fr.leboncoin.androidrecruitmenttestapp.utils.AnalyticsHelper
import fr.leboncoin.feature.albums.navigation.AlbumsTabRoute

class AppScreenViewModel(
    private val analyticsHelper: AnalyticsHelper,
) : ViewModel() {

    private val _selectedTab = MutableStateFlow<Any>(AlbumsTabRoute)
    val selectedTab = _selectedTab.asStateFlow()

    fun onTabSelected(destination: Any) {
        _selectedTab.update { destination }
        // Log analytics event
        analyticsHelper.trackTabSelection(destination::class.simpleName ?: "Unknown")
    }
}
