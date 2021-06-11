package com.panosdim.flatman.api

import com.panosdim.flatman.api.data.PostalCodeResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PostalCodeService {
    @GET("/")
    suspend fun getPostalCode(@Query("address") address: String): PostalCodeResponse
}