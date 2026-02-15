package com.jx.jxdatausage.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.appDataStore by preferencesDataStore(name = "jx_data_usage_prefs")

