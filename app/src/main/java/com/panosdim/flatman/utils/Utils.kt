package com.panosdim.flatman.utils

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.core.content.pm.PackageInfoCompat
import com.google.gson.Gson
import com.panosdim.flatman.R
import com.panosdim.flatman.TAG
import com.panosdim.flatman.model.FileMetadata
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import javax.net.ssl.HttpsURLConnection

var refId: Long = -1

fun moneyFormat(obj: Any): String {
    val symbols = DecimalFormatSymbols()
    symbols.groupingSeparator = '.'
    symbols.decimalSeparator = ','
    val moneyFormat = DecimalFormat("#,##0.00 €", symbols)
    return moneyFormat.format(obj)
}

fun checkForNewVersion(context: Context) {
    val metadataFileName = "output-metadata.json"
    val apkFileName = "app-release.apk"
    val backendUrl = "https://apps.dsw.mywire.org/flatman/"
    val url: URL

    try {
        url = URL(backendUrl + metadataFileName)
        val conn = url.openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = true
        conn.requestMethod = "GET"
        conn.readTimeout = 15000
        conn.connectTimeout = 15000
        conn.useCaches = false

        val responseCode = conn.responseCode

        if (responseCode == HttpsURLConnection.HTTP_OK) {
            val data = conn.inputStream.bufferedReader().use(BufferedReader::readText)
            val fileMetadata = Gson().fromJson(data, FileMetadata::class.java)
            val version = fileMetadata.elements[0].versionCode

            val appVersion = PackageInfoCompat.getLongVersionCode(
                context.packageManager.getPackageInfo(
                    context.packageName,
                    0
                )
            )

            if (version > appVersion) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        context.getString(R.string.new_version),
                        Toast.LENGTH_LONG
                    ).show()
                }

                val versionName = fileMetadata.elements[0].versionName

                // Download APK file
                val apkUri = Uri.parse(backendUrl + apkFileName)
                downloadNewVersion(context, apkUri, versionName)
            }
        }
    } catch (e: Exception) {
        Log.d(TAG, e.toString())
    }
}

private fun downloadNewVersion(context: Context, downloadUrl: Uri, version: String) {
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val request =
        DownloadManager.Request(downloadUrl)
    request.setDescription("Downloading new version of FlatMan.")
    request.setTitle("New FlatMan Version: $version")
    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
    request.setDestinationInExternalPublicDir(
        Environment.DIRECTORY_DOWNLOADS,
        "FlatMan-${version}.apk"
    )
    refId = manager.enqueue(request)
}

fun generateTextWatcher(validateFunc: () -> Unit): TextWatcher {
    return object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            validateFunc()
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            // Not required
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            // Not required
        }
    }
}

fun <T> startIntent(context: Context, cls: Class<T>) {
    val intent = Intent(context, cls)
    intent.flags =
        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    context.startActivity(intent)
}

fun isValidTIN(tin: String?): Boolean {
    val tinLen = 9
    if (tin == null || tin.length != tinLen || tin == "000000000") {
        return false
    }
    var total = 0
    var check = -1
    for (i in tinLen - 1 downTo 0) {
        val c = tin[i]
        if (!Character.isDigit(c)) {
            return false
        }
        val digit = c - '0'
        if (i == tinLen - 1) {
            check = digit
            continue
        }
        total += digit shl tinLen - i - 1
    }
    return check == total % 11 % 10
}
