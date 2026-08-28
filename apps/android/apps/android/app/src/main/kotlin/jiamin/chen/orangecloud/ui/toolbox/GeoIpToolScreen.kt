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
import jiamin.chen.orangecloud.data.model.GeoIpResponse
import jiamin.chen.orangecloud.data.repository.ToolboxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GeoIpUiState(
    val input: String = "",
    val isLoading: Boolean = false,
    val result: GeoIpResponse? = null,
    val error: ToolErrorKind? = null,
)

@HiltViewModel
class GeoIpViewModel @Inject constructor(private val repo: ToolboxRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(GeoIpUiState())
    val uiState = _uiState.asStateFlow()

    fun setInput(v: String) = _uiState.update { it.copy(input = v) }

    fun run() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, result = null) }
            try {
                val r = repo.geoIp(_uiState.value.input)
                if (r.success) {
                    _uiState.update { it.copy(isLoading = false, result = r) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = ToolErrorKind.NOT_FOUND) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.toToolErrorKind()) }
            }
        }
    }
}

@Composable
fun GeoIpToolScreen(onBack: () -> Unit, viewModel: GeoIpViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky

    SkyBackground(phase = phase) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            ToolHeader(stringResource(R.string.tool_geoip_title), onSky, onBack)
            ToolInputBar(
                value = state.input,
                onValueChange = viewModel::setInput,
                placeholder = stringResource(R.string.tool_geoip_hint),
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
                            ToolFieldRow("IP", r.ip, mono = true)
                            val location = listOfNotNull(r.city, r.region, r.country).filter { it.isNotBlank() }.joinToString(", ")
                            if (location.isNotBlank()) {
                                ToolFieldRow(stringResource(R.string.tool_geoip_location), "${r.flag?.emoji.orEmpty()} $location".trim())
                            }
                            r.connection?.let { c ->
                                (c.isp ?: c.org)?.let { ToolFieldRow(stringResource(R.string.tool_geoip_isp), it) }
                                c.asn?.let { ToolFieldRow("ASN", "AS$it", mono = true) }
                            }
                            r.timezone?.id?.let { ToolFieldRow(stringResource(R.string.tool_geoip_timezone), it) }
                            if (r.latitude != null && r.longitude != null) {
                                ToolFieldRow(stringResource(R.string.tool_geoip_coords), "${r.latitude}, ${r.longitude}", mono = true)
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }
                else -> ToolHint(stringResource(R.string.tool_geoip_blurb), onSky)
            }
        }
    }
}
