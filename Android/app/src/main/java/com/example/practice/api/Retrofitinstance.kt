package com.example.practice.api

import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    // Production Railway Backend
    private const val BASE_URL = "https://backend-production-7f08.up.railway.app/api/"

    /**
     * Custom DNS resolver that falls back to Google Public DNS (8.8.8.8, 8.8.4.4)
     * when the system/ISP DNS fails to resolve Railway's hostname.
     */
    private val fallbackDns = object : Dns {
        private val googleDns = listOf(
            InetAddress.getByName("8.8.8.8"),
            InetAddress.getByName("8.8.4.4")
        )

        override fun lookup(hostname: String): List<InetAddress> {
            // Try system DNS first
            return try {
                val systemResult = Dns.SYSTEM.lookup(hostname)
                if (systemResult.isNotEmpty()) systemResult
                else resolveViaGoogle(hostname)
            } catch (e: Exception) {
                // System DNS failed — fall back to Google DNS
                resolveViaGoogle(hostname)
            }
        }

        private fun resolveViaGoogle(hostname: String): List<InetAddress> {
            // Query Google DNS-over-HTTPS (JSON API)
            try {
                val url = java.net.URL("https://dns.google/resolve?name=$hostname&type=A")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val response = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                // Parse IP addresses from the JSON response
                val addresses = mutableListOf<InetAddress>()
                val regex = """"data"\s*:\s*"([^"]+)"""".toRegex()
                regex.findAll(response).forEach { match ->
                    try {
                        addresses.add(InetAddress.getByName(match.groupValues[1]))
                    } catch (_: Exception) { }
                }
                if (addresses.isNotEmpty()) return addresses
            } catch (_: Exception) { }

            // Last resort: direct lookup via Google DNS nameserver
            return googleDns.flatMap { dns ->
                try {
                    InetAddress.getAllByName(hostname).toList()
                } catch (_: Exception) { emptyList() }
            }.ifEmpty {
                throw java.net.UnknownHostException("Unable to resolve $hostname")
            }
        }
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .dns(fallbackDns)
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