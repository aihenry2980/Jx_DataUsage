package com.jx.jxdatausage.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SettingsRepository(private val context: Context) {

    val showWifiUsage: Flow<Boolean> = context.appDataStore.data.map { prefs ->
        prefs[SHOW_WIFI_USAGE_KEY] ?: false
    }

    suspend fun setShowWifiUsage(enabled: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[SHOW_WIFI_USAGE_KEY] = enabled
        }
    }

    suspend fun getShowWifiUsage(): Boolean {
        return context.appDataStore.data.first()[SHOW_WIFI_USAGE_KEY] ?: false
    }

    companion object {
        val SHOW_WIFI_USAGE_KEY = booleanPreferencesKey("show_wifi_usage")
    }
}

