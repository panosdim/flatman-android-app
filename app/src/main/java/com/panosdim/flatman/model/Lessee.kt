package com.panosdim.flatman.model

import com.google.gson.annotations.SerializedName

data class Lessee(
    var id: Int? = null,
    var name: String,
    var address: String,
    @SerializedName("postal_code") var postalCode: String,
    var from: String,
    var until: String,
    @SerializedName("flat_id") var flatId: Int,
    var rent: Int,
    var tin: Int
)