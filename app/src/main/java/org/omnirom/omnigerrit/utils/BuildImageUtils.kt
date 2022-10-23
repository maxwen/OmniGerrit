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


object BuildImageUtils {
    private val TAG = "BuildImageUtils"

    var device: String = ""
    var buildType: String= ""
    var version: Version = Version("0")

    suspend fun getDeviceBuilds(): List<BuildImage> {
        val omniOtaApi =
            RetrofitManager.getOtaInstance().create(OmniOtaApi::class.java)
        try {
            val reponse = omniOtaApi.getBuilds()
            if (reponse.isSuccessful && reponse.body() != null) {
                val buildList = reponse.body()!!
                if (buildList.containsKey(device)) {
                    val deviceBuilds = buildList[device]!!.filter { image ->
                        image.getBuildType() == buildType && image.getVersion() == version
                    }
                    return deviceBuilds
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "getDeviceBuilds", e)
        }
        return listOf()
    }

    fun getDeviceBuildsMap(builds: List<BuildImage>) : Map<Long, BuildImage> {
        val buildsMap = mutableMapOf<Long, BuildImage>()
        builds.forEach { build ->  buildsMap.put(build.getBuildDateInMillis(), build)}
        return buildsMap
    }

    private suspend fun scanDeviceBuilds(rootDir: String): Boolean {
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
            var buildImageFound = scanDeviceBuilds("")
            if (!buildImageFound) {
                buildImageFound = scanDeviceBuilds("tmp")
                if (buildImageFound) {
                    RetrofitManager.deviceRootDir = "tmp"
                    return@runBlocking
                }
            }
            RetrofitManager.deviceRootDir = ""
        }
    }
}