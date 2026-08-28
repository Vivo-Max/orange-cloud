package jiamin.chen.orangecloud.ui.managedheaders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import jiamin.chen.orangecloud.data.model.ManagedTransform

/** 托管请求/响应头：Cloudflare 维护的开关式头部改写，不用自己写 Transform 规则。 */
@Composable
fun ManagedHeadersScreen(
    onBack: () -> Unit,
    viewModel: ManagedHeadersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val snackbarHostState = remember { SnackbarHostState() }
    val genericErr = stringResource(R.string.error_generic)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ManagedHeadersEvent.Error -> snackbarHostState.showSnackbar(event.message ?: genericErr)
            }
        }
    }

    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                SkyHeader(
                    title = stringResource(R.string.mh_title),
                    onSky = onSky,
                    isLoading = state.isLoading,
                    onRefresh = { viewModel.load() },
                    onBack = onBack,
                    titleSize = 22,
                    backDescription = stringResource(R.string.common_back),
                    refreshDescription = stringResource(R.string.common_refresh),
                )
                if (state.missingScope) {
                    Box(Modifier.fillMaxSize()) {
                        SkyEmptyState(
                            Icons.Outlined.Tune,
                            stringResource(R.string.scope_missing), onSky, stringResource(R.string.common_refresh),
                        ) { viewModel.load() }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item { SectionLabel(stringResource(R.string.mh_request), onSky) }
                        items(state.requestHeaders, key = { "req-${it.id}" }) { item ->
                            HeaderCard(item, state.canWrite && !state.isMutating) { on ->
                                viewModel.setEnabled(item, on, isRequest = true)
                            }
                        }
                        item { SectionLabel(stringResource(R.string.mh_response), onSky) }
                        items(state.responseHeaders, key = { "res-${it.id}" }) { item ->
                            HeaderCard(item, state.canWrite && !state.isMutating) { on ->
                                viewModel.setEnabled(item, on, isRequest = false)
                            }
                        }
                        item {
                            Text(
                                stringResource(R.string.mh_hint),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            }
            SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun SectionLabel(text: String, onSky: androidx.compose.ui.graphics.Color) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = onSky,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun HeaderCard(item: ManagedTransform, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.displayName,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                // 互斥关系由接口给出，开之前让用户知道会顶掉谁
                item.conflictsWith?.takeIf { it.isNotEmpty() }?.let {
                    Text(
                        stringResource(R.string.mh_conflicts, it.joinToString("、")),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = item.enabled == true, onCheckedChange = onChange, enabled = enabled)
        }
    }
}
