package jiamin.chen.orangecloud.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.design.theme.OcOrange
import jiamin.chen.orangecloud.core.design.theme.OcSuccess

// MARK: - 类型 → 图标 / 名称

@Composable
private fun typeIcon(type: DashboardResourceType): ImageVector = when (type) {
    DashboardResourceType.ZONE -> Icons.Outlined.Language
    DashboardResourceType.WORKER -> Icons.Outlined.Bolt
    DashboardResourceType.R2_BUCKET -> Icons.Outlined.Cloud
    DashboardResourceType.D1_DATABASE -> Icons.Outlined.Storage
    DashboardResourceType.KV_NAMESPACE -> Icons.Outlined.Key
    DashboardResourceType.TUNNEL -> Icons.Outlined.Hub
}

@Composable
private fun typeLabel(type: DashboardResourceType): String = stringResource(
    when (type) {
        DashboardResourceType.ZONE -> R.string.hub_type_zone
        DashboardResourceType.WORKER -> R.string.hub_type_worker
        DashboardResourceType.R2_BUCKET -> R.string.hub_type_bucket
        DashboardResourceType.D1_DATABASE -> R.string.hub_type_d1
        DashboardResourceType.KV_NAMESPACE -> R.string.hub_type_kv
        DashboardResourceType.TUNNEL -> R.string.hub_type_tunnel
    },
)

// MARK: - 已固定横滑条

/** Dashboard「已固定」区：横滑 chip，点击直跳资源详情。空置顶时整块不渲染。 */
@Composable
fun PinnedResourceRow(
    pinned: List<DashboardResource>,
    onSky: Color,
    onOpen: (DashboardResource) -> Unit,
) {
    if (pinned.isEmpty()) return
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.hub_pinned),
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = onSky,
            modifier = Modifier.padding(start = 24.dp, top = 26.dp, bottom = 10.dp),
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            pinned.forEach { resource ->
                Row(
                    Modifier
                        .clickable { onOpen(resource) }
                        .background(cs.surfaceContainerLow, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        typeIcon(resource.type),
                        contentDescription = null,
                        tint = if (resource.resolved) cs.primary else cs.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            resource.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            if (resource.resolved) typeLabel(resource.type) else stringResource(R.string.hub_unresolved),
                            fontSize = 11.sp,
                            color = cs.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

// MARK: - 告警中心

/** 告警中心卡：最多 5 条，级别配色圆点；全部正常时显示「暂无告警」。 */
@Composable
fun AlertCenterCard(
    alerts: List<DashboardAlert>,
    onSky: Color,
    onOpen: (DashboardResource) -> Unit,
) {
    if (alerts.isEmpty()) return
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.hub_alerts),
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = onSky,
            modifier = Modifier.padding(start = 24.dp, top = 26.dp, bottom = 10.dp),
        )
        Surface(
            color = cs.surfaceContainerLow,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Column {
                alerts.forEachIndexed { index, alert ->
                    AlertRow(alert, onOpen)
                    if (index < alerts.lastIndex) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 42.dp)
                                .height(1.dp)
                                .background(cs.outlineVariant.copy(alpha = 0.5f)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertRow(alert: DashboardAlert, onOpen: (DashboardResource) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val dotColor = when (alert.severity) {
        AlertSeverity.CRITICAL -> cs.error
        AlertSeverity.WARN -> OcOrange
        AlertSeverity.INFO -> cs.onSurfaceVariant
        AlertSeverity.OK -> OcSuccess
    }
    val title = when (alert.kind) {
        AlertKind.ZONE_INACTIVE -> stringResource(R.string.hub_alert_zone_inactive, alert.name)
        AlertKind.TUNNEL_UNHEALTHY -> stringResource(R.string.hub_alert_tunnel, alert.name)
        AlertKind.NO_WORKERS -> stringResource(R.string.hub_alert_no_workers)
        AlertKind.ALL_CLEAR -> stringResource(R.string.hub_alert_all_clear)
    }
    val detail = when (alert.kind) {
        AlertKind.ZONE_INACTIVE, AlertKind.TUNNEL_UNHEALTHY -> alert.detail
        AlertKind.NO_WORKERS -> stringResource(R.string.hub_alert_no_workers_sub)
        AlertKind.ALL_CLEAR -> stringResource(R.string.hub_alert_all_clear_sub)
    }
    val target = alert.target
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (target != null) Modifier.clickable { onOpen(target) } else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (alert.kind == AlertKind.ALL_CLEAR) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = dotColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        } else {
            Box(Modifier.size(10.dp).background(dotColor, CircleShape))
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (detail.isNotBlank()) {
                Text(detail, fontSize = 12.sp, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// MARK: - 命令搜索

/** 全账号资源命令搜索表：实时过滤，行右侧 Star 切换置顶，点行跳详情。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceSearchSheet(
    resources: List<DashboardResource>,
    pinnedKeys: Set<String>,
    loading: Boolean,
    onOpen: (DashboardResource) -> Unit,
    onTogglePin: (DashboardResource) -> Unit,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val results = remember(resources, query) { filterResources(resources, query) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.hub_search_hint)) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .focusRequester(focusRequester),
            )
            if (loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                )
            } else {
                Spacer(Modifier.height(10.dp))
            }
            if (results.isEmpty()) {
                Text(
                    stringResource(R.string.hub_search_empty),
                    fontSize = 14.sp,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                )
            } else {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                    items(results, key = { it.pinKey }) { resource ->
                        SearchResultRow(
                            resource = resource,
                            pinned = resource.pinKey in pinnedKeys,
                            onOpen = { onOpen(resource) },
                            onTogglePin = { onTogglePin(resource) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    resource: DashboardResource,
    pinned: Boolean,
    onOpen: () -> Unit,
    onTogglePin: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).background(cs.surfaceContainerHighest, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(typeIcon(resource.type), contentDescription = null, tint = cs.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                resource.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(typeLabel(resource.type), resource.subtitle?.takeIf { it.isNotBlank() }).joinToString(" · "),
                fontSize = 12.sp,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onTogglePin) {
            Icon(
                if (pinned) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                contentDescription = stringResource(if (pinned) R.string.hub_unpin else R.string.hub_pin),
                tint = if (pinned) OcOrange else cs.onSurfaceVariant,
            )
        }
    }
}
