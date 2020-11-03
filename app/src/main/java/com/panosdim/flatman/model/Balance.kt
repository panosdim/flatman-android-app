package com.panosdim.flatman.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity
data class Balance(
    @PrimaryKey var id: Int? = null,
    @ColumnInfo var date: String,
    @ColumnInfo var comment: String,
    @ColumnInfo @SerializedName("flat_id") var flatId: Int,
    @ColumnInfo var amount: Float
)