package com.panosdim.flatman.api

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.panosdim.flatman.App
import com.panosdim.flatman.R
import com.panosdim.flatman.api.data.Resource
import com.panosdim.flatman.db
import com.panosdim.flatman.db.dao.BalanceDao
import com.panosdim.flatman.model.Balance
import com.panosdim.flatman.prefs
import com.panosdim.flatman.utils.fromEpochMilli
import com.panosdim.flatman.utils.toEpochMilli
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class BalanceRepository {
    private var client: Webservice = webservice
    private val scope = CoroutineScope(Dispatchers.Main)
    private val balanceDao: BalanceDao = db.balanceDao()
    private val balanceList: MutableLiveData<Resource<List<Balance>>> = MutableLiveData()

    fun get(): LiveData<Resource<List<Balance>>> {
        balanceList.postValue(Resource.Loading())

        if (prefs.balanceFetchDate == -1L ||
            ChronoUnit.DAYS.between(fromEpochMilli(prefs.balanceFetchDate), LocalDate.now()) >= 1
        ) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val response = client.balance()
                        balanceDao.deleteAndCreate(response)
                        prefs.balanceFetchDate = LocalDate.now().toEpochMilli()
                        balanceList.postValue(Resource.Success(response))
                    }
                } catch (ex: Exception) {
                    withContext(Dispatchers.IO) {
                        balanceList.postValue(Resource.Success(balanceDao.get()))
                    }
                }
            }
        }

        scope.launch {
            withContext(Dispatchers.IO) {
                balanceList.postValue(Resource.Success(balanceDao.get()))
            }
        }

        return balanceList
    }

    fun delete(balance: Balance): LiveData<Resource<Void>> {
        val result: MutableLiveData<Resource<Void>> = MutableLiveData()
        result.postValue(Resource.Loading())

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val response = client.balance(balance.id!!)
                    when (response.code()) {
                        204 -> {
                            balanceDao.delete(balance)
                            result.postValue(Resource.Success())
                            balanceList.postValue(Resource.Success(balanceDao.get()))
                        }
                        404 -> {
                            result.postValue(Resource.Error("Error deleting balance. Balance not found."))
                        }
                    }
                }
            } catch (ex: HttpException) {
                result.postValue(Resource.Error("Error deleting balance."))
            } catch (t: SocketTimeoutException) {
                result.postValue(Resource.Error(App.instance.getString(R.string.connection_timeout)))
            } catch (d: UnknownHostException) {
                result.postValue(Resource.Error(App.instance.getString(R.string.unknown_host)))
            }
        }

        return result
    }

    fun add(balance: Balance): LiveData<Resource<Void>> {
        val result: MutableLiveData<Resource<Void>> = MutableLiveData()
        result.postValue(Resource.Loading())

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val response = client.balance(balance)
                    balanceDao.insert(response)
                    result.postValue(Resource.Success())
                    balanceList.postValue(Resource.Success(balanceDao.get()))
                }

            } catch (e: HttpException) {
                result.postValue(Resource.Error("Error saving balance."))
            } catch (t: SocketTimeoutException) {
                result.postValue(
                    Resource.Error(
                        App.instance.getString(R.string.connection_timeout)
                    )
                )
            } catch (d: UnknownHostException) {
                result.postValue(
                    Resource.Error(
                        App.instance.getString(R.string.unknown_host)
                    )
                )
            }
        }
        return result
    }

    fun update(balance: Balance): LiveData<Resource<Void>> {
        val result: MutableLiveData<Resource<Void>> = MutableLiveData()
        result.postValue(Resource.Loading())

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val response = client.balance(balance.id!!, balance)
                    balanceDao.update(response)
                    result.postValue(Resource.Success())
                    balanceList.postValue(Resource.Success(balanceDao.get()))
                }

            } catch (e: HttpException) {
                result.postValue(Resource.Error("Error updating balance."))
            } catch (t: SocketTimeoutException) {
                result.postValue(
                    Resource.Error(
                        App.instance.getString(R.string.connection_timeout)
                    )
                )
            } catch (d: UnknownHostException) {
                result.postValue(
                    Resource.Error(
                        App.instance.getString(R.string.unknown_host)
                    )
                )
            }
        }
        return result
    }

    fun refresh() {
        balanceList.postValue(Resource.Loading())
        
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val response = client.balance()
                    balanceDao.deleteAndCreate(response)
                    prefs.balanceFetchDate = LocalDate.now().toEpochMilli()
                    balanceList.postValue(Resource.Success(response))
                }
            } catch (ex: Exception) {
                println(ex)
            }
        }
    }
}