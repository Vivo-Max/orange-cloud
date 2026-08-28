package jiamin.chen.orangecloud.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.auth.PermissionCatalog
import jiamin.chen.orangecloud.core.design.SkyBackground
import jiamin.chen.orangecloud.core.design.ZoneAvatar
import jiamin.chen.orangecloud.core.design.onSky
import jiamin.chen.orangecloud.core.design.rememberSkyPhase
import jiamin.chen.orangecloud.core.design.theme.OcSuccess
import jiamin.chen.orangecloud.core.design.theme.OcSuccessDark

@Composable
fun IdentityDetailScreen(
    sessionId: String,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = rememberSkyPhase()
    val onSky = phase.onSky
    val cs = MaterialTheme.colorScheme
    val green = if (jiamin.chen.orangecloud.core.design.theme.LocalIsDark.current) OcSuccessDark else OcSuccess

    val session = state.sessions.firstOrNull { it.id == sessionId }
    val isCurrent = sessionId == state.currentSessionId
    val isLastSession = state.sessions.size <= 1
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    SkyBackground(phase = phase) {
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()),
        ) {
            // 页头：返回 + 标题
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = onSky)
                }
                Text(stringResource(R.string.identity_title), color = onSky, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            if (session == null) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.identity_not_found), color = onSky.copy(alpha = 0.7f))
                }
                return@Column
            }

            // ── 身份信息卡 ──
            Surface(color = cs.surfaceContainerLow, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ZoneAvatar(session.label, size = 64.dp)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(session.label, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(6.dp))
                        IconButton(
                            onClick = { renameText = session.label; showRenameDialog = true },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.identity_rename), tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.background(green.copy(alpha = 0.14f), RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Icon(Icons.Outlined.Shield, contentDescription = null, tint = green, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(stringResource(R.string.identity_oauth_badge), color = green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── 当前身份 / 切换 ──
            SectionHeader(stringResource(R.string.identity_section_current))
            Surface(color = cs.surfaceContainerLow, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                if (isCurrent) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = green, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.identity_current), fontSize = 16.sp, color = cs.onSurface)
                    }
                } else {
                    Button(
                        onClick = { viewModel.switchSession(sessionId); onBack() },
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                    ) { Text(stringResource(R.string.identity_switch)) }
                }
            }

            // ── 已授权权限 ──
            val granted = PermissionCatalog.features.filter { f -> session.scopes.containsAll(f.readScopes) }
            SectionHeader(stringResource(R.string.identity_section_perms))
            Surface(color = cs.surfaceContainerLow, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column {
                    granted.forEachIndexed { index, f ->
                        val readWrite = f.editScopes.isNotEmpty() && session.scopes.containsAll(f.editScopes)
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = green, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(f.nameRes), fontSize = 16.sp, color = cs.onSurface, modifier = Modifier.weight(1f))
                            Text(
                                stringResource(if (readWrite) R.string.identity_perm_readwrite else R.string.identity_perm_readonly),
                                fontSize = 13.sp,
                                color = cs.onSurfaceVariant,
                            )
                        }
                        if (index != granted.lastIndex) {
                            Box(Modifier.fillMaxWidth().padding(start = 46.dp).height(1.dp).background(cs.outlineVariant.copy(alpha = 0.5f)))
                        }
                    }
                }
            }

            // ── 退出登录 ──
            Spacer(Modifier.height(20.dp))
            Surface(color = cs.surfaceContainerLow, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                TextButton(
                    onClick = { showLogoutConfirm = true },
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                ) {
                    Text(stringResource(R.string.settings_logout), color = cs.error, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(stringResource(R.string.identity_logout_confirm_title)) },
            text = { Text(stringResource(if (isLastSession) R.string.identity_logout_confirm_msg_last else R.string.identity_logout_confirm_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    viewModel.logout(sessionId)
                    onBack()
                }) { Text(stringResource(R.string.settings_logout), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.identity_rename_title)) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.identity_rename_hint)) },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = renameText.trim()
                    if (trimmed.isNotEmpty() && trimmed != session?.label) {
                        viewModel.renameSession(sessionId, trimmed)
                    }
                    showRenameDialog = false
                }) { Text(stringResource(R.string.common_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 8.dp),
    )
}
