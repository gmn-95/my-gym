package com.gmn95.mygym.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gmn95.mygym.MyGymApplication
import com.gmn95.mygym.feature.editor.WorkoutEditorViewModel
import com.gmn95.mygym.feature.home.HomeViewModel
import com.gmn95.mygym.feature.session.SessionViewModel
import com.gmn95.mygym.ui.editor.WorkoutEditorScreen
import com.gmn95.mygym.ui.home.HomeScreen
import com.gmn95.mygym.ui.session.SessionScreen
import com.gmn95.mygym.ui.theme.MyGymTheme

@Composable
fun MyGymApp() {
    MyGymTheme {
        val application = LocalContext.current.applicationContext as MyGymApplication
        var selectedWorkoutId by rememberSaveable { mutableStateOf<String?>(null) }
        var showActiveSession by rememberSaveable { mutableStateOf(false) }

        val workoutId = selectedWorkoutId
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
                    onSetExerciseCompleted = sessionViewModel::setExerciseCompleted,
                    onStartRest = sessionViewModel::startRest,
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

            workoutId == null -> {
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.factory(
                    workoutRepository = application.workoutRepository,
                    sessionRepository = application.sessionRepository,
                ),
            )
            val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
            HomeScreen(
                uiState = uiState,
                onCreateWorkout = { name ->
                    homeViewModel.createWorkout(name) { createdId ->
                        selectedWorkoutId = createdId
                    }
                },
                onOpenWorkout = { selectedWorkoutId = it },
                onDuplicateWorkout = homeViewModel::duplicateWorkout,
                onDeleteWorkout = homeViewModel::deleteWorkout,
                onStartWorkout = { id ->
                    homeViewModel.startWorkout(id) {
                        showActiveSession = true
                    }
                },
                onResumeActiveSession = { showActiveSession = true },
                onFinishActiveAndStart = { id ->
                    homeViewModel.finishActiveAndStart(id) {
                        showActiveSession = true
                    }
                },
                onMessageShown = homeViewModel::clearMessage,
            )
            }

            else -> {
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
        }
    }
}
