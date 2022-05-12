package com.panosdim.flatman

import android.app.Application
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.panosdim.flatman.db.AppDatabase

val prefs: Prefs by lazy {
    App.prefs
}

val db by lazy {
    App.db
}

val auth by lazy {
    App.auth
}

const val BACKEND_URL = "https://flatman.dsw.mywire.org/api/"
const val POSTAL_URL = "https://postal.dsw.mywire.org/"
const val TAG = "FLAT_MAN"

class App : Application() {
    companion object {
        lateinit var prefs: Prefs
        lateinit var db: AppDatabase
        lateinit var instance: App private set
        lateinit var auth: FirebaseAuth
    }

    override fun onCreate() {
        prefs = Prefs(applicationContext)
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "flatman"
        )
            .fallbackToDestructiveMigration()
            .build()
        auth = Firebase.auth
        super.onCreate()
        instance = this
    }
}