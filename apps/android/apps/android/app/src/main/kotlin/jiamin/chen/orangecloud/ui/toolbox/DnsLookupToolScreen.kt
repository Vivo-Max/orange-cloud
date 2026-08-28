package jiamin.chen.orangecloud.ui.toolbox

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import jiamin.chen.orangecloud.data.model.DnsLookupResult
import jiamin.chen.orangecloud.data.model.DnsRecordLine
import jiamin.chen.orangecloud.data.repository.ToolboxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val DNS_TYPES = listOf("A", "AAAA", "CNAME", "MX", "TXT", "NS", "CAA")

data class DnsLookupUiState(
    val input: String = "",
    val type: String = "A",
    val isLoading: Boolean = false,
    val result: DnsLookupResult? = null,
    val error: ToolErrorKind? = null,
)

@HiltViewModel
class DnsLookupViewModel @Inject constructor(private val repo: ToolboxRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(DnsLookupUiState())
    val uiState = _uiState.asStateFlow()

    fun setInput(v: String) = _uiState.update { it.copy(input = v) }
    fun setType(t: String) = _uiState.update { it.copy(type = t) }

    fun run() {
        val name = _uiState.value.input.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(error = ToolErrorKind.INPUT) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, result = null) }
            try {
                val r = repo.dnsLookup(name, _uiState.value.type)
                _uiState.update { it.copy(isLoading = false, result = r) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.toToolErrorKind()) }
            }
        }
    }
}

@Composable
fun DnsLookupToolScreen(onBack: () -> Unit, viewModel: DnsLookupViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky

    SkyBackground(phase = phase) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            ToolHeader(stringResource(R.string.tool_dns_title), onSky, onBack)
            ToolInputBar(
                value = state.input,
                onValueChange = viewModel::setInput,
                placeholder = stringResource(R.string.tool_dns_hint),
                isLoading = state.isLoading,
                onRun = viewModel::run,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DNS_TYPES.forEach { t ->
                    FilterChip(
                        selected = state.type == t,
                        onClick = { viewModel.setType(t) },
                        label = { Text(t, fontSize = 13.sp) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            when {
                state.error != null -> ToolHint(toolErrorText(state.error!!), onSky)
                state.result != null -> {
                    val r = state.result!!
                    if (r.records.isEmpty()) {
                        ToolHint(r.authorityNote ?: stringResource(R.string.tool_dns_empty), onSky)
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(r.records) { rec -> DnsRecordRow(rec) }
                        }
                    }
                }
                else -> ToolHint(stringResource(R.string.tool_dns_blurb), onSky)
            }
        }
    }
}

@Composable
private fun DnsRecordRow(rec: DnsRecordLine) {
    ToolResultCard {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                rec.type,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(64.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    rec.data,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "TTL ${rec.ttl}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
