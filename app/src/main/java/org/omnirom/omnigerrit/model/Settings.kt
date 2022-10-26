package org.omnirom.omnigerrit.model

import android.content.Context
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

object Settings {
    private val TAG = "Settings"

    lateinit var myDataStore: DataStore<Preferences>
    const val SETTINGS_STORE_NAME = "user_preferences"
    val DETAILS_HINT_SHOWN = booleanPreferencesKey("details_hint_shown")

    suspend fun setDetailsHintShown(value: Boolean) {
        val res = Result.runCatching {
            myDataStore.edit { settings ->
                settings[DETAILS_HINT_SHOWN] = value
            }
        }
    }
    suspend fun isDetailsHintShown() : Boolean {
        val settings = myDataStore.data.first().toPreferences()
        return settings[DETAILS_HINT_SHOWN] ?: false
    }
}
