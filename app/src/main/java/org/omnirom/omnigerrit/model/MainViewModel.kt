package org.omnirom.omnigerrit.model

import androidx.compose.material.BottomSheetState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.omnirom.omnigerrit.retrofit.GerritApi
import org.omnirom.omnigerrit.utils.BuildImageUtils
import org.omnirom.omnigerrit.utils.LogUtils
import org.omnirom.omnigerrit.utils.NetworkUtils
import org.omnirom.omniota.model.ConnectivityObserver
import org.omnirom.omniota.model.RetrofitManager

@OptIn(ExperimentalMaterialApi::class)
class MainViewModel() : ViewModel() {
    val TAG = "MainViewModel"

    var changesPager = getChangesPaging()

    val gerritApi: GerritApi = RetrofitManager.getGerritInstance().create(GerritApi::class.java)

    private val _changeDetail = MutableStateFlow<Change?>(null)
    val changeDetail = _changeDetail.asStateFlow()

    val triggerReload = MutableSharedFlow<Boolean>()

    var buildsMap = mapOf<Long, BuildImage>()
    private var _buildsMapLoaded = MutableStateFlow<Boolean>(false)
    val buildsMapLoaded = _buildsMapLoaded.asStateFlow()

    private val initialIsConnected by lazy {
        NetworkUtils.connectivityObserver.peekStatus()
    }
    private var _isConnected =
        MutableStateFlow(initialIsConnected == ConnectivityObserver.Status.Available)
    val isConnected = _isConnected.asStateFlow()

    val queryString = MutableStateFlow<String>("")
    val queryDateAfter = MutableStateFlow<String>("")
    val projectFilter = MutableStateFlow<Boolean>(false)

    private var _snackbarShow = MutableSharedFlow<String>()
    val snackbarShow = _snackbarShow.asSharedFlow()

    val queryFilter = combine(
        queryString,
        queryDateAfter,
        projectFilter
    ) { queryString, queryDateAfter, projectFilter ->
        ChangeFilter.QueryFilter(queryString, queryDateAfter, projectFilter)
    }.stateIn(CoroutineScope(Dispatchers.Default), SharingStarted.WhileSubscribed(), ChangeFilter.QueryFilter())

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
        //_changeDetail.value = null
        viewModelScope.launch {
            triggerReload.emit(true)
        }
    }

    fun loadChange(change: Change) {
        viewModelScope.launch {
            //_changeDetail.value = null
            val changes = gerritApi.getChanges(
                params = change.id,
                options = listOf("CURRENT_REVISION", "DETAILED_ACCOUNTS")
            )
            if (changes.isSuccessful && changes.body() != null) {
                val changeList = changes.body()!!
                if (changeList.size == 1) {
                    val changeDetail = changeList.first()
                    val commit = gerritApi.getCommit(changeDetail.id, changeDetail.current_revision)
                    if (commit.isSuccessful && commit.body() != null) {
                        changeDetail.commit = commit.body()
                    }
                    _changeDetail.value = changeDetail
                    LogUtils.d(TAG, "changeDetail = " + this@MainViewModel.changeDetail.value)
                    return@launch
                }
            }
        }
    }


    private fun initDeviceBuilds() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                buildsMap =
                    BuildImageUtils.getDeviceBuildsMap(BuildImageUtils.getDeviceBuilds())
                LogUtils.d(TAG, "buildsMap = " + buildsMap)
                _buildsMapLoaded.value = true
                triggerReload.emit(true)
            }
        }
    }

    fun setQueryString(q: String) {
        queryString.value = q
    }

    fun setQueryDateAfter(date: String) {
        queryDateAfter.value = date
    }

    fun toggleProjectFilter() {
        projectFilter.value = !projectFilter.value
    }

    fun showSnackbarMessage(message: String) {
        viewModelScope.launch {
            _snackbarShow.emit(message)
        }
    }
}