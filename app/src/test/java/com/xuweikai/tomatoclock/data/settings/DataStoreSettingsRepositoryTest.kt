package com.xuweikai.tomatoclock.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.xuweikai.tomatoclock.core.model.AppSettings
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreSettingsRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun returnsDefaultSettingsWhenNothingSaved() = runBlocking {
        val repository = createRepository()

        assertEquals(AppSettings(), repository.getSettings())
    }

    @Test
    fun saveThenReadKeepsSettings() = runBlocking {
        val repository = createRepository()
        val settings = AppSettings(
            focusDurationMin = 45,
            shortBreakDurationMin = 10,
            longBreakDurationMin = 20,
            longBreakInterval = 6,
            alertSound = "soft",
            vibrationEnabled = false,
        )

        repository.saveSettings(settings)

        assertEquals(settings, repository.getSettings())
    }

    @Test(expected = IllegalArgumentException::class)
    fun saveRejectsInvalidSettings() = runBlocking {
        val repository = createRepository()

        repository.saveSettings(AppSettings(focusDurationMin = 0))
    }

    @Test
    fun alertSoundSelectionPersists() = runBlocking {
        val repository = createRepository()

        repository.saveSettings(AppSettings(alertSound = "pulse"))

        assertEquals("pulse", repository.getSettings().alertSound)
    }

    private fun createRepository(): DataStoreSettingsRepository {
        val file = File(temporaryFolder.newFolder(), "settings.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { file },
        )
        return DataStoreSettingsRepository(dataStore)
    }
}
