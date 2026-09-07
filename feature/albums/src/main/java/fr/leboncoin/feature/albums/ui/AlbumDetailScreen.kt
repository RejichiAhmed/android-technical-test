package fr.leboncoin.feature.albums.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adevinta.spark.components.buttons.ButtonFilled
import fr.leboncoin.data.network.model.AlbumDto

@Composable
fun AlbumDetailScreen(
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
            ButtonFilled(
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
        ButtonFilled(
            onClick = onBack,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Back")
        }
    }
}
