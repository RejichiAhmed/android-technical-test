package fr.leboncoin.androidrecruitmenttestapp.di

import fr.leboncoin.androidrecruitmenttestapp.PhotoApp
import fr.leboncoin.androidrecruitmenttestapp.utils.AnalyticsHelper
import kotlinx.coroutines.CoroutineScope
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val AppDependenciesProvider = module {
    single { AnalyticsHelper() }

    single<CoroutineScope> {
        (androidApplication() as PhotoApp).applicationScope
    }
}