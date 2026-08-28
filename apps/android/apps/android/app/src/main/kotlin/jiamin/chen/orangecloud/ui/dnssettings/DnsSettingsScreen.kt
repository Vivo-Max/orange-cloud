package jiamin.chen.orangecloud.ui.dnssettings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

/**
 * 域名的 DNS 设置：DNSSEC、CNAME 展平、多提供商 DNS、名称服务器、域名模式。
 * 与 DNS 记录列表分开——那里是逐条记录，这里是 zone 级策略。
 */
@Composable
fun DnsSettingsScreen(
    onBack: () -> Unit,
    viewModel: DnsSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val genericErr = stringResource(R.string.error_generic)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DnsSettingsEvent.Error -> snackbarHostState.showSnackbar(event.message ?: genericErr)
            }
        }
    }

    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                SkyHeader(
                    title = stringResource(R.string.dnss_title),
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
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.dnssec?.let { dnssec ->
                        Card {
                            ToggleLine(
                                title = "DNSSEC",
                                subtitle = stringResource(dnssecStatusLabel(dnssec.status)),
                                checked = dnssec.isEnabled,
                                enabled = state.canWriteDns && !state.isMutating,
                                onChange = viewModel::setDnssec,
                            )
                            // DS 记录必须由用户粘到注册商处，DNSSEC 才真正生效——最易被忽略的一步
                            if (!dnssec.ds.isNullOrBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.dnss_ds_record),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    dnssec.ds,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                TextButton(onClick = {
                                    copyToClipboard(context, dnssec.ds)
                                }) { Text(stringResource(R.string.dnss_copy_ds)) }
                                if (dnssec.status == "pending") {
                                    Text(
                                        stringResource(R.string.dnss_ds_pending_hint),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    state.settings?.let { settings ->
                        Card {
                            ToggleLine(
                                title = stringResource(R.string.dnss_flatten),
                                subtitle = stringResource(R.string.dnss_flatten_desc),
                                checked = settings.flattenAllCnames == true,
                                enabled = state.canWriteSettings && !state.isMutating,
                                onChange = viewModel::setFlattenAllCnames,
                            )
                            Spacer(Modifier.height(10.dp))
                            ToggleLine(
                                title = stringResource(R.string.dnss_multi_provider),
                                subtitle = stringResource(R.string.dnss_multi_provider_desc),
                                checked = settings.multiProvider == true,
                                enabled = state.canWriteSettings && !state.isMutating,
                                onChange = viewModel::setMultiProvider,
                            )
                            Spacer(Modifier.height(10.dp))
                            ToggleLine(
                                title = stringResource(R.string.dnss_advanced_ns),
                                subtitle = stringResource(R.string.dnss_advanced_ns_desc),
                                checked = settings.nameservers?.type == "cloudflare.advanced",
                                enabled = state.canWriteSettings && !state.isMutating,
                                onChange = viewModel::setAdvancedNameservers,
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.dnss_zone_mode),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(8.dp))
                            val modes = listOf(
                                "standard" to stringResource(R.string.dnss_mode_standard),
                                "cdn_only" to stringResource(R.string.dnss_mode_cdn),
                                "dns_only" to stringResource(R.string.dnss_mode_dns),
                            )
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                modes.forEachIndexed { index, (value, label) ->
                                    SegmentedButton(
                                        selected = (settings.zoneMode ?: "standard") == value,
                                        onClick = { viewModel.setZoneMode(value) },
                                        enabled = state.canWriteSettings && !state.isMutating,
                                        shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                                    ) { Text(label, fontSize = 13.sp, maxLines = 1) }
                                }
                            }
                            settings.nsTtl?.let {
                                Spacer(Modifier.height(10.dp))
                                Row {
                                    Text(
                                        stringResource(R.string.dnss_ns_ttl),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.hc_interval_seconds, it.toInt()),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }

                    if (!state.isLoading && state.settings == null && state.dnssec == null) {
                        Text(
                            stringResource(R.string.dnss_unavailable),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("DS", text))
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}


@Composable
private fun ToggleLine(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

private fun dnssecStatusLabel(status: String?) = when (status) {
    "active" -> R.string.dnss_status_active
    "pending" -> R.string.dnss_status_pending
    "pending-disabled" -> R.string.dnss_status_disabling
    "error" -> R.string.dnss_status_error
    else -> R.string.dnss_status_disabled
}
