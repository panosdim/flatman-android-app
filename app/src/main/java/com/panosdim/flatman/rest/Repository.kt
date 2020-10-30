package com.panosdim.flatman.rest

import com.panosdim.flatman.model.Balance
import com.panosdim.flatman.model.Lessee
import com.panosdim.flatman.rest.data.LoginRequest

class Repository {
    private var client: Webservice = webservice

    suspend fun login(loginRequest: LoginRequest) = client.login(loginRequest)
    suspend fun checkSession() = client.user()
    suspend fun checkTin(tin: String) = client.checkTin(tin)

    suspend fun getAllLessees() = client.lessee()
    suspend fun createNewLessee(newLessee: Lessee) = client.lessee(newLessee)
    suspend fun deleteLessee(id: Int) = client.lessee(id)
    suspend fun updateLessee(id: Int, updatedLessee: Lessee) = client.lessee(id, updatedLessee)

    suspend fun getAllBalance() = client.balance()
    suspend fun createNewBalance(newBalance: Balance) = client.balance(newBalance)
    suspend fun deleteBalance(id: Int) = client.balance(id)
    suspend fun updateBalance(id: Int, updatedBalance: Balance) = client.balance(id, updatedBalance)
}