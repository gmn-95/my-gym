package com.gmn95.mygym.data.repository

import com.gmn95.mygym.data.local.AppSettingsDao
import com.gmn95.mygym.data.local.AppSettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppSettingsRepository(
    private val dao: AppSettingsDao,
) {
    fun observe(): Flow<AppSettingsEntity> = dao.observe().map { it ?: AppSettingsEntity() }

    suspend fun setThemeMode(mode: String) {
        require(mode in THEMES)
        dao.upsert((dao.get() ?: AppSettingsEntity()).copy(themeMode = mode))
    }

    suspend fun setLanguageMode(mode: String) {
        require(mode in LANGUAGES)
        dao.upsert((dao.get() ?: AppSettingsEntity()).copy(languageMode = mode))
    }

    companion object {
        val THEMES = setOf("SYSTEM", "LIGHT", "DARK")
        val LANGUAGES = setOf("SYSTEM", "EN", "PT_BR")
    }
}
