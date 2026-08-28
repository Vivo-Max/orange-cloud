package jiamin.chen.orangecloud.ui.pages

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.data.model.PagesDeployFile
import jiamin.chen.orangecloud.data.repository.AccountStore
import jiamin.chen.orangecloud.data.repository.PagesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.zip.ZipInputStream
import javax.inject.Inject

enum class DeployPhase { IDLE, PREPARING, DEPLOYING }

data class PagesDeployUiState(
    val projectName: String = "",
    val phase: DeployPhase = DeployPhase.IDLE,
    val uploaded: Int = 0,
    val total: Int = 0,
    val fileCount: Int = 0,
    val canWrite: Boolean = false,
    val error: String? = null,
) {
    val isBusy: Boolean get() = phase != DeployPhase.IDLE
}

sealed interface PagesDeployEvent {
    data object Deployed : PagesDeployEvent
}

@HiltViewModel
class PagesDeployViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val accountStore: AccountStore,
    private val repository: PagesRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val projectName: String = checkNotNull(savedStateHandle["project"])

    private val _uiState = MutableStateFlow(
        PagesDeployUiState(projectName = projectName, canWrite = authRepository.hasScope(Scopes.PAGES_WRITE)),
    )
    val uiState: StateFlow<PagesDeployUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<PagesDeployEvent>(Channel.BUFFERED)
    val events: Flow<PagesDeployEvent> = eventChannel.receiveAsFlow()

    /** 用户选中单个文件或 zip → 解析成部署文件集 → 直接上传部署。 */
    fun deployFrom(uri: Uri, isZip: Boolean) {
        if (!_uiState.value.canWrite || _uiState.value.isBusy) return
        _uiState.update { it.copy(phase = DeployPhase.PREPARING, error = null, uploaded = 0, total = 0) }
        viewModelScope.launch {
            try {
                val files = withContext(Dispatchers.IO) { readFiles(uri, isZip) }
                if (files.isEmpty()) { _uiState.update { it.copy(phase = DeployPhase.IDLE, error = context.getString(jiamin.chen.orangecloud.R.string.pages_deploy_empty)) }; return@launch }
                val big = files.firstOrNull { it.data.size > repository.maxFileBytes }
                if (big != null) {
                    _uiState.update { it.copy(phase = DeployPhase.IDLE, error = context.getString(jiamin.chen.orangecloud.R.string.pages_deploy_too_big, big.path)) }
                    return@launch
                }
                accountStore.ensureLoaded()
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                _uiState.update { it.copy(phase = DeployPhase.DEPLOYING, fileCount = files.size) }
                repository.deployFiles(accountId, projectName, files) { uploaded, total ->
                    _uiState.update { it.copy(uploaded = uploaded, total = total) }
                }
                eventChannel.send(PagesDeployEvent.Deployed)
                _uiState.update { it.copy(phase = DeployPhase.IDLE) }
            } catch (e: Exception) {
                _uiState.update { it.copy(phase = DeployPhase.IDLE, error = e.message) }
            }
        }
    }

    private fun readFiles(uri: Uri, isZip: Boolean): List<PagesDeployFile> {
        val resolver = context.contentResolver
        if (isZip) {
            val entries = ArrayList<Pair<String, ByteArray>>()
            resolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zis ->
                    var e = zis.nextEntry
                    while (e != null) {
                        if (!e.isDirectory) entries.add(e.name to zis.readBytes())
                        zis.closeEntry()
                        e = zis.nextEntry
                    }
                }
            }
            return normalize(entries)
        }
        // 单文件：放到根（/文件名）
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return emptyList()
        val name = displayName(uri) ?: "index.html"
        return listOf(PagesDeployFile("/" + name.trimStart('/'), bytes))
    }

    /** zip 路径归一化：反斜杠→正斜杠、剥共同顶层目录、统一加前导 /。 */
    private fun normalize(entries: List<Pair<String, ByteArray>>): List<PagesDeployFile> {
        if (entries.isEmpty()) return emptyList()
        val cleaned = entries.map { (n, b) -> n.replace('\\', '/').trimStart('/') to b }.filter { it.first.isNotEmpty() }
        // 共同顶层目录（如 dist/、build/）→ 剥掉
        val firstSegs = cleaned.map { it.first.substringBefore('/') }.toSet()
        val strip = if (firstSegs.size == 1 && cleaned.all { it.first.contains('/') }) firstSegs.first() + "/" else ""
        return cleaned.mapNotNull { (n, b) ->
            val rel = n.removePrefix(strip)
            if (rel.isEmpty() || rel.endsWith("/")) null else PagesDeployFile("/$rel", b)
        }
    }

    private fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()
}
