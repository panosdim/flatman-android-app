package com.panosdim.flatman.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.panosdim.flatman.api.LesseeRepository
import com.panosdim.flatman.api.data.PostalCodeResponse
import com.panosdim.flatman.api.data.Resource
import com.panosdim.flatman.model.Flat
import com.panosdim.flatman.model.Lessee

class LesseeViewModel : ViewModel() {
    private val lesseeRepository = LesseeRepository()

    fun getLessees(): LiveData<Resource<List<Lessee>>> {
        return lesseeRepository.get()
    }

    fun removeLessee(lessee: Lessee): LiveData<Resource<Void>> {
        return lesseeRepository.delete(lessee)
    }

    fun addLessee(lessee: Lessee): LiveData<Resource<Void>> {
        return lesseeRepository.add(lessee)
    }

    fun updateLessee(lessee: Lessee): LiveData<Resource<Void>> {
        return lesseeRepository.update(lessee)
    }

    fun refreshLessee() {
        return lesseeRepository.refresh()
    }

    fun getPostalCode(address: String): PostalCodeResponse? {
        return lesseeRepository.getPostalCode(address)
    }

    fun getMonthlyRent(flat: Flat): Int? {
        return lesseeRepository.getMonthlyRent(flat)
    }
}