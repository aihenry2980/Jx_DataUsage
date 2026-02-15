package com.jx.jxdatausage.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryInstrumentedTest {

    @Test
    fun showWifiUsage_defaultsFalse_and_persists(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repository = SettingsRepository(context)

        repository.setShowWifiUsage(false)
        val defaultValue = repository.getShowWifiUsage()
        assertFalse(defaultValue)

        repository.setShowWifiUsage(true)
        val updatedValue = repository.getShowWifiUsage()
        assertTrue(updatedValue)

        // Cleanup to avoid leaking state to other tests.
        repository.setShowWifiUsage(false)
    }
}

