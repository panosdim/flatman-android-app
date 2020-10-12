package com.panosdim.flatman.ui.login

import com.panosdim.flatman.rest.data.LoginResponse

/**
 * Authentication result : success (user details) or error message.
 */
data class LoginResult(
        val success: LoginResponse? = null,
        val error: Int? = null
)