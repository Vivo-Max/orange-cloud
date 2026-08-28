package jiamin.chen.orangecloud.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.data.model.AIChatMessage
import jiamin.chen.orangecloud.data.repository.AccountStore
import jiamin.chen.orangecloud.data.repository.DeveloperPlatformRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AI 助手（对应 iOS 设备端 AI 助手）。Android 走 Workers AI 云端推理（任意设备可用，
 * 不依赖 Gemini Nano 的设备门槛）；设备端 Gemini Nano 留作后续设备可用时的渐进增强。
 * 需账号开启 Workers AI（ai.read + ai.write）。
 */
@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val accountStore: AccountStore,
    private val repository: DeveloperPlatformRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    val canUse: Boolean = authRepository.hasScope(Scopes.AI_WRITE)

    private val _uiState = MutableStateFlow(AssistantUiState(missingScope = !canUse))
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    fun send(text: String) {
        if (!canUse || text.isBlank() || _uiState.value.isThinking) return
        val userMsg = ChatTurn(role = "user", content = text)
        _uiState.update { it.copy(turns = it.turns + userMsg, isThinking = true, error = null) }
        viewModelScope.launch {
            try {
                accountStore.ensureLoaded()
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                val history = _uiState.value.turns.map { AIChatMessage(it.role, it.content) }
                val messages = listOf(AIChatMessage("system", SYSTEM_PROMPT)) + history
                val answer = repository.runTextGeneration(accountId, DEFAULT_MODEL, messages)
                _uiState.update { it.copy(turns = it.turns + ChatTurn("assistant", answer.ifBlank { "…" })) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "error") }
            } finally {
                _uiState.update { it.copy(isThinking = false) }
            }
        }
    }

    fun clear() = _uiState.update { AssistantUiState(missingScope = !canUse) }

    companion object {
        const val DEFAULT_MODEL = "@cf/meta/llama-3.1-8b-instruct"
        const val SYSTEM_PROMPT =
            "You are Orange Cloud's assistant, an expert on Cloudflare (Zones, DNS, Workers, WAF, R2, Pages, " +
                "Zero Trust). Answer concisely and helpfully. Reply in the user's language."
    }
}

data class ChatTurn(val role: String, val content: String)

data class AssistantUiState(
    val turns: List<ChatTurn> = emptyList(),
    val isThinking: Boolean = false,
    val missingScope: Boolean = false,
    val error: String? = null,
)
