package jiamin.chen.orangecloud.ui.turnstile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import jiamin.chen.orangecloud.core.util.copyToClipboard
import jiamin.chen.orangecloud.data.model.TurnstileWidget
import jiamin.chen.orangecloud.data.model.TurnstileWidgetInput
import jiamin.chen.orangecloud.ui.storage.StorageListBody

// MARK: - 列表

@Composable
fun TurnstileListScreen(
    onBack: () -> Unit,
    onOpenWidget: (sitekey: String) -> Unit,
    viewModel: TurnstileListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreate by remember { mutableStateOf(false) }
    val deletedMsg = stringResource(R.string.dns_deleted)
    val errMsg = stringResource(R.string.error_generic)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TurnstileListEvent.Created -> { showCreate = false; onOpenWidget(event.widget.sitekey) }
                TurnstileListEvent.Deleted -> snackbarHostState.showSnackbar(deletedMsg)
                is TurnstileListEvent.Error -> snackbarHostState.showSnackbar(event.message ?: errMsg)
            }
        }
    }

    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                SkyHeader(
                    title = stringResource(R.string.ts_title),
                    onSky = onSky,
                    isLoading = state.isLoading,
                    onRefresh = { viewModel.load() },
                    onBack = onBack,
                    titleSize = 22,
                    backDescription = stringResource(R.string.common_back),
                    refreshDescription = stringResource(R.string.common_refresh),
                )
                StorageListBody(state, onSky, Icons.Outlined.VerifiedUser, stringResource(R.string.ts_empty), { viewModel.load() }) { widget ->
                    TurnstileRow(widget = widget, onClick = { onOpenWidget(widget.sitekey) })
                }
            }
            if (viewModel.canWrite) {
                FloatingActionButton(
                    onClick = { showCreate = true },
                    containerColor = OcOrange,
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp).navigationBarsPadding(),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.ts_new))
                }
            }
            SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
        }
    }

    if (showCreate) {
        TurnstileEditorSheet(
            existing = null,
            isSaving = isSaving,
            onDismiss = { showCreate = false },
            onSave = { viewModel.create(it) },
        )
    }
}

@Composable
private fun TurnstileRow(widget: TurnstileWidget, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    widget.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    widget.sitekey,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    modeLabel(widget.mode),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OcOrange,
                    modifier = Modifier
                        .background(OcOrange.copy(alpha = 0.14f), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
                Text(
                    if (widget.domains.isEmpty()) stringResource(R.string.ts_any_hostname)
                    else stringResource(R.string.ts_domain_count, widget.domains.size),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// MARK: - 详情

@Composable
fun TurnstileDetailScreen(
    onBack: () -> Unit,
    viewModel: TurnstileDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showEdit by remember { mutableStateOf(false) }
    var showRotate by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var secretRevealed by remember { mutableStateOf(false) }
    val rotatedMsg = stringResource(R.string.ts_rotated)
    val errMsg = stringResource(R.string.error_generic)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                TurnstileDetailEvent.Deleted -> onBack()
                TurnstileDetailEvent.Rotated -> { showRotate = false; secretRevealed = true; snackbarHostState.showSnackbar(rotatedMsg) }
                is TurnstileDetailEvent.Error -> snackbarHostState.showSnackbar(event.message ?: errMsg)
            }
        }
    }

    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                SkyHeader(
                    title = state.widget?.name ?: stringResource(R.string.ts_title),
                    onSky = onSky,
                    isLoading = state.isLoading,
                    onRefresh = { viewModel.load() },
                    onBack = onBack,
                    titleSize = 22,
                    backDescription = stringResource(R.string.common_back),
                    refreshDescription = stringResource(R.string.common_refresh),
                    actions = {
                        if (state.canWrite && state.widget != null) {
                            IconButton(onClick = { showEdit = true }) {
                                Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.ts_edit), tint = onSky)
                            }
                        }
                    },
                )
                val widget = state.widget
                if (widget == null) {
                    if (state.isLoading) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = onSky) }
                    } else {
                        state.error?.let {
                            Text(it, color = Color(0xFFE5484D), fontSize = 13.sp, modifier = Modifier.padding(24.dp))
                        }
                    }
                } else {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        SectionCard(stringResource(R.string.ts_keys_section)) {
                            KeyRow(
                                label = "Sitekey",
                                value = widget.sitekey,
                                revealed = true,
                                onTap = {
                                    copyToClipboard(context, widget.sitekey)
                                },
                            )
                            widget.secret?.let { secret ->
                                KeyRow(
                                    label = "Secret",
                                    value = if (secretRevealed) secret else "•".repeat(24),
                                    revealed = secretRevealed,
                                    onTap = {
                                        if (secretRevealed) copyToClipboard(context, secret) else secretRevealed = true
                                    },
                                )
                            }
                            Text(
                                stringResource(R.string.ts_keys_footer),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (state.canWrite) {
                                TextButton(onClick = { showRotate = true }) {
                                    Text(stringResource(R.string.ts_rotate), color = OcOrange, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        SectionCard(stringResource(R.string.ts_config_section)) {
                            InfoRow(stringResource(R.string.ts_field_mode), modeLabel(widget.mode))
                            InfoRow(
                                stringResource(R.string.ts_field_domains),
                                if (widget.domains.isEmpty()) stringResource(R.string.ts_any_hostname)
                                else widget.domains.joinToString("\n"),
                                mono = true,
                            )
                            widget.region?.let {
                                InfoRow(
                                    stringResource(R.string.ts_field_region),
                                    if (it == "china") stringResource(R.string.ts_region_china) else stringResource(R.string.ts_region_world),
                                )
                            }
                            if (widget.botFightMode == true) {
                                InfoRow("Bot Fight Mode", stringResource(R.string.common_on))
                            }
                        }

                        if (state.canWrite) {
                            Button(
                                onClick = { showDelete = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE5484D).copy(alpha = 0.12f),
                                    contentColor = Color(0xFFE5484D),
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.ts_delete), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
        }
    }

    if (showEdit) {
        state.widget?.let { widget ->
            TurnstileEditorSheet(
                existing = widget,
                isSaving = state.isSaving,
                onDismiss = { showEdit = false },
                onSave = { viewModel.update(it) { showEdit = false } },
            )
        }
    }

    if (showRotate) {
        AlertDialog(
            onDismissRequest = { showRotate = false },
            title = { Text(stringResource(R.string.ts_rotate)) },
            text = { Text(stringResource(R.string.ts_rotate_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.rotateSecret(immediately = false) }) {
                    Text(stringResource(R.string.ts_rotate_grace))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.rotateSecret(immediately = true) }) {
                    Text(stringResource(R.string.ts_rotate_now), color = Color(0xFFE5484D))
                }
            },
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.ts_delete_confirm, state.widget?.name ?: "")) },
            text = { Text(stringResource(R.string.ts_delete_message)) },
            confirmButton = {
                TextButton(onClick = { showDelete = false; viewModel.delete() }) {
                    Text(stringResource(R.string.dns_delete), color = Color(0xFFE5484D))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

// MARK: - 新建 / 编辑 sheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TurnstileEditorSheet(
    existing: TurnstileWidget?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (TurnstileWidgetInput) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var mode by remember { mutableStateOf(existing?.mode ?: "managed") }
    var domainsText by remember { mutableStateOf(existing?.domains?.joinToString("\n") ?: "") }
    var botFightMode by remember { mutableStateOf(existing?.botFightMode ?: false) }
    var region by remember { mutableStateOf("world") }

    // 每行一个域名，去空白去空行去重（保序）
    val domains = domainsText.lines()
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }
        .distinct()
    val canSave = name.isNotBlank() && domains.size <= 10 && !isSaving

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(if (existing == null) R.string.ts_new else R.string.ts_edit),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.ts_field_name)) },
                placeholder = { Text(stringResource(R.string.ts_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.ts_field_mode), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("managed", "non-interactive", "invisible").forEach { value ->
                    FilterChip(
                        selected = mode == value,
                        onClick = { mode = value },
                        label = { Text(modeLabel(value), fontSize = 12.sp) },
                    )
                }
            }
            Text(modeDetail(mode), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(
                value = domainsText,
                onValueChange = { domainsText = it },
                label = { Text(stringResource(R.string.ts_field_domains)) },
                supportingText = {
                    if (domains.size > 10) Text(stringResource(R.string.ts_domains_too_many, domains.size), color = Color(0xFFE5484D))
                    else Text(stringResource(R.string.ts_domains_hint))
                },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Bot Fight Mode", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.ts_bfm_hint), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = botFightMode, onCheckedChange = { botFightMode = it })
            }

            if (existing == null) {
                Text(stringResource(R.string.ts_field_region), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = region == "world", onClick = { region = "world" }, label = { Text(stringResource(R.string.ts_region_world), fontSize = 12.sp) })
                    FilterChip(selected = region == "china", onClick = { region = "china" }, label = { Text(stringResource(R.string.ts_region_china), fontSize = 12.sp) })
                }
                Text(stringResource(R.string.ts_region_hint), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Button(
                onClick = {
                    onSave(
                        TurnstileWidgetInput(
                            name = name.trim(),
                            mode = mode,
                            domains = domains,
                            botFightMode = botFightMode,
                            region = if (existing == null) region else null,
                            // 原样回写，避免 PUT 丢配置
                            clearanceLevel = existing?.clearanceLevel,
                        )
                    )
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = OcOrange, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            ) {
                Text(stringResource(R.string.dns_save), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// MARK: - 小组件

@Composable
private fun KeyRow(label: String, value: String, revealed: Boolean, onTap: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onTap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                value,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            if (revealed) Icons.Outlined.ContentCopy else Icons.Outlined.Visibility,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(4.dp),
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
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
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, mono: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Spacer(Modifier.weight(1f))
        Text(
            value,
            fontSize = if (mono) 12.sp else 14.sp,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
private fun modeLabel(mode: String): String = when (mode) {
    "non-interactive" -> stringResource(R.string.ts_mode_noninteractive)
    "invisible" -> stringResource(R.string.ts_mode_invisible)
    else -> stringResource(R.string.ts_mode_managed)
}

@Composable
private fun modeDetail(mode: String): String = when (mode) {
    "non-interactive" -> stringResource(R.string.ts_mode_noninteractive_desc)
    "invisible" -> stringResource(R.string.ts_mode_invisible_desc)
    else -> stringResource(R.string.ts_mode_managed_desc)
}
