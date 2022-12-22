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
package org.omnirom.omnigerrit.utils

import kotlinx.coroutines.runBlocking
import org.omnirom.omnigerrit.model.BuildImage
import org.omnirom.omnigerrit.model.Version
import org.omnirom.omnigerrit.retrofit.OmniOtaApi
import org.omnirom.omniota.model.RetrofitManager
import java.text.SimpleDateFormat
import java.util.*


object BuildImageUtils {
    private val TAG = "BuildImageUtils"

    // set from app
    var device: String = ""
    var buildType: String = ""
    var version: Version = Version("0")

    val otaDateFormat by lazy {
        initDateFormat()
    }
    val otaDateTimeFormat by lazy {
        initDateTimeFormat()
    }

    private fun initDateTimeFormat(): SimpleDateFormat {
        val format = SimpleDateFormat("yyyyMMddHHmm")
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format
    }

    private fun initDateFormat(): SimpleDateFormat {
        val format = SimpleDateFormat("yyyyMMdd")
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format
    }

    suspend fun getDeviceBuilds(): List<BuildImage> {
        LogUtils.d(TAG, "getDeviceBuilds")

        val omniOtaApi =
            RetrofitManager.getOtaInstance().create(OmniOtaApi::class.java)
        try {
            val reponse = omniOtaApi.getBuilds()
            if (reponse.isSuccessful) {
                if (reponse.body() != null) {
                    val buildList = reponse.body()!!
                    if (buildList.containsKey(device)) {
                        val deviceBuilds = buildList[device]!!.filter { image ->
                            image.getBuildType() == buildType && image.getVersion() == version
                        }
                        return deviceBuilds
                    }
                }
            } else {
                LogUtils.e(TAG, "getDeviceBuilds error = ", reponse.code())
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "getDeviceBuilds", e)
        }
        return listOf()
    }

    fun getDeviceBuildsMap(builds: List<BuildImage>): Map<Long, BuildImage> {
        val buildsMap = mutableMapOf<Long, BuildImage>()
        builds.forEach { build -> buildsMap[build.getBuildDateInMillis()] = build }
        return buildsMap
    }

    private suspend fun scanDeviceBuilds(rootDir: String): Boolean {
        LogUtils.d(TAG, "scanDeviceBuilds rootDir = " + rootDir)

        val omniOtaApi =
            RetrofitManager.getOtaInstance(rootDir).create(OmniOtaApi::class.java)
        try {
            val reponse = omniOtaApi.getBuilds()
            if (reponse.isSuccessful && reponse.body() != null) {
                val buildList = reponse.body()!!
                return filterLatestDeviceBuild(buildList) != null
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "scanDeviceBuilds", e)
        }
        return false
    }

    private fun filterLatestDeviceBuild(buildList: Map<String, List<BuildImage>>): BuildImage? {
        if (buildList.containsKey(device)) {
            val deviceBuilds = buildList[device]!!.filter { image ->
                image.getBuildType() == buildType && image.getVersion() == version
            }
            return deviceBuilds.minOrNull()
        } else {
            LogUtils.d(TAG, "device = $device no build found")
        }
        return null
    }

    fun findDeviceRootDir() {
        runBlocking {
            LogUtils.d(TAG, "findDeviceRootDir")
            var buildImageFound = scanDeviceBuilds("")
            if (!buildImageFound) {
                buildImageFound = scanDeviceBuilds("tmp")
                if (buildImageFound) {
                    RetrofitManager.deviceRootDir = "tmp"
                    return@runBlocking
                }
            } else {
                RetrofitManager.deviceRootDir = ""
            }
        }
    }
}