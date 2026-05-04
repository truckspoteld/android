package com.eagleye.eld.di

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.GsonBuilder
import com.eagleye.eld.BooleanTypeAdapter
import com.eagleye.eld.BuildConfig
//import com.eagleye.eld.BuildConfig
//import com.eagleye.eld.BuildConfig.API_KEY
import com.eagleye.eld.api.TruckSpotAPI
import com.eagleye.eld.utils.Constants.BASE_URL
import com.eagleye.eld.utils.Constants.ACTION_SESSION_REPLACED
import com.eagleye.eld.utils.PrefRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class NetworkModule {

    @Singleton
    @Provides
    fun provideRetrofit(client: OkHttpClient): Retrofit {

        val loggingInterceptor = HttpLoggingInterceptor()
        // Use HEADERS instead of BODY to prevent OutOfMemoryError on large API responses
        loggingInterceptor.setLevel(if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS else HttpLoggingInterceptor.Level.NONE)
        val clientBuilder = client.newBuilder()
        clientBuilder.addInterceptor(loggingInterceptor)

        return Retrofit.Builder().client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create(gson)).baseUrl(BASE_URL).build()
    }

    val gson = GsonBuilder()
        .registerTypeAdapter(Boolean::class.java, BooleanTypeAdapter())
        .create()

    @Singleton
    @Provides
    fun provideGeIdeaAPI(retrofit: Retrofit):TruckSpotAPI{
        return  retrofit.create(TruckSpotAPI::class.java)

    }

    @Singleton
    @Provides
    fun provideCsvDownloadAPI(retrofit: Retrofit): com.eagleye.eld.repository.CsvDownloadApi {
        return retrofit.create(com.eagleye.eld.repository.CsvDownloadApi::class.java)
    }


    @Singleton
    @Provides
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val token = PrefRepository(context)

        val requestInterceptor = Interceptor { chain ->
            val request = chain.request()
                .newBuilder()
                .addHeader("Authorization", "Bearer ${token.getToken()}")
                .build()
            chain.proceed(request)
        }

        val sessionInterceptor = Interceptor { chain ->
            val response = chain.proceed(chain.request())
            if (response.code == 401) {
                token.setToken("")
                LocalBroadcastManager.getInstance(context)
                    .sendBroadcast(Intent(ACTION_SESSION_REPLACED))
            }
            response
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(requestInterceptor)
            .addInterceptor(sessionInterceptor)
            .build()
    }




}