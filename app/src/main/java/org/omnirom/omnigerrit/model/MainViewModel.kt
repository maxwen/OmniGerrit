package org.omnirom.omnigerrit.model

import android.util.Log
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
import org.omnirom.omniota.model.RetrofitManager

class MainViewModel() : ViewModel() {
    val TAG = "MainViewModel"

    private val changeList = mutableListOf<Change>()
    private val _changes = MutableStateFlow<List<Change>>(changeList)
    val changesFlow = _changes.asStateFlow()
    var changesPager: Flow<PagingData<Change>>? = null
    val gerritApi: GerritApi = RetrofitManager.getInstance().create(GerritApi::class.java)
    private val _change = MutableStateFlow<Change?>(null)
    val changeFlow = _change.asStateFlow()

    init {
        initChangesPaging()
    }

    fun loadChanges() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val changes = gerritApi.getChanges(
                    params = createQueryString(
                        branch = "android-13.0",
                        status = "merged"
                    ), "10", "0"
                )
                if (changes.isSuccessful && changes.body() != null) {
                    _changes.value = changes.body()!!
                }
            }
        }
    }

    fun createQueryString(branch: String = "", project: String = "", status: String = ""): String {
        val q = mutableListOf<String>()
        if (branch.isNotEmpty()) {
            q.add("branch:" + branch)
        }
        if (project.isNotEmpty()) {
            q.add("project:" + project)
        }
        if (status.isNotEmpty()) {
            q.add("status:" + status)
        }
        return q.joinToString(separator = "+")
    }

    fun getQueryOptionsList() : List<String> {
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
                pageSize = 25,
                enablePlaceholders = false,
                initialLoadSize = 25
            ),
            pagingSourceFactory = { ChangesPagingSource(this) }
        ).flow
    }

    fun loadChange(change: Change) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val change = gerritApi.getChange(changeId = change.change_id)
                if (change.isSuccessful && change.body() != null) {
                    _change.value = change.body()!!
                    Log.d(TAG, "change = " + changeFlow.value)
                }
            }
        }
    }
}