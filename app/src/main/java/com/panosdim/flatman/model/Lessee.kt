package com.panosdim.flatman.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity
data class Lessee(
    @PrimaryKey val id: Int? = null,
    @ColumnInfo val name: String,
    @ColumnInfo val address: String,
    @ColumnInfo @SerializedName("postal_code") val postalCode: String,
    @ColumnInfo val from: String,
    @ColumnInfo val until: String,
    @ColumnInfo @SerializedName("flat_id") val flatId: Int,
    @ColumnInfo val rent: Int,
    @ColumnInfo val tin: String
)