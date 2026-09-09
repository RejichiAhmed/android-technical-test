package fr.leboncoin.feature.favorites.di

import fr.leboncoin.feature.favorites.presentation.FavoritesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val FavoritesModule = module {
    viewModel { FavoritesViewModel(get()) }
}
