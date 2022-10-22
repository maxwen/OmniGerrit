/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.omnirom.omnigerrit.model

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.omnirom.omnigerrit.utils.LogUtils

class ChangesPagingSource(private val viewModel: MainViewModel) :
    PagingSource<Int, Change>() {
    val TAG = "ChangesPagingSource"
    private var offset: Int = 0

    companion object {
        val STARTING_KEY = 0
        val GERRIT_QUERY_LIMIT = 20
        val PAGE_SIZE = 20
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Change> {
        return try {
            withContext(Dispatchers.IO) {
                // If params.key is null, it is the first load, so we start loading with STARTING_KEY
                val startKey = params.key ?: STARTING_KEY
                val pageIndex = startKey

                val queryResultList = fillResultList()

                val newCount = queryResultList.size
                val prevKey = if (pageIndex == 0) null else pageIndex - 1
                val nextKey = if (queryResultList.isEmpty()) null else pageIndex + 1

                LoadResult.Page(
                    data = queryResultList,
                    prevKey = prevKey,
                    nextKey = nextKey,
                )
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "load " + e.message, e)

            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Change>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    private suspend fun fillResultList(): List<Change> {
        val queryResultList = mutableListOf<Change>()

        while (queryResultList.size < 10) {
            val changes = viewModel.gerritApi.getChanges(
                ChangeFilter.createQueryString(), GERRIT_QUERY_LIMIT.toString(), offset = offset.toString()
            )
            if (changes.isSuccessful && changes.body() != null) {
                val changeList = changes.body()!!
                if (changeList.isEmpty()) {
                    break
                } else {
                    queryResultList.addAll(changeList.filter { !it.project.startsWith("android_device_") })
                    offset += GERRIT_QUERY_LIMIT
                }
            }
        }
        return queryResultList
    }
}
