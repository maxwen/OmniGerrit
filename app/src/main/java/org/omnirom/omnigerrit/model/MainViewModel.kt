package org.omnirom.omnigerrit.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.omnirom.omnigerrit.retrofit.GerritApi
import org.omnirom.omnigerrit.utils.LogUtils
import org.omnirom.omniota.model.RetrofitManager

class MainViewModel() : ViewModel() {
    val TAG = "MainViewModel"

    private val changeList = mutableListOf<Change>()
    private val _changes = MutableStateFlow<List<Change>>(changeList)
    val changesFlow = _changes.asStateFlow()
    var changesPager: Flow<PagingData<Change>>? = null
    val gerritApi: GerritApi = RetrofitManager.getGerritInstance().create(GerritApi::class.java)
    private val _change = MutableStateFlow<Change?>(null)
    val changeFlow = _change.asStateFlow()

    init {
        initChangesPaging()
    }

    fun loadChanges() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val changes = gerritApi.getChanges(
                    params = ChangeFilter.createQueryString(), limit = "10", offset = "0"
                )
                if (changes.isSuccessful && changes.body() != null) {
                    _changes.value = changes.body()!!
                }
            }
        }
    }

    fun getQueryOptionsList(): List<String> {
        val optionsList = mutableListOf<String>()
        optionsList.add("CURRENT_REVISION")
        optionsList.add("CURRENT_COMMIT")
        optionsList.add("MESSAGES")
        optionsList.add("DETAILED_ACCOUNT")
        return optionsList
    }

    private fun initChangesPaging() {
        changesPager = Pager(
            config = PagingConfig(
                pageSize = ChangesPagingSource.PAGE_SIZE,
                enablePlaceholders = false,
                initialLoadSize = ChangesPagingSource.PAGE_SIZE
            ),
            pagingSourceFactory = { ChangesPagingSource(this) }
        ).flow
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
}