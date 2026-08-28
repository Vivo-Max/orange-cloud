package jiamin.chen.orangecloud.ui.zerotrust

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.design.SkyBackground
import jiamin.chen.orangecloud.core.design.SkyEmptyState
import jiamin.chen.orangecloud.core.design.SkyHeader
import jiamin.chen.orangecloud.core.design.onSky
import jiamin.chen.orangecloud.core.design.rememberSkyPhase
import jiamin.chen.orangecloud.core.design.theme.OcOrange
import jiamin.chen.orangecloud.data.model.AccessApp
import jiamin.chen.orangecloud.data.model.GatewayRule
import jiamin.chen.orangecloud.ui.storage.StorageListBody
import jiamin.chen.orangecloud.ui.storage.StorageRow

@Composable
fun ZeroTrustHubScreen(
    onBack: () -> Unit,
    onOpenAccess: () -> Unit,
    onOpenGateway: () -> Unit,
) {
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    SkyBackground(phase = phase) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            SkyHeader(
                title = stringResource(R.string.zt_title),
                onSky = onSky,
                isLoading = false,
                onRefresh = {},
                onBack = onBack,
                titleSize = 24,
                backDescription = stringResource(R.string.common_back),
            )
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StorageRow(Icons.Outlined.Apps, stringResource(R.string.zt_access_apps), stringResource(R.string.zt_access_sub), onClick = onOpenAccess)
                StorageRow(Icons.Outlined.Policy, stringResource(R.string.zt_gateway), stringResource(R.string.zt_gateway_sub), onClick = onOpenGateway)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessAppsScreen(
    onBack: () -> Unit,
    viewModel: AccessAppsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val snackbar = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<AppEdit?>(null) }
    var toDelete by remember { mutableStateOf<AccessApp?>(null) }
    LaunchedEffect(Unit) { viewModel.errors.collect { snackbar.showSnackbar(it) } }

    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                SkyHeader(
                    title = stringResource(R.string.zt_access_apps), onSky = onSky,
                    isLoading = state.isLoading || busy, onRefresh = { viewModel.load() }, onBack = onBack,
                    titleSize = 22, backDescription = stringResource(R.string.common_back), refreshDescription = stringResource(R.string.common_refresh),
                )
                StorageListBody(state = state, onSky = onSky, emptyIcon = Icons.Outlined.Apps, emptyText = stringResource(R.string.zt_access_empty), onRetry = { viewModel.load() }) { app ->
                    AccessAppRow(app, viewModel.canWrite, { editing = AppEdit(app) }, { toDelete = app })
                }
            }
            if (viewModel.canWrite && !state.missingScope) {
                FloatingActionButton(onClick = { editing = AppEdit(null) }, containerColor = OcOrange, contentColor = Color.White, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).systemBarsPadding()) {
                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.zt_app_create))
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).systemBarsPadding())
        }
    }

    editing?.let { target ->
        ModalBottomSheet(onDismissRequest = { if (!busy) editing = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            AccessAppForm(target.app, busy) { name, domain, type, session, decision, rules -> viewModel.save(target.app?.id, name, domain, type, session, decision, rules); editing = null }
        }
    }
    toDelete?.let { app -> ZtConfirmDelete(app.name ?: app.id, { viewModel.delete(app); toDelete = null }, { toDelete = null }) }
}

private data class AppEdit(val app: AccessApp?)

@Composable
private fun AccessAppRow(app: AccessApp, canWrite: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = OcOrange, modifier = Modifier.width(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(app.name ?: app.id, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(stringResource(accessTypeLabel(app.type)), app.domain?.takeIf { it.isNotBlank() }).joinToString(" · "), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (canWrite) {
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.lb_edit), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.dns_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

private val ZT_APP_TYPES = listOf("self_hosted", "saas", "ssh", "vnc", "bookmark")
private val ZT_SESSIONS = listOf("30m", "1h", "6h", "24h", "168h", "730h")
private val ZT_DECISIONS = listOf("allow", "deny", "bypass")
private val ZT_RULE_KINDS = listOf("everyone", "email", "email_domain", "ip", "geo")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccessAppForm(app: AccessApp?, isSaving: Boolean, onSave: (name: String, domain: String, type: String, session: String, decision: String, rules: List<Pair<String, String>>) -> Unit) {
    var name by rememberSaveable { mutableStateOf(app?.name.orEmpty()) }
    var domain by rememberSaveable { mutableStateOf(app?.domain.orEmpty()) }
    var type by rememberSaveable { mutableStateOf(app?.type ?: "self_hosted") }
    var session by rememberSaveable { mutableStateOf(app?.sessionDuration ?: "24h") }
    var decision by rememberSaveable { mutableStateOf("allow") }
    val rules = remember { mutableStateListOf<MutableList<String>>().apply { add(mutableListOf("email_domain", "")) } }
    var tick by remember { mutableStateOf(0) }
    val canSave = name.isNotBlank() && domain.isNotBlank() && !isSaving

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(if (app == null) R.string.zt_app_create else R.string.zt_app_edit), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.zt_field_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(domain, { domain = it }, label = { Text(stringResource(R.string.zt_field_domain)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        ZtDropdown(stringResource(R.string.zt_field_type), type, ZT_APP_TYPES) { type = it }
        ZtDropdown(stringResource(R.string.zt_field_session), session, ZT_SESSIONS) { session = it }
        ZtDropdown(stringResource(R.string.zt_field_decision), decision, ZT_DECISIONS) { decision = it }
        Text(stringResource(R.string.zt_field_include), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        tick.let {
            rules.forEachIndexed { idx, r ->
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) { ZtDropdown(stringResource(R.string.zt_rule_kind), r[0], ZT_RULE_KINDS) { r[0] = it; tick++ } }
                            if (rules.size > 1) IconButton(onClick = { rules.removeAt(idx); tick++ }) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.dns_delete)) }
                        }
                        if (r[0] != "everyone") {
                            OutlinedTextField(r[1], { r[1] = it; tick++ }, label = { Text(stringResource(R.string.zt_rule_value)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
        TextButton(onClick = { rules.add(mutableListOf("email", "")); tick++ }) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = OcOrange); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.zt_rule_add), color = OcOrange)
        }
        ZtSaveButton(canSave, isSaving) {
            val list = rules.filter { it[0] == "everyone" || it[1].isNotBlank() }.map { it[0] to it[1] }
            onSave(name, domain, type, session, decision, list)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GatewayRulesScreen(
    onBack: () -> Unit,
    viewModel: GatewayRulesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val snackbarHostState = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<GwEdit?>(null) }
    var toDelete by remember { mutableStateOf<GatewayRule?>(null) }

    LaunchedEffect(Unit) { viewModel.errors.collect { snackbarHostState.showSnackbar(it) } }

    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                SkyHeader(
                    title = stringResource(R.string.zt_gateway), onSky = onSky,
                    isLoading = state.isLoading || busy, onRefresh = { viewModel.load() }, onBack = onBack,
                    titleSize = 22, backDescription = stringResource(R.string.common_back), refreshDescription = stringResource(R.string.common_refresh),
                )
                if (!state.canWrite && !state.missingScope) {
                    Text(stringResource(R.string.zt_readonly), color = onSky.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp))
                }
                when {
                    state.missingScope ->
                        SkyEmptyState(Icons.Outlined.Lock, stringResource(R.string.scope_missing), onSky, stringResource(R.string.common_refresh)) { viewModel.load() }
                    state.rules.isEmpty() && state.isLoading ->
                        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = onSky) }
                    state.rules.isEmpty() ->
                        SkyEmptyState(Icons.Outlined.Policy, stringResource(R.string.zt_gateway_empty), onSky, stringResource(R.string.common_refresh)) { viewModel.load() }
                    else -> LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = if (state.canWrite) 96.dp else 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.rules, key = { it.id }) { rule ->
                            GatewayRow(rule, state.canWrite, { viewModel.toggle(rule, it) }, { editing = GwEdit(rule) }, { toDelete = rule })
                        }
                    }
                }
            }
            if (state.canWrite && !state.missingScope) {
                FloatingActionButton(onClick = { editing = GwEdit(null) }, containerColor = OcOrange, contentColor = Color.White, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).systemBarsPadding()) {
                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.zt_gw_create))
                }
            }
            SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).systemBarsPadding())
        }
    }

    editing?.let { target ->
        ModalBottomSheet(onDismissRequest = { if (!busy) editing = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            GatewayRuleForm(target.rule, busy) { name, type, action, traffic, enabled -> viewModel.save(target.rule, name, type, action, traffic, enabled); editing = null }
        }
    }
    toDelete?.let { rule -> ZtConfirmDelete(rule.name ?: rule.id, { viewModel.delete(rule); toDelete = null }, { toDelete = null }) }
}

private data class GwEdit(val rule: GatewayRule?)

private val ZT_GW_TYPES = listOf("dns", "http", "l4")
private fun ztGwActions(type: String): List<String> = when (type) {
    "dns" -> listOf("allow", "block", "safesearch", "ytrestricted")
    "http" -> listOf("allow", "block", "isolate", "off", "noscan")
    else -> listOf("allow", "block")
}

@Composable
private fun GatewayRow(rule: GatewayRule, canWrite: Boolean, onToggle: (Boolean) -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.background(OcOrange.copy(alpha = 0.16f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text(stringResource(gatewayKindLabel(rule.filters?.firstOrNull())), color = OcOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(rule.name ?: rule.id, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(stringResource(gatewayActionLabel(rule.action)), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = rule.isEnabled, onCheckedChange = onToggle, enabled = canWrite)
            if (canWrite) {
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.lb_edit), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.dns_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GatewayRuleForm(rule: GatewayRule?, isSaving: Boolean, onSave: (name: String, type: String, action: String, traffic: String, enabled: Boolean) -> Unit) {
    var name by rememberSaveable { mutableStateOf(rule?.name.orEmpty()) }
    var type by rememberSaveable { mutableStateOf(rule?.filters?.firstOrNull() ?: "dns") }
    var action by rememberSaveable { mutableStateOf(rule?.action ?: "block") }
    var traffic by rememberSaveable { mutableStateOf(rule?.traffic.orEmpty()) }
    var enabled by rememberSaveable { mutableStateOf(rule?.isEnabled ?: true) }
    if (action !in ztGwActions(type)) action = ztGwActions(type).first()
    val canSave = name.isNotBlank() && !isSaving

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(if (rule == null) R.string.zt_gw_create else R.string.zt_gw_edit), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.zt_field_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        ZtDropdown(stringResource(R.string.zt_field_gw_type), type, ZT_GW_TYPES) { type = it; action = ztGwActions(it).first() }
        ZtDropdown(stringResource(R.string.zt_field_action), action, ztGwActions(type)) { action = it }
        OutlinedTextField(traffic, { traffic = it }, label = { Text(stringResource(R.string.zt_field_traffic)) }, minLines = 2, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.zt_traffic_hint), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.lb_field_enabled), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface); Spacer(Modifier.weight(1f)); Switch(checked = enabled, onCheckedChange = { enabled = it })
        }
        ZtSaveButton(canSave, isSaving) { onSave(name, type, action, traffic, enabled) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZtDropdown(label: String, current: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(value = current, onValueChange = {}, readOnly = true, label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { o -> DropdownMenuItem(text = { Text(o) }, onClick = { onSelect(o); expanded = false }) }
        }
    }
}

@Composable
private fun ZtSaveButton(canSave: Boolean, isSaving: Boolean, onSave: () -> Unit) {
    Button(onClick = onSave, enabled = canSave, colors = ButtonDefaults.buttonColors(containerColor = OcOrange, contentColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        if (isSaving) { CircularProgressIndicator(Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp, color = Color.White); Spacer(Modifier.width(8.dp)) }
        Text(stringResource(R.string.dns_save))
    }
}

@Composable
private fun ZtConfirmDelete(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.dev_delete_confirm)) }, text = { Text(name) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.dns_delete), color = Color(0xFFE5484D)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } })
}

private fun accessTypeLabel(type: String?): Int = when (type) {
    "self_hosted" -> R.string.zt_type_self_hosted
    "saas" -> R.string.zt_type_saas
    "ssh" -> R.string.zt_type_ssh
    "vnc" -> R.string.zt_type_vnc
    "app_launcher" -> R.string.zt_type_launcher
    "warp" -> R.string.zt_type_warp
    "bookmark" -> R.string.zt_type_bookmark
    else -> R.string.zt_type_app
}

private fun gatewayKindLabel(filter: String?): Int = when (filter) {
    "dns" -> R.string.zt_kind_dns
    "http" -> R.string.zt_kind_http
    "l4" -> R.string.zt_kind_network
    "egress" -> R.string.zt_kind_egress
    "resolver" -> R.string.zt_kind_resolver
    else -> R.string.zt_kind_gateway
}

private fun gatewayActionLabel(action: String?): Int = when (action) {
    "allow" -> R.string.zt_action_allow
    "block" -> R.string.zt_action_block
    "isolate" -> R.string.zt_action_isolate
    "override" -> R.string.zt_action_override
    "safesearch" -> R.string.zt_action_safesearch
    "off" -> R.string.zt_action_off
    "on" -> R.string.zt_action_on
    else -> R.string.zt_action_other
}
