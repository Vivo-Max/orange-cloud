package jiamin.chen.orangecloud.ui.loadbalancer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.design.SkyBackground
import jiamin.chen.orangecloud.core.design.SkyEmptyState
import jiamin.chen.orangecloud.core.design.SkyHeader
import jiamin.chen.orangecloud.core.design.StatusDot
import jiamin.chen.orangecloud.core.design.onSky
import jiamin.chen.orangecloud.core.design.rememberSkyPhase
import jiamin.chen.orangecloud.core.design.theme.OcOrange
import jiamin.chen.orangecloud.core.design.theme.OcSuccess
import jiamin.chen.orangecloud.data.model.LoadBalancer
import jiamin.chen.orangecloud.data.model.Monitor
import jiamin.chen.orangecloud.data.model.OriginInput
import jiamin.chen.orangecloud.data.model.Pool
import jiamin.chen.orangecloud.data.model.PoolHealthResponse
import jiamin.chen.orangecloud.ui.storage.StorageRow

private val STEERING = listOf("off", "geo", "random", "dynamic_latency", "proximity", "least_outstanding_requests", "least_connections")
private val MONITOR_TYPES = listOf("http", "https", "tcp", "udp_icmp", "icmp_ping", "smtp")

private fun steeringLabelRes(policy: String?): Int = when (policy) {
    "geo" -> R.string.lb_steering_geo
    "random" -> R.string.lb_steering_random
    "dynamic_latency" -> R.string.lb_steering_dynamic
    "proximity" -> R.string.lb_steering_proximity
    "least_outstanding_requests" -> R.string.lb_steering_least_requests
    "least_connections" -> R.string.lb_steering_least_conn
    else -> R.string.lb_steering_off
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneLoadBalancerScreen(
    onBack: () -> Unit,
    onOpenPools: () -> Unit,
    onOpenMonitors: () -> Unit,
    viewModel: ZoneLoadBalancerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val snackbar = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<LbEdit?>(null) }
    var toDelete by remember { mutableStateOf<LoadBalancer?>(null) }
    LaunchedEffect(Unit) { viewModel.errors.collect { snackbar.showSnackbar(it) } }

    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                SkyHeader(
                    title = stringResource(R.string.lb_title), onSky = onSky,
                    isLoading = state.isLoading || state.isBusy, onRefresh = { viewModel.load() }, onBack = onBack,
                    titleSize = 22, backDescription = stringResource(R.string.common_back), refreshDescription = stringResource(R.string.common_refresh),
                )
                when {
                    state.missingScope -> SkyEmptyState(Icons.Outlined.Lock, stringResource(R.string.scope_missing), onSky, stringResource(R.string.common_refresh)) { viewModel.load() }
                    state.loadBalancers.isEmpty() && state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = onSky) }
                    else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = if (state.canWrite) 96.dp else 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (state.loadBalancers.isEmpty()) item { Text(stringResource(R.string.lb_empty), color = onSky.copy(alpha = 0.8f), fontSize = 14.sp, modifier = Modifier.padding(8.dp)) }
                        items(state.loadBalancers, key = { it.id }) { lb ->
                            LbRow(lb, state.canWrite, { viewModel.toggle(lb, it) }, { editing = LbEdit(lb) }, { toDelete = lb })
                        }
                        item { StorageRow(Icons.Outlined.Hub, stringResource(R.string.lb_pools), stringResource(R.string.lb_pools_sub), onClick = onOpenPools) }
                        item { StorageRow(Icons.Outlined.MonitorHeart, stringResource(R.string.lb_monitors), stringResource(R.string.lb_monitors_sub), onClick = onOpenMonitors) }
                    }
                }
            }
            if (state.canWrite && !state.missingScope) {
                FloatingActionButton(onClick = { editing = LbEdit(null) }, containerColor = OcOrange, contentColor = Color.White, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).systemBarsPadding()) {
                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.lb_create))
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).systemBarsPadding())
        }
    }

    editing?.let { target ->
        ModalBottomSheet(onDismissRequest = { if (!state.isBusy) editing = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            LbForm(target.lb, state.pools, state.isBusy) { name, steering, pools, fallback ->
                viewModel.save(target.lb?.id, name, steering, pools, fallback); editing = null
            }
        }
    }
    toDelete?.let { lb -> ConfirmDelete(lb.name ?: lb.id, { viewModel.delete(lb); toDelete = null }, { toDelete = null }) }
}

private data class LbEdit(val lb: LoadBalancer?)

@Composable
private fun LbRow(lb: LoadBalancer, canWrite: Boolean, onToggle: (Boolean) -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(lb.name ?: lb.id, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${stringResource(steeringLabelRes(lb.steeringPolicy))} · ${stringResource(R.string.lb_pool_count, lb.defaultPools?.size ?: 0)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = lb.enabled ?: false, onCheckedChange = onToggle, enabled = canWrite)
            if (canWrite) {
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.lb_edit), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.dns_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun LbForm(lb: LoadBalancer?, pools: List<Pool>, isSaving: Boolean, onSave: (name: String, steering: String, defaultPools: List<String>, fallback: String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(lb?.name.orEmpty()) }
    var steering by rememberSaveable { mutableStateOf(lb?.steeringPolicy ?: "off") }
    val selected = remember { mutableStateListOf<String>().apply { lb?.defaultPools?.let { addAll(it) } } }
    var fallback by rememberSaveable { mutableStateOf(lb?.fallbackPool.orEmpty()) }
    val canSave = name.isNotBlank() && selected.isNotEmpty() && fallback.isNotBlank() && !isSaving

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(if (lb == null) R.string.lb_create else R.string.lb_edit), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.lb_field_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        DropdownField(stringResource(R.string.lb_field_steering), stringResource(steeringLabelRes(steering)), STEERING) { steering = it; }
        Text(stringResource(R.string.lb_field_pools), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pools.forEach { p ->
                FilterChip(
                    selected = selected.contains(p.id),
                    onClick = { if (selected.contains(p.id)) selected.remove(p.id) else selected.add(p.id); if (fallback.isBlank() && selected.isNotEmpty()) fallback = selected.first() },
                    label = { Text(p.name ?: p.id, maxLines = 1) },
                )
            }
        }
        if (selected.isNotEmpty()) {
            DropdownFieldRaw(stringResource(R.string.lb_field_fallback), poolName(pools, fallback), selected.toList()) { fallback = it }
        }
        SaveButton(canSave, isSaving) { onSave(name, steering, selected.toList(), fallback) }
    }
}

// MARK: - 源站池

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoolListScreen(onBack: () -> Unit, viewModel: PoolListViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val snackbar = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<PoolEdit?>(null) }
    var toDelete by remember { mutableStateOf<Pool?>(null) }
    LaunchedEffect(Unit) { viewModel.errors.collect { snackbar.showSnackbar(it) } }

    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                SkyHeader(stringResource(R.string.lb_pools), onSky, state.isLoading || state.isBusy, { viewModel.load() }, onBack = onBack, titleSize = 22, backDescription = stringResource(R.string.common_back), refreshDescription = stringResource(R.string.common_refresh))
                when {
                    state.missingScope -> SkyEmptyState(Icons.Outlined.Lock, stringResource(R.string.scope_missing), onSky, stringResource(R.string.common_refresh)) { viewModel.load() }
                    state.pools.isEmpty() && state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = onSky) }
                    state.pools.isEmpty() -> SkyEmptyState(Icons.Outlined.Hub, stringResource(R.string.lb_pools_empty), onSky, stringResource(R.string.common_refresh)) { viewModel.load() }
                    else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = if (state.canWrite) 96.dp else 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(state.pools, key = { it.id }) { pool -> PoolRow(pool, state.healthByPool[pool.id], state.canWrite, { editing = PoolEdit(pool) }, { toDelete = pool }) }
                    }
                }
            }
            if (state.canWrite && !state.missingScope) {
                FloatingActionButton(onClick = { editing = PoolEdit(null) }, containerColor = OcOrange, contentColor = Color.White, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).systemBarsPadding()) { Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.lb_pool_create)) }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).systemBarsPadding())
        }
    }

    editing?.let { target ->
        ModalBottomSheet(onDismissRequest = { if (!state.isBusy) editing = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            PoolForm(target.pool, state.monitors, state.isBusy) { name, monitor, enabled, origins -> viewModel.save(target.pool?.id, name, monitor, enabled, origins); editing = null }
        }
    }
    toDelete?.let { p -> ConfirmDelete(p.name ?: p.id, { viewModel.delete(p); toDelete = null }, { toDelete = null }) }
}

private data class PoolEdit(val pool: Pool?)

@Composable
private fun PoolRow(pool: Pool, health: PoolHealthResponse?, canWrite: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            val healthy = (health?.healthyCount ?: 0) > 0 || (health?.totalCount ?: 0) == 0
            StatusDot(if (pool.enabled != false && healthy) OcSuccess else MaterialTheme.colorScheme.error, size = 8.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(pool.name ?: pool.id, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.lb_origins_count, pool.enabledOriginsCount, pool.originsCount), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (canWrite) {
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.lb_edit), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.dns_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PoolForm(pool: Pool?, monitors: List<Monitor>, isSaving: Boolean, onSave: (name: String, monitor: String?, enabled: Boolean, origins: List<OriginInput>) -> Unit) {
    var name by rememberSaveable { mutableStateOf(pool?.name.orEmpty()) }
    var enabled by rememberSaveable { mutableStateOf(pool?.enabled ?: true) }
    var monitor by rememberSaveable { mutableStateOf(pool?.monitor.orEmpty()) }
    val origins = remember {
        mutableStateListOf<MutableList<String>>().apply {
            pool?.origins?.forEach { add(mutableListOf(it.name ?: "", it.address ?: "", (it.weight ?: 1.0).toString())) }
            if (isEmpty()) add(mutableListOf("", "", "1"))
        }
    }
    var tick by remember { mutableStateOf(0) }
    val canSave = name.isNotBlank() && origins.any { it[1].isNotBlank() } && !isSaving

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(if (pool == null) R.string.lb_pool_create else R.string.lb_pool_edit), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.lb_field_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (monitors.isNotEmpty()) {
            DropdownFieldRaw(stringResource(R.string.lb_field_monitor), monitors.firstOrNull { it.id == monitor }?.let { it.description ?: it.typeLabel } ?: stringResource(R.string.lb_monitor_none), listOf("") + monitors.map { it.id }) { monitor = it }
        }
        Text(stringResource(R.string.lb_field_origins), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        tick.let {
            origins.forEachIndexed { idx, o ->
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(o[0], { o[0] = it; tick++ }, label = { Text(stringResource(R.string.lb_origin_name)) }, singleLine = true, modifier = Modifier.weight(1f))
                            if (origins.size > 1) IconButton(onClick = { origins.removeAt(idx); tick++ }) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.dns_delete)) }
                        }
                        OutlinedTextField(o[1], { o[1] = it; tick++ }, label = { Text(stringResource(R.string.lb_origin_address)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(o[2], { o[2] = it.filter { c -> c.isDigit() || c == '.' }; tick++ }, label = { Text(stringResource(R.string.lb_origin_weight)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        TextButton(onClick = { origins.add(mutableListOf("", "", "1")); tick++ }) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = OcOrange); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.lb_origin_add), color = OcOrange)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.lb_field_enabled), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface); Spacer(Modifier.weight(1f)); Switch(checked = enabled, onCheckedChange = { enabled = it })
        }
        SaveButton(canSave, isSaving) {
            val list = origins.filter { it[1].isNotBlank() }.map { OriginInput(name = it[0].ifBlank { it[1] }, address = it[1], enabled = true, weight = it[2].toDoubleOrNull() ?: 1.0) }
            onSave(name, monitor.ifBlank { null }, enabled, list)
        }
    }
}

// MARK: - 健康监测

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorListScreen(onBack: () -> Unit, viewModel: MonitorListViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val snackbar = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<MonEdit?>(null) }
    var toDelete by remember { mutableStateOf<Monitor?>(null) }
    LaunchedEffect(Unit) { viewModel.errors.collect { snackbar.showSnackbar(it) } }

    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                SkyHeader(stringResource(R.string.lb_monitors), onSky, state.isLoading || state.isBusy, { viewModel.load() }, onBack = onBack, titleSize = 22, backDescription = stringResource(R.string.common_back), refreshDescription = stringResource(R.string.common_refresh))
                when {
                    state.missingScope -> SkyEmptyState(Icons.Outlined.Lock, stringResource(R.string.scope_missing), onSky, stringResource(R.string.common_refresh)) { viewModel.load() }
                    state.monitors.isEmpty() && state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = onSky) }
                    state.monitors.isEmpty() -> SkyEmptyState(Icons.Outlined.MonitorHeart, stringResource(R.string.lb_monitors_empty), onSky, stringResource(R.string.common_refresh)) { viewModel.load() }
                    else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = if (state.canWrite) 96.dp else 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(state.monitors, key = { it.id }) { m -> MonitorRow(m, state.canWrite, { editing = MonEdit(m) }, { toDelete = m }) }
                    }
                }
            }
            if (state.canWrite && !state.missingScope) {
                FloatingActionButton(onClick = { editing = MonEdit(null) }, containerColor = OcOrange, contentColor = Color.White, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).systemBarsPadding()) { Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.lb_monitor_create)) }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).systemBarsPadding())
        }
    }

    editing?.let { target ->
        ModalBottomSheet(onDismissRequest = { if (!state.isBusy) editing = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            MonitorForm(target.monitor, state.isBusy) { type, method, path, codes, interval, timeout, retries, port -> viewModel.save(target.monitor?.id, type, method, path, codes, interval, timeout, retries, port); editing = null }
        }
    }
    toDelete?.let { m -> ConfirmDelete(m.description ?: m.typeLabel, { viewModel.delete(m); toDelete = null }, { toDelete = null }) }
}

private data class MonEdit(val monitor: Monitor?)

@Composable
private fun MonitorRow(monitor: Monitor, canWrite: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val summary = if (monitor.type == "http" || monitor.type == "https") "${monitor.method ?: "GET"} ${monitor.path ?: "/"}"
    else monitor.port?.let { stringResource(R.string.lb_monitor_port, it) } ?: monitor.typeLabel
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.MonitorHeart, contentDescription = null, tint = OcOrange, modifier = Modifier.width(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(monitor.description?.takeIf { it.isNotBlank() } ?: monitor.typeLabel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text("${monitor.typeLabel} · $summary", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (canWrite) {
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.lb_edit), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.dns_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonitorForm(monitor: Monitor?, isSaving: Boolean, onSave: (type: String, method: String, path: String, codes: String, interval: Int, timeout: Int, retries: Int, port: Int?) -> Unit) {
    var type by rememberSaveable { mutableStateOf(monitor?.type ?: "https") }
    var method by rememberSaveable { mutableStateOf(monitor?.method ?: "GET") }
    var path by rememberSaveable { mutableStateOf(monitor?.path ?: "/") }
    var codes by rememberSaveable { mutableStateOf(monitor?.expectedCodes ?: "2xx") }
    var interval by rememberSaveable { mutableStateOf((monitor?.interval ?: 60).toString()) }
    var timeout by rememberSaveable { mutableStateOf((monitor?.timeout ?: 5).toString()) }
    var retries by rememberSaveable { mutableStateOf((monitor?.retries ?: 2).toString()) }
    var port by rememberSaveable { mutableStateOf((monitor?.port ?: 80).toString()) }
    val isHttp = type == "http" || type == "https"
    val canSave = !isSaving

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(if (monitor == null) R.string.lb_monitor_create else R.string.lb_monitor_edit), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        DropdownFieldRaw(stringResource(R.string.lb_field_type), type.uppercase(), MONITOR_TYPES) { type = it }
        if (isHttp) {
            DropdownFieldRaw(stringResource(R.string.lb_field_method), method, listOf("GET", "HEAD")) { method = it }
            OutlinedTextField(path, { path = it }, label = { Text(stringResource(R.string.lb_field_path)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(codes, { codes = it }, label = { Text(stringResource(R.string.lb_field_codes)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        } else {
            OutlinedTextField(port, { v -> port = v.filter { it.isDigit() } }, label = { Text(stringResource(R.string.lb_field_port)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(interval, { v -> interval = v.filter { it.isDigit() } }, label = { Text(stringResource(R.string.lb_field_interval)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(timeout, { v -> timeout = v.filter { it.isDigit() } }, label = { Text(stringResource(R.string.lb_field_timeout)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(retries, { v -> retries = v.filter { it.isDigit() } }, label = { Text(stringResource(R.string.lb_field_retries)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
        }
        SaveButton(canSave, isSaving) {
            onSave(type, method, path, codes, interval.toIntOrNull() ?: 60, timeout.toIntOrNull() ?: 5, retries.toIntOrNull() ?: 2, port.toIntOrNull())
        }
    }
}

// MARK: - 共享小组件

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(label: String, current: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(value = current, onValueChange = {}, readOnly = true, label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { o -> DropdownMenuItem(text = { Text(stringResource(steeringLabelRes(o))) }, onClick = { onSelect(o); expanded = false }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownFieldRaw(label: String, current: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(value = current, onValueChange = {}, readOnly = true, label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { o -> DropdownMenuItem(text = { Text(o.ifBlank { "—" }) }, onClick = { onSelect(o); expanded = false }) }
        }
    }
}

@Composable
private fun SaveButton(canSave: Boolean, isSaving: Boolean, onSave: () -> Unit) {
    Button(onClick = onSave, enabled = canSave, colors = ButtonDefaults.buttonColors(containerColor = OcOrange, contentColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        if (isSaving) { CircularProgressIndicator(Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp, color = Color.White); Spacer(Modifier.width(8.dp)) }
        Text(stringResource(R.string.dns_save))
    }
}

@Composable
private fun ConfirmDelete(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.dev_delete_confirm)) }, text = { Text(name) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.dns_delete), color = Color(0xFFE5484D)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } })
}

private fun poolName(pools: List<Pool>, id: String): String = pools.firstOrNull { it.id == id }?.name ?: id
