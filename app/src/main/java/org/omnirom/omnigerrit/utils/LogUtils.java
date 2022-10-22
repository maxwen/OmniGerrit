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
package org.omnirom.omnigerrit.utils;

import android.util.Log;

public class LogUtils {

    public final static String LOGTAG = "OmniGerrit";
    public final static boolean DEBUG = true;

    public static void v(String message, Object... args) {
        if (DEBUG || Log.isLoggable(LOGTAG, Log.VERBOSE)) {
            Log.v(LOGTAG, args == null ? message : strFormat(message, args));
        }
    }

    public static void v(String tag, String message, Object... args) {
        if (DEBUG || Log.isLoggable(LOGTAG, Log.VERBOSE)) {
            Log.v(LOGTAG + "/" + tag, args == null ? message : strFormat(message, args));
        }
    }

    public static void d(String message, Object... args) {
        if (DEBUG || Log.isLoggable(LOGTAG, Log.DEBUG)) {
            Log.d(LOGTAG, args == null ? message : strFormat(message, args));
        }
    }

    public static void d(String tag, String message, Object... args) {
        if (DEBUG || Log.isLoggable(LOGTAG, Log.DEBUG)) {
            Log.d(LOGTAG + "/" + tag, args == null ? message : strFormat(message, args));
        }
    }

    public static void i(String message, Object... args) {
        if (DEBUG || Log.isLoggable(LOGTAG, Log.INFO)) {
            Log.i(LOGTAG, args == null ? message : strFormat(message, args));
        }
    }

    public static void i(String tag, String message, Object... args) {
        if (DEBUG || Log.isLoggable(LOGTAG, Log.INFO)) {
            Log.i(LOGTAG + "/" + tag, args == null ? message : strFormat(message, args));
        }
    }

    public static void w(String message, Object... args) {
        if (DEBUG || Log.isLoggable(LOGTAG, Log.WARN)) {
            Log.w(LOGTAG, args == null ? message : strFormat(message, args));
        }
    }

    public static void w(String tag, String message, Object... args) {
        if (DEBUG || Log.isLoggable(LOGTAG, Log.WARN)) {
            Log.w(LOGTAG + "/" + tag, args == null ? message : strFormat(message, args));
        }
    }

    public static void e(String message, Object... args) {
        if (DEBUG || Log.isLoggable(LOGTAG, Log.ERROR)) {
            Log.e(LOGTAG, args == null ? message : strFormat(message, args));
        }
    }

    public static void e(String tag, String message, Object... args) {
        if (DEBUG || Log.isLoggable(LOGTAG, Log.ERROR)) {
            Log.e(LOGTAG + "/" + tag, args == null ? message : strFormat(message, args));
        }
    }

    public static void e(String message, Exception e) {
        if (DEBUG || Log.isLoggable(LOGTAG, Log.ERROR)) {
            Log.e(LOGTAG, message, e);
        }
    }

    public static void e(String tag, String message, Exception e) {
        if (DEBUG || Log.isLoggable(LOGTAG, Log.ERROR)) {
            Log.e(LOGTAG + "/" + tag, message, e);
        }
    }

    public static void wtf(String message, Object... args) {
        if (DEBUG || Log.isLoggable(LOGTAG, Log.ASSERT)) {
            Log.wtf(LOGTAG, args == null ? message : strFormat(message, args));
        }
    }

    public static void wtf(String tag, String message, Object... args) {
        if (DEBUG || Log.isLoggable(LOGTAG, Log.ASSERT)) {
            Log.wtf(LOGTAG + "/" + tag, args == null ? message : strFormat(message, args));
        }
    }

    private static String strFormat(String message, Object... args) {
        try {
            return String.format(message, args);
        } catch (Exception e) {
        }
        return message;
    }
}
