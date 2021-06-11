package com.panosdim.flatman.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity
data class Lessee(
    @PrimaryKey var id: Int? = null,
    @ColumnInfo var name: String,
    @ColumnInfo var address: String,
    @ColumnInfo @SerializedName("postal_code") var postalCode: String,
    @ColumnInfo var from: String,
    @ColumnInfo var until: String,
    @ColumnInfo @SerializedName("flat_id") var flatId: Int,
    @ColumnInfo var rent: Int,
    @ColumnInfo var tin: String
)