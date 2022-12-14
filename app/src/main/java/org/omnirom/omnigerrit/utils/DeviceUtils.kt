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

import android.content.Context
import java.lang.reflect.Method

object DeviceUtils {
    private val TAG = "DeviceUtils"

    fun getProperty(
        context: Context,
        key: String,
        default: String = ""
    ): String {
        try {
            val systemProperties = context.classLoader.loadClass(
                "android.os.SystemProperties"
            )
            val get = systemProperties.getMethod(
                "get", *arrayOf<Class<*>>(
                    String::class.java, String::class.java
                )
            )
            return get.invoke(null, key, default) as String
        } catch (e: java.lang.Exception) {
            LogUtils.e(TAG, "getProperty", e)
        }
        return default
    }
}
