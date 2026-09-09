package fr.leboncoin.androidrecruitmenttestapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.leboncoin.feature.albums.navigation.AlbumsTabRoute
import fr.leboncoin.feature.favorites.navigation.FavoritesTabRoute

@Composable
fun BottomNavigationBar(
    selectedTab: Any,
    onTabSelected: (Any) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, "Albums") },
            label = { Text("Albums") },
            selected = selectedTab is AlbumsTabRoute || selectedTab == AlbumsTabRoute,
            onClick = { onTabSelected(AlbumsTabRoute) },
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.Favorite, "Favorites") },
            label = { Text("Favorites") },
            selected = selectedTab is FavoritesTabRoute || selectedTab == FavoritesTabRoute,
            onClick = { onTabSelected(FavoritesTabRoute) },
        )
    }
}
