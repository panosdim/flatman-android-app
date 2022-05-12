package com.panosdim.flatman

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences

const val PREFS_FILENAME = "credentials"
const val TOKEN = "token"
const val EMAIL = "email"
const val PASSWORD = "password"
const val FLAT_FETCH_DATE = "flat_fetch_date"
const val BALANCE_FETCH_DATE = "balance_fetch_date"
const val LESSEE_FETCH_DATE = "lessee_fetch_date"


class Prefs(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_FILENAME, MODE_PRIVATE)

    var token: String
        get() = prefs.getString(TOKEN, "").toString()
        set(value) = prefs.edit().putString(TOKEN, value).apply()

    var email: String
        get() = prefs.getString(EMAIL, "").toString()
        set(value) = prefs.edit().putString(EMAIL, value).apply()

    var password: String
        get() = prefs.getString(PASSWORD, "").toString()
        set(value) = prefs.edit().putString(PASSWORD, value).apply()

    var flatFetchDate: Long
        get() = prefs.getLong(FLAT_FETCH_DATE, -1)
        set(value) = prefs.edit().putLong(FLAT_FETCH_DATE, value).apply()

    var balanceFetchDate: Long
        get() = prefs.getLong(BALANCE_FETCH_DATE, -1)
        set(value) = prefs.edit().putLong(BALANCE_FETCH_DATE, value).apply()

    var lesseeFetchDate: Long
        get() = prefs.getLong(LESSEE_FETCH_DATE, -1)
        set(value) = prefs.edit().putLong(LESSEE_FETCH_DATE, value).apply()
}