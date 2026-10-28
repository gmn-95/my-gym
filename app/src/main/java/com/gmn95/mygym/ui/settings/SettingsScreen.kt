package com.gmn95.mygym.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gmn95.mygym.R
import com.gmn95.mygym.data.local.AppSettingsEntity
import com.gmn95.mygym.feature.settings.SettingsViewModel
import com.gmn95.mygym.ui.components.BackNavigationHeader

@Composable
fun SettingsScreen(
    settings: AppSettingsEntity,
    onBack: () -> Unit,
    onThemeSelected: (String) -> Unit,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    Scaffold(modifier = modifier) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            BackNavigationHeader(
                title = stringResource(R.string.settings_title),
                onBack = onBack,
                horizontalPadding = 0.dp,
            )
            Text(
                text = stringResource(R.string.theme_setting),
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            SettingOption(
                label = stringResource(R.string.theme_system),
                selected = settings.themeMode == SettingsViewModel.THEME_SYSTEM,
                onClick = { onThemeSelected(SettingsViewModel.THEME_SYSTEM) },
            )
            SettingOption(
                label = stringResource(R.string.theme_light),
                selected = settings.themeMode == SettingsViewModel.THEME_LIGHT,
                onClick = { onThemeSelected(SettingsViewModel.THEME_LIGHT) },
            )
            SettingOption(
                label = stringResource(R.string.theme_dark),
                selected = settings.themeMode == SettingsViewModel.THEME_DARK,
                onClick = { onThemeSelected(SettingsViewModel.THEME_DARK) },
            )
            Text(
                text = stringResource(R.string.language_setting),
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            SettingOption(
                label = stringResource(R.string.language_system),
                selected = settings.languageMode == SettingsViewModel.LANGUAGE_SYSTEM,
                onClick = { onLanguageSelected(SettingsViewModel.LANGUAGE_SYSTEM) },
            )
            SettingOption(
                label = stringResource(R.string.language_english),
                selected = settings.languageMode == SettingsViewModel.LANGUAGE_ENGLISH,
                onClick = { onLanguageSelected(SettingsViewModel.LANGUAGE_ENGLISH) },
            )
            SettingOption(
                label = stringResource(R.string.language_portuguese),
                selected = settings.languageMode == SettingsViewModel.LANGUAGE_PORTUGUESE,
                onClick = { onLanguageSelected(SettingsViewModel.LANGUAGE_PORTUGUESE) },
            )
            Text(
                text = stringResource(R.string.catalog_credits_title),
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.catalog_attribution),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SettingOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
