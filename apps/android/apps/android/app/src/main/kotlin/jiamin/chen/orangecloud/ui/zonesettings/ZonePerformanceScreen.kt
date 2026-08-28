package jiamin.chen.orangecloud.ui.zonesettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.design.SkyBackground
import jiamin.chen.orangecloud.core.design.SkyEmptyState
import jiamin.chen.orangecloud.core.design.SkyHeader
import jiamin.chen.orangecloud.core.design.onSky
import jiamin.chen.orangecloud.core.design.rememberSkyPhase

@Composable
fun ZonePerformanceScreen(
    onBack: () -> Unit,
    viewModel: ZonePerformanceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val snackbarHostState = remember { SnackbarHostState() }
    val genericErr = stringResource(R.string.error_generic)
    var cacheDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.errors.collect { msg -> snackbarHostState.showSnackbar(msg ?: genericErr) }
    }

    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                SkyHeader(
                    title = stringResource(R.string.perf_title),
                    onSky = onSky,
                    isLoading = state.isLoading,
                    onRefresh = { viewModel.load() },
                    onBack = onBack,
                    titleSize = 22,
                    backDescription = stringResource(R.string.common_back),
                    refreshDescription = stringResource(R.string.common_refresh),
                )
                if (state.missingScope) {
                    SkyEmptyState(
                        Icons.Outlined.Speed,
                        stringResource(R.string.scope_missing), onSky, stringResource(R.string.common_refresh),
                    ) { viewModel.load() }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SectionLabel(stringResource(R.string.perf_section_network))
                        ZonePerformanceViewModel.NETWORK_TOGGLES.forEach { toggle ->
                            PerfToggleRow(
                                title = stringResource(toggle.labelRes),
                                checked = state.isOn(toggle.id),
                                enabled = state.canWrite && toggle.id !in state.updating,
                                onChange = { viewModel.setToggle(toggle.id, it) },
                            )
                        }
                        Spacer(Modifier.width(0.dp))
                        SectionLabel(stringResource(R.string.perf_section_cache))
                        PerfSelectorCard(
                            title = stringResource(R.string.perf_cache_level),
                            value = stringResource(state.cacheLevel.titleRes),
                            enabled = state.canWrite && "cache_level" !in state.updating,
                            onClick = { cacheDialog = true },
                        )
                        PerfToggleRow(
                            title = stringResource(R.string.perf_always_online),
                            checked = state.isOn("always_online"),
                            enabled = state.canWrite && "always_online" !in state.updating,
                            onChange = { viewModel.setToggle("always_online", it) },
                        )
                        PerfToggleRow(
                            title = stringResource(R.string.perf_sort_qs),
                            checked = state.isOn("sort_query_string_for_cache"),
                            enabled = state.canWrite && "sort_query_string_for_cache" !in state.updating,
                            onChange = { viewModel.setToggle("sort_query_string_for_cache", it) },
                        )
                    }
                }
            }
            SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    if (cacheDialog) {
        AlertDialog(
            onDismissRequest = { cacheDialog = false },
            title = { Text(stringResource(R.string.perf_cache_level)) },
            text = {
                Column {
                    CacheLevel.entries.forEach { level ->
                        Row(
                            Modifier.fillMaxWidth()
                                .selectable(selected = level == state.cacheLevel, onClick = {
                                    viewModel.setCacheLevel(level); cacheDialog = false
                                })
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = level == state.cacheLevel, onClick = {
                                viewModel.setCacheLevel(level); cacheDialog = false
                            })
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(level.titleRes), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { cacheDialog = false }) { Text(stringResource(R.string.common_done)) } },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
}

@Composable
private fun PerfToggleRow(title: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
        }
    }
}

@Composable
private fun PerfSelectorCard(title: String, value: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
