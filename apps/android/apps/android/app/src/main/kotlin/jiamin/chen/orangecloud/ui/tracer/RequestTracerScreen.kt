package jiamin.chen.orangecloud.ui.tracer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Troubleshoot
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.design.SkyBackground
import jiamin.chen.orangecloud.core.design.SkyEmptyState
import jiamin.chen.orangecloud.core.design.SkyHeader
import jiamin.chen.orangecloud.core.design.onSky
import jiamin.chen.orangecloud.core.design.rememberSkyPhase
import jiamin.chen.orangecloud.data.model.FlatTraceStep

private val BRAND_ORANGE = Color(0xFFF48120)

/** 「这个请求为什么被拦了」——模拟一条请求打过 Cloudflare，看它逐步走了哪些规则。 */
@Composable
fun RequestTracerScreen(
    onBack: () -> Unit,
    viewModel: RequestTracerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val snackbarHostState = remember { SnackbarHostState() }
    val genericErr = stringResource(R.string.error_generic)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TracerEvent.Error -> snackbarHostState.showSnackbar(event.message ?: genericErr)
            }
        }
    }

    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                SkyHeader(
                    title = stringResource(R.string.trace_title),
                    onSky = onSky,
                    isLoading = state.isTracing,
                    onRefresh = { viewModel.run() },
                    onBack = onBack,
                    titleSize = 22,
                    backDescription = stringResource(R.string.common_back),
                    refreshDescription = stringResource(R.string.common_refresh),
                )
                if (state.missingScope) {
                    Box(Modifier.fillMaxSize()) {
                        SkyEmptyState(
                            Icons.Outlined.Troubleshoot,
                            stringResource(R.string.scope_missing), onSky, stringResource(R.string.common_refresh),
                        ) { }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                        RequestTracerViewModel.METHODS.forEachIndexed { index, method ->
                                            SegmentedButton(
                                                selected = state.method == method,
                                                onClick = { viewModel.setMethod(method) },
                                                shape = SegmentedButtonDefaults.itemShape(
                                                    index, RequestTracerViewModel.METHODS.size,
                                                ),
                                            ) { Text(method, fontSize = 11.sp, maxLines = 1) }
                                        }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = state.url,
                                        onValueChange = viewModel::setUrl,
                                        label = { Text(stringResource(R.string.trace_url_label)) },
                                        placeholder = { Text("https://example.com/path") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Button(
                                        onClick = { viewModel.run() },
                                        enabled = state.canTrace,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        if (state.isTracing) {
                                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                            Spacer(Modifier.width(8.dp))
                                        }
                                        Text(stringResource(R.string.trace_run))
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    // 说清楚这是模拟而非真实流量，否则用户会以为在看线上请求
                                    Text(
                                        stringResource(R.string.trace_hint),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        if (state.hasResult) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        stringResource(R.string.trace_steps),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = onSky,
                                        modifier = Modifier.weight(1f),
                                    )
                                    state.statusCode?.let {
                                        Text("HTTP $it", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = onSky)
                                    }
                                }
                            }
                            items(state.steps) { flat -> StepRow(flat) }
                            if (state.steps.isEmpty()) {
                                item {
                                    Text(
                                        stringResource(R.string.trace_no_steps),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun StepRow(flat: FlatTraceStep) {
    val matched = flat.step.matched == true
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(start = (flat.depth * 14).dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 命中与否用图标 + 颜色双重编码，不只靠颜色
                Icon(
                    if (matched) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = null,
                    tint = if (matched) BRAND_ORANGE else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    flat.step.title,
                    fontSize = 14.sp,
                    fontWeight = if (matched) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                flat.step.action?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (matched) BRAND_ORANGE else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            flat.step.expression?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            flat.step.type?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
