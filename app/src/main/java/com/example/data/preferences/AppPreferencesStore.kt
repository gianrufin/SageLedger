package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.ThemeModePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "sage_ledger_prefs")

class AppPreferencesStore(private val context: Context) {
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val dismissedReleaseTagKey = stringPreferencesKey("dismissed_release_tag")
    private val onboardingCompleteKey = androidx.datastore.preferences.core.booleanPreferencesKey("onboarding_complete")

    val themeModeFlow: Flow<ThemeModePreference> = context.appDataStore.data.map { prefs ->
        ThemeModePreference.fromRaw(prefs[themeModeKey])
    }

    val dismissedReleaseTagFlow: Flow<String?> = context.appDataStore.data.map { prefs ->
        prefs[dismissedReleaseTagKey]
    }

    val onboardingCompleteFlow: Flow<Boolean> = context.appDataStore.data.map { prefs ->
        prefs[onboardingCompleteKey] == true
    }

    suspend fun setThemeMode(mode: ThemeModePreference) {
        context.appDataStore.edit { prefs ->
            prefs[themeModeKey] = mode.name
        }
    }

    suspend fun dismissReleaseTag(tag: String) {
        context.appDataStore.edit { prefs ->
            prefs[dismissedReleaseTagKey] = tag
        }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[onboardingCompleteKey] = complete
        }
    }
}
