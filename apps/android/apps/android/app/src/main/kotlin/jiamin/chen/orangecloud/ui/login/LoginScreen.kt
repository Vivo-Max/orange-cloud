package jiamin.chen.orangecloud.ui.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.auth.PermissionCatalog
import jiamin.chen.orangecloud.core.auth.PermissionFeature
import jiamin.chen.orangecloud.core.design.SkyBackground
import jiamin.chen.orangecloud.core.design.rememberSkyPhase
import jiamin.chen.orangecloud.core.design.theme.OcOrange
import jiamin.chen.orangecloud.core.design.theme.OcSuccess
import jiamin.chen.orangecloud.core.util.launchCustomTab

@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel(), onOpenToolbox: () -> Unit = {}) {
    val context = LocalContext.current
    val error by viewModel.redirectError.collectAsStateWithLifecycle()
    var showAuthorize by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.launchAuthTab.collect { launch -> context.launchCustomTab(launch.uri, launch.ephemeral) }
    }

    if (showAuthorize) {
        AuthorizeView(
            onCancel = { showAuthorize = false },
            onContinue = { levels -> showAuthorize = false; viewModel.loginWithLevels(levels) },
        )
    } else {
        BrandView(error = error, onSignIn = { showAuthorize = true }, onOpenToolbox = onOpenToolbox)
    }
}

@Composable
private fun BrandView(error: String?, onSignIn: () -> Unit, onOpenToolbox: () -> Unit) {
    val phase = rememberSkyPhase()
    val cs = MaterialTheme.colorScheme

    SkyBackground(phase = phase) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                BrandCloud()
                Spacer(Modifier.height(20.dp))
                Text("Orange Cloud", fontSize = 36.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.login_subtitle), fontSize = 16.sp, color = cs.onSurfaceVariant)
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = onSignIn,
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary, contentColor = cs.onPrimary),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Icon(Icons.Filled.Cloud, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.login_cta), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = OcSuccess, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(stringResource(R.string.login_secure), fontSize = 12.sp, color = cs.onSurfaceVariant)
                }
                Spacer(Modifier.height(16.dp))
                // 免登录 · 开发者工具箱（次级入口，方案 A：白底 0.5px 描边胶囊）
                OutlinedButton(
                    onClick = onOpenToolbox,
                    border = BorderStroke(0.5.dp, cs.outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.onSurface),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) {
                    Icon(Icons.Outlined.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.toolbox_login_cta), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                if (error != null) {
                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(R.string.login_failed), color = cs.error, fontSize = 14.sp)
                }
            }
        }
    }
}

/** 品牌云：橙渐变填充 + 橙色柔光投影（对齐 iOS LoginView 的 cloud.fill 渐变）。 */
@Composable
private fun BrandCloud() {
    val gradient = Brush.linearGradient(listOf(Color(0xFFFFA64F), OcOrange, Color(0xFFD9700F)))
    Box(contentAlignment = Alignment.Center) {
        // 橙色柔光：同形状半透明橙、下移 + 模糊
        Icon(
            Icons.Filled.Cloud,
            contentDescription = null,
            tint = OcOrange.copy(alpha = 0.34f),
            modifier = Modifier.size(84.dp).offset(y = 10.dp).blur(16.dp),
        )
        // 渐变云体（离屏层 + SrcAtop 把线性渐变贴进云的轮廓）
        Icon(
            Icons.Filled.Cloud,
            contentDescription = null,
            tint = OcOrange,
            modifier = Modifier
                .size(84.dp)
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(gradient, blendMode = BlendMode.SrcAtop)
                },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthorizeView(onCancel: () -> Unit, onContinue: (Map<String, Boolean>) -> Unit) {
    val phase = rememberSkyPhase()
    val cs = MaterialTheme.colorScheme
    // id -> isWrite；默认全选（有写权限的取读写），zones 必选
    val levels = remember {
        mutableStateMapOf<String, Boolean>().apply {
            PermissionCatalog.features.forEach { put(it.id, it.editScopes.isNotEmpty()) }
        }
    }
    val count = levels.size

    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCancel) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.dns_cancel), tint = cs.onSurface) }
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.perm_title), fontSize = 22.sp, fontWeight = FontWeight.Medium, color = cs.onSurface)
                }
                Text(
                    stringResource(R.string.perm_subtitle),
                    fontSize = 14.sp,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 4.dp),
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(PermissionCatalog.features, key = { it.id }) { feature ->
                        ModuleRow(
                            feature = feature,
                            on = feature.id in levels,
                            write = levels[feature.id] == true,
                            required = feature.required,
                            onToggle = { checked ->
                                if (feature.required) return@ModuleRow
                                if (checked) levels[feature.id] = feature.editScopes.isNotEmpty() else levels.remove(feature.id)
                            },
                            onLevel = { w -> levels[feature.id] = w },
                        )
                    }
                }
                Surface(color = cs.surfaceContainer, shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.perm_count, count), fontSize = 14.sp, color = cs.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { onContinue(levels.toMap()) },
                            enabled = count > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = cs.primary, contentColor = cs.onPrimary),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) {
                            Text(stringResource(R.string.perm_continue), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModuleRow(
    feature: PermissionFeature,
    on: Boolean,
    write: Boolean,
    required: Boolean,
    onToggle: (Boolean) -> Unit,
    onLevel: (Boolean) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Surface(color = cs.surfaceContainerLow, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(percent = 28))
                        .background(if (on) cs.primaryContainer else cs.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(featureIcon(feature.id), contentDescription = null, tint = if (on) cs.onPrimaryContainer else cs.onSurfaceVariant, modifier = Modifier.size(21.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(feature.nameRes), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(feature.descRes), fontSize = 12.sp, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Switch(checked = on, onCheckedChange = onToggle, enabled = !required)
            }
            if (on && feature.editScopes.isNotEmpty()) {
                Row(Modifier.padding(start = 76.dp, end = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(selected = !write, onClick = { onLevel(false) }, shape = SegmentedButtonDefaults.itemShape(0, 2)) {
                            Text(stringResource(R.string.perm_read), fontSize = 13.sp)
                        }
                        SegmentedButton(selected = write, onClick = { onLevel(true) }, shape = SegmentedButtonDefaults.itemShape(1, 2)) {
                            Text(stringResource(R.string.perm_write), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun featureIcon(id: String) = when (id) {
    "account" -> Icons.Outlined.Person
    "zones" -> Icons.Outlined.Language
    "dns" -> Icons.Outlined.Dns
    "workers" -> Icons.Outlined.Bolt
    "workers_tail" -> Icons.Outlined.Terminal
    "snippets" -> Icons.Outlined.Code
    "r2" -> Icons.Filled.Cloud
    "d1" -> Icons.Outlined.Storage
    "kv" -> Icons.Outlined.Key
    "tunnels" -> Icons.Outlined.Hub
    "waf" -> Icons.Outlined.Shield
    "zone_settings" -> Icons.Outlined.Tune
    "analytics" -> Icons.Outlined.BarChart
    else -> Icons.Outlined.Language
}
