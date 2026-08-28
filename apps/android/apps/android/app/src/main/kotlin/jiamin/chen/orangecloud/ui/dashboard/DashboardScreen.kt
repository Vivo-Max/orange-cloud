package jiamin.chen.orangecloud.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.outlined.Troubleshoot
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import jiamin.chen.orangecloud.core.design.SkyBackground
import jiamin.chen.orangecloud.core.design.StatTile
import jiamin.chen.orangecloud.core.design.StatusDot
import jiamin.chen.orangecloud.core.design.ZoneAvatar
import jiamin.chen.orangecloud.core.design.onSky
import jiamin.chen.orangecloud.core.design.rememberSkyPhase
import jiamin.chen.orangecloud.core.design.theme.OcOrange
import jiamin.chen.orangecloud.core.design.theme.OcSuccess
import jiamin.chen.orangecloud.core.auth.AuthSessionMeta
import jiamin.chen.orangecloud.data.model.Account
import jiamin.chen.orangecloud.data.model.Zone

@Composable
fun DashboardScreen(
    onOpenTunnels: () -> Unit,
    onOpenTurnstile: () -> Unit,
    onOpenRegistrar: () -> Unit,
    onOpenTracer: () -> Unit,
    onOpenUrlScanner: () -> Unit,
    onOpenZones: () -> Unit,
    onOpenZone: (Zone) -> Unit,
    onAddAccount: () -> Unit,
    onOpenRedirects: () -> Unit = {},
    onOpenZeroTrust: () -> Unit = {},
    onOpenResource: (DashboardResourceType, String, String) -> Unit = { _, _, _ -> },
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val cs = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    val openResource: (DashboardResource) -> Unit = { res -> onOpenResource(res.type, res.id, res.title) }

    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 0.dp),
            ) {
                Spacer(Modifier.height(0.dp))
                // 顶栏：账号头像（点开切换菜单）
                Row(
                    Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 52.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.weight(1f))
                    // 命令搜索入口：打开时兜底触发一次目录补拉（首屏那次若失败/未跑完，这里补上）
                    IconButton(onClick = { searchOpen = true; viewModel.ensureCatalog() }) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = stringResource(R.string.hub_search),
                            tint = onSky,
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Box(
                        Modifier.size(40.dp).clickable { menuOpen = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.accountName.isNotBlank()) {
                            ZoneAvatar(state.accountName, size = 40.dp)
                        } else {
                            Icon(Icons.Outlined.Person, contentDescription = null, tint = onSky)
                        }
                    }
                }

                // 问候
                Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 16.dp)) {
                    Text(stringResource(R.string.dashboard_greeting), fontSize = 16.sp, color = cs.onSurfaceVariant)
                    Text(
                        state.accountName.ifBlank { stringResource(R.string.app_name) },
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Medium,
                        color = onSky,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (state.accountEmail.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                            Icon(Icons.Outlined.Cloud, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(state.accountEmail, fontSize = 12.sp, color = cs.onSurfaceVariant)
                        }
                    }
                }

                // 统计磁贴 2×2
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(Icons.Outlined.Language, state.zoneCount, stringResource(R.string.nav_zones), stringResource(R.string.dash_sub_zones), primary = true, modifier = Modifier.weight(1f))
                        StatTile(Icons.Outlined.Bolt, state.workerCount, stringResource(R.string.nav_workers), stringResource(R.string.dash_sub_workers), modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(Icons.Outlined.Cloud, state.bucketCount, stringResource(R.string.dash_buckets), stringResource(R.string.dash_sub_buckets), modifier = Modifier.weight(1f))
                        StatTile(Icons.Outlined.BarChart, state.requestsToday, stringResource(R.string.dash_requests), stringResource(R.string.dash_sub_requests), modifier = Modifier.weight(1f))
                    }
                }

                // 已固定（跨资源类型置顶，横滑 chip）
                PinnedResourceRow(pinned = state.pinned, onSky = onSky, onOpen = openResource)

                // 用量模块（账号级 Workers/R2/D1/KV 用量，环形仪表 + 点开明细）
                Spacer(Modifier.height(26.dp))
                DashboardUsageSection(
                    usage = state.usage,
                    plan = state.usagePlan,
                    loading = state.usageLoading,
                    loadFailed = state.usageLoadFailed,
                    hasScope = state.hasAccountAnalytics,
                    unavailable = state.accountAnalyticsUnavailable,
                    onSky = onSky,
                    onRetry = { viewModel.loadUsage(force = true) },
                    onSetWorkersPaid = { viewModel.setUsageWorkersPaid(it) },
                    onSetR2Paid = { viewModel.setUsageR2Paid(it) },
                    onSetBillingDay = { viewModel.setUsageBillingDay(it) },
                )

                // 告警中心（域名未激活 / 隧道异常 / 无 Worker，全部正常显示「暂无告警」）
                AlertCenterCard(alerts = state.alerts, onSky = onSky, onOpen = openResource)

                // 最近访问
                Row(
                    Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 26.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.dash_recent), fontSize = 22.sp, fontWeight = FontWeight.Medium, color = onSky, modifier = Modifier.weight(1f))
                    Text(
                        stringResource(R.string.dash_view_all),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.primary,
                        modifier = Modifier.clickable(onClick = onOpenZones).padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                if (state.recentZones.isNotEmpty()) {
                    Surface(
                        color = cs.surfaceContainerLow,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    ) {
                        Column {
                            state.recentZones.forEachIndexed { i, zone ->
                                RecentZoneRow(zone, onClick = { onOpenZone(zone) }, divider = i < state.recentZones.lastIndex)
                            }
                        }
                    }
                }

                // 快捷操作
                Text(
                    stringResource(R.string.dash_quick),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = onSky,
                    modifier = Modifier.padding(start = 24.dp, top = 26.dp, bottom = 10.dp),
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 110.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    QuickAction(Icons.Outlined.Refresh, stringResource(R.string.dash_refresh)) { viewModel.refresh() }
                    QuickAction(Icons.Outlined.Hub, stringResource(R.string.tunnel_title), onOpenTunnels)
                    QuickAction(Icons.Outlined.Shield, stringResource(R.string.ts_title), onOpenTurnstile)
                    QuickAction(Icons.Outlined.VerifiedUser, stringResource(R.string.reg_title), onOpenRegistrar)
                    QuickAction(Icons.Outlined.Troubleshoot, stringResource(R.string.trace_title), onOpenTracer)
                    QuickAction(Icons.Outlined.TravelExplore, stringResource(R.string.urls_title), onOpenUrlScanner)
                    QuickAction(Icons.Outlined.Link, stringResource(R.string.redirect_title), onOpenRedirects)
                    QuickAction(Icons.Outlined.VerifiedUser, stringResource(R.string.zt_title), onOpenZeroTrust)
                }
            }

            if (searchOpen) {
                ResourceSearchSheet(
                    resources = state.resources,
                    pinnedKeys = remember(state.pinned) { state.pinned.map { it.pinKey }.toSet() },
                    loading = state.catalogLoading,
                    onOpen = { res -> searchOpen = false; openResource(res) },
                    onTogglePin = { res -> viewModel.togglePin(res) },
                    onDismiss = { searchOpen = false },
                )
            }

            if (menuOpen) {
                AccountMenu(
                    sessions = state.authSessions,
                    currentSessionId = state.currentAuthSessionId,
                    accounts = state.accounts,
                    currentId = state.selectedAccountId,
                    onPickSession = { viewModel.switchAuthSession(it); menuOpen = false },
                    onPick = { viewModel.selectAccount(it); menuOpen = false },
                    onAddAccount = { menuOpen = false; onAddAccount() },
                    onDismiss = { menuOpen = false },
                )
            }
        }
    }
}

@Composable
private fun RecentZoneRow(zone: Zone, onClick: () -> Unit, divider: Boolean) {
    val cs = MaterialTheme.colorScheme
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ZoneAvatar(zone.name, size = 40.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(zone.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                zone.plan?.name?.let { Text(it, fontSize = 13.sp, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            StatusDot(if (zone.isActive) OcSuccess else cs.error)
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = cs.onSurfaceVariant)
        }
        if (divider) {
            Box(Modifier.fillMaxWidth().padding(start = 70.dp).height(1.dp).background(cs.outlineVariant.copy(alpha = 0.5f)))
        }
    }
}

@Composable
private fun QuickAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .clickable(onClick = onClick)
            .background(cs.surfaceContainerLow, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = cs.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
    }
}

@Composable
private fun AccountMenu(
    sessions: List<AuthSessionMeta>,
    currentSessionId: String?,
    accounts: List<Account>,
    currentId: String?,
    onPickSession: (String) -> Unit,
    onPick: (String) -> Unit,
    onAddAccount: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().clickable(onClick = onDismiss)) {
        Surface(
            color = cs.surfaceContainerHigh,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                // bottom padding 把菜单高度封在屏幕内——否则账号一多就被底部裁掉（issue #71）
                .padding(top = 96.dp, end = 12.dp, bottom = 24.dp)
                .width(276.dp),
        ) {
            Column(Modifier.padding(8.dp)) {
                // 身份 + 账号列表内滚动（高度已被上面的 padding 封顶），
                // 「添加账号」留在滚动区外固定在底部，账号再多也点得到
                Column(
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (sessions.size > 1) {
                        Text(
                            stringResource(R.string.dash_switch_identity),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = cs.onSurfaceVariant,
                            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 6.dp),
                        )
                        sessions.forEach { session ->
                            Row(
                                Modifier.fillMaxWidth().clickable { onPickSession(session.id) }.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ZoneAvatar(session.label, size = 38.dp)
                                Spacer(Modifier.width(12.dp))
                                Text(session.label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = cs.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                if (session.id == currentSessionId) Icon(Icons.Outlined.Check, contentDescription = null, tint = cs.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp).height(1.dp).background(cs.outlineVariant))
                    }
                    Text(
                        stringResource(R.string.dash_switch_account),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 6.dp),
                    )
                    accounts.forEach { account ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onPick(account.id) }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ZoneAvatar(account.name, size = 38.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(account.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = cs.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            if (account.id == currentId) Icon(Icons.Outlined.Check, contentDescription = null, tint = cs.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp).height(1.dp).background(cs.outlineVariant))
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onAddAccount).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(38.dp).background(cs.primaryContainer, RoundedCornerShape(percent = 50)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = cs.onPrimaryContainer, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.dash_add_account), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = cs.primary)
                }
            }
        }
    }
}
