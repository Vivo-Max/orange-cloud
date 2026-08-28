package jiamin.chen.orangecloud.ui.ratelimit

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
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
import jiamin.chen.orangecloud.core.design.theme.OcOrange
import jiamin.chen.orangecloud.data.model.RateLimitRule

private enum class RlAction(val value: String, val labelRes: Int) {
    BLOCK("block", R.string.rl_action_block),
    MANAGED_CHALLENGE("managed_challenge", R.string.rl_action_managed_challenge),
    JS_CHALLENGE("js_challenge", R.string.rl_action_js_challenge),
    LOG("log", R.string.rl_action_log);

    companion object {
        fun fromRaw(raw: String?): RlAction = entries.firstOrNull { it.value == raw } ?: BLOCK
    }
}

private enum class RlPeriod(val seconds: Int, val labelRes: Int) {
    S10(10, R.string.rl_period_10s),
    S60(60, R.string.rl_period_1m),
    S600(600, R.string.rl_period_10m),
    S3600(3600, R.string.rl_period_1h);

    companion object {
        fun fromSeconds(s: Int?): RlPeriod = entries.firstOrNull { it.seconds == s } ?: S60
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneRateLimitScreen(
    onBack: () -> Unit,
    viewModel: ZoneRateLimitViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val snackbarHostState = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<RateLimitRule?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var ruleToDelete by remember { mutableStateOf<RateLimitRule?>(null) }

    val savedMsg = stringResource(R.string.rl_saved)
    val deletedMsg = stringResource(R.string.rl_deleted)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                RateLimitEvent.Saved -> { editing = null; showCreate = false; snackbarHostState.showSnackbar(savedMsg) }
                RateLimitEvent.Deleted -> snackbarHostState.showSnackbar(deletedMsg)
                is RateLimitEvent.Error -> snackbarHostState.showSnackbar(event.message ?: "")
            }
        }
    }

    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                SkyHeader(
                    title = stringResource(R.string.rl_title),
                    onSky = onSky,
                    isLoading = state.isLoading,
                    onRefresh = { viewModel.load() },
                    onBack = onBack,
                    titleSize = 22,
                    backDescription = stringResource(R.string.common_back),
                    refreshDescription = stringResource(R.string.common_refresh),
                )
                if (!state.canWrite && !state.missingScope) {
                    Text(
                        stringResource(R.string.rl_readonly),
                        color = onSky.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                    )
                }
                when {
                    state.missingScope ->
                        SkyEmptyState(Icons.Outlined.Lock, stringResource(R.string.scope_missing), onSky, stringResource(R.string.common_refresh)) { viewModel.load() }

                    state.rules.isEmpty() && state.isLoading ->
                        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = onSky) }

                    state.rules.isEmpty() ->
                        SkyEmptyState(Icons.Outlined.Timer, stringResource(R.string.rl_empty), onSky, stringResource(R.string.common_refresh)) { viewModel.load() }

                    else -> LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = if (state.canWrite) 96.dp else 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.rules, key = { it.id }) { rule ->
                            RateLimitRow(
                                rule = rule,
                                canWrite = state.canWrite,
                                onToggle = { viewModel.toggle(rule, it) },
                                onEdit = if (state.canWrite) ({ editing = rule }) else null,
                                onDelete = { ruleToDelete = rule },
                            )
                        }
                    }
                }
            }

            if (state.canWrite && !state.missingScope) {
                FloatingActionButton(
                    onClick = { showCreate = true },
                    containerColor = OcOrange,
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).systemBarsPadding(),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.rl_add_title))
                }
            }
            SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).systemBarsPadding())
        }
    }

    val target = editing
    if (showCreate || target != null) {
        ModalBottomSheet(
            onDismissRequest = { if (!state.isSaving) { editing = null; showCreate = false } },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            RateLimitForm(
                rule = target,
                isSaving = state.isSaving,
                onSave = { expr, requests, period, action, mitigation, desc, enabled ->
                    viewModel.save(target?.id, expr, requests, period, action, mitigation, desc, enabled)
                },
            )
        }
    }

    ruleToDelete?.let { rule ->
        AlertDialog(
            onDismissRequest = { ruleToDelete = null },
            title = { Text(stringResource(R.string.rl_delete_confirm_title)) },
            text = { Text(stringResource(R.string.rl_delete_confirm_msg)) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(rule); ruleToDelete = null }) {
                    Text(stringResource(R.string.dns_delete), color = Color(0xFFE5484D))
                }
            },
            dismissButton = {
                TextButton(onClick = { ruleToDelete = null }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

@Composable
private fun RateLimitRow(
    rule: RateLimitRule,
    canWrite: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().let { if (onEdit != null) it.clickable(onClick = onEdit) else it },
    ) {
        Row(Modifier.padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .background(OcOrange.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        val r = rule.ratelimit
                        Text(
                            stringResource(R.string.rl_summary, r?.requestsPerPeriod ?: 0, r?.period ?: 0),
                            color = OcOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    rule.description?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(it, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                rule.expression?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        it,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Switch(checked = rule.enabled ?: false, onCheckedChange = onToggle, enabled = canWrite)
            if (canWrite) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.dns_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RateLimitForm(
    rule: RateLimitRule?,
    isSaving: Boolean,
    onSave: (expression: String, requests: Int, period: Int, action: String, mitigation: Int, description: String, enabled: Boolean) -> Unit,
) {
    var description by rememberSaveable { mutableStateOf(rule?.description.orEmpty()) }
    var expression by rememberSaveable { mutableStateOf(rule?.expression.orEmpty()) }
    var requests by rememberSaveable { mutableStateOf(rule?.ratelimit?.requestsPerPeriod?.toString() ?: "100") }
    var period by rememberSaveable { mutableStateOf(RlPeriod.fromSeconds(rule?.ratelimit?.period)) }
    var action by rememberSaveable { mutableStateOf(RlAction.fromRaw(rule?.action)) }
    var mitigation by rememberSaveable { mutableStateOf(RlPeriod.fromSeconds(rule?.ratelimit?.mitigationTimeout)) }
    var enabled by rememberSaveable { mutableStateOf(rule?.enabled ?: true) }

    val canSave = expression.isNotBlank() && (requests.toIntOrNull() ?: 0) > 0 && !isSaving

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(if (rule == null) R.string.rl_add_title else R.string.rl_edit_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(R.string.rl_field_description)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = expression,
            onValueChange = { expression = it },
            label = { Text(stringResource(R.string.rl_field_expression)) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = requests,
            onValueChange = { v -> requests = v.filter { it.isDigit() } },
            label = { Text(stringResource(R.string.rl_field_requests)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        PeriodField(stringResource(R.string.rl_field_period), period) { period = it }
        ActionField(action) { action = it }
        PeriodField(stringResource(R.string.rl_field_mitigation), mitigation) { mitigation = it }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.rl_field_enabled), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            Switch(checked = enabled, onCheckedChange = { enabled = it })
        }

        Button(
            onClick = {
                onSave(expression, requests.toIntOrNull() ?: 0, period.seconds, action.value, mitigation.seconds, description, enabled)
            },
            enabled = canSave,
            colors = ButtonDefaults.buttonColors(containerColor = OcOrange, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isSaving) {
                CircularProgressIndicator(Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(Modifier.width(8.dp))
            }
            Text(stringResource(R.string.dns_save))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodField(label: String, period: RlPeriod, onSelect: (RlPeriod) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = stringResource(period.labelRes),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            RlPeriod.entries.forEach { option ->
                DropdownMenuItem(text = { Text(stringResource(option.labelRes)) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionField(action: RlAction, onSelect: (RlAction) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = stringResource(action.labelRes),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.rl_field_action)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            RlAction.entries.forEach { option ->
                DropdownMenuItem(text = { Text(stringResource(option.labelRes)) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}
