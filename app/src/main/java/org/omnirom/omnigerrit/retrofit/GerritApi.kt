/*
 *  Copyright (C) 2022 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.omnirom.omnigerrit.retrofit

import org.omnirom.omnigerrit.model.Change
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query


interface GerritApi {
    @GET("changes/")
    suspend fun getChanges(
        @Query("q", encoded=true) params: String,
        @Query("n") limit: String = "10",
        @Query("S") offset: String = "0",
        @Query("o") options: List<String> = listOf()
    ): Response<List<Change>>

    @GET("changes/{change-id}/detail")
    suspend fun getChange(
        @Path("change-id") changeId: String,
    ): Response<Change>
}