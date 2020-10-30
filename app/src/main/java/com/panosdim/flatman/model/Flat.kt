package com.panosdim.flatman.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Flat(
    @PrimaryKey var id: Int? = null,
    @ColumnInfo var name: String,
    @ColumnInfo var address: String,
    @ColumnInfo var floor: Int
) {
    override fun toString(): String {
        return name
    }
}