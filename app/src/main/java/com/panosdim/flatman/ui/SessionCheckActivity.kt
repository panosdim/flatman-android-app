package com.panosdim.flatman.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.panosdim.flatman.R
import com.panosdim.flatman.prefs
import com.panosdim.flatman.repository
import com.panosdim.flatman.rest.data.LoginRequest
import com.panosdim.flatman.ui.login.LoginActivity
import com.panosdim.flatman.utils.downloadData
import kotlinx.android.synthetic.main.activity_session_check.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class SessionCheckActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_check)

        if (prefs.token.isEmpty()) {
            startIntent(LoginActivity::class.java)
        } else {
            checkActiveSession()
        }
    }

    private fun loginWithStoredCredentials() {
        if (prefs.email.isNotEmpty() and prefs.password.isNotEmpty()) {
            val scope = CoroutineScope(Dispatchers.Main)

            scope.launch {
                tvMessage.text = getString(R.string.no_active_session)
                try {
                    withContext(Dispatchers.IO) {
                        val response =
                            repository.login(
                                LoginRequest(
                                    prefs.email, prefs.password
                                )
                            )
                        prefs.token = response.token
                    }

                    tvMessage.text = getString(R.string.download_data)
                    withContext(Dispatchers.IO) {
                        downloadData(this@SessionCheckActivity)
                    }

                    startIntent(MainActivity::class.java)
                } catch (e: HttpException) {
                    startIntent(LoginActivity::class.java)
                }
            }
        } else {
            startIntent(LoginActivity::class.java)
        }
    }

    private fun checkActiveSession() {
        val scope = CoroutineScope(Dispatchers.Main)

        scope.launch {
            tvMessage.text = getString(R.string.check_session)
            try {
                withContext(Dispatchers.IO) {
                    repository.checkSession()
                }

                tvMessage.text = getString(R.string.download_data)
                withContext(Dispatchers.IO) {
                    downloadData(this@SessionCheckActivity)
                }

                startIntent(MainActivity::class.java)
            } catch (e: HttpException) {
                loginWithStoredCredentials()
            } catch (t: SocketTimeoutException) {
                Toast.makeText(
                    this@SessionCheckActivity,
                    getString(R.string.connection_timeout),
                    Toast.LENGTH_LONG
                )
                    .show()
                finish()
            } catch (d: UnknownHostException) {
                Toast.makeText(
                    this@SessionCheckActivity,
                    getString(R.string.unknown_host),
                    Toast.LENGTH_LONG
                )
                    .show()
                finish()
            }
        }
    }

    private fun <T> startIntent(cls: Class<T>) {
        val intent = Intent(this@SessionCheckActivity, cls)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}