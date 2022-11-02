package com.panosdim.flatman.model

class BalanceFilters {
    val isFilterSet: Boolean
        get() {
            return flat != null || balance != null || date != null
        }
    var flat: Int? = null
    var balance: Int? = null
    var date: Int? = null
}