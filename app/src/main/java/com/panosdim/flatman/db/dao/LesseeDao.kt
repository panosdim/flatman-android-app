package com.panosdim.flatman.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.panosdim.flatman.model.Lessee

@Dao
interface LesseeDao {
    @Query("SELECT * FROM Lessee")
    suspend fun get(): List<Lessee>

    @Query("SELECT * FROM Lessee")
    fun getLiveData(): LiveData<List<Lessee>>

    @Insert
    suspend fun insert(lessee: Lessee)

    @Update
    suspend fun update(lessee: Lessee)

    @Delete
    suspend fun delete(lessee: Lessee)

    @Transaction
    suspend fun deleteAndCreate(lessees: List<Lessee>) {
        deleteAll()
        insertAll(lessees)
    }

    @Query("DELETE FROM Lessee")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(lessees: List<Lessee>)
}