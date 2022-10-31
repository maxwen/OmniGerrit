package org.omnirom.omnigerrit.model

import android.content.Context
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

object Settings {
    private val TAG = "Settings"

    lateinit var myDataStore: DataStore<Preferences>
    const val SETTINGS_STORE_NAME = "user_preferences"
    val PROJECT_FILTER = booleanPreferencesKey("project_filter")
    val DATE_AFTER = stringPreferencesKey("date_after")
    val BRANCH = stringPreferencesKey("branch")

    suspend fun setProjectFilter(value: Boolean) {
        val res = Result.runCatching {
            myDataStore.edit { settings ->
                settings[PROJECT_FILTER] = value
            }
        }
    }
    suspend fun isProjectFilter(default : Boolean) : Boolean {
        val settings = myDataStore.data.first().toPreferences()
        return settings[PROJECT_FILTER] ?: default
    }

    suspend fun setDateAfter(dateAfter: String) {
        val res = Result.runCatching {
            myDataStore.edit { settings ->
                settings[DATE_AFTER] = dateAfter
            }
        }
    }
    suspend fun getDateAfter(default: String) : String {
        val settings = myDataStore.data.first().toPreferences()
        return settings[DATE_AFTER] ?: default
    }

    suspend fun setBranch(branch: String) {
        val res = Result.runCatching {
            myDataStore.edit { settings ->
                settings[BRANCH] = branch
            }
        }
    }
    suspend fun getBranch(default: String) : String {
        val settings = myDataStore.data.first().toPreferences()
        return settings[BRANCH] ?: default
    }
}
