package fr.leboncoin.androidrecruitmenttestapp

import android.app.Application
import fr.leboncoin.androidrecruitmenttestapp.di.AppDependenciesProvider
import fr.leboncoin.data.di.DataModule
import fr.leboncoin.feature.albums.di.AlbumsModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class PhotoApp : Application() {


    val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@PhotoApp)
            modules(DataModule, AppDependenciesProvider, AlbumsModule)
        }
    }
}