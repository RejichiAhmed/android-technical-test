package fr.leboncoin.feature.albums.di

import fr.leboncoin.feature.albums.presentation.AlbumsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val AlbumsModule = module {
    viewModel { AlbumsViewModel(get()) }
}
