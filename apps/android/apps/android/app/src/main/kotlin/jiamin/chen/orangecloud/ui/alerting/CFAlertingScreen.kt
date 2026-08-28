package jiamin.chen.orangecloud.ui.alerting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import jiamin.chen.orangecloud.data.model.CFAvailableAlert
import jiamin.chen.orangecloud.ui.toolbox.ToolResultCard

@Composable
fun CFAlertingScreen(onBack: () -> Unit, viewModel: CFAlertingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky

    SkyBackground(phase = phase) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            SkyHeader(
                title = stringResource(R.string.alerting_title),
                onSky = onSky,
                isLoading = state.isLoading,
                onRefresh = { viewModel.load() },
                onBack = onBack,
                titleSize = 22,
                backDescription = stringResource(R.string.common_back),
                refreshDescription = stringResource(R.string.common_refresh),
            )
            Text(
                stringResource(R.string.alerting_subtitle),
                color = onSky.copy(alpha = 0.8f),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
            )
            when {
                state.missingScope ->
                    SkyEmptyState(Icons.Outlined.Lock, stringResource(R.string.alerting_missing_scope), onSky, stringResource(R.string.common_refresh)) { viewModel.load() }

                state.noEndpoint ->
                    SkyEmptyState(Icons.Outlined.NotificationsActive, stringResource(R.string.alerting_no_endpoint), onSky, stringResource(R.string.common_refresh)) { viewModel.load() }

                state.groups.isEmpty() && state.isLoading ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = onSky) }

                state.groups.isEmpty() && state.error ->
                    SkyEmptyState(Icons.Outlined.NotificationsActive, stringResource(R.string.error_generic), onSky, stringResource(R.string.common_refresh)) { viewModel.load() }

                state.groups.isEmpty() ->
                    SkyEmptyState(Icons.Outlined.NotificationsActive, stringResource(R.string.alerting_empty), onSky, stringResource(R.string.common_refresh)) { viewModel.load() }

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.groups.forEach { group ->
                        item(key = "h_${group.category}") {
                            Text(
                                group.category,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = onSky.copy(alpha = 0.9f),
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
                            )
                        }
                        item(key = "c_${group.category}") {
                            ToolResultCard {
                                group.alerts.forEach { alert ->
                                    AlertRow(
                                        alert = alert,
                                        enabled = alert.type in state.enabledTypes,
                                        busy = state.busyType == alert.type,
                                        onToggle = { viewModel.toggle(alert) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertRow(alert: CFAvailableAlert, enabled: Boolean, busy: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                alert.label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            alert.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(12.dp))
        if (busy) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
        } else {
            Switch(checked = enabled, onCheckedChange = { onToggle() })
        }
    }
}
