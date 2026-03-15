package com.jx.jxdatausage.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SettingsRepository(private val context: Context) {

    val showWifiUsage: Flow<Boolean> = context.appDataStore.data.map { prefs ->
        prefs[SHOW_WIFI_USAGE_KEY] ?: false
    }

    val monthlyCapBytes: Flow<Long?> = context.appDataStore.data.map { prefs ->
        prefs[MONTHLY_CAP_BYTES_KEY]?.takeIf { it > 0L }
    }

    val monthlyCapUnit: Flow<DataUnit> = context.appDataStore.data.map { prefs ->
        when (prefs[MONTHLY_CAP_UNIT_KEY]) {
            DataUnit.MB.name -> DataUnit.MB
            DataUnit.GB.name -> DataUnit.GB
            else -> DataUnit.GB
        }
    }

    val themeMode: Flow<ThemeMode> = context.appDataStore.data.map { prefs ->
        when (prefs[THEME_MODE_KEY]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    suspend fun setShowWifiUsage(enabled: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[SHOW_WIFI_USAGE_KEY] = enabled
        }
    }

    suspend fun getShowWifiUsage(): Boolean {
        return context.appDataStore.data.first()[SHOW_WIFI_USAGE_KEY] ?: false
    }

    suspend fun setMonthlyCap(bytes: Long, preferredUnit: DataUnit) {
        require(bytes > 0L) { "Monthly cap must be greater than zero" }
        context.appDataStore.edit { prefs ->
            prefs[MONTHLY_CAP_BYTES_KEY] = bytes
            prefs[MONTHLY_CAP_UNIT_KEY] = preferredUnit.name
        }
    }

    suspend fun clearMonthlyCap() {
        context.appDataStore.edit { prefs ->
            prefs.remove(MONTHLY_CAP_BYTES_KEY)
        }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.appDataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = themeMode.name
        }
    }

    companion object {
        val SHOW_WIFI_USAGE_KEY = booleanPreferencesKey("show_wifi_usage")
        val MONTHLY_CAP_BYTES_KEY = longPreferencesKey("monthly_cap_bytes")
        val MONTHLY_CAP_UNIT_KEY = stringPreferencesKey("monthly_cap_unit")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }
}
