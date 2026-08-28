package jiamin.chen.orangecloud.ui.zones

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.design.PlanBadge
import jiamin.chen.orangecloud.core.design.SkyBackground
import jiamin.chen.orangecloud.core.design.SkyPhase
import jiamin.chen.orangecloud.core.design.StatusDot
import jiamin.chen.orangecloud.core.design.ZoneAvatar
import jiamin.chen.orangecloud.core.design.theme.OcSuccess
import jiamin.chen.orangecloud.data.model.Zone
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneListScreen(
    onZoneClick: (Zone) -> Unit = {},
    viewModel: ZoneListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val addState by viewModel.addState.collectAsStateWithLifecycle()
    val isDark = jiamin.chen.orangecloud.core.design.theme.LocalIsDark.current
    val phase = remember(isDark) { SkyPhase.current(isDark, LocalTime.now().hour) }
    val onSky = if (phase.isDark) Color(0xFFF3ECE4) else Color(0xFF24190F)
    val activeCount = uiState.zones.count { it.isActive }
    var showAdd by remember { mutableStateOf(false) }
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    SkyBackground(phase = phase) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.zones_title),
                        color = onSky,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    if (uiState.zones.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.zones_subtitle, uiState.zones.size, activeCount),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                        )
                    }
                }
                if (viewModel.canWrite) {
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Outlined.Add, stringResource(R.string.addzone_title), tint = onSky)
                    }
                }
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = onSky, strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                } else {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Outlined.Refresh, stringResource(R.string.common_refresh), tint = onSky)
                    }
                }
            }

            when {
                uiState.zones.isEmpty() && uiState.isLoading ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = onSky) }

                uiState.zones.isEmpty() ->
                    EmptyZones(onSky, viewModel.canWrite, onAdd = { showAdd = true }) { viewModel.refresh() }

                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(uiState.zones, key = { it.id }) { zone ->
                        ZoneRow(zone, onClick = { onZoneClick(zone) })
                    }
                }
            }
        }

        if (showAdd) {
            AddZoneSheet(
                state = addState,
                accountName = viewModel.currentAccountName(),
                sheetState = addSheetState,
                onCreate = { viewModel.createZone(it) },
                onDismiss = {
                    showAdd = false
                    viewModel.resetAddState()
                },
            )
        }
    }
}

@Composable
private fun ZoneRow(zone: Zone, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ZoneAvatar(zone.name, size = 42.dp)
            Spacer(Modifier.width(14.dp))
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = zone.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                )
                zone.plan?.name?.substringBefore(" ")?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.width(8.dp))
                    PlanBadge(it)
                }
            }
            Spacer(Modifier.width(10.dp))
            StatusDot(statusColor(zone.displayStatus))
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyZones(onSky: Color, canWrite: Boolean, onAdd: () -> Unit, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Dns, contentDescription = null, tint = onSky.copy(alpha = 0.6f), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.zones_empty), color = onSky.copy(alpha = 0.85f), fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            if (canWrite) {
                TextButton(onClick = onAdd) { Text(stringResource(R.string.addzone_title)) }
            }
            TextButton(onClick = onRetry) { Text(stringResource(R.string.common_refresh)) }
        }
    }
}

@Composable
private fun statusColor(status: String): Color = when (status) {
    "active" -> OcSuccess
    "pending", "initializing", "moved" -> Color(0xFFC77C00)
    "paused", "deactivated" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.outline
}
