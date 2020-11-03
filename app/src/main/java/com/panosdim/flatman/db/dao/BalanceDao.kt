package com.panosdim.flatman.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.panosdim.flatman.model.Balance

@Dao
interface BalanceDao {
    @Query("SELECT * FROM Balance")
    suspend fun get(): List<Balance>

    @Query("SELECT * FROM Balance")
    fun getLiveData(): LiveData<List<Balance>>

    @Insert
    suspend fun insert(balance: Balance)

    @Update
    suspend fun update(balance: Balance)

    @Delete
    suspend fun delete(balance: Balance)

    @Transaction
    suspend fun deleteAndCreate(balance: List<Balance>) {
        deleteAll()
        insertAll(balance)
    }

    @Query("DELETE FROM Balance")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(balance: List<Balance>)
}