package jiamin.chen.orangecloud.ui.waf

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

data class WafAiState(
    val isGenerating: Boolean = false,
    val result: String? = null,
    val error: String? = null,
    val available: Boolean = false,
)

/**
 * 用自然语言生成 WAF 表达式（对应 iOS 1.3.2「用自然语言写 WAF 规则」）。
 * 走 Workers AI 云端推理，需账号开启 Workers AI（ai.write）。
 */
@HiltViewModel
class WafAiViewModel @Inject constructor(
    private val accountStore: AccountStore,
    private val repository: DeveloperPlatformRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val canUse = authRepository.hasScope(Scopes.AI_WRITE)

    private val _state = MutableStateFlow(WafAiState(available = canUse))
    val state: StateFlow<WafAiState> = _state.asStateFlow()

    fun generate(description: String) {
        if (!canUse || description.isBlank() || _state.value.isGenerating) return
        _state.update { it.copy(isGenerating = true, error = null, result = null) }
        viewModelScope.launch {
            try {
                accountStore.ensureLoaded()
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                val messages = listOf(
                    AIChatMessage("system", SYSTEM_PROMPT),
                    AIChatMessage("user", description),
                )
                val raw = repository.runTextGeneration(accountId, MODEL, messages)
                _state.update { it.copy(result = cleanExpression(raw)) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "error") }
            } finally {
                _state.update { it.copy(isGenerating = false) }
            }
        }
    }

    fun consumeResult() = _state.update { it.copy(result = null) }

    /** 去除模型可能附带的代码围栏 / 多余解释，仅留表达式那行。 */
    private fun cleanExpression(raw: String): String {
        var s = raw.trim()
        s = s.removePrefix("```").removeSuffix("```").trim()
        s = s.lineSequence().firstOrNull { it.contains(" eq ") || it.contains(" contains ") || it.contains("http.") || it.contains("ip.") } ?: s
        return s.trim().trim('`').trim()
    }

    companion object {
        const val MODEL = "@cf/meta/llama-3.1-8b-instruct"
        const val SYSTEM_PROMPT =
            "Convert the user's request into a single Cloudflare WAF firewall rule expression using Wirefilter syntax " +
                "(fields like http.request.uri.path, http.host, ip.src, ip.geoip.country, http.user_agent, " +
                "http.request.method; operators eq, ne, contains; strings in double quotes; combine with 'and'/'or'). " +
                "Output ONLY the expression on a single line, no explanation, no code fences."
    }
}
