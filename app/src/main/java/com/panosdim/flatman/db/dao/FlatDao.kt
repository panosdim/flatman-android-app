package com.panosdim.flatman.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.panosdim.flatman.model.Flat

@Dao
interface FlatDao {
    @Query("SELECT * FROM Flat")
    fun getAll(): LiveData<List<Flat>>

    @Insert
    fun insert(flat: Flat)

    @Update
    fun update(flat: Flat)

    @Delete
    fun delete(flat: Flat)

    @Transaction
    fun deleteAndCreate(flats: List<Flat>) {
        deleteAll()
        insertAll(flats)
    }

    @Query("DELETE FROM Flat")
    fun deleteAll()

    @Insert
    fun insertAll(users: List<Flat>)
}
