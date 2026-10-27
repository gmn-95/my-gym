package com.gmn95.mygym.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.gmn95.mygym.MyGymApplication
import com.gmn95.mygym.feature.editor.WorkoutEditorViewModel
import com.gmn95.mygym.feature.folder.WorkoutFolderViewModel
import com.gmn95.mygym.feature.backup.BackupViewModel
import com.gmn95.mygym.feature.home.HomeViewModel
import com.gmn95.mygym.feature.settings.SettingsViewModel
import com.gmn95.mygym.feature.history.HistoryViewModel
import com.gmn95.mygym.feature.history.HistoryDetailViewModel
import com.gmn95.mygym.feature.session.SessionViewModel
import com.gmn95.mygym.ui.editor.WorkoutEditorScreen
import com.gmn95.mygym.ui.backup.BackupScreen
import com.gmn95.mygym.ui.history.HistoryScreen
import com.gmn95.mygym.ui.history.HistoryDetailScreen
import com.gmn95.mygym.ui.history.CalendarScreen
import com.gmn95.mygym.ui.home.HomeScreen
import com.gmn95.mygym.ui.folder.WorkoutFolderScreen
import com.gmn95.mygym.ui.session.SessionScreen
import com.gmn95.mygym.ui.settings.SettingsScreen
import com.gmn95.mygym.ui.theme.MyGymTheme

@Composable
fun MyGymApp() {
    val application = LocalContext.current.applicationContext as MyGymApplication
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(application.appSettingsRepository),
    )
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    LaunchedEffect(settings.languageMode) {
        val preferredLocales = when (settings.languageMode) {
            SettingsViewModel.LANGUAGE_ENGLISH -> LocaleListCompat.forLanguageTags("en")
            SettingsViewModel.LANGUAGE_PORTUGUESE -> LocaleListCompat.forLanguageTags("pt-BR")
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() !=
            preferredLocales.toLanguageTags()
        ) {
            AppCompatDelegate.setApplicationLocales(preferredLocales)
        }
    }
    val darkTheme = when (settings.themeMode) {
        SettingsViewModel.THEME_LIGHT -> false
        SettingsViewModel.THEME_DARK -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    MyGymTheme(darkTheme = darkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            var selectedWorkoutId by rememberSaveable { mutableStateOf<String?>(null) }
            var selectedFolderId by rememberSaveable { mutableStateOf<String?>(null) }
            var showActiveSession by rememberSaveable { mutableStateOf(false) }
            var showHistory by rememberSaveable { mutableStateOf(false) }
            var showCalendar by rememberSaveable { mutableStateOf(false) }
        var showBackup by rememberSaveable { mutableStateOf(false) }
            var showSettings by rememberSaveable { mutableStateOf(false) }
            var selectedHistorySessionId by rememberSaveable { mutableStateOf<String?>(null) }

            val workoutId = selectedWorkoutId
            val folderId = selectedFolderId
            when {
            showActiveSession -> {
                val sessionViewModel: SessionViewModel = viewModel(
                    factory = SessionViewModel.factory(
                        repository = application.sessionRepository,
                        clock = application.sessionClock,
                    ),
                )
                val uiState by sessionViewModel.uiState.collectAsStateWithLifecycle()
                SessionScreen(
                    uiState = uiState,
                    onBack = { showActiveSession = false },
                    onPauseSession = sessionViewModel::pauseSession,
                    onResumeSession = sessionViewModel::resumeSession,
                    onSetSeriesCompleted = sessionViewModel::setExerciseSetCompleted,
                    onSetExerciseCompleted = sessionViewModel::setExerciseCompleted,
                    onPauseRest = sessionViewModel::pauseRest,
                    onResumeRest = sessionViewModel::resumeRest,
                    onResetRest = sessionViewModel::resetRest,
                    onEndRest = sessionViewModel::endRest,
                    onCompleteSession = {
                        sessionViewModel.completeSession {
                            showActiveSession = false
                        }
                    },
                    onCancelSession = {
                        sessionViewModel.cancelSession {
                            showActiveSession = false
                        }
                    },
                    onOperationErrorShown = sessionViewModel::clearOperationError,
                )
            }

            showBackup -> {
                val backupViewModel: BackupViewModel = viewModel(
                    factory = BackupViewModel.factory(application.workoutBackupRepository),
                )
                BackupScreen(
                    viewModel = backupViewModel,
                    onBack = { showBackup = false },
                )
            }

            showSettings -> {
                SettingsScreen(
                    settings = settings,
                    onBack = { showSettings = false },
                    onThemeSelected = settingsViewModel::setThemeMode,
                    onLanguageSelected = settingsViewModel::setLanguageMode,
                )
            }

            selectedHistorySessionId != null -> {
                val sessionId = requireNotNull(selectedHistorySessionId)
                val historyDetailViewModel: HistoryDetailViewModel = viewModel(
                    key = "history-session-$sessionId",
                    factory = HistoryDetailViewModel.factory(
                        sessionId = sessionId,
                        sessionRepository = application.sessionRepository,
                    ),
                )
                val uiState by historyDetailViewModel.uiState.collectAsStateWithLifecycle()
                HistoryDetailScreen(
                    uiState = uiState,
                    onBack = { selectedHistorySessionId = null },
                )
            }

            showHistory -> {
                val historyViewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModel.factory(application.sessionRepository),
                )
                val uiState by historyViewModel.uiState.collectAsStateWithLifecycle()
                HistoryScreen(
                    uiState = uiState,
                    onBack = { showHistory = false },
                    onOpenSession = { selectedHistorySessionId = it },
                )
            }

            showCalendar -> {
                val historyViewModel: HistoryViewModel = viewModel(
                    key = "calendar",
                    factory = HistoryViewModel.factory(application.sessionRepository),
                )
                val uiState by historyViewModel.uiState.collectAsStateWithLifecycle()
                CalendarScreen(
                    uiState = uiState,
                    onBack = { showCalendar = false },
                    onOpenSession = { selectedHistorySessionId = it },
                )
            }

            workoutId != null -> {
            val editorViewModel: WorkoutEditorViewModel = viewModel(
                key = "workout-editor-$workoutId",
                factory = WorkoutEditorViewModel.factory(
                    workoutId = workoutId,
                    repository = application.workoutRepository,
                ),
            )
            val uiState by editorViewModel.uiState.collectAsStateWithLifecycle()
            WorkoutEditorScreen(
                uiState = uiState,
                onBack = { selectedWorkoutId = null },
                onRenameWorkout = editorViewModel::renameWorkout,
                onCreateAndAddExercise = editorViewModel::createAndAddExercise,
                onAddExistingExercise = editorViewModel::addExistingExercise,
                onUpdateExercise = editorViewModel::updateExercise,
                onRemoveExercise = editorViewModel::removeExercise,
                onMoveExercise = editorViewModel::moveExercise,
                onOperationErrorShown = editorViewModel::clearOperationError,
            )
            }

            folderId != null -> {
                val folderViewModel: WorkoutFolderViewModel = viewModel(
                    key = "workout-folder-$folderId",
                    factory = WorkoutFolderViewModel.factory(
                        folderId = folderId,
                        workoutRepository = application.workoutRepository,
                        sessionRepository = application.sessionRepository,
                    ),
                )
                val uiState by folderViewModel.uiState.collectAsStateWithLifecycle()
                WorkoutFolderScreen(
                    uiState = uiState,
                    onBack = { selectedFolderId = null },
                    onCreateWorkout = { name ->
                        folderViewModel.createWorkout(name) { selectedWorkoutId = it }
                    },
                    onOpenWorkout = { selectedWorkoutId = it },
                    onDuplicateWorkout = folderViewModel::duplicateWorkout,
                    onDeleteWorkout = folderViewModel::deleteWorkout,
                    onStartWorkout = { id -> folderViewModel.startWorkout(id) { showActiveSession = true } },
                    onResumeActiveSession = { showActiveSession = true },
                    onFinishActiveAndStart = { id ->
                        folderViewModel.finishActiveAndStart(id) { showActiveSession = true }
                    },
                    onMessageShown = folderViewModel::clearMessage,
                )
            }

            else -> {
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.factory(
                    workoutRepository = application.workoutRepository,
                    sessionRepository = application.sessionRepository,
                ),
            )
            val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
            HomeScreen(
                uiState = uiState,
                onCreateFolder = { name ->
                    homeViewModel.createFolder(name) { createdId ->
                        selectedFolderId = createdId
                    }
                },
                onOpenFolder = { selectedFolderId = it },
                onOpenHistory = {
                    showCalendar = false
                    showHistory = true
                },
                onOpenCalendar = {
                    showHistory = false
                    showCalendar = true
                },
                onOpenBackup = { showBackup = true },
                onOpenSettings = { showSettings = true },
                onDeleteFolder = homeViewModel::deleteFolder,
                onResumeActiveSession = { showActiveSession = true },
            )
            }
            }
        }
    }
}
