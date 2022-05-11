package com.panosdim.flatman.api

import com.panosdim.flatman.api.data.LoginRequest
import com.panosdim.flatman.api.data.LoginResponse
import com.panosdim.flatman.model.Balance
import com.panosdim.flatman.model.Flat
import com.panosdim.flatman.model.Lessee
import retrofit2.Response
import retrofit2.http.*

interface Webservice {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("flat")
    suspend fun flat(): List<Flat>

    @POST("flat")
    suspend fun flat(@Body request: Flat): Flat

    @PUT("flat/{id}")
    suspend fun flat(@Path("id") id: Int, @Body request: Flat): Flat

    @DELETE("flat/{id}")
    suspend fun flat(@Path("id") id: Int): Response<Void>

    @GET("lessee")
    suspend fun lessee(): List<Lessee>

    @POST("lessee")
    suspend fun lessee(@Body request: Lessee): Lessee

    @PUT("lessee/{id}")
    suspend fun lessee(@Path("id") id: Int, @Body request: Lessee): Lessee

    @DELETE("lessee/{id}")
    suspend fun lessee(@Path("id") id: Int): Response<Void>

    @GET("balance")
    suspend fun balance(): List<Balance>

    @POST("balance")
    suspend fun balance(@Body request: Balance): Balance

    @PUT("balance/{id}")
    suspend fun balance(@Path("id") id: Int, @Body request: Balance): Balance

    @DELETE("balance/{id}")
    suspend fun balance(@Path("id") id: Int): Response<Void>
}