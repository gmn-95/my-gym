package com.gmn95.mygym.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gmn95.mygym.R
import com.gmn95.mygym.data.repository.WorkoutFolderSummary
import com.gmn95.mygym.feature.home.HomeMessage
import com.gmn95.mygym.feature.home.HomeUiState
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onCreateFolder: (String) -> Unit,
    onOpenFolder: (String) -> Unit,
    onDuplicateFolder: (String, String) -> Unit,
    onMoveFolder: (String, Int) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenSettings: () -> Unit,
    onResumeActiveSession: () -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var folderToDelete by remember { mutableStateOf<WorkoutFolderSummary?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val drawerState = androidx.compose.material3.rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val message = when (uiState.message) {
        HomeMessage.OPERATION_FAILED -> stringResource(R.string.operation_error)
        null -> null
    }
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            onMessageShown()
        }
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    stringResource(R.string.navigation_title),
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.history)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenHistory()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.calendar)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenCalendar()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.backup_short)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenBackup()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.settings_short)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenSettings()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        },
    ) {
        Scaffold(
            modifier = modifier,
            snackbarHost = { SnackbarHost(snackbar) },
            floatingActionButton = {
                ExtendedFloatingActionButton(onClick = { showCreateDialog = true }) {
                    Text(stringResource(R.string.create_folder))
                }
            },
        ) { innerPadding ->
            Column(Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_menu),
                        contentDescription = stringResource(R.string.open_menu),
                    )
                }
                Text(
                    stringResource(R.string.my_workouts),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            Text(stringResource(R.string.home_description), Modifier.padding(bottom = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            uiState.activeSession?.let { active ->
                Card(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.active_session_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            active.workoutName,
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = onResumeActiveSession,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                        ) {
                            Text(stringResource(R.string.resume_workout))
                        }
                    }
                }
            }
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                uiState.folders.isEmpty() -> EmptyFolders()
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(uiState.folders, key = WorkoutFolderSummary::id) { folder ->
                        val folderIndex = uiState.folders.indexOf(folder)
                        val duplicateName = stringResource(R.string.folder_copy_name, folder.name)
                        FolderCard(
                            folder = folder,
                            onOpen = { onOpenFolder(folder.id) },
                            onDuplicate = { onDuplicateFolder(folder.id, duplicateName) },
                            canMoveUp = folderIndex > 0,
                            canMoveDown = folderIndex < uiState.folders.lastIndex,
                            onMoveUp = { onMoveFolder(folder.id, -1) },
                            onMoveDown = { onMoveFolder(folder.id, 1) },
                            onDelete = { folderToDelete = folder },
                        )
                    }
                }
            }
        }
        }
    }
    if (showCreateDialog) {
        NameDialog(stringResource(R.string.create_folder), stringResource(R.string.folder_name), { showCreateDialog = false }) {
            onCreateFolder(it)
            showCreateDialog = false
        }
    }
    folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text(stringResource(R.string.delete_folder_title)) },
            text = { Text(stringResource(R.string.delete_folder_message, folder.name)) },
            confirmButton = { Button(onClick = { onDeleteFolder(folder.id); folderToDelete = null }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { folderToDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun FolderCard(
    folder: WorkoutFolderSummary,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column(Modifier.padding(16.dp)) {
            Text(folder.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(pluralStringResource(R.plurals.workout_count, folder.workoutCount, folder.workoutCount), Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onOpen, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text(stringResource(R.string.workouts))
            }
            OutlinedButton(
                onClick = onDuplicate,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.duplicate))
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                TextButton(
                    onClick = onMoveUp,
                    modifier = Modifier.weight(1f),
                    enabled = canMoveUp,
                ) {
                    Text(stringResource(R.string.move_up))
                }
                TextButton(
                    onClick = onMoveDown,
                    modifier = Modifier.weight(1f),
                    enabled = canMoveDown,
                ) {
                    Text(stringResource(R.string.move_down))
                }
            }
            TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.delete))
            }
        }
    }
}

@Composable
private fun EmptyFolders() {
    Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.no_folders_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.no_folders_description), Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NameDialog(title: String, label: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(name, { name = it }, label = { Text(label) }, singleLine = true) },
        confirmButton = { Button(onClick = { onConfirm(name) }, enabled = name.trim().isNotEmpty()) { Text(stringResource(R.string.create)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
