package org.omnirom.omnigerrit.model

import androidx.lifecycle.*
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.omnirom.omnigerrit.retrofit.GerritApi
import org.omnirom.omnigerrit.utils.BuildImageUtils
import org.omnirom.omnigerrit.utils.LogUtils
import org.omnirom.omniota.model.RetrofitManager

class MainViewModel() : ViewModel() {
    val TAG = "MainViewModel"

    var changesPager: Flow<PagingData<Change>>? = null

    val gerritApi: GerritApi = RetrofitManager.getGerritInstance().create(GerritApi::class.java)

    private val _change = MutableStateFlow<Change?>(null)
    val changeFlow = _change.asStateFlow()

    val buildsTimestampList = mutableListOf<Long>()
    var buildsMap = mapOf<Long, BuildImage>()

    init {
        initDeviceBuilds()
        initChangesPaging()
    }

    /*fun loadChanges() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val changes = gerritApi.getChanges(
                    params = ChangeFilter.createQueryString(), limit = "10", offset = "0"
                )
                if (changes.isSuccessful && changes.body() != null) {
                    val changes = changes.body()!!
                }
            }
        }
    }*/

    private fun initChangesPaging() {
        changesPager = Pager(
            config = PagingConfig(
                pageSize = ChangesPagingSource.PAGE_SIZE,
                enablePlaceholders = false,
                initialLoadSize = ChangesPagingSource.PAGE_SIZE
            ),
            pagingSourceFactory = { ChangesPagingSource(this) }
        ).flow.cachedIn(viewModelScope)
    }

    fun loadChange(change: Change) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val change = gerritApi.getChange(id = change.id)
                if (change.isSuccessful && change.body() != null) {
                    _change.value = change.body()!!
                    LogUtils.d(TAG, "change = " + changeFlow.value)
                }
            }
        }
    }

    private fun initDeviceBuilds() {
        viewModelScope.launch {
            runBlocking {
                withContext(Dispatchers.IO) {
                    buildsMap =
                        BuildImageUtils.getDeviceBuildsMap(BuildImageUtils.getDeviceBuilds())
                    LogUtils.d(TAG, "buildsMap = " + buildsMap)

                    buildsTimestampList.addAll(buildsMap.keys.sorted().reversed())
                    LogUtils.d(TAG, "builds = " + buildsTimestampList)
                }
            }
        }
    }
}