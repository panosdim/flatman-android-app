package com.panosdim.flatman.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.panosdim.flatman.api.FlatRepository
import com.panosdim.flatman.api.data.Resource
import com.panosdim.flatman.model.Flat


class FlatViewModel : ViewModel() {
    private val flatRepository = FlatRepository()

    fun getFlats(): LiveData<Resource<List<Flat>>> {
        return flatRepository.get()
    }

    fun removeFlat(flat: Flat): LiveData<Resource<Void>> {
        return flatRepository.delete(flat)
    }

    fun addFlat(flat: Flat): LiveData<Resource<Void>> {
        return flatRepository.add(flat)
    }

    fun updateFlat(flat: Flat): LiveData<Resource<Void>> {
        return flatRepository.update(flat)
    }

    fun refreshFlats() {
        return flatRepository.refresh()
    }
}
