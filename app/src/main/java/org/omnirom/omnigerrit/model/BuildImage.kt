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

import androidx.annotation.Keep
import org.omnirom.omnigerrit.utils.BuildImageUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Keep
data class BuildImage(val filename: String, val timestamp: Long, val size: Long) :
    Comparable<BuildImage> {

    override fun compareTo(other: BuildImage): Int {
        return other.getBuildDate().compareTo(getBuildDate())
    }

    fun getVersion(): Version {
        return try {
            Version(filename.splitToSequence('-').elementAt(1))
        } catch (e: Exception) {
            Version("0")
        }
    }

    fun getBuildDate(): String {
        return try {
            filename.splitToSequence('-').elementAt(2)
        } catch (e: Exception) {
            ""
        }
    }

    fun getBuildDateInMillis() : Long {
        val buildDate = getBuildDate()
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

    fun getDevice(): String {
        return try {
            filename.splitToSequence('-').elementAt(3)
        } catch (e: Exception) {
            ""
        }
    }

    fun getBuildType(): String {
        return try {
            filename.splitToSequence('-').elementAt(4).splitToSequence('.').first()
        } catch (e: Exception) {
            ""
        }
    }

    fun getBuildSizeInMB() : Long {
        return size / 1024 / 1024
    }
}