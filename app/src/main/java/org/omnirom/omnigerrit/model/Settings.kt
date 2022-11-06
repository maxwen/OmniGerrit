package org.omnirom.omnigerrit.model

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.first

object Settings {
    private val TAG = "Settings"

    lateinit var myDataStore: DataStore<Preferences>
    const val SETTINGS_STORE_NAME = "user_preferences"
    val PROJECT_FILTER = booleanPreferencesKey("project_filter")
    val DATE_AFTER = longPreferencesKey("date_after_millis")
    val BRANCH = stringPreferencesKey("branch")

    suspend fun setProjectFilter(value: Boolean) {
        Result.runCatching {
            myDataStore.edit { settings ->
                settings[PROJECT_FILTER] = value
            }
        }
    }

    suspend fun isProjectFilter(default: Boolean): Boolean {
        val settings = myDataStore.data.first().toPreferences()
        return settings[PROJECT_FILTER] ?: default
    }

    suspend fun setDateAfter(dateAfter: Long) {
        Result.runCatching {
            myDataStore.edit { settings ->
                settings[DATE_AFTER] = dateAfter
            }
        }
    }

    suspend fun getDateAfter(default: Long): Long {
        val settings = myDataStore.data.first().toPreferences()
        return settings[DATE_AFTER] ?: default
    }

    suspend fun setBranch(branch: String) {
        Result.runCatching {
            myDataStore.edit { settings ->
                settings[BRANCH] = branch
            }
        }
    }

    suspend fun getBranch(default: String): String {
        val settings = myDataStore.data.first().toPreferences()
        return settings[BRANCH] ?: default
    }
}
