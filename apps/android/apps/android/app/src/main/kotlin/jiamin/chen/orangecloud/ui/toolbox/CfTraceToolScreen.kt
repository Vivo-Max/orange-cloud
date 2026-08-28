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
import jiamin.chen.orangecloud.data.model.ToolField
import jiamin.chen.orangecloud.data.repository.ToolboxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CfTraceUiState(
    val input: String = "",
    val isLoading: Boolean = false,
    val result: List<ToolField>? = null,
    val error: ToolErrorKind? = null,
)

@HiltViewModel
class CfTraceViewModel @Inject constructor(private val repo: ToolboxRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CfTraceUiState())
    val uiState = _uiState.asStateFlow()

    fun setInput(v: String) = _uiState.update { it.copy(input = v) }

    fun run() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, result = null) }
            try {
                val r = repo.cfTrace(_uiState.value.input)
                _uiState.update { it.copy(isLoading = false, result = r) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.toToolErrorKind()) }
            }
        }
    }
}

@Composable
fun CfTraceToolScreen(onBack: () -> Unit, viewModel: CfTraceViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky

    SkyBackground(phase = phase) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            ToolHeader(stringResource(R.string.tool_trace_title), onSky, onBack)
            ToolInputBar(
                value = state.input,
                onValueChange = viewModel::setInput,
                placeholder = stringResource(R.string.tool_trace_hint),
                isLoading = state.isLoading,
                onRun = viewModel::run,
            )
            Spacer(Modifier.height(12.dp))
            when {
                state.error != null -> ToolHint(toolErrorText(state.error!!), onSky)
                state.result != null -> Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(PaddingValues(horizontal = 16.dp)),
                ) {
                    ToolResultCard {
                        state.result!!.forEach { f -> ToolFieldRow(f.label, f.value, mono = true) }
                    }
                    Spacer(Modifier.height(20.dp))
                }
                else -> ToolHint(stringResource(R.string.tool_trace_blurb), onSky)
            }
        }
    }
}
