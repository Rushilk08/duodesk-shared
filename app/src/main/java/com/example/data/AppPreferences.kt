package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "duodesk_prefs")

class AppPreferences(private val context: Context) {
    companion object {
        private val KEY_ACTIVE_USER = stringPreferencesKey("active_user_id")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private const val LOGGED_OUT = "__logged_out__"
    }

    val activeUserIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        when (val saved = prefs[KEY_ACTIVE_USER]) {
            null -> "rushil" // Default active demo profile on first launch, matching React app
            LOGGED_OUT -> null
            else -> saved
        }
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        if (prefs[KEY_THEME_MODE] == ThemeMode.DARK.name) ThemeMode.DARK else ThemeMode.LIGHT
    }

    suspend fun setActiveUserId(userId: String?) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_USER] = userId ?: LOGGED_OUT
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }
}
