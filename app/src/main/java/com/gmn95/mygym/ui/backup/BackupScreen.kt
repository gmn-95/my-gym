package com.gmn95.mygym.ui.backup

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmn95.mygym.R
import com.gmn95.mygym.feature.backup.BackupMessage
import com.gmn95.mygym.feature.backup.BackupViewModel

@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val resolver = context.contentResolver
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { viewModel.exportTo(it, resolver) } }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.inspectImport(it, resolver) } }
    val messageText = uiState.message?.let { message ->
        stringResource(
            when (message) {
                BackupMessage.EXPORTED -> R.string.backup_exported
                BackupMessage.RESTORED -> R.string.backup_restored
                BackupMessage.INVALID_FILE -> R.string.backup_invalid_file
                BackupMessage.OPERATION_FAILED -> R.string.operation_error
            },
        )
    }
    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                text = stringResource(R.string.backup_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { exportLauncher.launch("my-gym-backup.json") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isWorking,
            ) {
                Text(stringResource(R.string.export_backup))
            }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isWorking,
            ) {
                Text(stringResource(R.string.import_backup))
            }
            if (uiState.isWorking) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }

    uiState.pendingImport?.let { summary ->
        AlertDialog(
            onDismissRequest = viewModel::cancelImport,
            title = { Text(stringResource(R.string.confirm_import_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.confirm_import_message,
                        summary.workoutCount,
                        summary.customExerciseCount,
                        summary.completedSessionCount,
                    ),
                )
            },
            confirmButton = {
                Button(onClick = viewModel::confirmImport) {
                    Text(stringResource(R.string.replace_data))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelImport) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
