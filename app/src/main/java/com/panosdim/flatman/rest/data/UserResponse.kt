package com.panosdim.flatman.rest.data

import com.google.gson.annotations.SerializedName

data class UserResponse(
    val id: Int = 0,
    val username: String = "",
    val email: String = "",
    @SerializedName("first_name")
    val firstName: String = "",
    @SerializedName("last_name")
    val lastName: String = ""
)