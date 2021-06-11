package com.panosdim.flatman

import android.app.Application
import androidx.room.Room
import com.panosdim.flatman.db.AppDatabase

val prefs: Prefs by lazy {
    App.prefs!!
}

val db by lazy {
    App.db
}

const val BACKEND_URL = "https://api.flat.cc.nf/"
const val POSTAL_URL = "https://postal.cc.nf/"

enum class RC(val code: Int) {
    PERMISSION_REQUEST(0)
}

class App : Application() {
    companion object {
        var prefs: Prefs? = null
        lateinit var db: AppDatabase
        lateinit var instance: App private set
    }

    override fun onCreate() {
        prefs = Prefs(applicationContext)
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "flatman"
        )
            .fallbackToDestructiveMigration()
            .build()
        super.onCreate()
        instance = this
    }
}