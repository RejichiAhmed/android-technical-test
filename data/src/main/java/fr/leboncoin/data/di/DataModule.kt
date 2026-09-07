package fr.leboncoin.data.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import fr.leboncoin.data.network.api.AlbumApiService
import fr.leboncoin.data.repository.AlbumRepositoryImp
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit

val DataModule = module {

    single {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    single {
        OkHttpClient.Builder()
            .addInterceptor(get<HttpLoggingInterceptor>())
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl(AlbumApiService.BASE_URL)
            .client(get<OkHttpClient>())
            .addConverterFactory(get<Json>().asConverterFactory("application/json".toMediaType()))
            .build()
    }

    single {
        get<Retrofit>().create(AlbumApiService::class.java)
    }

    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }

    single {
        AlbumRepositoryImp(get(), get<AlbumApiService>())
    }
}
