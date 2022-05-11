package com.panosdim.flatman.utils

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.core.content.pm.PackageInfoCompat
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.google.gson.Gson
import com.panosdim.flatman.R
import com.panosdim.flatman.TAG
import com.panosdim.flatman.model.FileMetadata
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

var refId: Long = -1

fun moneyFormat(obj: Any): String {
    val symbols = DecimalFormatSymbols()
    symbols.groupingSeparator = '.'
    symbols.decimalSeparator = ','
    val moneyFormat = DecimalFormat("#,##0.00 €", symbols)
    return moneyFormat.format(obj)
}

fun checkForNewVersion(context: Context) {
    val storage = Firebase.storage
    val metadataFileName = "output-metadata.json"
    val apkFileName = "app-release.apk"

    // Create a storage reference from our app
    val storageRef = storage.reference

    // Create a metadata reference
    val metadataRef: StorageReference = storageRef.child(metadataFileName)

    metadataRef.getBytes(Long.MAX_VALUE).addOnSuccessListener {
        // Use the bytes to display the image
        val data = String(it)
        val fileMetadata = Gson().fromJson(data, FileMetadata::class.java)
        val version = fileMetadata.elements[0].versionCode

        val appVersion = PackageInfoCompat.getLongVersionCode(
            context.packageManager.getPackageInfo(
                context.packageName,
                0
            )
        )

        if (version > appVersion) {
            Toast.makeText(
                context,
                context.getString(R.string.new_version),
                Toast.LENGTH_LONG
            ).show()

            val versionName = fileMetadata.elements[0].versionName

            // Create an apk reference
            val apkRef = storageRef.child(apkFileName)

            apkRef.downloadUrl.addOnSuccessListener { uri ->
                downloadNewVersion(context, uri, versionName)
            }.addOnFailureListener {
                // Handle any errors
                Log.w(TAG, "Fail to download file $apkFileName")
            }
        }
    }.addOnFailureListener {
        // Handle any errors
        Log.w(TAG, "Fail to retrieve $metadataFileName")
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
