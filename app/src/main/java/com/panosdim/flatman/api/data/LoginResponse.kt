package com.panosdim.flatman.api.data

import com.panosdim.flatman.model.User

data class LoginResponse(
    val token: String = "",
    val user: User? = null
)