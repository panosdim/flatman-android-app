package com.panosdim.flatman.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.panosdim.flatman.api.LesseeRepository
import com.panosdim.flatman.api.data.CheckTinResponse
import com.panosdim.flatman.api.data.Resource
import com.panosdim.flatman.model.Lessee

class LesseeViewModel : ViewModel() {
    private val lesseeRepository = LesseeRepository()
    val lessee: LiveData<List<Lessee>> = lesseeRepository.getLiveData()

    fun getAllLessee(): LiveData<Resource<List<Lessee>>> {
        return lesseeRepository.get()
    }

    fun removeLessee(balance: Lessee): LiveData<Resource<Lessee>> {
        return lesseeRepository.delete(balance)
    }

    fun addLessee(balance: Lessee): LiveData<Resource<Lessee>> {
        return lesseeRepository.add(balance)
    }

    fun updateLessee(balance: Lessee): LiveData<Resource<Lessee>> {
        return lesseeRepository.update(balance)
    }

    fun refreshLessee() {
        return lesseeRepository.refresh()
    }

    fun checkTin(tin: String): CheckTinResponse? {
        return lesseeRepository.checkTin(tin)
    }
}