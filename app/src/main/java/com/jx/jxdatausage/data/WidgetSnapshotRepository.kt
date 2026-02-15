package com.jx.jxdatausage.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class WidgetSnapshotRepository(private val context: Context) {

    val snapshotFlow: Flow<WidgetSnapshot?> = context.appDataStore.data.map { prefs ->
        prefs[SNAPSHOT_KEY]?.let(::decodeSnapshot)
    }

    suspend fun saveSnapshot(snapshot: WidgetSnapshot) {
        context.appDataStore.edit { prefs ->
            prefs[SNAPSHOT_KEY] = encodeSnapshot(snapshot)
        }
    }

    suspend fun readSnapshot(): WidgetSnapshot? {
        val raw = context.appDataStore.data.first()[SNAPSHOT_KEY] ?: return null
        return decodeSnapshot(raw)
    }

    private fun encodeSnapshot(snapshot: WidgetSnapshot): String {
        val root = JSONObject()
        root.put("generatedAtMs", snapshot.generatedAtMs)
        root.put("dailyTotalBytes", snapshot.dailyTotalBytes)
        val topAppsArray = JSONArray()
        snapshot.topApps.forEach { app ->
            topAppsArray.put(
                JSONObject().apply {
                    put("uid", app.uid)
                    put("appName", app.appName)
                    put("packageName", app.packageName)
                    put("foregroundRx", app.foregroundRx)
                    put("foregroundTx", app.foregroundTx)
                    put("backgroundRx", app.backgroundRx)
                    put("backgroundTx", app.backgroundTx)
                    put("totalBytes", app.totalBytes)
                }
            )
        }
        root.put("topApps", topAppsArray)
        return root.toString()
    }

    private fun decodeSnapshot(raw: String): WidgetSnapshot? {
        return runCatching {
            val root = JSONObject(raw)
            val appsArray = root.optJSONArray("topApps") ?: JSONArray()
            val apps = buildList {
                for (index in 0 until appsArray.length()) {
                    val item = appsArray.getJSONObject(index)
                    add(
                        WidgetRankItem(
                            uid = item.optInt("uid", -1),
                            appName = item.optString("appName", "Unknown App"),
                            packageName = item.optString("packageName").ifBlank { null },
                            foregroundRx = item.optLong("foregroundRx", 0L),
                            foregroundTx = item.optLong("foregroundTx", 0L),
                            backgroundRx = item.optLong("backgroundRx", 0L),
                            backgroundTx = item.optLong("backgroundTx", 0L),
                            totalBytes = item.optLong("totalBytes", 0L)
                        )
                    )
                }
            }
            WidgetSnapshot(
                generatedAtMs = root.optLong("generatedAtMs", 0L),
                dailyTotalBytes = root.optLong("dailyTotalBytes", 0L),
                topApps = apps
            )
        }.getOrNull()
    }

    companion object {
        private val SNAPSHOT_KEY = stringPreferencesKey("widget_snapshot_json")
    }
}

