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
import androidx.compose.ui.graphics.Color
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
import jiamin.chen.orangecloud.core.design.theme.OcSuccess
import jiamin.chen.orangecloud.data.model.CertInspectResult
import jiamin.chen.orangecloud.data.repository.ToolboxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SslInspectUiState(
    val input: String = "",
    val isLoading: Boolean = false,
    val result: CertInspectResult? = null,
    val error: ToolErrorKind? = null,
)

@HiltViewModel
class SslInspectViewModel @Inject constructor(private val repo: ToolboxRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SslInspectUiState())
    val uiState = _uiState.asStateFlow()

    fun setInput(v: String) = _uiState.update { it.copy(input = v) }

    fun run() {
        if (_uiState.value.input.isBlank()) {
            _uiState.update { it.copy(error = ToolErrorKind.INPUT) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, result = null) }
            try {
                val r = repo.sslInspect(_uiState.value.input)
                _uiState.update { it.copy(isLoading = false, result = r) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.toToolErrorKind()) }
            }
        }
    }
}

@Composable
fun SslInspectToolScreen(onBack: () -> Unit, viewModel: SslInspectViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky

    SkyBackground(phase = phase) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            ToolHeader(stringResource(R.string.tool_ssl_title), onSky, onBack)
            ToolInputBar(
                value = state.input,
                onValueChange = viewModel::setInput,
                placeholder = stringResource(R.string.tool_ssl_hint),
                isLoading = state.isLoading,
                onRun = viewModel::run,
            )
            Spacer(Modifier.height(12.dp))
            when {
                state.error != null -> ToolHint(toolErrorText(state.error!!), onSky)
                state.result != null -> SslResultBody(state.result!!)
                else -> ToolHint(stringResource(R.string.tool_ssl_blurb), onSky)
            }
        }
    }
}

@Composable
private fun SslResultBody(r: CertInspectResult) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(PaddingValues(horizontal = 16.dp))) {
        // 有效期状态横幅
        val (bannerColor, bannerText) = when {
            r.expired -> Color(0xFFE5484D) to stringResource(R.string.tool_ssl_expired)
            r.daysRemaining < 30 -> Color(0xFFE08C00) to stringResource(R.string.tool_ssl_expiring, r.daysRemaining)
            else -> OcSuccess to stringResource(R.string.tool_ssl_valid, r.daysRemaining)
        }
        Text(
            bannerText,
            color = bannerColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        )
        ToolResultCard {
            ToolFieldRow(stringResource(R.string.tool_ssl_subject), r.subject)
            ToolFieldRow(stringResource(R.string.tool_ssl_issuer), r.issuer)
            ToolFieldRow(stringResource(R.string.tool_ssl_not_before), r.notBefore, mono = true)
            ToolFieldRow(stringResource(R.string.tool_ssl_not_after), r.notAfter, mono = true)
            ToolFieldRow(stringResource(R.string.tool_ssl_sig_alg), r.sigAlg)
            ToolFieldRow(stringResource(R.string.tool_ssl_key), r.keyDescription)
            ToolFieldRow(
                stringResource(R.string.tool_ssl_trusted),
                stringResource(if (r.trusted) R.string.tool_ssl_trusted_yes else R.string.tool_ssl_trusted_no),
                copyable = false,
            )
            ToolFieldRow(stringResource(R.string.tool_ssl_chain), r.chainLength.toString(), copyable = false)
        }
        if (r.sans.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.tool_ssl_sans),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            )
            ToolResultCard {
                r.sans.forEach { ToolFieldRow("DNS", it, mono = true) }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}
