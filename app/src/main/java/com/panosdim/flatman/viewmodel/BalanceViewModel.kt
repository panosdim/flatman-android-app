package com.panosdim.flatman.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.panosdim.flatman.api.BalanceRepository
import com.panosdim.flatman.api.data.Resource
import com.panosdim.flatman.model.Balance

class BalanceViewModel : ViewModel() {
    private val balanceRepository = BalanceRepository()
    val balance: LiveData<List<Balance>> = balanceRepository.getLiveData()

    fun getAllBalance(): LiveData<Resource<List<Balance>>> {
        return balanceRepository.get()
    }

    fun removeBalance(balance: Balance): LiveData<Resource<Balance>> {
        return balanceRepository.delete(balance)
    }

    fun addBalance(balance: Balance): LiveData<Resource<Balance>> {
        return balanceRepository.add(balance)
    }

    fun updateBalance(balance: Balance): LiveData<Resource<Balance>> {
        return balanceRepository.update(balance)
    }

    fun refreshBalance() {
        return balanceRepository.refresh()
    }
}