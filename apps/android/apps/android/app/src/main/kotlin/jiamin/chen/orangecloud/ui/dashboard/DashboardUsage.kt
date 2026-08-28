package jiamin.chen.orangecloud.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.design.theme.OcOrange
import jiamin.chen.orangecloud.core.system.UsagePlanPrefs
import jiamin.chen.orangecloud.data.model.AccountUsage
import jiamin.chen.orangecloud.ui.storage.formatBytes
import kotlin.math.abs

private val WarnAmber = Color(0xFFF5A623)
private val WarnRed = Color(0xFFE5484D)

// MARK: - 数据

/** 一条用量指标：标签 + 值 + 可选额度文案与消耗比例（ratio 为 null 表示无额度环/条）。 */
private data class UsageMetric(
    val label: String,
    val valueText: String,
    val quotaText: String?,
    val ratio: Double?,
)

/** 一个服务瓦片：展示消耗比例最高的「瓶颈」指标。 */
private data class UsageTile(
    val service: String,
    val context: String,
    val valueText: String,
    val quotaText: String?,
    val ratio: Double?,
    val metrics: List<UsageMetric>,
)

// MARK: - Section 入口

@Composable
fun DashboardUsageSection(
    usage: AccountUsage?,
    plan: UsagePlanPrefs,
    loading: Boolean,
    loadFailed: Boolean,
    hasScope: Boolean,
    unavailable: Boolean,
    onSky: Color,
    onRetry: () -> Unit,
    onSetWorkersPaid: (Boolean) -> Unit,
    onSetR2Paid: (Boolean) -> Unit,
    onSetBillingDay: (Int) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.usage_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = onSky,
                modifier = Modifier.weight(1f),
            )
            if (hasScope) PlanMenu(plan, onSetWorkersPaid, onSetR2Paid, onSetBillingDay)
        }

        when {
            !hasScope -> InfoCard(Icons.Outlined.Lock, stringResource(R.string.usage_locked), null)
            usage != null -> UsageGrid(usage, plan)
            unavailable -> InfoCard(
                Icons.Outlined.QueryStats,
                stringResource(R.string.usage_unavailable_title),
                stringResource(R.string.usage_unavailable_body),
            )
            loadFailed -> Surface(
                color = cs.surfaceContainerLow,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().clickable(onClick = onRetry),
            ) {
                Text(
                    stringResource(R.string.usage_failed),
                    fontSize = 13.sp,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            else -> UsageSkeleton()
        }
    }
}

@Composable
private fun PlanMenu(
    plan: UsagePlanPrefs,
    onSetWorkersPaid: (Boolean) -> Unit,
    onSetR2Paid: (Boolean) -> Unit,
    onSetBillingDay: (Int) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }
    val summary = "W ${planWord(plan.workersPaid)} · R2 ${planWord(plan.r2Paid)}"
    Box {
        Row(
            Modifier.clickable { open = true }.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Tune, contentDescription = null, tint = cs.primary, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(summary, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = cs.primary)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MenuSectionLabel(stringResource(R.string.usage_plan_workers))
            PlanChoice(FREE, !plan.workersPaid) { onSetWorkersPaid(false); open = false }
            PlanChoice(PAID, plan.workersPaid) { onSetWorkersPaid(true); open = false }
            HorizontalDivider()
            MenuSectionLabel(stringResource(R.string.usage_plan_r2))
            PlanChoice(FREE, !plan.r2Paid) { onSetR2Paid(false); open = false }
            PlanChoice(PAID, plan.r2Paid) { onSetR2Paid(true); open = false }
            HorizontalDivider()
            MenuSectionLabel(stringResource(R.string.usage_billing_day))
            DropdownMenuItem(
                text = { Text(billingLabel(plan.billingDay), color = cs.onSurface) },
                trailingIcon = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                onClick = { open = false; showDayPicker = true },
            )
        }
    }
    if (showDayPicker) {
        BillingDayDialog(
            current = plan.billingDay,
            onPick = { onSetBillingDay(it); showDayPicker = false },
            onDismiss = { showDayPicker = false },
        )
    }
}

@Composable
private fun billingLabel(day: Int): String =
    if (day == 1) stringResource(R.string.usage_billing_natural) else stringResource(R.string.usage_billing_day_n, day)

@Composable
private fun BillingDayDialog(current: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.usage_billing_day)) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                (1..28).forEach { day ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(day) }.padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            billingLabel(day),
                            modifier = Modifier.weight(1f),
                            fontSize = 15.sp,
                            color = if (day == current) cs.primary else cs.onSurface,
                            fontWeight = if (day == current) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        if (day == current) Icon(Icons.Outlined.Check, contentDescription = null, tint = cs.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
private fun MenuSectionLabel(text: String) {
    Text(
        text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun PlanChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    DropdownMenuItem(
        text = { Text(label, color = if (selected) cs.primary else cs.onSurface, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
        onClick = onClick,
    )
}

// MARK: - 宫格

@Composable
private fun UsageGrid(usage: AccountUsage, plan: UsagePlanPrefs) {
    val tiles = buildTiles(usage, plan)
    var detail by remember { mutableStateOf<UsageTile?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(2).forEach { rowTiles ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowTiles.forEach { tile ->
                    UsageTileCard(tile, Modifier.weight(1f)) { detail = tile }
                }
                if (rowTiles.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }

    detail?.let { tile -> UsageDetailSheet(tile) { detail = null } }
}

@Composable
private fun UsageTileCard(tile: UsageTile, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        color = cs.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "${tile.service} · ${tile.context}",
                fontSize = 12.sp,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                tile.ratio?.let { UsageRing(it, Modifier.size(34.dp)) }
                Column {
                    Text(
                        tile.valueText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    tile.quotaText?.let {
                        Text(it, fontSize = 11.sp, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

/** 额度环：随消耗比例从品牌橙渐变到警示红。 */
@Composable
private fun UsageRing(ratio: Double, modifier: Modifier) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val color = ringColor(ratio)
    Canvas(modifier) {
        val stroke = 4.dp.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(
            color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false,
            topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke),
        )
        val sweep = (ratio.coerceIn(0.02, 1.0) * 360.0).toFloat()
        drawArc(
            color = color, startAngle = -90f, sweepAngle = sweep, useCenter = false,
            topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
}

// MARK: - 明细底部弹窗

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsageDetailSheet(tile: UsageTile, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(tile.service, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            tile.metrics.forEach { UsageDetailRow(it) }
        }
    }
}

@Composable
private fun UsageDetailRow(metric: UsageMetric) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(metric.label, fontSize = 14.sp, color = cs.onSurface, modifier = Modifier.weight(1f))
            Text(
                metric.valueText + (metric.quotaText?.let { " $it" } ?: ""),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = cs.onSurfaceVariant,
            )
        }
        metric.ratio?.let { r ->
            LinearProgressIndicator(
                progress = { r.coerceIn(0.0, 1.0).toFloat() },
                color = ringColor(r),
                trackColor = cs.onSurface.copy(alpha = 0.10f),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// MARK: - 骨架

@Composable
private fun UsageSkeleton() {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) {
                    Surface(
                        color = cs.surfaceContainerLow,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(width = 80.dp, height = 10.dp).skeleton(cs))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                Canvas(Modifier.size(34.dp)) {
                                    drawArc(
                                        color = cs.onSurface.copy(alpha = 0.10f), startAngle = 0f, sweepAngle = 360f,
                                        useCenter = false, style = Stroke(4.dp.toPx()),
                                        topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                                        size = Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()),
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Box(Modifier.size(width = 56.dp, height = 13.dp).skeleton(cs))
                                    Box(Modifier.size(width = 40.dp, height = 9.dp).skeleton(cs))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.skeleton(cs: androidx.compose.material3.ColorScheme): Modifier =
    this.background(cs.onSurface.copy(alpha = 0.08f), RoundedCornerShape(4.dp))

// MARK: - 指标构建（对齐 iOS workersTile/r2Tile/d1Tile/kvTile）

@Composable
private fun buildTiles(usage: AccountUsage, plan: UsagePlanPrefs): List<UsageTile> {
    val today = stringResource(R.string.usage_today)
    val periodLabel = if (plan.billingDay != 1) stringResource(R.string.usage_period_cycle) else stringResource(R.string.usage_period_month)
    val wLabel = if (plan.workersPaid) periodLabel else today

    // 预解析所有可能用到的 context 文案（分支只挑数据，不改 stringResource 调用集）
    val ctxRequests = stringResource(R.string.usage_ctx_requests, wLabel)
    val ctxCpu = stringResource(R.string.usage_ctx_cpu, wLabel)
    val ctxCpuToday = stringResource(R.string.usage_ctx_cpu, today)
    val ctxR2A = stringResource(R.string.usage_ctx_r2a, periodLabel)
    val ctxR2B = stringResource(R.string.usage_ctx_r2b, periodLabel)
    val ctxStorage = stringResource(R.string.usage_ctx_storage)
    val ctxRowsRead = stringResource(R.string.usage_ctx_rows_read, wLabel)
    val ctxRowsWrite = stringResource(R.string.usage_ctx_rows_write, wLabel)
    val ctxReads = stringResource(R.string.usage_ctx_reads, wLabel)
    val ctxWrites = stringResource(R.string.usage_ctx_writes, wLabel)

    val tiles = mutableListOf<UsageTile>()

    // Workers
    run {
        val metrics = mutableListOf<UsageMetric>()
        val reqs = if (plan.workersPaid) usage.workersRequestsMonth else usage.workersRequestsToday
        val reqQuota = if (plan.workersPaid) 10_000_000L else 100_000L
        metrics += UsageMetric(ctxRequests, compact(reqs), "/ ${compact(reqQuota)}", ratio(reqs, reqQuota))
        if (plan.workersPaid && usage.cpuTimeMonthUs != null) {
            val ms = (usage.cpuTimeMonthUs / 1000).toLong()
            metrics += UsageMetric(ctxCpu, "${compact(ms)} ms", "/ 30M ms", ratio(ms, 30_000_000L))
        } else if (usage.cpuTimeTodayUs != null) {
            val ms = (usage.cpuTimeTodayUs / 1000).toLong()
            metrics += UsageMetric(ctxCpuToday, "${compact(ms)} ms", null, null)
        }
        tiles += tileOf("Workers", metrics)
    }

    // R2
    run {
        val metrics = mutableListOf(
            UsageMetric(ctxR2A, compact(usage.r2ClassAMonth), "/ 1M", ratio(usage.r2ClassAMonth, 1_000_000L)),
            UsageMetric(ctxR2B, compact(usage.r2ClassBMonth), "/ 10M", ratio(usage.r2ClassBMonth, 10_000_000L)),
        )
        metrics += if (plan.r2Paid) {
            UsageMetric(ctxStorage, formatBytes(usage.r2StorageBytes), null, null)
        } else {
            UsageMetric(ctxStorage, formatBytes(usage.r2StorageBytes), "/ 10 GB", ratio(usage.r2StorageBytes, 10_000_000_000L))
        }
        tiles += tileOf("R2", metrics)
    }

    // D1
    if (usage.d1Usage != null || usage.d1StorageBytes != null) {
        val metrics = mutableListOf<UsageMetric>()
        usage.d1Usage?.let { d1 ->
            val reads = if (plan.workersPaid) d1.rowsReadPeriod else d1.rowsReadToday
            val readQuota = if (plan.workersPaid) 25_000_000_000L else 5_000_000L
            val writes = if (plan.workersPaid) d1.rowsWrittenPeriod else d1.rowsWrittenToday
            val writeQuota = if (plan.workersPaid) 50_000_000L else 100_000L
            metrics += UsageMetric(ctxRowsRead, compact(reads), "/ ${compact(readQuota)}", ratio(reads, readQuota))
            metrics += UsageMetric(ctxRowsWrite, compact(writes), "/ ${compact(writeQuota)}", ratio(writes, writeQuota))
        }
        usage.d1StorageBytes?.let { storage ->
            metrics += UsageMetric(ctxStorage, formatBytes(storage), "/ 5 GB", ratio(storage, 5_000_000_000L))
        }
        if (metrics.isNotEmpty()) tiles += tileOf("D1", metrics)
    }

    // KV
    if (usage.kvUsage != null || usage.kvStorageBytes != null) {
        val metrics = mutableListOf<UsageMetric>()
        usage.kvUsage?.let { kv ->
            val reads = if (plan.workersPaid) kv.readsPeriod else kv.readsToday
            val readQuota = if (plan.workersPaid) 10_000_000L else 100_000L
            val writes = if (plan.workersPaid) kv.writesPeriod else kv.writesToday
            val writeQuota = if (plan.workersPaid) 1_000_000L else 1_000L
            metrics += UsageMetric(ctxReads, compact(reads), "/ ${compact(readQuota)}", ratio(reads, readQuota))
            metrics += UsageMetric(ctxWrites, compact(writes), "/ ${compact(writeQuota)}", ratio(writes, writeQuota))
        }
        usage.kvStorageBytes?.let { storage ->
            metrics += UsageMetric(ctxStorage, formatBytes(storage), "/ 1 GB", ratio(storage, 1_000_000_000L))
        }
        if (metrics.isNotEmpty()) tiles += tileOf("KV", metrics)
    }

    return tiles
}

/** 瓦片取「瓶颈」指标（消耗比例最高的那条），无额度指标则取首条。 */
private fun tileOf(service: String, metrics: List<UsageMetric>): UsageTile {
    val top = metrics.filter { it.ratio != null }.maxByOrNull { it.ratio!! } ?: metrics.first()
    return UsageTile(service, top.label, top.valueText, top.quotaText, top.ratio, metrics)
}

private fun ringColor(ratio: Double): Color = when {
    ratio >= 0.9 -> WarnRed
    ratio >= 0.7 -> WarnAmber
    else -> OcOrange
}

private fun ratio(used: Long, quota: Long): Double = if (quota > 0) used.toDouble() / quota else 0.0

private fun compact(n: Long): String {
    val a = abs(n)
    return when {
        a >= 1_000_000_000 -> trimDec(n / 1e9) + "B"
        a >= 1_000_000 -> trimDec(n / 1e6) + "M"
        a >= 1_000 -> trimDec(n / 1e3) + "K"
        else -> n.toString()
    }
}

private fun trimDec(v: Double): String {
    val s = "%.1f".format(v)
    return if (s.endsWith(".0")) s.dropLast(2) else s
}

@Composable
private fun InfoCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String?) {
    val cs = MaterialTheme.colorScheme
    Surface(color = cs.surfaceContainerLow, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(22.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = cs.onSurface, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            body?.let {
                Text(it, fontSize = 12.sp, color = cs.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

private const val FREE = "Free"
private const val PAID = "Paid"
private fun planWord(paid: Boolean): String = if (paid) PAID else FREE
