package com.panosdim.flatman.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.panosdim.flatman.model.Flat
import com.panosdim.flatman.rest.FlatRepository
import com.panosdim.flatman.rest.data.Resource

class FlatViewModel : ViewModel() {
    private val flatRepository = FlatRepository()
    val flats: LiveData<List<Flat>> = flatRepository.getAllFlats()

    fun removeFlat(flat: Flat): LiveData<Resource<Flat>> {
        return flatRepository.delete(flat)
    }

    fun addFlat(flat: Flat): LiveData<Resource<Flat>> {
        return flatRepository.add(flat)
    }

    fun updateFlat(flat: Flat): LiveData<Resource<Flat>> {
        return flatRepository.update(flat)
    }
}
