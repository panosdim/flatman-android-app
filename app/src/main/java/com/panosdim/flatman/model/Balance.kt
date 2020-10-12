package com.panosdim.flatman.model

import com.google.gson.annotations.SerializedName

data class Balance(
    var id: Int? = null,
    var date: String,
    var comment: String,
    @SerializedName("flat_id") var flatId: Int,
    var amount: Float
)