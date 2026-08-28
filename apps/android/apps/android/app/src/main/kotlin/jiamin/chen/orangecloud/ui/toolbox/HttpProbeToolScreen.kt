package jiamin.chen.orangecloud.ui.toolbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
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
import jiamin.chen.orangecloud.core.design.theme.OcSuccess
import jiamin.chen.orangecloud.data.model.HttpProbeResult
import jiamin.chen.orangecloud.data.repository.ToolboxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import javax.inject.Inject

data class HttpProbeUiState(
    val input: String = "",
    val method: String = "GET",
    val isLoading: Boolean = false,
    val result: HttpProbeResult? = null,
    val error: ToolErrorKind? = null,
)

@HiltViewModel
class HttpProbeViewModel @Inject constructor(private val repo: ToolboxRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(HttpProbeUiState())
    val uiState = _uiState.asStateFlow()

    fun setInput(v: String) = _uiState.update { it.copy(input = v) }
    fun setMethod(m: String) = _uiState.update { it.copy(method = m) }

    fun run() {
        if (_uiState.value.input.isBlank()) {
            _uiState.update { it.copy(error = ToolErrorKind.INPUT) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, result = null) }
            try {
                val r = repo.httpProbe(_uiState.value.input, _uiState.value.method)
                _uiState.update { it.copy(isLoading = false, result = r) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.toToolErrorKind()) }
            }
        }
    }
}

@Composable
fun HttpProbeToolScreen(onBack: () -> Unit, viewModel: HttpProbeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val methods = listOf("GET", "HEAD")

    SkyBackground(phase = phase) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            ToolHeader(stringResource(R.string.tool_http_title), onSky, onBack)
            ToolInputBar(
                value = state.input,
                onValueChange = viewModel::setInput,
                placeholder = stringResource(R.string.tool_http_hint),
                isLoading = state.isLoading,
                onRun = viewModel::run,
            )
            Spacer(Modifier.height(10.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                methods.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = state.method == m,
                        onClick = { viewModel.setMethod(m) },
                        shape = SegmentedButtonDefaults.itemShape(i, methods.size),
                    ) { Text(m, fontSize = 13.sp) }
                }
            }
            Spacer(Modifier.height(12.dp))
            when {
                state.error != null -> ToolHint(toolErrorText(state.error!!), onSky)
                state.result != null -> HttpResultBody(state.result!!)
                else -> ToolHint(stringResource(R.string.tool_http_blurb), onSky)
            }
        }
    }
}

@Composable
private fun HttpResultBody(r: HttpProbeResult) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(PaddingValues(horizontal = 16.dp))) {
        ToolResultCard {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val statusColor = when (r.statusCode) {
                    in 200..299 -> OcSuccess
                    in 300..399 -> MaterialTheme.colorScheme.primary
                    else -> Color(0xFFE5484D)
                }
                Text(r.statusCode.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = statusColor)
                Column(Modifier.weight(1f)) {
                    Text(r.statusLine, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface)
                    Text("${r.elapsedMs} ms", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.tool_http_headers),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        )
        ToolResultCard {
            r.headers.forEach { h -> ToolFieldRow(h.label, h.value, mono = true) }
        }
        if (r.bodyPreview.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.tool_http_body),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            )
            ToolResultCard {
                Text(
                    r.bodyPreview,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}
