package com.jx.jxdatausage.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.flow.first
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

    @Test
    fun monthlyCap_persists_and_clears(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repository = SettingsRepository(context)

        repository.clearMonthlyCap()
        assertNull(repository.monthlyCapBytes.first())
        assertEquals(DataUnit.GB, repository.monthlyCapUnit.first())

        repository.setMonthlyCap(5L * 1024L * 1024L * 1024L, DataUnit.GB)
        assertEquals(5L * 1024L * 1024L * 1024L, repository.monthlyCapBytes.first())
        assertEquals(DataUnit.GB, repository.monthlyCapUnit.first())

        repository.setMonthlyCap(500L * 1024L * 1024L, DataUnit.MB)
        assertEquals(500L * 1024L * 1024L, repository.monthlyCapBytes.first())
        assertEquals(DataUnit.MB, repository.monthlyCapUnit.first())

        repository.clearMonthlyCap()
        assertNull(repository.monthlyCapBytes.first())
    }

    @Test
    fun themeMode_defaultsSystem_and_persists(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repository = SettingsRepository(context)

        repository.setThemeMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, repository.themeMode.first())

        repository.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repository.themeMode.first())

        repository.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, repository.themeMode.first())

        repository.setThemeMode(ThemeMode.SYSTEM)
    }
}
