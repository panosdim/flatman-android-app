package com.panosdim.flatman

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.room.Room
import com.panosdim.flatman.db.AppDatabase
import com.panosdim.flatman.model.Balance
import com.panosdim.flatman.model.Lessee
import com.panosdim.flatman.rest.Repository

val prefs: Prefs by lazy {
    App.prefs!!
}

val repository: Repository by lazy {
    App.repository
}

val lesseesList: MutableList<Lessee> by lazy {
    App.lessees
}

val balanceList: MutableLiveData<MutableList<Balance>> by lazy {
    App.balance
}

val db by lazy {
    App.db
}

const
val BACKEND_URL = "https://api.flat.cc.nf/"

enum class RC(val code: Int) {
    PERMISSION_REQUEST(0)
}

class App : Application() {
    companion object {
        var prefs: Prefs? = null
        val repository = Repository()
        lateinit var db: AppDatabase
        lateinit var instance: App private set

        var lessees: MutableList<Lessee> = mutableListOf()
        val balance = MutableLiveData<MutableList<Balance>>().apply {
            value = mutableListOf()
        }
    }

    override fun onCreate() {
        prefs = Prefs(applicationContext)
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "flatman"
        ).build()
        super.onCreate()
        instance = this
    }
}