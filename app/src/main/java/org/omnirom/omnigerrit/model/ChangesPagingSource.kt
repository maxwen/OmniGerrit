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

class ChangesPagingSource(private val viewModel: MainViewModel) :
    PagingSource<Int, Change>() {
    val TAG = "ChangesPagingSource"

    companion object {
        val STARTING_KEY = 0
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Change> {
        return try {
            withContext(Dispatchers.IO) {
                // If params.key is null, it is the first load, so we start loading with STARTING_KEY
                val startKey = params.key ?: STARTING_KEY
                val pageIndex = startKey
                val offset = pageIndex * params.loadSize

                val queryResultList = mutableListOf<Change>()

                val changes = viewModel.gerritApi.getChanges(
                    viewModel.createQueryString(
                        branch = "android-13.0",
                        status = "merged"
                    ), "10", offset = offset.toString()
                )
                if (changes.isSuccessful && changes.body() != null) {
                    queryResultList.addAll(changes.body()!!)
                }

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
            Log.e(TAG, "load " + e.message, e)

            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Change>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}
