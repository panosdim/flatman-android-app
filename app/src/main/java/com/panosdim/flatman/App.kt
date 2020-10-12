package com.panosdim.flatman

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.panosdim.flatman.model.Balance
import com.panosdim.flatman.model.Flat
import com.panosdim.flatman.model.Lessee
import com.panosdim.flatman.rest.Repository

val prefs: Prefs by lazy {
    App.prefs!!
}

val repository: Repository by lazy {
    App.repository
}

val flatsList: MutableLiveData<MutableList<Flat>> by lazy {
    App.flats
}

val lesseesList: MutableList<Lessee> by lazy {
    App.lessees
}

val balanceList: MutableLiveData<MutableList<Balance>> by lazy {
    App.balance
}

const val BACKEND_URL = "https://api.flat.cc.nf/"

enum class RC(val code: Int) {
    PERMISSION_REQUEST(0)
}

class App : Application() {
    companion object {
        var prefs: Prefs? = null
        var repository = Repository()
        var flats = MutableLiveData<MutableList<Flat>>().apply {
            value = mutableListOf()
        }
        var lessees: MutableList<Lessee> = mutableListOf()
        val balance = MutableLiveData<MutableList<Balance>>().apply {
            value = mutableListOf()
        }
    }

    override fun onCreate() {
        prefs = Prefs(applicationContext)
        super.onCreate()
    }
}