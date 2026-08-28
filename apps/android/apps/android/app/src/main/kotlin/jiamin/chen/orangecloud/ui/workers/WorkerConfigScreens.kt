package jiamin.chen.orangecloud.ui.workers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.design.SkyBackground
import jiamin.chen.orangecloud.core.design.SkyHeader
import jiamin.chen.orangecloud.core.design.onSky
import jiamin.chen.orangecloud.core.design.rememberSkyPhase
import jiamin.chen.orangecloud.core.design.theme.OcOrange
import jiamin.chen.orangecloud.data.model.D1Database
import jiamin.chen.orangecloud.data.model.KVNamespace
import jiamin.chen.orangecloud.data.model.WorkerBinding
import jiamin.chen.orangecloud.data.model.WorkerBindingInput
import jiamin.chen.orangecloud.data.model.WorkerSchedule
import jiamin.chen.orangecloud.data.model.Zone

// ============================================================
//  共享小组件
// ============================================================

@Composable
private fun WorkerSection(title: String, footer: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
        }
        if (footer != null) {
            Text(footer, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun AddInlineButton(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null, tint = OcOrange, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = OcOrange, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// ============================================================
//  变量与密钥
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerSecretsScreen(
    onBack: () -> Unit,
    viewModel: WorkerBindingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    // sheet: null = closed; "secret" = new secret; "var:" + name = edit/new variable
    var sheet by remember { mutableStateOf<EditorTarget?>(null) }
    var showBindSheet by remember { mutableStateOf(false) }
    var showBulkSheet by remember { mutableStateOf(false) }

    SkyBackground(phase = phase) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            SkyHeader(
                title = stringResource(R.string.worker_secrets_title),
                onSky = onSky,
                isLoading = state.isLoading,
                onRefresh = { viewModel.load() },
                onBack = onBack,
                titleSize = 22,
                backDescription = stringResource(R.string.common_back),
                refreshDescription = stringResource(R.string.common_refresh),
            )
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 密钥
                WorkerSection(
                    stringResource(R.string.worker_secrets_section),
                    footer = if (state.canWrite) stringResource(R.string.worker_secrets_footer) else stringResource(R.string.worker_secrets_readonly),
                ) {
                    if (state.secrets.isEmpty()) EmptyHint(stringResource(R.string.worker_secrets_empty))
                    state.secrets.forEach { secret ->
                        ItemRow(
                            title = secret.name,
                            mono = false,
                            onDelete = if (state.canWrite) ({ viewModel.deleteSecret(secret) }) else null,
                        )
                    }
                    if (state.canWrite) AddInlineButton(stringResource(R.string.worker_secrets_add)) { sheet = EditorTarget.NewSecret }
                    if (state.canWrite) AddInlineButton(stringResource(R.string.worker_bulk_import)) { showBulkSheet = true }
                }

                // 环境变量
                WorkerSection(
                    stringResource(R.string.worker_vars_section),
                    footer = stringResource(R.string.worker_vars_footer),
                ) {
                    if (state.variables.isEmpty()) EmptyHint(stringResource(R.string.worker_vars_empty))
                    state.variables.forEach { binding ->
                        ItemRow(
                            title = binding.name,
                            subtitle = binding.text,
                            mono = true,
                            onClick = if (state.canWrite) ({ sheet = EditorTarget.EditVar(binding.name, binding.text ?: "") }) else null,
                            onDelete = if (state.canWrite) ({ viewModel.deleteVariable(binding) }) else null,
                        )
                    }
                    if (state.canWrite) AddInlineButton(stringResource(R.string.worker_vars_add)) { sheet = EditorTarget.NewVar }
                }

                // 资源绑定（D1 / KV 可增删，其余只读）
                if (state.otherBindings.isNotEmpty() || state.canBind) {
                    WorkerSection(
                        stringResource(R.string.worker_other_section),
                        footer = if (state.canBind) stringResource(R.string.worker_bind_footer) else stringResource(R.string.worker_other_footer),
                    ) {
                        if (state.otherBindings.isEmpty()) EmptyHint(stringResource(R.string.worker_bind_empty))
                        state.otherBindings.forEach { binding ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(binding.name, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                Text(bindingTypeLabel(binding), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (state.canWrite && binding.isQuickManaged) {
                                    IconButton(onClick = { viewModel.unbindResource(binding) }) {
                                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.worker_bind_remove), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        if (state.canBind) {
                            AddInlineButton(stringResource(R.string.worker_bind_add)) {
                                viewModel.loadResources()
                                showBindSheet = true
                            }
                        }
                    }
                }

                state.error?.let { Text(it, color = Color(0xFFE5484D), fontSize = 13.sp) }
            }
        }
    }

    sheet?.let { target ->
        WorkerValueSheet(
            target = target,
            isSaving = state.isSaving,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onSubmit = { name, value ->
                when (target) {
                    EditorTarget.NewSecret -> viewModel.addSecret(name, value)
                    EditorTarget.NewVar -> viewModel.setVariable(name, value)
                    is EditorTarget.EditVar -> viewModel.setVariable(target.name, value)
                }
                sheet = null
            },
            onDismiss = { sheet = null },
        )
    }

    if (showBindSheet) {
        WorkerBindResourceSheet(
            state = state,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onSubmit = { resource -> viewModel.bindResource(resource); showBindSheet = false },
            onDismiss = { showBindSheet = false },
        )
    }

    if (showBulkSheet) {
        WorkerBulkImportSheet(
            isSaving = state.isSaving,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onImportVariables = { pairs -> viewModel.bulkImportVariables(pairs) { showBulkSheet = false } },
            onImportSecrets = { pairs -> viewModel.bulkImportSecrets(pairs) { showBulkSheet = false } },
            onDismiss = { showBulkSheet = false },
        )
    }
}

private sealed interface EditorTarget {
    data object NewSecret : EditorTarget
    data object NewVar : EditorTarget
    data class EditVar(val name: String, val value: String) : EditorTarget
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkerValueSheet(
    target: EditorTarget,
    isSaving: Boolean,
    sheetState: androidx.compose.material3.SheetState,
    onSubmit: (name: String, value: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val isSecret = target is EditorTarget.NewSecret
    val lockedName = (target as? EditorTarget.EditVar)?.name
    var name by remember { mutableStateOf(lockedName ?: "") }
    var value by remember { mutableStateOf((target as? EditorTarget.EditVar)?.value ?: "") }

    val nameValid = (lockedName ?: name).matches(Regex("^[A-Za-z_][A-Za-z0-9_]*$"))
    val canSave = nameValid && value.isNotEmpty() && !isSaving
    val title = when {
        isSecret -> stringResource(R.string.worker_secret_add_title)
        lockedName == null -> stringResource(R.string.worker_var_add_title)
        else -> stringResource(R.string.worker_var_edit_title)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (lockedName != null) {
                Text(lockedName, fontSize = 14.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.worker_binding_name)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.worker_binding_name_hint), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(stringResource(if (isSecret) R.string.worker_secret_value else R.string.worker_binding_value)) },
                singleLine = !isSecret,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onSubmit((lockedName ?: name).trim(), value) },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = OcOrange, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.dns_save))
            }
        }
    }
}

/** 绑定既有 D1 数据库 / KV 命名空间：选类型 → 选资源 → 填绑定变量名 → PATCH settings（其余绑定 inherit）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkerBindResourceSheet(
    state: WorkerBindingsUiState,
    sheetState: androidx.compose.material3.SheetState,
    onSubmit: (WorkerBindingInput) -> Unit,
    onDismiss: () -> Unit,
) {
    val kvKind = "kv"
    val d1Kind = "d1"
    val r2Kind = "r2"
    val availableKinds = buildList {
        if (state.canReadKV) add(kvKind)
        if (state.canReadD1) add(d1Kind)
        if (state.canReadR2) add(r2Kind)
    }
    var kind by remember { mutableStateOf(availableKinds.firstOrNull() ?: kvKind) }
    var selectedId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var nameEdited by remember { mutableStateOf(false) }
    var resourceExpanded by remember { mutableStateOf(false) }

    // R2 按桶名引用，故 id 即桶名
    val options: List<Pair<String, String>> = when (kind) {
        kvKind -> state.kvNamespaces.map { it.id to it.title }
        d1Kind -> state.d1Databases.map { it.uuid to it.name }
        else -> state.r2Buckets.map { it.name to it.name }
    }
    val selectedTitle = options.firstOrNull { it.first == selectedId }?.second

    val nameValid = name.matches(Regex("^[A-Za-z_][A-Za-z0-9_]*$"))
    val nameDuplicate = state.boundNames.contains(name)
    val canSave = selectedId.isNotEmpty() && nameValid && !nameDuplicate && !state.isSaving

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.worker_bind_add), fontSize = 20.sp, fontWeight = FontWeight.Bold)

            // 类型选择（两类都可读时才显示）
            if (availableKinds.size > 1) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableKinds.forEach { k ->
                        val selected = k == kind
                        Button(
                            onClick = {
                                if (k != kind) {
                                    kind = k
                                    selectedId = ""
                                    if (!nameEdited) name = ""
                                }
                            },
                            colors = if (selected)
                                ButtonDefaults.buttonColors(containerColor = OcOrange, contentColor = Color.White)
                            else
                                ButtonDefaults.outlinedButtonColors(),
                            modifier = Modifier.weight(1f),
                        ) { Text(when (k) { kvKind -> "KV"; d1Kind -> "D1"; else -> "R2" }) }
                    }
                }
            }

            // 资源选择
            if (state.loadingResources && options.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = OcOrange)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.common_loading), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (options.isEmpty()) {
                Text(
                    stringResource(
                        when (kind) {
                            kvKind -> R.string.worker_bind_no_kv
                            d1Kind -> R.string.worker_bind_no_d1
                            else -> R.string.worker_bind_no_r2
                        }
                    ),
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                ExposedDropdownMenuBox(expanded = resourceExpanded, onExpandedChange = { resourceExpanded = !resourceExpanded }) {
                    OutlinedTextField(
                        value = selectedTitle ?: stringResource(R.string.worker_bind_pick),
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(
                                stringResource(
                                    when (kind) {
                                        kvKind -> R.string.worker_bind_kv_ns
                                        d1Kind -> R.string.worker_bind_d1_db
                                        else -> R.string.worker_bind_r2_bucket
                                    }
                                )
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = resourceExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = resourceExpanded, onDismissRequest = { resourceExpanded = false }) {
                        options.forEach { (id, title) ->
                            DropdownMenuItem(
                                text = { Text(title) },
                                onClick = {
                                    selectedId = id
                                    if (!nameEdited) name = suggestBindingName(title)
                                    resourceExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            // 绑定变量名
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameEdited = true },
                label = { Text(stringResource(R.string.worker_bind_var_name)) },
                singleLine = true,
                isError = nameDuplicate,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                if (nameDuplicate) stringResource(R.string.worker_bind_dup) else stringResource(R.string.worker_bind_var_name_hint),
                fontSize = 12.sp,
                color = if (nameDuplicate) Color(0xFFE5484D) else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = {
                    val resource = when (kind) {
                        kvKind -> WorkerBindingInput.kv(name.trim(), selectedId)
                        d1Kind -> WorkerBindingInput.d1(name.trim(), selectedId)
                        else -> WorkerBindingInput.r2(name.trim(), selectedId)
                    }
                    onSubmit(resource)
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = OcOrange, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.worker_bind_confirm))
            }
        }
    }
}

/** 资源名 → 合法绑定变量名（大写、非法字符转下划线、数字开头补前缀）。 */
private fun suggestBindingName(title: String): String {
    val mapped = title.uppercase().map { if (it.isLetterOrDigit()) it else '_' }.joinToString("")
    val prefixed = if (mapped.firstOrNull()?.isDigit() == true) "_$mapped" else mapped
    return prefixed.ifEmpty { "BINDING" }
}

@Composable
private fun bindingTypeLabel(binding: WorkerBinding): String = when (binding.type) {
    "plain_text" -> stringResource(R.string.worker_bind_var)
    "secret_text", "secrets_store_secret" -> stringResource(R.string.worker_bind_secret)
    "kv_namespace" -> "KV"
    "d1" -> "D1"
    "r2_bucket" -> "R2"
    "queue" -> stringResource(R.string.worker_bind_queue)
    "durable_object_namespace" -> "Durable Object"
    "service" -> stringResource(R.string.worker_bind_service)
    "ai" -> "Workers AI"
    "vectorize" -> "Vectorize"
    "analytics_engine" -> "Analytics Engine"
    "browser" -> stringResource(R.string.worker_bind_browser)
    else -> binding.type
}

/** 通用行：标题 + 可选副标题 + 可选点击/删除。 */
@Composable
private fun ItemRow(
    title: String,
    subtitle: String? = null,
    mono: Boolean = false,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.dns_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ============================================================
//  触发器（Cron）
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerTriggersScreen(
    onBack: () -> Unit,
    viewModel: WorkerTriggersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    var showAdd by remember { mutableStateOf(false) }

    SkyBackground(phase = phase) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            SkyHeader(
                title = stringResource(R.string.worker_triggers_title),
                onSky = onSky,
                isLoading = state.isLoading,
                onRefresh = { viewModel.load() },
                onBack = onBack,
                titleSize = 22,
                backDescription = stringResource(R.string.common_back),
                refreshDescription = stringResource(R.string.common_refresh),
            )
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                WorkerSection(
                    stringResource(R.string.worker_triggers_title),
                    footer = if (state.canWrite) stringResource(R.string.worker_triggers_footer) else stringResource(R.string.worker_secrets_readonly),
                ) {
                    if (state.schedules.isEmpty()) {
                        EmptyHint(stringResource(R.string.worker_triggers_empty_desc))
                    }
                    state.schedules.forEach { schedule ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(schedule.cron, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface)
                                Text(cronDescribe(schedule.cron), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (state.canWrite) {
                                IconButton(onClick = { viewModel.deleteCron(schedule) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.dns_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    if (state.canWrite) AddInlineButton(stringResource(R.string.worker_triggers_add)) { showAdd = true }
                }
                state.error?.let { Text(it, color = Color(0xFFE5484D), fontSize = 13.sp) }
            }
        }
    }

    if (showAdd) {
        CronSheet(
            isSaving = state.isSaving,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onSubmit = { viewModel.addCron(it); showAdd = false },
            onDismiss = { showAdd = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CronSheet(
    isSaving: Boolean,
    sheetState: androidx.compose.material3.SheetState,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var cron by remember { mutableStateOf("") }
    val valid = cron.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size == 5
    val presets = listOf(
        "*/5 * * * *" to stringResource(R.string.worker_cron_5m),
        "0 * * * *" to stringResource(R.string.worker_cron_hourly),
        "0 0 * * *" to stringResource(R.string.worker_cron_daily),
        "0 0 * * 1" to stringResource(R.string.worker_cron_weekly),
    )

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.worker_triggers_add), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = cron,
                onValueChange = { cron = it },
                label = { Text(stringResource(R.string.worker_cron_expr)) },
                placeholder = { Text("*/5 * * * *") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                if (valid) cronDescribe(cron) else stringResource(R.string.worker_cron_hint),
                fontSize = 12.sp,
                color = if (valid) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFC77C00),
            )
            Text(stringResource(R.string.worker_cron_presets), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            presets.forEach { (expr, label) ->
                Row(
                    Modifier.fillMaxWidth().clickable { cron = expr }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(expr, fontSize = 14.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(
                onClick = { onSubmit(cron.trim()) },
                enabled = valid && !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = OcOrange, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.dns_save))
            }
        }
    }
}

/** Cron 人类可读释义（覆盖常见样式，其余回退）。 */
@Composable
private fun cronDescribe(cron: String): String {
    val f = cron.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (f.size != 5) return stringResource(R.string.worker_cron_custom)
    val (minute, hour, dom, month, dow) = f
    if (minute.startsWith("*/") && hour == "*" && dom == "*" && month == "*" && dow == "*") {
        minute.removePrefix("*/").toIntOrNull()?.let { return stringResource(R.string.worker_cron_every_n, it) }
    }
    if (minute == "0" && hour == "*" && dom == "*" && month == "*" && dow == "*") {
        return stringResource(R.string.worker_cron_desc_hourly)
    }
    val m = minute.toIntOrNull()
    val h = hour.toIntOrNull()
    if (m != null && h != null && dom == "*" && month == "*" && dow == "*") {
        return stringResource(R.string.worker_cron_desc_daily, "%02d:%02d".format(h, m))
    }
    return stringResource(R.string.worker_cron_custom)
}

// ============================================================
//  域名 / 路由
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerRoutesScreen(
    onBack: () -> Unit,
    viewModel: WorkerRoutesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    var routeSheet by remember { mutableStateOf<RouteSheetKind?>(null) }

    SkyBackground(phase = phase) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            SkyHeader(
                title = stringResource(R.string.worker_domains_title),
                onSky = onSky,
                isLoading = state.isLoading,
                onRefresh = { viewModel.load() },
                onBack = onBack,
                titleSize = 22,
                backDescription = stringResource(R.string.common_back),
                refreshDescription = stringResource(R.string.common_refresh),
            )
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // workers.dev 子域
                WorkerSection(
                    stringResource(R.string.worker_subdomain_section),
                    footer = stringResource(R.string.worker_subdomain_footer),
                ) {
                    val sub = state.subdomain
                    if (sub == null) {
                        EmptyHint(stringResource(R.string.worker_subdomain_none))
                    } else {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.worker_subdomain_label), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            if (state.togglingSubdomain) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = OcOrange)
                            } else {
                                Switch(
                                    checked = sub.enabled,
                                    onCheckedChange = { viewModel.toggleSubdomain(it) },
                                    enabled = state.canWrite,
                                )
                            }
                        }
                        state.workersDevUrl?.let { url ->
                            val uriHandler = LocalUriHandler.current
                            Row(
                                Modifier.fillMaxWidth().clickable { uriHandler.openUri(url) },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    url.removePrefix("https://"),
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = OcOrange,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = OcOrange, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // 自定义域
                WorkerSection(
                    stringResource(R.string.worker_customdomains_section),
                    footer = stringResource(R.string.worker_customdomains_footer),
                ) {
                    if (state.customDomains.isEmpty()) EmptyHint(stringResource(R.string.worker_customdomains_empty))
                    state.customDomains.forEach { domain ->
                        ItemRow(
                            title = domain.hostname,
                            subtitle = domain.zoneName,
                            onDelete = if (state.canWrite) ({ viewModel.detachDomain(domain) }) else null,
                        )
                    }
                    if (state.canWrite && state.zones.isNotEmpty()) {
                        AddInlineButton(stringResource(R.string.worker_customdomains_add)) { routeSheet = RouteSheetKind.Domain }
                    }
                }

                // Zone 路由
                WorkerSection(
                    stringResource(R.string.worker_routes_section),
                    footer = stringResource(R.string.worker_routes_footer),
                ) {
                    if (state.routes.isEmpty()) EmptyHint(stringResource(R.string.worker_routes_empty))
                    state.routes.forEach { scoped ->
                        ItemRow(
                            title = scoped.route.pattern,
                            subtitle = scoped.zoneName,
                            mono = true,
                            onDelete = if (state.canWrite) ({ viewModel.deleteRoute(scoped) }) else null,
                        )
                    }
                    if (state.canWrite && state.zones.isNotEmpty()) {
                        AddInlineButton(stringResource(R.string.worker_routes_add)) { routeSheet = RouteSheetKind.Route }
                    }
                }

                state.error?.let { Text(it, color = Color(0xFFE5484D), fontSize = 13.sp) }
            }
        }
    }

    routeSheet?.let { kind ->
        RouteEditorSheet(
            kind = kind,
            zones = state.zones,
            isSaving = state.isSaving,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onSubmit = { zoneId, text ->
                if (kind == RouteSheetKind.Domain) viewModel.attachDomain(text, zoneId) else viewModel.addRoute(zoneId, text)
                routeSheet = null
            },
            onDismiss = { routeSheet = null },
        )
    }
}

private enum class RouteSheetKind { Domain, Route }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RouteEditorSheet(
    kind: RouteSheetKind,
    zones: List<Zone>,
    isSaving: Boolean,
    sheetState: androidx.compose.material3.SheetState,
    onSubmit: (zoneId: String, text: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val isDomain = kind == RouteSheetKind.Domain
    var zoneId by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val selectedZoneName = zones.firstOrNull { it.id == zoneId }?.name ?: stringResource(R.string.worker_route_zone_pick)
    val canSave = zoneId.isNotEmpty() && text.trim().isNotEmpty() && !isSaving

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(if (isDomain) R.string.worker_attach_domain_title else R.string.worker_add_route_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = selectedZoneName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.worker_route_zone)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    zones.forEach { zone ->
                        DropdownMenuItem(text = { Text(zone.name) }, onClick = { zoneId = zone.id; expanded = false })
                    }
                }
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(if (isDomain) R.string.worker_domain_hostname else R.string.worker_route_pattern)) },
                placeholder = { Text(if (isDomain) "api.example.com" else "example.com/api/*") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(if (isDomain) R.string.worker_domain_hostname_hint else R.string.worker_route_pattern_hint),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { onSubmit(zoneId, text.trim()) },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = OcOrange, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.dns_save))
            }
        }
    }
}


// MARK: - 批量导入 JSON（变量走 PATCH settings 原子；密钥走 secrets-bulk 原子）

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkerBulkImportSheet(
    isSaving: Boolean,
    sheetState: androidx.compose.material3.SheetState,
    onImportVariables: (List<Pair<String, String>>) -> Unit,
    onImportSecrets: (List<Pair<String, String>>) -> Unit,
    onDismiss: () -> Unit,
) {
    var asSecret by remember { mutableStateOf(false) }
    var jsonText by remember { mutableStateOf("") }

    // 解析一个扁平 JSON 对象为 (name, value) 列表；键须符合环境变量命名，值取字符串/数字/布尔
    val parsed: Result<List<Pair<String, String>>> = remember(jsonText) {
        runCatching {
            val trimmed = jsonText.trim()
            if (trimmed.isEmpty()) return@runCatching emptyList()
            val obj = org.json.JSONObject(trimmed)
            val keyPattern = Regex("^[A-Za-z_][A-Za-z0-9_]*$")
            val pairs = mutableListOf<Pair<String, String>>()
            for (key in obj.keys()) {
                require(keyPattern.matches(key)) { "invalid key: $key" }
                val raw = obj.get(key)
                require(raw is String || raw is Number || raw is Boolean) { "invalid value for $key" }
                pairs += key to raw.toString()
            }
            pairs.sortedBy { it.first }
        }
    }
    val pairs = parsed.getOrDefault(emptyList())
    val hasInput = jsonText.isNotBlank()
    val canImport = pairs.isNotEmpty() && !isSaving

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.worker_bulk_import), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !asSecret, onClick = { asSecret = false }, label = { Text(stringResource(R.string.worker_vars_section)) })
                FilterChip(selected = asSecret, onClick = { asSecret = true }, label = { Text(stringResource(R.string.worker_secrets_section)) })
            }
            Text(
                stringResource(if (asSecret) R.string.worker_bulk_secret_hint else R.string.worker_bulk_var_hint),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = jsonText,
                onValueChange = { jsonText = it },
                label = { Text("JSON") },
                placeholder = { Text("{\n  \"API_KEY\": \"abc123\",\n  \"MAX_RETRY\": 3\n}") },
                minLines = 5,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth(),
            )
            if (hasInput && parsed.isFailure) {
                Text(stringResource(R.string.worker_bulk_invalid), fontSize = 12.sp, color = Color(0xFFE5484D))
            } else if (pairs.isNotEmpty()) {
                Text(stringResource(R.string.worker_bulk_count, pairs.size), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = { if (asSecret) onImportSecrets(pairs) else onImportVariables(pairs) },
                enabled = canImport,
                colors = ButtonDefaults.buttonColors(containerColor = OcOrange, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Text(stringResource(R.string.worker_bulk_do_import), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
