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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.design.SkyBackground
import jiamin.chen.orangecloud.core.design.onSky
import jiamin.chen.orangecloud.core.design.rememberSkyPhase
import jiamin.chen.orangecloud.data.model.CidrResult
import jiamin.chen.orangecloud.data.repository.ToolboxRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class CidrUiState(
    val input: String = "",
    val result: CidrResult? = null,
    val error: ToolErrorKind? = null,
)

@HiltViewModel
class CidrViewModel @Inject constructor(private val repo: ToolboxRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CidrUiState())
    val uiState = _uiState.asStateFlow()

    fun setInput(v: String) = _uiState.update { it.copy(input = v) }

    fun run() {
        val raw = _uiState.value.input.trim()
        if (raw.isEmpty()) {
            _uiState.update { it.copy(error = ToolErrorKind.INPUT) }
            return
        }
        viewModelScope.launch {
            try {
                // 字面量 IP 解析无网络，但仍放 Default 线程避开 InetAddress 偶发主机名解析。
                val r = withContext(Dispatchers.Default) { repo.computeCidr(raw) }
                _uiState.update { it.copy(result = r, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(result = null, error = ToolErrorKind.INPUT) }
            }
        }
    }
}

@Composable
fun CidrToolScreen(onBack: () -> Unit, viewModel: CidrViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky

    SkyBackground(phase = phase) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            ToolHeader(stringResource(R.string.tool_cidr_title), onSky, onBack)
            ToolInputBar(
                value = state.input,
                onValueChange = viewModel::setInput,
                placeholder = stringResource(R.string.tool_cidr_hint),
                isLoading = false,
                onRun = viewModel::run,
                keyboardType = KeyboardType.Uri,
            )
            Spacer(Modifier.height(12.dp))
            when {
                state.error != null -> ToolHint(toolErrorText(state.error!!), onSky)
                state.result != null -> {
                    val r = state.result!!
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(PaddingValues(horizontal = 16.dp))) {
                        ToolResultCard {
                            ToolFieldRow(stringResource(R.string.tool_cidr_network), r.network, mono = true)
                            r.broadcast?.let { ToolFieldRow(stringResource(R.string.tool_cidr_broadcast), it, mono = true) }
                            ToolFieldRow(stringResource(R.string.tool_cidr_first), r.firstHost, mono = true)
                            ToolFieldRow(stringResource(R.string.tool_cidr_last), r.lastHost, mono = true)
                            ToolFieldRow(stringResource(R.string.tool_cidr_total), r.totalAddresses, mono = true)
                            ToolFieldRow(stringResource(R.string.tool_cidr_usable), r.usableHosts, mono = true)
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }
                else -> ToolHint(stringResource(R.string.tool_cidr_blurb), onSky)
            }
        }
    }
}
