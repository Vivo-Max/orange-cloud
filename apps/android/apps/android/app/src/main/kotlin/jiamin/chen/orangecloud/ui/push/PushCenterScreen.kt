package jiamin.chen.orangecloud.ui.push

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import jiamin.chen.orangecloud.core.design.onSky
import jiamin.chen.orangecloud.core.design.rememberSkyPhase
import jiamin.chen.orangecloud.ui.toolbox.ToolFieldRow
import jiamin.chen.orangecloud.ui.toolbox.ToolHeader
import jiamin.chen.orangecloud.ui.toolbox.ToolResultCard
import jiamin.chen.orangecloud.ui.toolbox.copyToClipboard

@Composable
fun PushCenterScreen(
    onBack: () -> Unit,
    onOpenInbox: () -> Unit,
    viewModel: PushViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val testTitle = stringResource(R.string.push_test_title)
    val testBody = stringResource(R.string.push_test_body)

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.enable()
    }
    fun enableWithPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.enable()
        }
    }

    SkyBackground(phase = phase) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            ToolHeader(stringResource(R.string.push_title), onSky, onBack)
            Column(Modifier.verticalScroll(rememberScrollState()).padding(PaddingValues(horizontal = 16.dp))) {
                Text(
                    stringResource(R.string.push_subtitle),
                    color = onSky.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 6.dp),
                )

                if (!state.available) {
                    ToolResultCard {
                        Text(
                            stringResource(R.string.push_unavailable),
                            fontSize = 14.sp,
                            color = cs.onSurface,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                } else {
                    // 启用开关
                    ToolResultCard {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.push_enable_title), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = cs.onSurface)
                                Text(stringResource(R.string.push_enable_sub), fontSize = 12.sp, color = cs.onSurfaceVariant)
                            }
                            Switch(
                                checked = state.enabled,
                                enabled = !state.working,
                                onCheckedChange = { on -> if (on) enableWithPermission() else viewModel.disable() },
                            )
                        }
                    }

                    if (state.enabled && state.endpoint != null) {
                        Spacer(Modifier.height(14.dp))
                        SectionLabel(stringResource(R.string.push_endpoint_label))
                        ToolResultCard { ToolFieldRow("URL", state.endpoint!!, mono = true) }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { viewModel.testPush(testTitle, testBody) },
                                enabled = !state.working,
                                colors = ButtonDefaults.buttonColors(containerColor = cs.primary, contentColor = cs.onPrimary),
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) { Text(stringResource(R.string.push_test)) }
                            OutlinedButton(
                                onClick = { copyToClipboard(context, "curl ${state.endpoint}/Hello") },
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) { Text(stringResource(R.string.push_copy_curl)) }
                        }
                    }

                    if (state.failed) {
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.push_failed), color = cs.error, fontSize = 13.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onOpenInbox,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text(stringResource(R.string.push_inbox_open, state.messages.size))
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
    )
}
