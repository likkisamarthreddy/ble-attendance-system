package com.example.practice.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    // Production Railway Backend
    private const val BASE_URL = "https://backend-production-7f08.up.railway.app/api/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Bypass-Tunnel-Reminder", "true")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val retrofit: Retrofit by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val authApi: AuthApi by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        println("Initializing AuthApi")
        retrofit.create(AuthApi::class.java)
    }

    val studentApi: StudentApi by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        println("Initializing StudentApi")
        retrofit.create(StudentApi::class.java)
    }

    val professorApi: ProfessorApi by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        println("Initializing ProfessorApi")
        retrofit.create(ProfessorApi::class.java)
    }

    val adminApi: AdminApi by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        println("Initializing AdminApi")
        retrofit.create(AdminApi::class.java)
    }
}