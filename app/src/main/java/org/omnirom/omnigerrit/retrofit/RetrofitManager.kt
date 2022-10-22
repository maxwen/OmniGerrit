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
package org.omnirom.omniota.model

import android.content.Context
import com.google.gson.GsonBuilder
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.omnirom.omnigerrit.utils.BuildImageUtils
import org.omnirom.omnigerrit.utils.LogUtils
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException


object RetrofitManager {
    private val TAG = "RetrofitManager"

    val gerritBaseUrl = "https:/gerrit.omnirom.org/"
    val otaBaseUrl = "https://dl.omnirom.org/"
    var deviceRootDir: String? = null

    class GerritMagicHaderInterceptor : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val response = chain.proceed(request)
            if (response.code == 200) {
                val jsonString = response.body!!.string().replace(")]}'\n", "").trim()
                val contentType: MediaType? = response.body!!.contentType()
                val body = jsonString.toResponseBody(contentType)
                return response.newBuilder().body(body).build()
            }
            return response
        }
    }

    fun getGerritInstance(): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BASIC

        val oktHttpClient = OkHttpClient.Builder()
            .addInterceptor(GerritMagicHaderInterceptor())
            .addInterceptor(loggingInterceptor)

        val gson = GsonBuilder()
            .setLenient()
            .create()

        return Retrofit.Builder().baseUrl(gerritBaseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(oktHttpClient.build())
            .build()
    }

    private fun getOtaBaseUrl(rootDir: String?): String {
        if (rootDir != null && rootDir.isNotEmpty()) {
            return "$otaBaseUrl$rootDir/"
        }
        return otaBaseUrl
    }

    fun getOtaInstance(): Retrofit {
        if (deviceRootDir == null) {
            BuildImageUtils.findDeviceRootDir()
            LogUtils.d(TAG, "findDeviceRootDir = " + deviceRootDir)
        }
        val oktHttpClient = OkHttpClient.Builder()

        return Retrofit.Builder().baseUrl(getOtaBaseUrl(deviceRootDir))
            .addConverterFactory(GsonConverterFactory.create())
            .client(oktHttpClient.build())
            .build()
    }

    fun getOtaInstance(rootDir: String): Retrofit {
        val oktHttpClient = OkHttpClient.Builder()

        return Retrofit.Builder().baseUrl(getOtaBaseUrl(rootDir))
            .addConverterFactory(GsonConverterFactory.create())
            .client(oktHttpClient.build())
            .build()
    }

}