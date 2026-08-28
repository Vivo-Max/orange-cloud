package jiamin.chen.orangecloud.ui.devplatform

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.design.SkyBackground
import jiamin.chen.orangecloud.core.design.SkyHeader
import jiamin.chen.orangecloud.core.design.onSky
import jiamin.chen.orangecloud.core.design.rememberSkyPhase
import jiamin.chen.orangecloud.core.design.theme.OcOrange
import jiamin.chen.orangecloud.data.model.AIGateway
import jiamin.chen.orangecloud.data.model.AIModel
import jiamin.chen.orangecloud.data.model.CFQueue
import jiamin.chen.orangecloud.data.model.DurableObjectMemory
import jiamin.chen.orangecloud.data.model.DurableObjectNamespace
import jiamin.chen.orangecloud.data.model.HyperdriveConfig
import jiamin.chen.orangecloud.ui.storage.formatBytes
import jiamin.chen.orangecloud.ui.storage.StorageGroupedListBody
import jiamin.chen.orangecloud.ui.storage.StorageListBody
import jiamin.chen.orangecloud.ui.storage.StorageRow

@Composable
fun DeveloperHubScreen(
    onOpenWorkers: () -> Unit,
    onOpenWorkersAI: () -> Unit,
    onOpenAIGateway: () -> Unit,
    onOpenQueues: () -> Unit,
    onOpenHyperdrive: () -> Unit,
    onOpenDurableObjects: () -> Unit,
    onOpenPages: () -> Unit,
    onOpenAssistant: () -> Unit = {},
) {
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    SkyBackground(phase = phase) {
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState())) {
            Text(
                stringResource(R.string.nav_dev_platform),
                color = onSky,
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp),
            )
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StorageRow(Icons.Outlined.AutoAwesome, stringResource(R.string.assistant_title), stringResource(R.string.dev_assistant_sub), onClick = onOpenAssistant)
                StorageRow(Icons.Outlined.Bolt, stringResource(R.string.nav_workers), stringResource(R.string.dev_workers_sub), onClick = onOpenWorkers)
                StorageRow(Icons.Outlined.Psychology, stringResource(R.string.dev_workers_ai), stringResource(R.string.dev_workers_ai_sub), onClick = onOpenWorkersAI)
                StorageRow(Icons.Outlined.AutoAwesome, stringResource(R.string.dev_ai_gateway), stringResource(R.string.dev_ai_gateway_sub), onClick = onOpenAIGateway)
                StorageRow(Icons.Outlined.Layers, stringResource(R.string.dev_queues), stringResource(R.string.dev_queues_sub), onClick = onOpenQueues)
                StorageRow(Icons.Outlined.Storage, stringResource(R.string.dev_hyperdrive), stringResource(R.string.dev_hyperdrive_sub), onClick = onOpenHyperdrive)
                StorageRow(Icons.Outlined.Memory, stringResource(R.string.dev_durable_objects), stringResource(R.string.dev_durable_objects_sub), onClick = onOpenDurableObjects)
                StorageRow(Icons.Outlined.Web, stringResource(R.string.pages_title), stringResource(R.string.dev_pages_sub), onClick = onOpenPages)
                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueuesScreen(onBack: () -> Unit, viewModel: QueuesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showCreate by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<CFQueue?>(null) }
    LaunchedEffect(Unit) { viewModel.errors.collect { snackbar.showSnackbar(it) } }

    WritableScaffold(
        title = stringResource(R.string.dev_queues),
        isLoading = state.isLoading || busy,
        onBack = onBack,
        onRefresh = { viewModel.load() },
        canCreate = viewModel.canWrite && !state.missingScope,
        createDescription = stringResource(R.string.dev_queue_create),
        onCreate = { showCreate = true },
        snackbar = snackbar,
    ) { onSky ->
        StorageListBody(state, onSky, Icons.Outlined.Layers, stringResource(R.string.dev_queues_empty), { viewModel.load() }) { q: CFQueue ->
            StorageRow(
                Icons.Outlined.Layers,
                q.name,
                stringResource(R.string.dev_queue_sub, q.producersTotalCount ?: 0, q.consumersTotalCount ?: 0),
                showChevron = viewModel.canWrite,
                onClick = if (viewModel.canWrite) ({ selected = q }) else null,
            )
        }
    }

    if (showCreate) {
        ModalBottomSheet(onDismissRequest = { if (!busy) showCreate = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            SingleFieldForm(
                title = stringResource(R.string.dev_queue_create),
                label = stringResource(R.string.dev_queue_name),
                isSaving = busy,
                onSave = { viewModel.create(it); showCreate = false },
            )
        }
    }
    selected?.let { q ->
        ModalBottomSheet(onDismissRequest = { selected = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(q.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                val paused = q.settings?.deliveryPaused == true
                ActionRow(
                    if (paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                    stringResource(if (paused) R.string.dev_queue_resume else R.string.dev_queue_pause),
                ) { viewModel.togglePause(q, !paused); selected = null }
                ActionRow(Icons.Outlined.DeleteSweep, stringResource(R.string.dev_queue_purge)) { viewModel.purge(q); selected = null }
                ActionRow(Icons.Outlined.Delete, stringResource(R.string.dns_delete), destructive = true) { viewModel.delete(q); selected = null }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIGatewayScreen(onBack: () -> Unit, viewModel: AIGatewayViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showCreate by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<AIGateway?>(null) }
    LaunchedEffect(Unit) { viewModel.errors.collect { snackbar.showSnackbar(it) } }

    WritableScaffold(
        title = stringResource(R.string.dev_ai_gateway),
        isLoading = state.isLoading || busy,
        onBack = onBack,
        onRefresh = { viewModel.load() },
        canCreate = viewModel.canWrite && !state.missingScope,
        createDescription = stringResource(R.string.dev_gateway_create),
        onCreate = { showCreate = true },
        snackbar = snackbar,
    ) { onSky ->
        StorageListBody(state, onSky, Icons.Outlined.AutoAwesome, stringResource(R.string.dev_ai_gateway_empty), { viewModel.load() }) { g: AIGateway ->
            DeletableRow(Icons.Outlined.AutoAwesome, g.id, stringResource(R.string.dev_gateway_sub, g.cacheTtl ?: 0), viewModel.canWrite) { toDelete = g }
        }
    }

    if (showCreate) {
        ModalBottomSheet(onDismissRequest = { if (!busy) showCreate = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            GatewayCreateForm(isSaving = busy) { id, ttl, limit, logs -> viewModel.create(id, ttl, limit, logs); showCreate = false }
        }
    }
    toDelete?.let { g ->
        DeleteDialog(g.id, { viewModel.delete(g); toDelete = null }, { toDelete = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GatewayCreateForm(isSaving: Boolean, onSave: (id: String, cacheTtl: Int, rateLimit: Int, collectLogs: Boolean) -> Unit) {
    var id by rememberSaveable { mutableStateOf("") }
    var ttl by rememberSaveable { mutableStateOf("0") }
    var limit by rememberSaveable { mutableStateOf("0") }
    var logs by rememberSaveable { mutableStateOf(true) }
    val canSave = id.matches(Regex("^[a-z0-9-]+$")) && !isSaving
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.dev_gateway_create), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        OutlinedTextField(id, { id = it.lowercase() }, label = { Text(stringResource(R.string.dev_gateway_id)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(ttl, { v -> ttl = v.filter { it.isDigit() } }, label = { Text(stringResource(R.string.dev_gateway_cache_ttl)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(limit, { v -> limit = v.filter { it.isDigit() } }, label = { Text(stringResource(R.string.dev_gateway_rate_limit)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.dev_gateway_collect_logs), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            Switch(checked = logs, onCheckedChange = { logs = it })
        }
        SaveButton(canSave, isSaving) { onSave(id, ttl.toIntOrNull() ?: 0, limit.toIntOrNull() ?: 0, logs) }
    }
}

@Composable
fun DurableObjectsScreen(onBack: () -> Unit, viewModel: DurableObjectsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val expanded by viewModel.expanded.collectAsStateWithLifecycle()
    val memory by viewModel.memory.collectAsStateWithLifecycle()
    DevListScaffold(stringResource(R.string.dev_durable_objects), state.isLoading, onBack, { viewModel.load() }) { onSky ->
        StorageListBody(state, onSky, Icons.Outlined.Memory, stringResource(R.string.dev_durable_objects_empty), { viewModel.load() }) { ns: DurableObjectNamespace ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StorageRow(
                    Icons.Outlined.Memory,
                    ns.name ?: ns.className ?: ns.id,
                    listOfNotNull(ns.className, ns.script).joinToString(" · ").ifBlank { null },
                    showChevron = false,
                    onClick = { viewModel.toggleMemory(ns.id) },
                )
                if (expanded == ns.id) DurableObjectMemoryCard(memory[ns.id], loaded = memory.containsKey(ns.id))
            }
        }
    }
}

/** 24h 内存分位数。口径是 V8 isolate 而非单个对象，footer 必须写清楚，否则会被读成「这个对象占了多少」。 */
@Composable
private fun DurableObjectMemoryCard(memory: DurableObjectMemory?, loaded: Boolean) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.do_memory_title),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            when {
                !loaded -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                memory == null -> Text(
                    stringResource(R.string.do_memory_unavailable),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> {
                    listOf(
                        "P50" to memory.p50,
                        "P90" to memory.p90,
                        "P99" to memory.p99,
                        "P99.9" to memory.p999,
                    ).forEach { (label, value) ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.weight(1f))
                            Text(
                                formatBytes(value),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.do_memory_footer),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HyperdriveScreen(onBack: () -> Unit, viewModel: HyperdriveViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showCreate by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<HyperdriveConfig?>(null) }
    LaunchedEffect(Unit) { viewModel.errors.collect { snackbar.showSnackbar(it) } }

    WritableScaffold(
        title = stringResource(R.string.dev_hyperdrive),
        isLoading = state.isLoading || busy,
        onBack = onBack,
        onRefresh = { viewModel.load() },
        canCreate = viewModel.canWrite && !state.missingScope,
        createDescription = stringResource(R.string.dev_hyperdrive_create),
        onCreate = { showCreate = true },
        snackbar = snackbar,
    ) { onSky ->
        StorageListBody(state, onSky, Icons.Outlined.Storage, stringResource(R.string.dev_hyperdrive_empty), { viewModel.load() }) { c: HyperdriveConfig ->
            HyperdriveRow(c, viewModel.canWrite, { viewModel.toggleCaching(c, it) }, { toDelete = c })
        }
    }

    if (showCreate) {
        ModalBottomSheet(onDismissRequest = { if (!busy) showCreate = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            HyperdriveCreateForm(isSaving = busy) { name, scheme, host, port, db, user, pw -> viewModel.create(name, scheme, host, port, db, user, pw); showCreate = false }
        }
    }
    toDelete?.let { c ->
        DeleteDialog(c.displayName, { viewModel.delete(c); toDelete = null }, { toDelete = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HyperdriveCreateForm(isSaving: Boolean, onSave: (name: String, scheme: String, host: String, port: Int, db: String, user: String, password: String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var scheme by rememberSaveable { mutableStateOf("postgres") }
    var host by rememberSaveable { mutableStateOf("") }
    var port by rememberSaveable { mutableStateOf("5432") }
    var db by rememberSaveable { mutableStateOf("") }
    var user by rememberSaveable { mutableStateOf("") }
    var pw by rememberSaveable { mutableStateOf("") }
    val canSave = name.isNotBlank() && host.isNotBlank() && db.isNotBlank() && user.isNotBlank() && pw.isNotBlank() && !isSaving
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.dev_hyperdrive_create), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.dev_hd_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = if (scheme == "postgres") "PostgreSQL" else "MySQL",
                onValueChange = {}, readOnly = true,
                label = { Text(stringResource(R.string.dev_hd_scheme)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("PostgreSQL") }, onClick = { scheme = "postgres"; port = "5432"; expanded = false })
                DropdownMenuItem(text = { Text("MySQL") }, onClick = { scheme = "mysql"; port = "3306"; expanded = false })
            }
        }
        OutlinedTextField(host, { host = it }, label = { Text(stringResource(R.string.dev_hd_host)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(port, { v -> port = v.filter { it.isDigit() } }, label = { Text(stringResource(R.string.dev_hd_port)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(db, { db = it }, label = { Text(stringResource(R.string.dev_hd_database)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(user, { user = it }, label = { Text(stringResource(R.string.dev_hd_user)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(pw, { pw = it }, label = { Text(stringResource(R.string.dev_hd_password)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        SaveButton(canSave, isSaving) { onSave(name, scheme, host, port.toIntOrNull() ?: 5432, db, user, pw) }
    }
}

@Composable
fun WorkersAIScreen(
    onBack: () -> Unit,
    onOpenModel: (AIModel) -> Unit,
    viewModel: WorkersAIViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DevListScaffold(stringResource(R.string.dev_workers_ai), state.isLoading, onBack, { viewModel.load() }) { onSky ->
        StorageGroupedListBody(
            state = state,
            onSky = onSky,
            emptyIcon = Icons.Outlined.Psychology,
            emptyText = stringResource(R.string.dev_workers_ai_empty),
            onRetry = { viewModel.load() },
            groupOf = { it.taskName },
        ) { m: AIModel ->
            StorageRow(
                Icons.Outlined.Psychology,
                m.shortName,
                m.description?.takeIf { it.isNotBlank() } ?: m.name,
                onClick = { onOpenModel(m) },
            )
        }
    }
}

@Composable
fun AIRunScreen(onBack: () -> Unit, viewModel: AIRunViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    SkyBackground(phase = phase) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            SkyHeader(
                title = state.model.substringAfterLast('/'),
                onSky = onSky,
                isLoading = state.isRunning,
                onRefresh = {},
                onBack = onBack,
                titleSize = 20,
                backDescription = stringResource(R.string.common_back),
            )
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 模型信息卡（说明 / 任务 / 模型 ID，参考 iOS 详情页）
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (state.description.isNotBlank()) {
                            Text(state.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        if (state.task.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.dev_ai_info_task), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.weight(1f))
                                Text(state.task, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(stringResource(R.string.dev_ai_info_model), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            SelectionContainer {
                                Text(state.model, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                when {
                    state.isRunnable && state.canRun -> AIPlayground(state, viewModel::updatePrompt, viewModel::run, onSky)

                    state.isRunnable ->
                        Text(stringResource(R.string.dev_ai_needs_write), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    else ->
                        Text(stringResource(R.string.dev_ai_unsupported), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** 试运行区：文本生成出文字，文生图出图片（同一套输入 + 运行按钮）。 */
@Composable
private fun ColumnScope.AIPlayground(
    state: AIRunUiState,
    onPromptChange: (String) -> Unit,
    onRun: () -> Unit,
    onSky: Color,
) {
    val isImage = state.isTextToImage
    Text(
        stringResource(if (isImage) R.string.dev_ai_image_playground else R.string.dev_ai_playground),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = onSky,
    )
    OutlinedTextField(
        value = state.prompt,
        onValueChange = onPromptChange,
        label = { Text(stringResource(if (isImage) R.string.dev_ai_image_prompt else R.string.dev_ai_prompt)) },
        minLines = 3,
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = onRun,
        enabled = state.prompt.isNotBlank() && !state.isRunning,
        colors = ButtonDefaults.buttonColors(containerColor = OcOrange, contentColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (state.isRunning) {
            CircularProgressIndicator(Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp, color = Color.White)
        } else {
            Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, modifier = Modifier.width(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(stringResource(if (isImage) R.string.dev_ai_image_generate else R.string.dev_ai_run))
    }
    Text(
        stringResource(if (isImage) R.string.dev_ai_image_run_note else R.string.dev_ai_run_note),
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    state.errorRes?.let {
        Text(stringResource(it), color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
    }
    state.error?.let {
        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
    }
    if (state.response.isNotBlank()) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            SelectionContainer {
                Text(
                    state.response,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
    state.image?.let { AIImageResult(it) }
}

/** 生成结果图片：后台线程降采样解码，失败给明确提示；下方提供分享入口。 */
@Composable
private fun AIImageResult(payload: AIImagePayload) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val chooserTitle = stringResource(R.string.dev_ai_image_share)
    val failedMessage = stringResource(R.string.dev_ai_image_share_failed)
    var shareError by remember(payload) { mutableStateOf(false) }

    val bitmap by produceState<Bitmap?>(initialValue = null, payload) {
        value = withContext(Dispatchers.Default) { decodeSampledBitmap(payload.bytes) }
    }

    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val bmp = bitmap
            if (bmp == null) {
                Text(
                    stringResource(R.string.dev_ai_image_decoding),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                )
            } else {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = stringResource(R.string.dev_ai_image_result),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                )
                TextButton(onClick = {
                    scope.launch { shareError = !shareGeneratedImage(context, payload.bytes, chooserTitle) }
                }) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.dev_ai_image_share))
                }
                if (shareError) {
                    Text(failedMessage, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        }
    }
}

/** 可写列表外壳：晨昏背景 + 头部 + 内容 + 创建 FAB + Snackbar。 */
@Composable
private fun WritableScaffold(
    title: String,
    isLoading: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    canCreate: Boolean,
    createDescription: String,
    onCreate: () -> Unit,
    snackbar: SnackbarHostState,
    content: @Composable (onSky: Color) -> Unit,
) {
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                SkyHeader(
                    title = title,
                    onSky = onSky,
                    isLoading = isLoading,
                    onRefresh = onRefresh,
                    onBack = onBack,
                    titleSize = 22,
                    backDescription = stringResource(R.string.common_back),
                    refreshDescription = stringResource(R.string.common_refresh),
                )
                content(onSky)
            }
            if (canCreate) {
                FloatingActionButton(
                    onClick = onCreate,
                    containerColor = OcOrange,
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).systemBarsPadding(),
                ) { Icon(Icons.Outlined.Add, contentDescription = createDescription) }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).systemBarsPadding())
        }
    }
}

/** Hyperdrive 行：名称 + 源摘要 + 查询缓存开关 + 删除。 */
@Composable
private fun HyperdriveRow(config: HyperdriveConfig, canWrite: Boolean, onToggleCaching: (Boolean) -> Unit, onDelete: () -> Unit) {
    val cachingOn = config.caching?.disabled != true
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 8.dp, end = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Storage, contentDescription = null, tint = OcOrange, modifier = Modifier.width(24.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(config.displayName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                    config.origin?.summary?.let { Text(it, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
                }
                if (canWrite) {
                    IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.dns_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 38.dp, end = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.dev_hd_caching), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Switch(checked = cachingOn, onCheckedChange = onToggleCaching, enabled = canWrite)
            }
        }
    }
}

/** 列表行 + 写权限时右侧删除按钮。 */
@Composable
private fun DeletableRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String?, canWrite: Boolean, onDelete: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = OcOrange, modifier = Modifier.width(24.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                subtitle?.let { Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
            }
            if (canWrite) {
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.dns_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun ActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, destructive: Boolean = false, onClick: () -> Unit) {
    val color = if (destructive) Color(0xFFE5484D) else MaterialTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.width(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 15.sp, color = color, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) { Text(stringResource(R.string.dev_action_do), color = if (destructive) Color(0xFFE5484D) else OcOrange) }
    }
}

@Composable
private fun DeleteDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dev_delete_confirm)) },
        text = { Text(name) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.dns_delete), color = Color(0xFFE5484D)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
private fun SaveButton(canSave: Boolean, isSaving: Boolean, onSave: () -> Unit) {
    Button(
        onClick = onSave,
        enabled = canSave,
        colors = ButtonDefaults.buttonColors(containerColor = OcOrange, contentColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (isSaving) { CircularProgressIndicator(Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp, color = Color.White); Spacer(Modifier.width(8.dp)) }
        Text(stringResource(R.string.dns_save))
    }
}

@Composable
private fun SingleFieldForm(title: String, label: String, isSaving: Boolean, onSave: (String) -> Unit) {
    var value by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        OutlinedTextField(value, { value = it }, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        SaveButton(value.isNotBlank() && !isSaving, isSaving) { onSave(value) }
    }
}

/** 开发者平台只读列表的统一外壳（晨昏背景 + 头部 + 内容回调注入 onSky）。 */
@Composable
private fun DevListScaffold(
    title: String,
    isLoading: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    content: @Composable (onSky: Color) -> Unit,
) {
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    SkyBackground(phase = phase) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            SkyHeader(
                title = title,
                onSky = onSky,
                isLoading = isLoading,
                onRefresh = onRefresh,
                onBack = onBack,
                titleSize = 22,
                backDescription = stringResource(R.string.common_back),
                refreshDescription = stringResource(R.string.common_refresh),
            )
            content(onSky)
        }
    }
}
