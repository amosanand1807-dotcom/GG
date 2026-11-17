package com.example.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.cityLoopDataStore: DataStore<Preferences> by preferencesDataStore(name = "cityloop_prefs")

enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(raw: String?): ThemeMode = entries.firstOrNull { it.key == raw } ?: SYSTEM
    }
}

enum class AppLanguage(val code: String, val label: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "हिन्दी");

    companion object {
        fun fromCode(raw: String?): AppLanguage = entries.firstOrNull { it.code == raw } ?: ENGLISH
    }
}

class PreferencesStore(private val context: Context) {
    private val keyLanguage = stringPreferencesKey("language_code")
    private val keyTheme = stringPreferencesKey("theme_mode")
    private val keySelectedCity = stringPreferencesKey("selected_city")

    val languageFlow: Flow<AppLanguage> = context.cityLoopDataStore.data.map { prefs ->
        AppLanguage.fromCode(prefs[keyLanguage])
    }

    val themeModeFlow: Flow<ThemeMode> = context.cityLoopDataStore.data.map { prefs ->
        ThemeMode.fromKey(prefs[keyTheme])
    }

    val selectedCityFlow: Flow<String> = context.cityLoopDataStore.data.map { prefs ->
        prefs[keySelectedCity] ?: CityData.defaultCity
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.cityLoopDataStore.edit { prefs ->
            prefs[keyLanguage] = language.code
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.cityLoopDataStore.edit { prefs ->
            prefs[keyTheme] = mode.key
        }
    }

    suspend fun setSelectedCity(city: String) {
        context.cityLoopDataStore.edit { prefs ->
            prefs[keySelectedCity] = city
        }
    }

    fun customAvatarUriFlow(userId: String): Flow<String?> {
        val key = stringPreferencesKey("custom_avatar_uri_$userId")
        return context.cityLoopDataStore.data.map { prefs -> prefs[key] }
    }

    suspend fun setCustomAvatarUri(userId: String, uri: String?) {
        val key = stringPreferencesKey("custom_avatar_uri_$userId")
        context.cityLoopDataStore.edit { prefs ->
            if (uri.isNullOrBlank()) prefs.remove(key) else prefs[key] = uri
        }
    }
}
