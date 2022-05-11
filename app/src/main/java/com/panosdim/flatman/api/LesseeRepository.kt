package com.panosdim.flatman.api

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.panosdim.flatman.App
import com.panosdim.flatman.R
import com.panosdim.flatman.api.data.PostalCodeResponse
import com.panosdim.flatman.api.data.Resource
import com.panosdim.flatman.db
import com.panosdim.flatman.db.dao.LesseeDao
import com.panosdim.flatman.model.Flat
import com.panosdim.flatman.model.Lessee
import com.panosdim.flatman.prefs
import com.panosdim.flatman.utils.fromEpochMilli
import com.panosdim.flatman.utils.toEpochMilli
import kotlinx.coroutines.*
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class LesseeRepository {
    private var client: Webservice = webservice
    private var postalClient: PostalCodeService = postalCodeService
    private val scope = CoroutineScope(Dispatchers.Main)
    private val lesseeDao: LesseeDao = db.lesseeDao()
    private val result: MutableLiveData<Resource<List<Lessee>>> = MutableLiveData()

    fun getLiveData(): LiveData<List<Lessee>> {
        return lesseeDao.getLiveData()
    }

    fun get(): LiveData<Resource<List<Lessee>>> {
        result.postValue(Resource.Loading())

        if (prefs.lesseeFetchDate == -1L ||
            ChronoUnit.DAYS.between(fromEpochMilli(prefs.lesseeFetchDate), LocalDate.now()) >= 1
        ) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val response = client.lessee()
                        lesseeDao.deleteAndCreate(response)
                        prefs.lesseeFetchDate = LocalDate.now().toEpochMilli()
                        result.postValue(Resource.Success(response))
                    }
                } catch (ex: Exception) {
                    withContext(Dispatchers.IO) {
                        result.postValue(Resource.Success(lesseeDao.get()))
                    }
                }
            }
        }

        scope.launch {
            withContext(Dispatchers.IO) {
                result.postValue(Resource.Success(lesseeDao.get()))
            }
        }

        return result
    }

    fun delete(lessee: Lessee): LiveData<Resource<Lessee>> {
        val result: MutableLiveData<Resource<Lessee>> = MutableLiveData()
        result.postValue(Resource.Loading())

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val response = client.lessee(lessee.id!!)
                    when (response.code()) {
                        204 -> {
                            lesseeDao.delete(lessee)
                            result.postValue(Resource.Success())
                        }
                        404 -> {
                            result.postValue(Resource.Error("Error deleting lessee. Lessee not found."))
                        }
                    }
                }
            } catch (ex: HttpException) {
                result.postValue(Resource.Error("Error deleting lessee."))
            } catch (t: SocketTimeoutException) {
                result.postValue(Resource.Error(App.instance.getString(R.string.connection_timeout)))
            } catch (d: UnknownHostException) {
                result.postValue(Resource.Error(App.instance.getString(R.string.unknown_host)))
            }
        }

        return result
    }

    fun add(lessee: Lessee): LiveData<Resource<Lessee>> {
        val result: MutableLiveData<Resource<Lessee>> = MutableLiveData()
        result.postValue(Resource.Loading())

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val response = client.lessee(lessee)
                    lesseeDao.insert(response)
                    result.postValue(Resource.Success())
                }

            } catch (e: HttpException) {
                result.postValue(Resource.Error("Error saving lessee."))
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

    fun update(lessee: Lessee): LiveData<Resource<Lessee>> {
        val result: MutableLiveData<Resource<Lessee>> = MutableLiveData()
        result.postValue(Resource.Loading())

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val response = client.lessee(lessee.id!!, lessee)
                    lesseeDao.update(response)
                    result.postValue(Resource.Success())
                }

            } catch (e: HttpException) {
                result.postValue(Resource.Error("Error updating lessee."))
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
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val response = client.lessee()
                    lesseeDao.deleteAndCreate(response)
                    prefs.lesseeFetchDate = LocalDate.now().toEpochMilli()
                }
            } catch (ex: Exception) {
                println(ex)
            }
        }
    }

    fun getPostalCode(address: String): PostalCodeResponse? {
        var response: PostalCodeResponse?
        runBlocking {
            response = try {
                postalClient.getPostalCode(address)
            } catch (ex: Exception) {
                null
            }
        }
        return response
    }

    fun getMonthlyRent(flat: Flat): Int? {
        var response: Int?
        runBlocking {
            response = try {
                flat.id?.let { flatId ->
                    val lessees = lesseeDao.getLessees(flatId)
                    val today = LocalDate.now()

                    val lessee = lessees.first {
                        today.isBefore(LocalDate.parse(it.until))
                    }
                    lessee.rent
                }
            } catch (ex: Exception) {
                null
            }
        }
        return response
    }
}