package com.panosdim.flatman.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.panosdim.flatman.db.dao.FlatDao
import com.panosdim.flatman.model.Flat

@Database(entities = [Flat::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun flatDao(): FlatDao
}
