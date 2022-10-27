package com.panosdim.flatman.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity
data class Balance(
    @PrimaryKey val id: Int? = null,
    @ColumnInfo val date: String,
    @ColumnInfo val comment: String,
    @ColumnInfo @SerializedName("flat_id") val flatId: Int,
    @ColumnInfo val amount: Float
)