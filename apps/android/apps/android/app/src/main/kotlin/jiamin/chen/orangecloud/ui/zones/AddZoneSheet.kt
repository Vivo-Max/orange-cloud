package jiamin.chen.orangecloud.ui.zones

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.design.ZoneAvatar
import jiamin.chen.orangecloud.core.util.copyToClipboard
import jiamin.chen.orangecloud.core.design.theme.OcOrange
import jiamin.chen.orangecloud.data.model.Zone

/**
 * 添加域名底部表单：填根域名 → POST /zones（full setup）→ 切到名称服务器结果页。
 * 对应 iOS AddZoneView（表单 + 结果两段）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddZoneSheet(
    state: AddZoneUiState,
    accountName: String?,
    sheetState: SheetState,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        val zone = state.createdZone
        if (zone == null) {
            AddZoneForm(state, accountName, onCreate)
        } else {
            AddZoneResult(zone, onDismiss)
        }
    }
}

/** 去掉协议头/路径/首尾点与空白，统一小写——用户常整段粘贴 URL。 */
private fun normalizeDomain(input: String): String {
    var s = input.trim().lowercase()
    s.indexOf("://").takeIf { it >= 0 }?.let { s = s.substring(it + 3) }
    s.indexOf('/').takeIf { it >= 0 }?.let { s = s.substring(0, it) }
    return s.trim('.')
}

/** 宽松校验：至少两段、无空段、无空格、TLD ≥ 2 字符（最终以服务端为准，不限 ASCII 以容国际化域名）。 */
private fun isValidDomain(input: String): Boolean {
    val s = normalizeDomain(input)
    if (s.length < 3 || s.contains(' ')) return false
    val labels = s.split('.')
    if (labels.size < 2 || labels.any { it.isEmpty() }) return false
    return (labels.lastOrNull()?.length ?: 0) >= 2
}

@Composable
private fun AddZoneForm(
    state: AddZoneUiState,
    accountName: String?,
    onCreate: (String) -> Unit,
) {
    var domain by remember { mutableStateOf("") }
    val canSubmit = isValidDomain(domain) && !state.isSaving

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.addzone_title), fontSize = 20.sp, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = domain,
            onValueChange = { domain = it },
            label = { Text(stringResource(R.string.addzone_domain_label)) },
            placeholder = { Text("example.com") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(R.string.addzone_domain_hint),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!accountName.isNullOrBlank()) {
            Text(
                stringResource(R.string.addzone_account, accountName),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Text(
            stringResource(R.string.addzone_ns_note),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        state.error?.let {
            Text(it, color = Color(0xFFE5484D), fontSize = 13.sp)
        }

        Button(
            onClick = { onCreate(normalizeDomain(domain)) },
            enabled = canSubmit,
            colors = ButtonDefaults.buttonColors(containerColor = OcOrange, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(Modifier.width(8.dp))
            }
            Text(stringResource(R.string.addzone_add))
        }
    }
}

@Composable
private fun AddZoneResult(zone: Zone, onDone: () -> Unit) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    val nameServers = zone.nameServers.orEmpty()
    val steps = listOf(
        stringResource(R.string.addzone_step1),
        stringResource(R.string.addzone_step2),
        stringResource(R.string.addzone_step3),
        stringResource(R.string.addzone_step4),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ZoneAvatar(zone.name, size = 52.dp)
            Text(zone.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
                stringResource(R.string.addzone_pending),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (nameServers.isEmpty()) {
            Text(
                stringResource(R.string.addzone_ns_missing),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            ResultCard(stringResource(R.string.addzone_ns_title)) {
                nameServers.forEach { server ->
                    Text(
                        server,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.size(2.dp))
                OutlinedButton(
                    onClick = {
                        copyToClipboard(context, nameServers.joinToString("\n"))
                        copied = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(if (copied) R.string.addzone_copied else R.string.addzone_copy))
                }
            }

            ResultCard(stringResource(R.string.addzone_steps_title)) {
                steps.forEachIndexed { index, step ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            Modifier.size(22.dp).clip(CircleShape)
                                .background(OcOrange),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${index + 1}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(step, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(containerColor = OcOrange, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.addzone_done))
        }
    }
}

@Composable
private fun ResultCard(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                content()
            }
        }
    }
}
