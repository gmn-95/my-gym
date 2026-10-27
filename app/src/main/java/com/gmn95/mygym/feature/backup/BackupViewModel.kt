package com.gmn95.mygym.feature.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.gmn95.mygym.data.backup.WorkoutBackupRepository
import com.gmn95.mygym.data.backup.WorkoutBackupSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class BackupMessage { EXPORTED, RESTORED, INVALID_FILE, OPERATION_FAILED }

data class BackupUiState(
    val isWorking: Boolean = false,
    val message: BackupMessage? = null,
    val pendingImport: WorkoutBackupSummary? = null,
)

class BackupViewModel(
    private val repository: WorkoutBackupRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = mutableUiState.asStateFlow()
    private var pendingJson: String? = null

    fun exportTo(uri: Uri, resolver: ContentResolver) {
        viewModelScope.launch {
            mutableUiState.value = BackupUiState(isWorking = true)
            runCatching {
                val json = withContext(Dispatchers.IO) { repository.createJson() }
                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(json) }
                        ?: error("Não foi possível criar o arquivo")
                }
            }.onSuccess {
                mutableUiState.value = BackupUiState(message = BackupMessage.EXPORTED)
            }.onFailure {
                mutableUiState.value = BackupUiState(message = BackupMessage.OPERATION_FAILED)
            }
        }
    }

    fun inspectImport(uri: Uri, resolver: ContentResolver) {
        viewModelScope.launch {
            mutableUiState.value = BackupUiState(isWorking = true)
            runCatching {
                val json = withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                        val content = StringBuilder()
                        val buffer = CharArray(8_192)
                        while (true) {
                            val count = reader.read(buffer)
                            if (count < 0) break
                            require(content.length + count <= MAX_BACKUP_CHARACTERS)
                            content.append(buffer, 0, count)
                        }
                        content.toString()
                    } ?: error("Não foi possível ler o arquivo")
                }
                val summary = withContext(Dispatchers.IO) { repository.validate(json) }
                json to summary
            }.onSuccess { (json, summary) ->
                pendingJson = json
                mutableUiState.value = BackupUiState(pendingImport = summary)
            }.onFailure {
                pendingJson = null
                mutableUiState.value = BackupUiState(message = BackupMessage.INVALID_FILE)
            }
        }
    }

    fun confirmImport() {
        val json = pendingJson ?: return
        viewModelScope.launch {
            mutableUiState.value = BackupUiState(isWorking = true)
            runCatching { withContext(Dispatchers.IO) { repository.restore(json) } }
                .onSuccess {
                    pendingJson = null
                    mutableUiState.value = BackupUiState(message = BackupMessage.RESTORED)
                }
                .onFailure {
                    mutableUiState.value = BackupUiState(message = BackupMessage.INVALID_FILE)
                }
        }
    }

    fun cancelImport() {
        pendingJson = null
        mutableUiState.value = BackupUiState()
    }

    fun clearMessage() {
        mutableUiState.value = mutableUiState.value.copy(message = null)
    }

    companion object {
        private const val MAX_BACKUP_CHARACTERS = 10_000_000

        fun factory(repository: WorkoutBackupRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras,
                ): T = BackupViewModel(repository) as T
            }
    }
}
