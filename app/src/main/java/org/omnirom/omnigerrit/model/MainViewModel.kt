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

    var buildsMap = mapOf<Long, BuildImage>()

    private val initialIsConnected by lazy {
        NetworkUtils.connectivityObserver.peekStatus()
    }
    private var _isConnected =
        MutableStateFlow(initialIsConnected == ConnectivityObserver.Status.Available)
    val isConnected = _isConnected.asStateFlow()

    private val _queryString = MutableStateFlow<String>("")
    val queryString = _queryString.asStateFlow()

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
                    val changeDetail = changeList.first()
                    val commit = gerritApi.getCommit(changeDetail.id, changeDetail.current_revision)
                    if (commit.isSuccessful && commit.body() != null) {
                        val commit = commit.body()
                        changeDetail.commit = commit
                    }
                    _change.value = changeDetail
                    LogUtils.d(TAG, "changeDetail = " + changeFlow.value)
                    return@launch
                }
            }
            _change.value = null
        }
    }


    private fun initDeviceBuilds() {
        viewModelScope.launch {
            runBlocking {
                buildsMap =
                    BuildImageUtils.getDeviceBuildsMap(BuildImageUtils.getDeviceBuilds())
                LogUtils.d(TAG, "buildsMap = " + buildsMap)
            }
        }
    }

    fun setQueryString(q : String) {
        _queryString.value = q
    }
}