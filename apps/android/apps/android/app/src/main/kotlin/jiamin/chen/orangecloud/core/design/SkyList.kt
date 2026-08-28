package jiamin.chen.orangecloud.core.design

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime

/** 当前晨昏相位（随生效外观 LocalIsDark + 本地时刻；不直接读系统深色，避免与主题发散）。 */
@Composable
fun rememberSkyPhase(): SkyPhase {
    val isDark = jiamin.chen.orangecloud.core.design.theme.LocalIsDark.current
    val hour = remember { LocalTime.now().hour }
    return remember(isDark, hour) { SkyPhase.current(isDark, hour) }
}

/** 天景之上的主文本色（深色天用暖白，浅色天用深棕）。 */
val SkyPhase.onSky: Color
    get() = if (isDark) Color(0xFFF3ECE4) else Color(0xFF24190F)

/** 统一页头：可选返回 + 标题 + 额外操作（如排序）+ 刷新/加载。 */
@Composable
fun SkyHeader(
    title: String,
    onSky: Color,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    titleSize: Int = 28,
    refreshDescription: String = "",
    backDescription: String = "",
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (onBack != null) 8.dp else 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, backDescription, tint = onSky)
            }
        }
        Text(
            text = title,
            color = onSky,
            fontSize = titleSize.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        actions()
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = onSky, strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
        } else {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, refreshDescription, tint = onSky)
            }
        }
    }
}

/** 空 / 错误占位（图标 + 文案 + 重试）。 */
@Composable
fun SkyEmptyState(
    icon: ImageVector,
    message: String,
    onSky: Color,
    retryLabel: String,
    onRetry: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = onSky.copy(alpha = 0.6f), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(message, color = onSky.copy(alpha = 0.85f), fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRetry) { Text(retryLabel) }
        }
    }
}
