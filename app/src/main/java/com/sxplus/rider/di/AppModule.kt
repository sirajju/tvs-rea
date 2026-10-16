package com.sxplus.rider.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.sxplus.rider.BuildConfig
import com.sxplus.rider.data.remote.AuthInterceptor
import com.sxplus.rider.data.remote.GraphQlApi
import com.sxplus.rider.data.remote.IqubeApi
import com.sxplus.rider.data.remote.IqubeAuthInterceptor
import com.sxplus.rider.data.remote.TvsApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun moshi(): Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private fun logging() = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
        else HttpLoggingInterceptor.Level.NONE
    }

    private fun baseClient() = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)

    @Provides @Singleton
    fun tvsApi(auth: AuthInterceptor, moshi: Moshi): TvsApi {
        val client = baseClient().addInterceptor(auth).addInterceptor(logging()).build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL_ICE)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TvsApi::class.java)
    }

    @Provides @Singleton
    fun iqubeApi(auth: IqubeAuthInterceptor, moshi: Moshi): IqubeApi {
        val client = baseClient().addInterceptor(auth).addInterceptor(logging()).build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL_EV)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(IqubeApi::class.java)
    }

    @Provides @Singleton
    fun graphQlApi(moshi: Moshi): GraphQlApi {
        val client = baseClient().addInterceptor(logging()).build()
        return Retrofit.Builder()
            .baseUrl("https://p360.tvsmotor.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GraphQlApi::class.java)
    }
}
