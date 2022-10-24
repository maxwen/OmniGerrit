package org.omnirom.omnigerrit.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.omnirom.omnigerrit.retrofit.GerritApi
import org.omnirom.omnigerrit.utils.BuildImageUtils
import org.omnirom.omnigerrit.utils.LogUtils
import org.omnirom.omnigerrit.utils.NetworkUtils
import org.omnirom.omniota.model.ConnectivityObserver
import org.omnirom.omniota.model.RetrofitManager

class MainViewModel() : ViewModel() {
    val TAG = "MainViewModel"

    var changesPager = getChangesPaging()

    val gerritApi: GerritApi = RetrofitManager.getGerritInstance().create(GerritApi::class.java)

    private val _change = MutableStateFlow<Change?>(null)
    val changeFlow = _change.asStateFlow()

    val reloadFlow = MutableSharedFlow<Boolean>()

    val buildsTimestampList = mutableListOf<Long>()
    var buildsMap = mapOf<Long, BuildImage>()

    private val initialIsConnected by lazy {
        NetworkUtils.connectivityObserver.peekStatus()
    }
    private var _isConnected =
        MutableStateFlow(initialIsConnected == ConnectivityObserver.Status.Available)
    val isConnected = _isConnected.asStateFlow()

    init {
        viewModelScope.launch {
            NetworkUtils.connectivityObserver.observe().collectLatest { status ->
                val connected = status == ConnectivityObserver.Status.Available
                if (initialIsConnected != ConnectivityObserver.Status.Available && connected) {
                    reload()
                }
                _isConnected.value = connected
            }
        }
        initDeviceBuilds()
    }

    private fun getChangesPaging(): Flow<PagingData<Change>> {
        return Pager(
            config = PagingConfig(
                pageSize = ChangesPagingSource.PAGE_SIZE,
                enablePlaceholders = false,
                initialLoadSize = ChangesPagingSource.PAGE_SIZE
            ),
            pagingSourceFactory = { ChangesPagingSource(this) }
        ).flow.cachedIn(viewModelScope)
    }

    fun reload() {
        LogUtils.d(TAG, "reload")
        initDeviceBuilds()
        _change.value = null
        viewModelScope.launch {
            reloadFlow.emit(true)
        }
    }

    fun loadChange(change: Change) {
        viewModelScope.launch {
            val changes = gerritApi.getChanges(params = change.id, options = listOf("CURRENT_REVISION"))
            if (changes.isSuccessful && changes.body() != null) {
                val changeList = changes.body()!!
                if (changeList.size == 1) {
                    val change = changeList.first()
                    val commit = gerritApi.getCommit(change.id, change.current_revision)
                    if (commit.isSuccessful && commit.body() != null) {
                        val commit = commit.body()
                        change.commit = commit
                    }
                    _change.value = change
                    LogUtils.d(TAG, "change = " + changeFlow.value)
                }
            }
        }
    }


    private fun initDeviceBuilds() {
        viewModelScope.launch {
            runBlocking {
                buildsTimestampList.clear()
                buildsMap =
                    BuildImageUtils.getDeviceBuildsMap(BuildImageUtils.getDeviceBuilds())
                LogUtils.d(TAG, "buildsMap = " + buildsMap)

                buildsTimestampList.addAll(buildsMap.keys.sorted().reversed())
                LogUtils.d(TAG, "builds = " + buildsTimestampList)
            }
        }
    }
}