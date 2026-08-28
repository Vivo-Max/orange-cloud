package jiamin.chen.orangecloud.ui.toolbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Http
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.design.SkyBackground
import jiamin.chen.orangecloud.core.design.TintIcon
import jiamin.chen.orangecloud.core.design.onSky
import jiamin.chen.orangecloud.core.design.rememberSkyPhase
import jiamin.chen.orangecloud.ui.toolbox.ToolRoutes as Routes

/** hub 里的一个工具条目（route 与 ToolboxNavHost 的内部路由一致）。 */
data class ToolboxEntry(
    val route: String,
    val titleRes: Int,
    val subtitleRes: Int,
    val icon: ImageVector,
)

/** 工具清单（推送中心作为旗舰卡随 Push Center 上线时置顶接入）。 */
val TOOLBOX_ENTRIES: List<ToolboxEntry> = listOf(
    ToolboxEntry(Routes.DNS, R.string.tool_dns_title, R.string.tool_dns_sub, Icons.Outlined.Dns),
    ToolboxEntry(Routes.SSL, R.string.tool_ssl_title, R.string.tool_ssl_sub, Icons.Outlined.Lock),
    ToolboxEntry(Routes.HTTP, R.string.tool_http_title, R.string.tool_http_sub, Icons.Outlined.Http),
    ToolboxEntry(Routes.WHOIS, R.string.tool_whois_title, R.string.tool_whois_sub, Icons.Outlined.TravelExplore),
    ToolboxEntry(Routes.GEOIP, R.string.tool_geoip_title, R.string.tool_geoip_sub, Icons.Outlined.Public),
    ToolboxEntry(Routes.CIDR, R.string.tool_cidr_title, R.string.tool_cidr_sub, Icons.Outlined.Calculate),
    ToolboxEntry(Routes.TRACE, R.string.tool_trace_title, R.string.tool_trace_sub, Icons.Outlined.Route),
)

@Composable
fun ToolboxHubScreen(onExit: () -> Unit, onOpenTool: (String) -> Unit) {
    val phase = rememberSkyPhase()
    val onSky = phase.onSky

    SkyBackground(phase = phase) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            ToolHeader(stringResource(R.string.toolbox_title), onSky, onExit)
            Text(
                stringResource(R.string.toolbox_subtitle),
                color = onSky.copy(alpha = 0.8f),
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { PushFlagshipCard(onClick = { onOpenTool(Routes.PUSH) }) }
                items(TOOLBOX_ENTRIES, key = { it.route }) { entry ->
                    ToolboxRow(entry, onClick = { onOpenTool(entry.route) })
                }
            }
        }
    }
}

/** 旗舰卡：推送中心（品牌色强调，hub 置顶）。 */
@Composable
private fun PushFlagshipCard(onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        color = cs.primaryContainer,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.layout.Box(
                Modifier.size(48.dp)
                    .clip(RoundedCornerShape(percent = 30))
                    .background(cs.primary.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = cs.onPrimaryContainer, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.push_title), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = cs.onPrimaryContainer)
                Text(stringResource(R.string.push_flagship_sub), fontSize = 12.5.sp, color = cs.onPrimaryContainer.copy(alpha = 0.85f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = cs.onPrimaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun ToolboxRow(entry: ToolboxEntry, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TintIcon(entry.icon)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(entry.titleRes),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(entry.subtitleRes),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
