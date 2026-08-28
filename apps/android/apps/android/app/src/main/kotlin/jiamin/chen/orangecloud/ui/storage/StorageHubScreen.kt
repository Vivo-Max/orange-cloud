package jiamin.chen.orangecloud.ui.storage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.design.SkyBackground
import jiamin.chen.orangecloud.core.design.onSky
import jiamin.chen.orangecloud.core.design.rememberSkyPhase

/** 存储顶级 Tab：分发到 R2 / D1 / KV（对应 iOS 存储 Tab）。 */
@Composable
fun StorageHubScreen(
    onOpenR2: () -> Unit,
    onOpenD1: () -> Unit,
    onOpenKV: () -> Unit,
) {
    val phase = rememberSkyPhase()
    val onSky = phase.onSky

    SkyBackground(phase = phase) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Text(
                text = stringResource(R.string.nav_storage),
                color = onSky,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StorageRow(Icons.Outlined.Cloud, stringResource(R.string.storage_r2), onClick = onOpenR2)
                StorageRow(Icons.Outlined.Storage, stringResource(R.string.storage_d1), onClick = onOpenD1)
                StorageRow(Icons.Outlined.Key, stringResource(R.string.storage_kv), onClick = onOpenKV)
            }
        }
    }
}
