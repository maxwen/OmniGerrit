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

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.omnirom.omnigerrit.utils.LogUtils

class ChangesPagingSource(private val viewModel: MainViewModel) :
    PagingSource<Int, Change>() {
    val TAG = "ChangesPagingSource"
    private var offset: Int = 0
    private val buildsTimestampList = mutableListOf<Long>()

    init {
        buildsTimestampList.addAll(viewModel.buildsMap.keys.sorted().reversed())
    }


    companion object {
        val STARTING_KEY = 0
        val GERRIT_QUERY_LIMIT = 30
        val PAGE_SIZE = 30
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Change> {
        return try {
            // If params.key is null, it is the first load, so we start loading with STARTING_KEY
            val startKey = params.key ?: STARTING_KEY
            val pageIndex = startKey

            val queryResultList = mutableListOf<Change>()
            var itemsBefore = 0;
            if (viewModel.isConnected.value && viewModel.changeListReady.value) {
                fillResultList(queryResultList)
            } else {
                itemsBefore = PAGE_SIZE
            }

            val newCount = queryResultList.size
            val prevKey = if (pageIndex == 0) null else pageIndex - 1
            val nextKey = if (queryResultList.isEmpty()) null else pageIndex + 1
            val itemsAfter = 0

            LogUtils.d(
                TAG,
                "queryResultList = " + pageIndex + " " + startKey + " " + newCount
            )
            LoadResult.Page(
                data = queryResultList,
                prevKey = prevKey,
                nextKey = nextKey,
                itemsAfter = itemsAfter,
                itemsBefore = itemsBefore,
            )
        } catch (e: Exception) {
            LogUtils.e(TAG, "load " + e.message, e)

            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Change>): Int? {
        // always use null!!!
        return null
    }

    private suspend fun fillResultList(queryResultList: MutableList<Change>) {
        val queryFilter = viewModel.getQueryFilter()
        LogUtils.d(TAG, "queryFilter = $queryFilter")
        while (queryResultList.size < PAGE_SIZE) {
            val changes = viewModel.gerritApi.getChanges(
                ChangeFilter.createQueryString(queryFilter),
                options = listOf("DETAILED_ACCOUNTS"),
                limit = GERRIT_QUERY_LIMIT.toString(),
                offset = offset.toString()
            )
            if (changes.isSuccessful && changes.body() != null) {
                val changeList = changes.body()!!
                if (changeList.isEmpty()) {
                    break
                } else {
                    if (queryFilter.projectFilter) {
                        queryResultList.addAll(changeList.filter { ChangeFilter.showProject(it.project) })
                    } else {
                        queryResultList.addAll(changeList)
                    }
                    offset += GERRIT_QUERY_LIMIT
                }
            }
        }
        if (queryResultList.isNotEmpty() && buildsTimestampList.isNotEmpty() && viewModel.queryString.value.isEmpty()) {
            val addedBuilds = mutableListOf<Long>()
            val queryResultListCopy = mutableListOf<Change>()
            queryResultListCopy.addAll(queryResultList)
            val firstChangeDate = queryResultListCopy.first().updatedInMillis
            val lastChangeDate = queryResultListCopy.last().updatedInMillis
            buildsTimestampList.filter { buildTime -> buildTime > firstChangeDate }
                .forEach { buildTime ->
                    queryResultList.add(
                        addedBuilds.size,
                        Change(viewModel.buildsMap[buildTime]!!)
                    )
                    addedBuilds.add(buildTime)
                }
            buildsTimestampList.removeAll(addedBuilds)

            queryResultListCopy.forEachIndexed() { index, change ->
                val changeTime = change.updatedInMillis
                // [0,1,2,3,4,5]
                // size = 6
                // 4 -> 5
                val nextChange =
                    if (index <= queryResultListCopy.size - 2) queryResultListCopy[index + 1] else null
                val nextChangeTime =
                    nextChange?.updatedInMillis ?: 0L

                buildsTimestampList.filter { buildTime -> buildTime > lastChangeDate && buildTime in (nextChangeTime + 1) until changeTime }
                    .forEach { buildTime ->
                        queryResultList.add(
                            index + addedBuilds.size + 1,
                            Change(viewModel.buildsMap[buildTime]!!)
                        )
                        addedBuilds.add(buildTime)
                    }
            }
            buildsTimestampList.removeAll(addedBuilds)
        }
    }
}
