package com.panosdim.flatman.ui

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.panosdim.flatman.R
import com.panosdim.flatman.TAG
import com.panosdim.flatman.api.Webservice
import com.panosdim.flatman.api.data.LoginRequest
import com.panosdim.flatman.api.webservice
import com.panosdim.flatman.auth
import com.panosdim.flatman.prefs
import com.panosdim.flatman.utils.checkForNewVersion
import com.panosdim.flatman.utils.refId
import com.panosdim.flatman.utils.startIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class MainActivity : AppCompatActivity() {
    private lateinit var manager: DownloadManager
    private lateinit var onComplete: BroadcastReceiver
    private var client: Webservice = webservice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController: NavController = navHostFragment.navController
        navView.setupWithNavController(navController)

        // Handle new version installation after the download of APK file.
        manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val referenceId = intent!!.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (referenceId != -1L && referenceId == refId) {
                    val apkUri = manager.getUriForDownloadedFile(refId)
                    val installIntent = Intent(Intent.ACTION_VIEW)
                    installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive")
                    installIntent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    startActivity(installIntent)
                }

            }
        }
        registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        // Check for permission to read/write to external storage
        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    checkForNewVersion(this)
                } else {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(resources.getString(R.string.permission_title))
                        .setMessage(resources.getString(R.string.permission_description))
                        .setPositiveButton(resources.getString(R.string.dismiss)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            }
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) -> {
                checkForNewVersion(this)
            }
            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        refreshToken()
    }

    private fun refreshToken() {
        if (prefs.email.isNotEmpty() and prefs.password.isNotEmpty()) {
            val scope = CoroutineScope(Dispatchers.Main)

            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val response =
                            client.login(
                                LoginRequest(
                                    prefs.email, prefs.password
                                )
                            )
                        prefs.token = response.token
                        auth.signInAnonymously()
                            .addOnCompleteListener(this@MainActivity) { task ->
                                if (task.isSuccessful) {
                                    // Sign in success
                                    Log.d(TAG, "signInAnonymously:success")
                                } else {
                                    // If sign in fails, display a message to the user.
                                    Log.w(TAG, "signInAnonymously:failure", task.exception)
                                }
                            }
                            .addOnFailureListener {
                                Log.w(TAG, "Fail to login anonymously.", it)
                            }
                    }
                } catch (e: HttpException) {
                    startIntent(this@MainActivity, LoginActivity::class.java)
                }
            }
        } else {
            startIntent(this, LoginActivity::class.java)
        }
    }
}