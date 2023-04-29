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
package org.omnirom.omnigerrit.model

import android.content.Context
import org.omnirom.omnigerrit.utils.BuildImageUtils
import org.omnirom.omnigerrit.utils.DeviceUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object Device {
    fun getDevice(context: Context): String = DeviceUtils.getProperty(context, "ro.omni.device")

    private fun getFullVersion(context: Context): String = DeviceUtils.getProperty(context, "ro.omni.version")

    fun getVersion(context: Context): Version {
        return try {
            Version(getFullVersion(context).splitToSequence('-').elementAt(0))
        } catch (e: Exception) {
            return Version("")
        }
    }

    fun getBuildType(context: Context): String {
        return try {
            getFullVersion(context).splitToSequence('-').elementAt(3)
        } catch (e: Exception) {
            return ""
        }
    }

    fun getBuildDate(context: Context): String {
        return try {
            getFullVersion(context).splitToSequence('-').elementAt(1)
        } catch (e: Exception) {
            return ""
        }
    }

    fun getBuildDateInMillis(context: Context) : Long {
        val buildDate = getBuildDate(context)
        if (buildDate.isNotEmpty()) {
            return try {
                // new format
                LocalDateTime.parse(buildDate, DateTimeFormatter.ofPattern(BuildImageUtils.otaDateTimeFormat)).toInstant(
                    ZoneOffset.UTC).toEpochMilli()
            } catch (e: DateTimeParseException) {
                // fallback to old format
                LocalDate.parse(buildDate, DateTimeFormatter.ofPattern(BuildImageUtils.otaDateFormat)).atTime(0,0).toInstant(
                    ZoneOffset.UTC).toEpochMilli()
            }
        }
        return 0
    }

    fun getBranch(context: Context): String = DeviceUtils.getProperty(context, "ro.omni.branch", "android-13.0")

}