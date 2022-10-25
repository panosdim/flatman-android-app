package com.panosdim.flatman.db.dao

import androidx.room.*
import com.panosdim.flatman.model.Flat

@Dao
interface FlatDao {
    @Query("SELECT * FROM Flat")
    suspend fun get(): List<Flat>

    @Insert
    suspend fun insert(flat: Flat)

    @Update
    suspend fun update(flat: Flat)

    @Delete
    suspend fun delete(flat: Flat)

    @Transaction
    suspend fun deleteAndCreate(flats: List<Flat>) {
        deleteAll()
        insertAll(flats)
    }

    @Query("DELETE FROM Flat")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(flats: List<Flat>)
}
