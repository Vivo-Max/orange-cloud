package jiamin.chen.orangecloud.ui.status

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.design.SkyBackground
import jiamin.chen.orangecloud.core.design.SkyEmptyState
import jiamin.chen.orangecloud.core.design.SkyHeader
import jiamin.chen.orangecloud.core.design.StatusDot
import jiamin.chen.orangecloud.core.design.TintIcon
import jiamin.chen.orangecloud.core.design.onSky
import jiamin.chen.orangecloud.core.design.rememberSkyPhase
import jiamin.chen.orangecloud.core.design.theme.OcSuccess
import jiamin.chen.orangecloud.core.design.theme.OcSuccessDark
import jiamin.chen.orangecloud.core.util.launchCustomTab
import jiamin.chen.orangecloud.data.model.StatusPageComponent
import jiamin.chen.orangecloud.data.model.StatusPageIncident
import jiamin.chen.orangecloud.data.model.StatusPageOverall
import jiamin.chen.orangecloud.data.model.StatusPageRegion
import jiamin.chen.orangecloud.data.repository.StatusRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

private const val SERVICE_GROUP = "Cloudflare Sites and Services"
private val GREEN_LIGHT = OcSuccess
private val GREEN_DARK = OcSuccessDark

@Composable
fun StatusScreen(
    onBack: () -> Unit,
    viewModel: StatusViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    var selected by remember { mutableStateOf<StatusPageIncident?>(null) }

    SkyBackground(phase = phase) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            SkyHeader(
                title = stringResource(R.string.status_title),
                onSky = onSky,
                isLoading = state.isLoading,
                onRefresh = { viewModel.load() },
                onBack = onBack,
                titleSize = 22,
                backDescription = stringResource(R.string.common_back),
                refreshDescription = stringResource(R.string.common_refresh),
            )
            when {
                state.overall == null && state.isLoading ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = onSky) }

                state.overall == null ->
                    SkyEmptyState(Icons.Outlined.CloudQueue, stringResource(R.string.error_generic), onSky, stringResource(R.string.common_refresh)) { viewModel.load() }

                else -> StatusContent(state, onIncidentClick = { selected = it })
            }
        }
    }

    selected?.let { incident ->
        IncidentDetailSheet(incident = incident, onDismiss = { selected = null })
    }
}

@Composable
private fun StatusContent(state: StatusUiState, onIncidentClick: (StatusPageIncident) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { OverallBanner(state.overall!!) }

        if (state.activeIncidents.isNotEmpty()) {
            item {
                SectionCard(stringResource(R.string.status_section_active)) {
                    Rows(state.activeIncidents) { IncidentRow(it) { onIncidentClick(it) } }
                }
            }
        }
        if (state.maintenances.isNotEmpty()) {
            item {
                SectionCard(stringResource(R.string.status_section_maintenance)) {
                    Rows(state.maintenances) { IncidentRow(it) { onIncidentClick(it) } }
                }
            }
        }

        item {
            SectionCard(stringResource(R.string.status_section_products)) {
                if (state.affectedProducts.isEmpty()) {
                    AllOkRow(state.productTotal)
                } else {
                    Rows(state.affectedProducts) { ComponentRow(it) }
                }
            }
        }

        if (state.regions.isNotEmpty()) {
            item {
                SectionCard(stringResource(R.string.status_section_edge)) {
                    Rows(state.regions) { RegionRow(it) }
                }
            }
        }

        if (state.recentIncidents.isNotEmpty()) {
            item {
                SectionCard(stringResource(R.string.status_section_recent)) {
                    Rows(state.recentIncidents) { IncidentRow(it) { onIncidentClick(it) } }
                }
            }
        }

        item { FullStatusLink() }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

// MARK: — 区块脚手架

@Composable
private fun SectionCard(title: String?, content: @Composable () -> Unit) {
    Column {
        if (title != null) {
            Text(
                title,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
            )
        }
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column { content() }
        }
    }
}

/** 列表渲染 + 行间细分隔线。 */
@Composable
private fun <T> Rows(items: List<T>, row: @Composable (T) -> Unit) {
    items.forEachIndexed { index, item ->
        row(item)
        if (index != items.lastIndex) {
            Box(Modifier.fillMaxWidth().padding(start = 14.dp).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)))
        }
    }
}

// MARK: — 行

@Composable
private fun OverallBanner(overall: StatusPageOverall) {
    val color = indicatorColor(overall.indicator)
    Surface(color = color.copy(alpha = 0.16f), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusDot(color = color, size = 11.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                indicatorText(overall.indicator, overall.description),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun AllOkRow(total: Int) {
    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        TintIcon(Icons.Outlined.CheckCircle, successGreen(), size = 30.dp)
        Spacer(Modifier.width(12.dp))
        Text(stringResource(R.string.status_products_all_ok, total), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ComponentRow(component: StatusPageComponent) {
    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        StatusDot(color = componentColor(component.status), size = 10.dp)
        Spacer(Modifier.width(12.dp))
        Text(component.name, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(componentStatusText(component.status), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RegionRow(region: StatusPageRegion) {
    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        StatusDot(color = if (region.impacted == 0) successGreen() else Color(0xFFF5A623), size = 10.dp)
        Spacer(Modifier.width(12.dp))
        Text(regionName(region.name), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        if (region.impacted == 0) {
            Text(stringResource(R.string.status_region_all_ok), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(stringResource(R.string.status_region_impacted, region.impacted), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun IncidentRow(incident: StatusPageIncident, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(14.dp)) {
        Text(incident.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(incidentStatusText(incident.status), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = impactColor(incident.impact))
            relativeTime(incident.updatedAt)?.let {
                Spacer(Modifier.width(8.dp))
                Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FullStatusLink() {
    val context = LocalContext.current
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable { context.launchCustomTab("https://www.cloudflarestatus.com".toUri()) }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TintIcon(Icons.Outlined.Public, MaterialTheme.colorScheme.onSurfaceVariant, size = 30.dp)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.status_full_page), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}

// MARK: — 事件详情底部表单

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncidentDetailSheet(incident: StatusPageIncident, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp),
        ) {
            Text(incident.name, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
            Spacer(Modifier.height(16.dp))
            DetailRow(stringResource(R.string.status_detail_status), incidentStatusText(incident.status))
            DetailRow(stringResource(R.string.status_detail_impact), impactText(incident.impact))
            formatDateTime(incident.createdAt)?.let { DetailRow(stringResource(R.string.status_detail_started), it) }

            incident.shortlink?.let { link ->
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { context.launchCustomTab(link.toUri()) }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.status_detail_open), fontSize = 15.sp, color = cs.primary, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = cs.primary, modifier = Modifier.size(16.dp))
                }
            }

            if (incident.incidentUpdates.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.status_detail_updates), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = cs.primary)
                Spacer(Modifier.height(10.dp))
                incident.incidentUpdates.forEach { update ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(incidentStatusText(update.status), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = cs.primary, modifier = Modifier.weight(1f))
                            formatDateTime(update.displayAt ?: update.createdAt)?.let {
                                Text(it, fontSize = 12.sp, color = cs.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(update.body, fontSize = 14.sp, color = cs.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(96.dp))
        Text(value, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

// MARK: — 文案与配色映射

@Composable
private fun successGreen(): Color = if (jiamin.chen.orangecloud.core.design.theme.LocalIsDark.current) GREEN_DARK else GREEN_LIGHT

@Composable
private fun indicatorText(indicator: String, fallback: String): String = when (indicator) {
    "none" -> stringResource(R.string.status_none)
    "minor" -> stringResource(R.string.status_minor)
    "major" -> stringResource(R.string.status_major)
    "critical" -> stringResource(R.string.status_critical)
    "maintenance" -> stringResource(R.string.status_maintenance)
    else -> fallback
}

@Composable
private fun componentStatusText(status: String): String = when (status) {
    "operational" -> stringResource(R.string.status_comp_operational)
    "degraded_performance" -> stringResource(R.string.status_comp_degraded)
    "partial_outage" -> stringResource(R.string.status_comp_partial)
    "major_outage" -> stringResource(R.string.status_comp_major)
    "under_maintenance" -> stringResource(R.string.status_comp_maintenance)
    else -> status
}

@Composable
private fun incidentStatusText(status: String): String = when (status) {
    "investigating" -> stringResource(R.string.status_inc_investigating)
    "identified" -> stringResource(R.string.status_inc_identified)
    "monitoring" -> stringResource(R.string.status_inc_monitoring)
    "resolved" -> stringResource(R.string.status_inc_resolved)
    "postmortem" -> stringResource(R.string.status_inc_postmortem)
    "scheduled" -> stringResource(R.string.status_inc_scheduled)
    "in_progress" -> stringResource(R.string.status_inc_in_progress)
    "verifying" -> stringResource(R.string.status_inc_verifying)
    "completed" -> stringResource(R.string.status_inc_completed)
    else -> status
}

@Composable
private fun impactText(impact: String): String = when (impact) {
    "critical" -> stringResource(R.string.status_impact_critical)
    "major" -> stringResource(R.string.status_impact_major)
    "minor" -> stringResource(R.string.status_impact_minor)
    "maintenance" -> stringResource(R.string.status_impact_maintenance)
    "none" -> stringResource(R.string.status_impact_none)
    else -> impact
}

@Composable
private fun regionName(name: String): String = when (name) {
    "Africa" -> stringResource(R.string.status_region_africa)
    "Asia" -> stringResource(R.string.status_region_asia)
    "Europe" -> stringResource(R.string.status_region_europe)
    "Latin America & the Caribbean" -> stringResource(R.string.status_region_latam)
    "Middle East" -> stringResource(R.string.status_region_middle_east)
    "North America" -> stringResource(R.string.status_region_north_america)
    "Oceania" -> stringResource(R.string.status_region_oceania)
    else -> name
}

@Composable
private fun relativeTime(iso: String?): String? {
    val instant = parseInstant(iso) ?: return null
    val seconds = (Instant.now().epochSecond - instant.epochSecond).coerceAtLeast(0)
    return when {
        seconds < 60 -> stringResource(R.string.status_time_just_now)
        seconds < 3600 -> stringResource(R.string.status_time_minutes, seconds / 60)
        seconds < 86400 -> stringResource(R.string.status_time_hours, seconds / 3600)
        else -> stringResource(R.string.status_time_days, seconds / 86400)
    }
}

private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

private fun formatDateTime(iso: String?): String? = runCatching {
    OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).format(dateTimeFormatter)
}.getOrNull()

private fun parseInstant(iso: String?): Instant? =
    iso?.let { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() }

private fun indicatorColor(indicator: String): Color = when (indicator) {
    "none" -> Color(0xFF2FBF71)
    "minor" -> Color(0xFFF5A623)
    "major" -> Color(0xFFF48120)
    "critical" -> Color(0xFFE5484D)
    else -> Color(0xFF5B8DEF)
}

private fun componentColor(status: String): Color = when (status) {
    "operational" -> Color(0xFF2FBF71)
    "degraded_performance" -> Color(0xFFF5A623)
    "partial_outage" -> Color(0xFFF48120)
    "major_outage" -> Color(0xFFE5484D)
    else -> Color(0xFF5B8DEF)
}

private fun impactColor(impact: String): Color = when (impact) {
    "critical" -> Color(0xFFE5484D)
    "major" -> Color(0xFFF48120)
    "minor" -> Color(0xFFF5A623)
    "maintenance" -> Color(0xFF5B8DEF)
    else -> Color(0xFF8A8A8E)
}

// MARK: — ViewModel

data class StatusUiState(
    val overall: StatusPageOverall? = null,
    val activeIncidents: List<StatusPageIncident> = emptyList(),
    val maintenances: List<StatusPageIncident> = emptyList(),
    val affectedProducts: List<StatusPageComponent> = emptyList(),
    val productTotal: Int = 0,
    val regions: List<StatusPageRegion> = emptyList(),
    val recentIncidents: List<StatusPageIncident> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val statusRepository: StatusRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(StatusUiState(isLoading = true))
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val summaryDeferred = async { statusRepository.summary() }
                val historyDeferred = async { runCatching { statusRepository.recentIncidents() }.getOrDefault(emptyList()) }
                val summary = summaryDeferred.await()
                val history = historyDeferred.await()

                val split = splitComponents(summary.components)
                val activeIds = summary.incidents.map { it.id }.toSet()
                _uiState.update {
                    it.copy(
                        overall = summary.status,
                        activeIncidents = summary.incidents,
                        maintenances = summary.scheduledMaintenances,
                        affectedProducts = split.affected,
                        productTotal = split.total,
                        regions = split.regions,
                        recentIncidents = history.filterNot { inc -> inc.id in activeIds }.take(10),
                    )
                }
            } catch (_: Exception) {
                // overall 仍为 null → 错误占位
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private data class Split(val total: Int, val affected: List<StatusPageComponent>, val regions: List<StatusPageRegion>)

    /** 组件拆成「产品服务」与「边缘网络大区」（对齐 iOS splitComponents）。 */
    private fun splitComponents(components: List<StatusPageComponent>): Split {
        val serviceGroup = components.firstOrNull { it.group == true && it.name == SERVICE_GROUP }
            ?: run {
                val leaves = components.filter { it.group != true }
                return Split(leaves.size, leaves.filter { it.status != "operational" }, emptyList())
            }
        val products = components.filter { it.groupId == serviceGroup.id }
        val regions = components
            .filter { it.group == true && it.id != serviceGroup.id }
            .map { group ->
                val nodes = components.filter { it.groupId == group.id }
                StatusPageRegion(group.id, group.name, nodes.size, nodes.count { it.status != "operational" })
            }
        return Split(products.size, products.filter { it.status != "operational" }, regions)
    }
}
