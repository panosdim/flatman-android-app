package com.panosdim.flatman.rest

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.panosdim.flatman.App
import com.panosdim.flatman.R
import com.panosdim.flatman.db
import com.panosdim.flatman.db.dao.FlatDao
import com.panosdim.flatman.model.Flat
import com.panosdim.flatman.prefs
import com.panosdim.flatman.rest.data.Resource
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

class FlatRepository {
    private var client: Webservice = webservice
    private val scope = CoroutineScope(Dispatchers.Main)
    private val flatDao: FlatDao = db.flatDao()

    fun getAllFlats(): LiveData<List<Flat>> {
        refreshFlats()
        return flatDao.getAll()
    }

    private fun refreshFlats() {
        if (prefs.flatFetchDate == -1L ||
            ChronoUnit.DAYS.between(fromEpochMilli(prefs.flatFetchDate), LocalDate.now()) >= 1
        ) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        flatDao.deleteAndCreate(client.flat())
                        prefs.flatFetchDate = LocalDate.now().toEpochMilli()
                    }
                } catch (e: HttpException) {
                    println(e)
                } catch (t: SocketTimeoutException) {
                    println(t)
                } catch (d: UnknownHostException) {
                    println(d)
                }
            }
        }
    }

    fun delete(flat: Flat): LiveData<Resource<Flat>> {
        val result: MutableLiveData<Resource<Flat>> = MutableLiveData()
        result.postValue(Resource.Loading())

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val response = client.flat(flat.id!!)
                    when (response.code()) {
                        204 -> {
                            result.postValue(Resource.Success())
                            flatDao.delete(flat)
                        }
                        404 -> {
                            result.postValue(Resource.Error("Error deleting flat. Flat not found."))
                        }
                    }
                }
            } catch (ex: HttpException) {
                result.postValue(Resource.Error("Error deleting flat."))
            } catch (t: SocketTimeoutException) {
                result.postValue(Resource.Error(App.instance.getString(R.string.connection_timeout)))
            } catch (d: UnknownHostException) {
                result.postValue(Resource.Error(App.instance.getString(R.string.unknown_host)))
            }
        }

        return result
    }

    fun add(flat: Flat): LiveData<Resource<Flat>> {
        val result: MutableLiveData<Resource<Flat>> = MutableLiveData()
        result.postValue(Resource.Loading())

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val response = client.flat(flat)
                    result.postValue(Resource.Success())
                    flatDao.insert(response)
                }

            } catch (e: HttpException) {
                result.postValue(Resource.Error("Error saving flat."))
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

    fun update(flat: Flat): LiveData<Resource<Flat>> {
        val result: MutableLiveData<Resource<Flat>> = MutableLiveData()
        result.postValue(Resource.Loading())

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val response = client.flat(flat.id!!, flat)
                    result.postValue(Resource.Success())
                    flatDao.update(response)
                }

            } catch (e: HttpException) {
                result.postValue(Resource.Error("Error updating flat."))
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
}
