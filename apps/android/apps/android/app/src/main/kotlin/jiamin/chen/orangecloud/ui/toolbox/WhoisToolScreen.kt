package jiamin.chen.orangecloud.ui.toolbox

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.design.SkyBackground
import jiamin.chen.orangecloud.core.design.onSky
import jiamin.chen.orangecloud.core.design.rememberSkyPhase
import jiamin.chen.orangecloud.data.model.RdapDomain
import jiamin.chen.orangecloud.data.repository.ToolboxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WhoisUiState(
    val input: String = "",
    val isLoading: Boolean = false,
    val result: RdapDomain? = null,
    val registrar: String? = null,
    val error: ToolErrorKind? = null,
)

@HiltViewModel
class WhoisViewModel @Inject constructor(private val repo: ToolboxRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(WhoisUiState())
    val uiState = _uiState.asStateFlow()

    fun setInput(v: String) = _uiState.update { it.copy(input = v) }

    fun run() {
        if (_uiState.value.input.isBlank()) {
            _uiState.update { it.copy(error = ToolErrorKind.INPUT) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, result = null, registrar = null) }
            try {
                val r = repo.whois(_uiState.value.input)
                val registrar = r.entities
                    .firstOrNull { e -> e.roles.any { it.equals("registrar", true) } }
                    ?.let { repo.entityName(it) }
                _uiState.update { it.copy(isLoading = false, result = r, registrar = registrar) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.toToolErrorKind()) }
            }
        }
    }

    fun eventDate(action: String): String? =
        repo.formatRdapDate(_uiState.value.result?.events?.firstOrNull { it.eventAction.equals(action, true) }?.eventDate)
}

@Composable
fun WhoisToolScreen(onBack: () -> Unit, viewModel: WhoisViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky

    SkyBackground(phase = phase) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            ToolHeader(stringResource(R.string.tool_whois_title), onSky, onBack)
            ToolInputBar(
                value = state.input,
                onValueChange = viewModel::setInput,
                placeholder = stringResource(R.string.tool_whois_hint),
                isLoading = state.isLoading,
                onRun = viewModel::run,
            )
            Spacer(Modifier.height(12.dp))
            when {
                state.error != null -> ToolHint(toolErrorText(state.error!!), onSky)
                state.result != null -> {
                    val r = state.result!!
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(PaddingValues(horizontal = 16.dp))) {
                        ToolResultCard {
                            (r.unicodeName ?: r.ldhName)?.let { ToolFieldRow(stringResource(R.string.tool_whois_domain), it, mono = true) }
                            state.registrar?.let { ToolFieldRow(stringResource(R.string.tool_whois_registrar), it) }
                            viewModel.eventDate("registration")?.let { ToolFieldRow(stringResource(R.string.tool_whois_created), it, mono = true) }
                            viewModel.eventDate("expiration")?.let { ToolFieldRow(stringResource(R.string.tool_whois_expires), it, mono = true) }
                            viewModel.eventDate("last changed")?.let { ToolFieldRow(stringResource(R.string.tool_whois_updated), it, mono = true) }
                            if (r.status.isNotEmpty()) ToolFieldRow(stringResource(R.string.tool_whois_status), r.status.joinToString(", "))
                        }
                        if (r.nameservers.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.tool_whois_nameservers),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                            )
                            ToolResultCard {
                                r.nameservers.mapNotNull { it.ldhName }.forEach {
                                    ToolFieldRow("NS", it.lowercase(), mono = true)
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }
                else -> ToolHint(stringResource(R.string.tool_whois_blurb), onSky)
            }
        }
    }
}
