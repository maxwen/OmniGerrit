package org.omnirom.omnigerrit.model

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.TextFieldColors
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
    val buildsMapLoaded = MutableStateFlow<Boolean>(false)

    private val initialIsConnected by lazy {
        NetworkUtils.connectivityObserver.peekStatus()
    }
    private var _isConnected =
        MutableStateFlow(initialIsConnected == ConnectivityObserver.Status.Available)
    val isConnected = _isConnected.asStateFlow()

    val queryString = MutableStateFlow<String>("")
    val queryDateAfter = MutableStateFlow<Long>(0)
    val projectFilter = MutableStateFlow<Boolean>(true)
    val queryBranch = MutableStateFlow<String>("")

    val snackbarShow = MutableSharedFlow<String>()

    val queryFilter = combine(
        queryString,
        queryDateAfter,
        projectFilter,
        queryBranch
    ) { queryString, queryDateAfter, projectFilter, queryBranch ->
        ChangeFilter.QueryFilter(queryString, queryDateAfter, projectFilter, queryBranch)
    }.stateIn(CoroutineScope(Dispatchers.Default), SharingStarted.WhileSubscribed(), ChangeFilter.QueryFilter())

    init {
        viewModelScope.launch {
            projectFilter.value = Settings.isProjectFilter(true)
            queryDateAfter.value = Settings.getDateAfter(0)
            queryBranch.value = Settings.getBranch(ChangeFilter.defaultBranch)
        }
        viewModelScope.launch {
            NetworkUtils.connectivityObserver.observe().collectLatest { status ->
                val connected = status == ConnectivityObserver.Status.Available
                if (initialIsConnected != ConnectivityObserver.Status.Available && connected) {
                    reload()
                }
                _isConnected.value = connected
            }
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                initDeviceBuildSync()
                ChangeFilter.loadProjectFilterConfigFile()
                if (!ChangeFilter.hasDeviceConfig()) {
                    snackbarShow.emit("No device config for " + BuildImageUtils.device)
                }
                triggerReload.emit(true)
            }
        }
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
                initDeviceBuildSync()
                triggerReload.emit(true)
            }
        }
    }

    private suspend fun initDeviceBuildSync() {
        buildsMap =
            BuildImageUtils.getDeviceBuildsMap(BuildImageUtils.getDeviceBuilds())
        LogUtils.d(TAG, "buildsMap = " + buildsMap)
        buildsMapLoaded.value = true
    }

    fun setQueryString(text: String) {
        queryString.value = text
    }

    fun setQueryDateAfter(dateInMillis: Long) {
        queryDateAfter.value = dateInMillis
        viewModelScope.launch {
            Settings.setDateAfter(dateInMillis)
        }
    }

    fun setProjectFilter(enabled: Boolean) {
        projectFilter.value = enabled
        viewModelScope.launch {
            Settings.setProjectFilter(enabled)
        }
    }

    fun setBranch(value: String) {
        queryBranch.value = value
        viewModelScope.launch {
            Settings.setBranch(value)
        }
    }

    fun showSnackbarMessage(message: String) {
        viewModelScope.launch {
            snackbarShow.emit(message)
        }
    }

    fun getQueryFilter() : ChangeFilter.QueryFilter {
        return ChangeFilter.QueryFilter(queryString.value, queryDateAfter.value,
            projectFilter.value,
            queryBranch.value, queryProject = "")
    }
}