package com.panosdim.flatman.rest

import com.google.gson.GsonBuilder
import com.panosdim.flatman.BACKEND_URL
import com.panosdim.flatman.prefs
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory


val webservice: Webservice by lazy {
    val client = OkHttpClient.Builder().addInterceptor { chain ->
        val newRequest = chain.request().newBuilder()
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .addHeader("Authorization", " Bearer " + prefs.token)
            .build()
        chain.proceed(newRequest)
    }.build()
    Retrofit.Builder()
        .baseUrl(BACKEND_URL)
        .client(client)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
        .build()
        .create(Webservice::class.java)
}