package jiamin.chen.orangecloud.ui.workers

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.design.SkyBackground
import jiamin.chen.orangecloud.core.design.SkyEmptyState
import jiamin.chen.orangecloud.core.design.SkyHeader
import jiamin.chen.orangecloud.core.design.onSky
import jiamin.chen.orangecloud.core.design.rememberSkyPhase
import jiamin.chen.orangecloud.core.design.theme.OcOrange
import jiamin.chen.orangecloud.core.util.copyToClipboard
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ConsoleBg = Color(0xFF1A1410)
private val ConsoleDim = Color(0xFF8A8178)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerTailScreen(
    onBack: () -> Unit,
    viewModel: WorkerTailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lines by viewModel.lines.collectAsStateWithLifecycle()
    val visibleLines by viewModel.visibleLines.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val levelFilter by viewModel.levelFilter.collectAsStateWithLifecycle()
    val paused by viewModel.paused.collectAsStateWithLifecycle()
    val pausedNewCount by viewModel.pausedNewCount.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val copiedMessage = stringResource(R.string.tail_copied)
    // 点击行展开的详情（底部弹层，不离开日志流页面）
    var detailLine by remember { mutableStateOf<TailLogLine?>(null) }
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(visibleLines.size, paused) {
        if (!paused && visibleLines.isNotEmpty()) listState.animateScrollToItem(visibleLines.lastIndex)
    }

    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                SkyHeader(
                    title = viewModel.scriptName,
                    onSky = onSky,
                    isLoading = false,
                    onRefresh = {},
                    onBack = onBack,
                    titleSize = 20,
                    backDescription = stringResource(R.string.common_back),
                )

                if (viewModel.missingScope) {
                    SkyEmptyState(Icons.Outlined.Lock, stringResource(R.string.scope_missing), onSky, stringResource(R.string.common_refresh)) {}
                    return@Column
                }

                StatusBar(state, paused, pausedNewCount, onSky, viewModel::togglePause, viewModel::clear, viewModel::start)

                TailFilterBar(
                    query = query,
                    onQueryChange = viewModel::updateQuery,
                    level = levelFilter,
                    onLevelChange = viewModel::setLevelFilter,
                    onSky = onSky,
                )

                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ConsoleBg),
                ) {
                    when {
                        lines.isEmpty() -> ConsoleHint(stringResource(R.string.tail_waiting))
                        visibleLines.isEmpty() -> ConsoleHint(stringResource(R.string.tail_filter_empty))
                        else -> LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(visibleLines, key = { it.id }) { line ->
                                LogRow(
                                    line = line,
                                    onCopy = {
                                        copyToClipboard(context, line.text)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        scope.launch { snackbar.showSnackbar(copiedMessage) }
                                    },
                                    onOpen = { detailLine = line },
                                )
                            }
                        }
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).systemBarsPadding())

            detailLine?.let { line ->
                ModalBottomSheet(
                    onDismissRequest = { detailLine = null },
                    sheetState = detailSheetState,
                ) {
                    LogDetailSheet(
                        line = line,
                        onCopyAll = {
                            copyToClipboard(context, line.text)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch { snackbar.showSnackbar(copiedMessage) }
                        },
                        onClose = { detailLine = null },
                    )
                }
            }
        }
    }
}

// MARK: 日志详情

/**
 * 一行日志按已有字段拆出的展示分区。不改 ViewModel 数据结构，
 * 只在展示层还原 event / exception 行的既定拼接格式。
 */
private data class TailLineDetail(
    val method: String? = null,
    val url: String? = null,
    val outcome: String? = null,
    val cron: String? = null,
    val exceptionName: String? = null,
)

private fun parseDetail(line: TailLogLine): TailLineDetail = when (line.level) {
    "event" -> {
        val parts = line.text.split(" → ")
        val head = parts.firstOrNull().orEmpty()
        val outcome = if (parts.size > 1) parts.drop(1).joinToString(" → ") else null
        if (head.startsWith("cron ")) {
            TailLineDetail(cron = head.removePrefix("cron "), outcome = outcome)
        } else {
            val tokens = head.split(" ", limit = 2)
            if (tokens.size == 2) {
                TailLineDetail(method = tokens[0], url = tokens[1], outcome = outcome)
            } else {
                TailLineDetail(outcome = outcome)
            }
        }
    }
    "exception" -> {
        val name = line.text.substringBefore(": ", "")
        TailLineDetail(exceptionName = name.ifEmpty { null })
    }
    else -> TailLineDetail()
}

private val DetailTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

@Composable
private fun LogDetailSheet(
    line: TailLogLine,
    onCopyAll: () -> Unit,
    onClose: () -> Unit,
) {
    val detail = remember(line.id) { parseDetail(line) }
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.tail_detail_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onCopyAll) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null, Modifier.size(16.dp), tint = OcOrange)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.tail_detail_copy_all), color = OcOrange, fontSize = 13.sp)
            }
        }

        DetailCard {
            DetailField(stringResource(R.string.tail_detail_level), line.level, levelColor(line.level))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            DetailField(
                stringResource(R.string.tail_detail_time),
                DetailTimeFormatter.format(Instant.ofEpochMilli(line.timestampMs)),
                MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (detail.method != null || detail.cron != null || detail.exceptionName != null || detail.outcome != null) {
            DetailCard {
                detail.method?.let {
                    DetailField(stringResource(R.string.tail_detail_method), it, MaterialTheme.colorScheme.onSurface)
                }
                detail.url?.let {
                    DetailField(stringResource(R.string.tail_detail_url), it, MaterialTheme.colorScheme.onSurface)
                }
                detail.cron?.let {
                    DetailField(stringResource(R.string.tail_detail_cron), it, MaterialTheme.colorScheme.onSurface)
                }
                detail.exceptionName?.let {
                    DetailField(stringResource(R.string.tail_detail_exception), it, Color(0xFFFF6B6B))
                }
                detail.outcome?.let {
                    DetailField(stringResource(R.string.tail_detail_outcome), it, MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        DetailCard {
            Text(
                stringResource(R.string.tail_detail_body),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // 详情态才开框选：列表行开了会吞掉长按复制手势
            SelectionContainer {
                Text(
                    text = line.text,
                    color = levelColor(line.level),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            Text(
                stringResource(R.string.tail_detail_select_hint),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.tail_detail_close))
        }
    }
}

@Composable
private fun DetailCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(14.dp),
    ) {
        content()
    }
}

/** 字段行：标签固定宽、值可框选（值恒定等宽，URL/JSON 不被 RTL 镜像干扰）。 */
@Composable
private fun DetailField(label: String, value: String, valueColor: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(76.dp),
        )
        SelectionContainer(Modifier.weight(1f)) {
            Text(
                text = value,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = valueColor,
            )
        }
    }
}

/** 关键词 + 级别过滤条：只影响展示，原始日志仍全量保留在 ViewModel。 */
@Composable
private fun TailFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    level: TailLevelFilter,
    onLevelChange: (TailLevelFilter) -> Unit,
    onSky: Color,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = { Text(stringResource(R.string.tail_search_hint), fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, Modifier.size(18.dp)) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.tail_search_clear), Modifier.size(18.dp))
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = onSky,
                unfocusedTextColor = onSky,
                focusedBorderColor = OcOrange,
                unfocusedBorderColor = onSky.copy(alpha = 0.35f),
                focusedLeadingIconColor = onSky.copy(alpha = 0.8f),
                unfocusedLeadingIconColor = onSky.copy(alpha = 0.6f),
                focusedTrailingIconColor = onSky.copy(alpha = 0.8f),
                unfocusedTrailingIconColor = onSky.copy(alpha = 0.6f),
                focusedPlaceholderColor = onSky.copy(alpha = 0.55f),
                unfocusedPlaceholderColor = onSky.copy(alpha = 0.55f),
                cursorColor = OcOrange,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TailLevelFilter.entries.forEach { option ->
                val selected = option == level
                FilterChip(
                    selected = selected,
                    onClick = { onLevelChange(option) },
                    label = { Text(stringResource(option.labelRes), fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        labelColor = onSky.copy(alpha = 0.85f),
                        selectedContainerColor = OcOrange.copy(alpha = 0.22f),
                        selectedLabelColor = onSky,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = onSky.copy(alpha = 0.3f),
                        selectedBorderColor = OcOrange,
                    ),
                )
            }
        }
    }
}

private val TailLevelFilter.labelRes: Int
    @StringRes get() = when (this) {
        TailLevelFilter.ALL -> R.string.tail_level_all
        TailLevelFilter.EVENT -> R.string.tail_level_event
        TailLevelFilter.LOG -> R.string.tail_level_log
        TailLevelFilter.DEBUG -> R.string.tail_level_debug
        TailLevelFilter.INFO -> R.string.tail_level_info
        TailLevelFilter.WARN -> R.string.tail_level_warn
        TailLevelFilter.ERROR -> R.string.tail_level_error
    }

@Composable
private fun ConsoleHint(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = ConsoleDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun StatusBar(
    state: TailConnState,
    paused: Boolean,
    pausedNewCount: Int,
    onSky: Color,
    onPause: () -> Unit,
    onClear: () -> Unit,
    onReconnect: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(state)
        Spacer(Modifier.width(8.dp))
        Text(statusText(state), color = onSky, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        if (paused) {
            Spacer(Modifier.width(8.dp))
            // 暂停只冻结视图，事件仍在入 buffer；点一下即恢复并滚到底
            Text(
                text = if (pausedNewCount > 0) {
                    stringResource(R.string.tail_paused_new_count, pausedNewCount)
                } else {
                    stringResource(R.string.tail_paused)
                },
                color = OcOrange,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onPause),
            )
        }
        Spacer(Modifier.weight(1f))
        if (state is TailConnState.Disconnected) {
            TextButton(onClick = onReconnect) { Text(stringResource(R.string.tail_reconnect)) }
        } else {
            IconButton(onClick = onPause) {
                Icon(
                    if (paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                    contentDescription = stringResource(if (paused) R.string.tail_resume else R.string.tail_pause),
                    tint = onSky,
                )
            }
        }
        IconButton(onClick = onClear) {
            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.tail_clear), tint = onSky)
        }
    }
}

@Composable
private fun StatusDot(state: TailConnState) {
    when (state) {
        is TailConnState.Connecting -> CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 2.dp, color = OcOrange)
        else -> Box(
            Modifier.size(10.dp).clip(CircleShape).background(
                when (state) {
                    TailConnState.Connected -> Color(0xFF2FBF71)
                    is TailConnState.Disconnected -> Color(0xFFE5484D)
                    else -> Color(0xFF9AA0A6)
                },
            ),
        )
    }
}

/**
 * 单行日志：轻点展开详情（详情里才可框选），长按复制整行。
 * 控制台是等宽正文，行尾图标会破坏对齐，故复制走长按；
 * combinedClickable 由同一个手势检测器同时仲裁点击与长按，两者不会互相吞。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LogRow(line: TailLogLine, onCopy: () -> Unit, onOpen: () -> Unit) {
    val openLabel = stringResource(R.string.tail_row_hint)
    val copyLabel = stringResource(R.string.tail_detail_copy_all)
    Text(
        text = line.text,
        color = levelColor(line.level),
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClickLabel = openLabel,
                onLongClickLabel = copyLabel,
                onClick = onOpen,
                onLongClick = onCopy,
            ),
    )
}

@Composable
private fun statusText(state: TailConnState): String = stringResource(
    when (state) {
        TailConnState.Idle -> R.string.tail_connecting
        TailConnState.Connecting -> R.string.tail_connecting
        TailConnState.Connected -> R.string.tail_connected
        is TailConnState.Disconnected -> R.string.tail_disconnected
    },
)

private fun levelColor(level: String): Color = when (level) {
    "event" -> OcOrange
    "error", "exception" -> Color(0xFFFF6B6B)
    "warn" -> Color(0xFFF5C451)
    else -> Color(0xFFD8CFC4)
}
