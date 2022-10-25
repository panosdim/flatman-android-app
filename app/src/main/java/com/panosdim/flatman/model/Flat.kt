package com.panosdim.flatman.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Flat(
    @PrimaryKey val id: Int? = null,
    @ColumnInfo val name: String,
    @ColumnInfo val address: String,
    @ColumnInfo val floor: Int
) {
    override fun toString(): String {
        return name
    }
}