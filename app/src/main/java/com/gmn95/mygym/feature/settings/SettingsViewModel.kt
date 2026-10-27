package com.gmn95.mygym.feature.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.gmn95.mygym.data.local.AppSettingsEntity
import com.gmn95.mygym.data.repository.AppSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: AppSettingsRepository,
) : ViewModel() {
    val settings: StateFlow<AppSettingsEntity> = repository.observe().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettingsEntity(),
    )

    fun setThemeMode(mode: String) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setLanguageMode(mode: String) {
        viewModelScope.launch {
            repository.setLanguageMode(mode)
            AppCompatDelegate.setApplicationLocales(
                when (mode) {
                    LANGUAGE_ENGLISH -> LocaleListCompat.forLanguageTags("en")
                    LANGUAGE_PORTUGUESE -> LocaleListCompat.forLanguageTags("pt-BR")
                    else -> LocaleListCompat.getEmptyLocaleList()
                },
            )
        }
    }

    companion object {
        const val THEME_SYSTEM = "SYSTEM"
        const val THEME_LIGHT = "LIGHT"
        const val THEME_DARK = "DARK"
        const val LANGUAGE_SYSTEM = "SYSTEM"
        const val LANGUAGE_ENGLISH = "EN"
        const val LANGUAGE_PORTUGUESE = "PT_BR"

        fun factory(repository: AppSettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras,
                ): T = SettingsViewModel(repository) as T
            }
    }
}
