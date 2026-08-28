package jiamin.chen.orangecloud.ui.redirects

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRightAlt
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
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
import jiamin.chen.orangecloud.data.model.RedirectList
import jiamin.chen.orangecloud.data.model.RedirectListItem
import jiamin.chen.orangecloud.ui.storage.StorageListBody
import jiamin.chen.orangecloud.ui.storage.StorageRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedirectListsScreen(
    onBack: () -> Unit,
    onOpenList: (listId: String, listName: String) -> Unit,
    viewModel: RedirectListsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreate by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<RedirectList?>(null) }

    val createdMsg = stringResource(R.string.redirect_created)
    val deletedMsg = stringResource(R.string.redirect_deleted)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                RedirectEvent.Created -> { showCreate = false; snackbarHostState.showSnackbar(createdMsg) }
                RedirectEvent.Deleted -> snackbarHostState.showSnackbar(deletedMsg)
                is RedirectEvent.Error -> snackbarHostState.showSnackbar(event.message ?: "")
            }
        }
    }

    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                SkyHeader(
                    title = stringResource(R.string.redirect_title),
                    onSky = onSky,
                    isLoading = state.isLoading,
                    onRefresh = { viewModel.load() },
                    onBack = onBack,
                    titleSize = 22,
                    backDescription = stringResource(R.string.common_back),
                    refreshDescription = stringResource(R.string.common_refresh),
                )
                StorageListBody(
                    state = state,
                    onSky = onSky,
                    emptyIcon = Icons.Outlined.Link,
                    emptyText = stringResource(R.string.redirect_empty),
                    onRetry = { viewModel.load() },
                ) { list ->
                    StorageRow(
                        icon = Icons.Outlined.Link,
                        title = list.name ?: list.id,
                        subtitle = stringResource(R.string.redirect_item_count, list.numItems ?: 0),
                        onClick = { onOpenList(list.id, list.name.orEmpty()) },
                        onLongClick = if (viewModel.canWrite) ({ toDelete = list }) else null,
                    )
                }
            }
            if (viewModel.canWrite && !state.missingScope) {
                FloatingActionButton(
                    onClick = { showCreate = true },
                    containerColor = OcOrange,
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).systemBarsPadding(),
                ) { Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.redirect_create_title)) }
            }
            SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).systemBarsPadding())
        }
    }

    if (showCreate) {
        ModalBottomSheet(onDismissRequest = { if (!busy) showCreate = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            CreateListForm(isSaving = busy, onSave = { name, desc -> viewModel.create(name, desc) })
        }
    }
    toDelete?.let { list ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.redirect_delete_list_title)) },
            text = { Text(list.name ?: list.id) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(list); toDelete = null }) { Text(stringResource(R.string.dns_delete), color = Color(0xFFE5484D)) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
private fun CreateListForm(isSaving: Boolean, onSave: (name: String, description: String?) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    // 列表名只允许小写字母/数字/下划线（CF 约束）。
    val canSave = name.matches(Regex("^[a-z][a-z0-9_]*$")) && !isSaving
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.redirect_create_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        OutlinedTextField(value = name, onValueChange = { name = it.lowercase() }, label = { Text(stringResource(R.string.redirect_field_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.redirect_name_hint), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text(stringResource(R.string.redirect_field_description)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = { onSave(name, description.takeIf { it.isNotBlank() }) },
            enabled = canSave,
            colors = ButtonDefaults.buttonColors(containerColor = OcOrange, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isSaving) { CircularProgressIndicator(Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp, color = Color.White); Spacer(Modifier.width(8.dp)) }
            Text(stringResource(R.string.dns_save))
        }
    }
}

// MARK: - 条目列表

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedirectItemsScreen(
    onBack: () -> Unit,
    viewModel: RedirectItemsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val snackbarHostState = remember { SnackbarHostState() }
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<RedirectListItem?>(null) }

    val createdMsg = stringResource(R.string.redirect_created)
    val deletedMsg = stringResource(R.string.redirect_deleted)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                RedirectEvent.Created -> { showAdd = false; snackbarHostState.showSnackbar(createdMsg) }
                RedirectEvent.Deleted -> snackbarHostState.showSnackbar(deletedMsg)
                is RedirectEvent.Error -> snackbarHostState.showSnackbar(event.message ?: "")
            }
        }
    }

    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                SkyHeader(
                    title = state.listName.ifBlank { stringResource(R.string.redirect_title) },
                    onSky = onSky,
                    isLoading = state.isLoading || state.isBusy,
                    onRefresh = { viewModel.load() },
                    onBack = onBack,
                    titleSize = 22,
                    backDescription = stringResource(R.string.common_back),
                    refreshDescription = stringResource(R.string.common_refresh),
                )
                when {
                    state.missingScope ->
                        SkyEmptyState(Icons.Outlined.Lock, stringResource(R.string.scope_missing), onSky, stringResource(R.string.common_refresh)) { viewModel.load() }
                    state.items.isEmpty() && state.isLoading ->
                        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = onSky) }
                    state.items.isEmpty() ->
                        SkyEmptyState(Icons.Outlined.Link, stringResource(R.string.redirect_items_empty), onSky, stringResource(R.string.common_refresh)) { viewModel.load() }
                    else -> LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = if (state.canWrite) 96.dp else 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.items, key = { it.id }) { item ->
                            RedirectItemRow(item, state.canWrite) { toDelete = item }
                        }
                    }
                }
            }
            if (state.canWrite && !state.missingScope) {
                FloatingActionButton(
                    onClick = { showAdd = true },
                    containerColor = OcOrange,
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).systemBarsPadding(),
                ) { Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.redirect_add_item)) }
            }
            SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).systemBarsPadding())
        }
    }

    if (showAdd) {
        ModalBottomSheet(onDismissRequest = { if (!state.isBusy) showAdd = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            AddRedirectForm(isSaving = state.isBusy, onSave = { src, dst, code, query, subpath -> viewModel.addRedirect(src, dst, code, query, subpath) })
        }
    }
    toDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.redirect_delete_item_title)) },
            text = { Text(item.redirect?.sourceUrl ?: item.id) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(item); toDelete = null }) { Text(stringResource(R.string.dns_delete), color = Color(0xFFE5484D)) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
private fun RedirectItemRow(item: RedirectListItem, canWrite: Boolean, onDelete: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.redirect?.sourceUrl.orEmpty(), fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowRightAlt, contentDescription = null, tint = OcOrange, modifier = Modifier.height(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(item.redirect?.targetUrl.orEmpty(), fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                item.redirect?.statusCode?.let {
                    Text(stringResource(R.string.redirect_status_code, it), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
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
private fun AddRedirectForm(
    isSaving: Boolean,
    onSave: (source: String, target: String, statusCode: Int, preserveQuery: Boolean, subpathMatching: Boolean) -> Unit,
) {
    var source by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf("") }
    var statusCode by rememberSaveable { mutableStateOf(301) }
    var preserveQuery by rememberSaveable { mutableStateOf(true) }
    var subpath by rememberSaveable { mutableStateOf(false) }
    val canSave = source.isNotBlank() && target.isNotBlank() && !isSaving

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.redirect_add_item), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        OutlinedTextField(value = source, onValueChange = { source = it }, label = { Text(stringResource(R.string.redirect_field_source)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text(stringResource(R.string.redirect_field_target)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        StatusCodeField(statusCode) { statusCode = it }
        ToggleLine(stringResource(R.string.redirect_preserve_query), preserveQuery) { preserveQuery = it }
        ToggleLine(stringResource(R.string.redirect_subpath_matching), subpath) { subpath = it }
        Button(
            onClick = { onSave(source, target, statusCode, preserveQuery, subpath) },
            enabled = canSave,
            colors = ButtonDefaults.buttonColors(containerColor = OcOrange, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isSaving) { CircularProgressIndicator(Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp, color = Color.White); Spacer(Modifier.width(8.dp)) }
            Text(stringResource(R.string.dns_save))
        }
    }
}

@Composable
private fun ToggleLine(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusCodeField(code: Int, onSelect: (Int) -> Unit) {
    val options = listOf(301 to R.string.redirect_301, 302 to R.string.redirect_302, 307 to R.string.redirect_307, 308 to R.string.redirect_308)
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = stringResource(options.firstOrNull { it.first == code }?.second ?: R.string.redirect_301),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.redirect_field_status)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, labelRes) ->
                DropdownMenuItem(text = { Text(stringResource(labelRes)) }, onClick = { onSelect(value); expanded = false })
            }
        }
    }
}
