package com.panosdim.flatman.ui.login

import android.content.Context
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.panosdim.flatman.R
import com.panosdim.flatman.prefs
import com.panosdim.flatman.repository
import com.panosdim.flatman.rest.data.LoginRequest
import com.panosdim.flatman.rest.data.LoginResponse
import com.panosdim.flatman.utils.downloadData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class LoginViewModel(private val context: Context) : ViewModel() {

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    fun login(username: String, password: String) {
        val scope = CoroutineScope(Dispatchers.Main)
        lateinit var response: LoginResponse

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    response = repository.login(LoginRequest(username, password))
                    prefs.token = response.token
                    prefs.email = username
                    prefs.password = password

                    downloadData(context)
                }
                _loginResult.value = LoginResult(success = response)
            } catch (e: HttpException) {
                _loginResult.value = LoginResult(error = R.string.login_failed)
            }
        }
    }

    fun loginDataChanged(username: String, password: String) {
        if (!isUserNameValid(username)) {
            _loginForm.value = LoginFormState(usernameError = R.string.invalid_username)
        } else if (!isPasswordValid(password)) {
            _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    // A placeholder username validation check
    private fun isUserNameValid(username: String): Boolean {
        return if (username.contains('@')) {
            Patterns.EMAIL_ADDRESS.matcher(username).matches()
        } else {
            username.isNotBlank()
        }
    }

    // A placeholder password validation check
    private fun isPasswordValid(password: String): Boolean {
        return password.length > 5
    }
}