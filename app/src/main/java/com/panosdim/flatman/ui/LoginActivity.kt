package com.panosdim.flatman.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.panosdim.flatman.R
import com.panosdim.flatman.TAG
import com.panosdim.flatman.api.Webservice
import com.panosdim.flatman.api.data.LoginRequest
import com.panosdim.flatman.api.data.LoginResponse
import com.panosdim.flatman.api.webservice
import com.panosdim.flatman.auth
import com.panosdim.flatman.model.User
import com.panosdim.flatman.prefs
import com.panosdim.flatman.utils.generateTextWatcher
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class LoginActivity : AppCompatActivity() {
    private var client: Webservice = webservice
    private val textWatcher = generateTextWatcher(::validateForm)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        username.addTextChangedListener(textWatcher)
        password.addTextChangedListener(textWatcher)

        password.setOnEditorActionListener { _, actionId, event ->
            if (isFormValid() && (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE)) {
                login()
            }
            false
        }

        login.setOnClickListener {
            login()
        }
    }

    private fun updateUiWithUser(model: User) {
        val welcome = getString(R.string.welcome)
        val displayName = model.firstName + " " + model.lastName
        Toast.makeText(
            applicationContext,
            "$welcome $displayName",
            Toast.LENGTH_LONG
        ).show()
        val intent = Intent(this@LoginActivity, MainActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun login() {
        val scope = CoroutineScope(Dispatchers.Main)
        lateinit var response: LoginResponse
        val username = username.text.toString()
        val password = password.text.toString()
        loading.visibility = View.VISIBLE
        login.isEnabled = false

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    response = client.login(
                        LoginRequest(username, password)
                    )
                    prefs.token = response.token
                    prefs.email = username
                    prefs.password = password

                    auth.signInAnonymously()
                        .addOnCompleteListener(this@LoginActivity) { task ->
                            if (task.isSuccessful) {
                                // Sign in success
                                Log.d(TAG, "signInAnonymously:success")
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w(TAG, "signInAnonymously:failure", task.exception)
                            }
                        }
                }
                updateUiWithUser(response.user!!)

                //Complete and destroy login activity once successful
                finish()
            } catch (e: HttpException) {
                Toast.makeText(applicationContext, R.string.login_failed, Toast.LENGTH_SHORT)
                    .show()
            } finally {
                loading.visibility = View.GONE
                login.isEnabled = true
            }
        }
    }

    private fun validateForm() {
        login.isEnabled = true
        username.error = null
        password.error = null

        // Store values.
        val email = username.text.toString()
        val pass = password.text.toString()

        if (!isUserNameValid(email)) {
            username.error = getString(R.string.invalid_username)
            login.isEnabled = false
        }

        if (!isPasswordValid(pass)) {
            password.error = getString(R.string.invalid_password)
            login.isEnabled = false
        }
    }

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

    private fun isFormValid(): Boolean {
        return username.error == null && password.error == null
    }
}
