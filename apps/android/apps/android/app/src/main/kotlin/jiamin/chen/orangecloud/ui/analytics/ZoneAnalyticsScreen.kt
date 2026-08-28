package jiamin.chen.orangecloud.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import jiamin.chen.orangecloud.data.model.AnalyticsTimeRange
import jiamin.chen.orangecloud.data.model.TrafficDataPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneAnalyticsScreen(
    onBack: () -> Unit,
    onShowPaywall: () -> Unit = {},
    viewModel: ZoneAnalyticsViewModel = hiltViewModel(),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.needsPro.collect { onShowPaywall() }
    }

    SkyBackground(phase = phase) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            SkyHeader(
                title = ui.zoneName.ifBlank { stringResource(R.string.analytics_title) },
                onSky = onSky,
                isLoading = ui.isLoading,
                onRefresh = { viewModel.refresh() },
                onBack = onBack,
                titleSize = 22,
                backDescription = stringResource(R.string.common_back),
                refreshDescription = stringResource(R.string.common_refresh),
            )

            when {
                ui.missingScope ->
                    SkyEmptyState(Icons.Outlined.Lock, stringResource(R.string.scope_missing), onSky, stringResource(R.string.common_refresh)) { viewModel.refresh() }

                ui.points.isEmpty() && ui.isLoading ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = onSky) }

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    RangeSelector(ui.range, onSky) { viewModel.selectRange(it) }

                    if (ui.points.isEmpty()) {
                        Spacer(Modifier.height(40.dp))
                        SkyEmptyState(Icons.AutoMirrored.Outlined.ShowChart, stringResource(R.string.analytics_empty), onSky, stringResource(R.string.common_refresh)) { viewModel.refresh() }
                    } else {
                        ui.summary?.let { SummaryGrid(it) }
                        ChartCard(stringResource(R.string.analytics_requests), ui.points) { it.requests.toFloat() }
                        ChartCard(stringResource(R.string.analytics_bandwidth), ui.points) { it.bytes.toFloat() }
                        if (ui.countries.isNotEmpty()) CountryBreakdownCard(ui.countries)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeSelector(range: AnalyticsTimeRange, onSky: Color, onSelect: (AnalyticsTimeRange) -> Unit) {
    val options = AnalyticsTimeRange.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == range,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) {
                Text(stringResource(rangeLabel(option)))
            }
        }
    }
}

@Composable
private fun SummaryGrid(summary: TrafficSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(stringResource(R.string.analytics_requests), formatCount(summary.requests), Modifier.weight(1f))
            StatCard(stringResource(R.string.analytics_bandwidth), formatBytes(summary.bytes), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(stringResource(R.string.analytics_uniques), formatCount(summary.uniques), Modifier.weight(1f))
            StatCard(stringResource(R.string.analytics_cache_hit), "${(summary.cacheHitRate * 100).toInt()}%", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChartCard(title: String, points: List<TrafficDataPoint>, value: (TrafficDataPoint) -> Float) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            TrafficChart(points, value, Modifier.fillMaxWidth().height(120.dp))
        }
    }
}

/** 晨昏面积图：橙色描边 + 橙→透明面积填充。 */
@Composable
private fun TrafficChart(points: List<TrafficDataPoint>, value: (TrafficDataPoint) -> Float, modifier: Modifier) {
    val values = points.map(value)
    val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
    Canvas(modifier) {
        val n = values.size
        if (n == 0) return@Canvas
        val w = size.width
        val h = size.height
        val stepX = if (n > 1) w / (n - 1) else 0f
        fun px(i: Int) = if (n > 1) i * stepX else w / 2
        fun py(v: Float) = h - (v / max) * h * 0.92f - h * 0.04f

        val line = Path().apply {
            moveTo(px(0), py(values[0]))
            for (i in 1 until n) lineTo(px(i), py(values[i]))
        }
        val area = Path().apply {
            addPath(line)
            lineTo(px(n - 1), h)
            lineTo(px(0), h)
            close()
        }
        drawPath(
            area,
            Brush.verticalGradient(listOf(OcOrange.copy(alpha = 0.35f), OcOrange.copy(alpha = 0f))),
        )
        drawPath(line, color = OcOrange, style = Stroke(width = 3f))
        // 末点高亮
        drawCircle(OcOrange, radius = 4f, center = Offset(px(n - 1), py(values[n - 1])))
    }
}

/** 按国家/地区请求量（晨昏地图的数据替身：旗帜 + 占比条 + 计数，取前 8）。 */
@Composable
private fun CountryBreakdownCard(countries: List<jiamin.chen.orangecloud.data.model.CountryTraffic>) {
    val top = countries.take(8)
    val max = (top.maxOfOrNull { it.requests } ?: 1L).coerceAtLeast(1L)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.analytics_top_countries), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            top.forEach { c ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(flagEmoji(c.country), fontSize = 16.sp)
                        Spacer(Modifier.height(0.dp))
                        Text(
                            "  ${c.country}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Text(formatCount(c.requests), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    // 占比条
                    Box(Modifier.fillMaxWidth().height(6.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp))) {
                        Box(
                            Modifier
                                .fillMaxWidth(fraction = (c.requests.toFloat() / max).coerceIn(0.02f, 1f))
                                .height(6.dp)
                                .background(OcOrange, RoundedCornerShape(3.dp)),
                        )
                    }
                }
            }
        }
    }
}

/** 两位国家码 → 国旗 emoji（区域指示符）。非两位字母回退地球。 */
private fun flagEmoji(code: String): String {
    val c = code.trim().uppercase()
    if (c.length != 2 || !c.all { it in 'A'..'Z' }) return "🌍"
    val first = Character.toChars(0x1F1E6 + (c[0] - 'A'))
    val second = Character.toChars(0x1F1E6 + (c[1] - 'A'))
    return String(first) + String(second)
}

private fun rangeLabel(range: AnalyticsTimeRange): Int = when (range) {
    AnalyticsTimeRange.LAST_24H -> R.string.range_24h
    AnalyticsTimeRange.LAST_7D -> R.string.range_7d
    AnalyticsTimeRange.LAST_30D -> R.string.range_30d
}

private fun formatCount(n: Long): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
    n >= 1_000 -> "%.1fK".format(n / 1_000.0)
    else -> n.toString()
}

private fun formatBytes(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var v = bytes.toDouble()
    var i = 0
    while (v >= 1024 && i < units.size - 1) {
        v /= 1024; i++
    }
    return if (i == 0) "${bytes} ${units[i]}" else "%.1f %s".format(v, units[i])
}
