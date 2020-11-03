package com.panosdim.flatman.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.panosdim.flatman.db.dao.BalanceDao
import com.panosdim.flatman.db.dao.FlatDao
import com.panosdim.flatman.db.dao.LesseeDao
import com.panosdim.flatman.model.Balance
import com.panosdim.flatman.model.Flat
import com.panosdim.flatman.model.Lessee

@Database(entities = [Flat::class, Balance::class, Lessee::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun flatDao(): FlatDao
    abstract fun balanceDao(): BalanceDao
    abstract fun lesseeDao(): LesseeDao
}
