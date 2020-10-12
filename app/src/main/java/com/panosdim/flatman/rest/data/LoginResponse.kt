package com.panosdim.flatman.rest.data

import com.panosdim.flatman.model.User

data class LoginResponse(
    val token: String = "",
    val user: User? = null
)