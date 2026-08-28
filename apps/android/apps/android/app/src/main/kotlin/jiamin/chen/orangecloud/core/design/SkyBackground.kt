package jiamin.chen.orangecloud.core.design

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import jiamin.chen.orangecloud.core.design.theme.OcOrange

/**
 * M3 / Material You Expressive 屏背景（对齐 UI 稿 screen.css `.and-screen`）：
 * 暖 surface 之上叠两道极淡径向辉光——右上橙 12%、左上橄榄 8%。
 *
 * 保留 `phase` 形参以兼容既有调用（明暗已由 MaterialTheme colorScheme 接管，故此处不再用相位）。
 */
@Composable
fun SkyBackground(
    phase: SkyPhase,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val surface = MaterialTheme.colorScheme.surface
    val tertiary = MaterialTheme.colorScheme.tertiary
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(surface)
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(OcOrange.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(size.width, 0f),
                        radius = size.width * 1.2f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(tertiary.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(0f, size.height * 0.02f),
                        radius = size.width * 1.1f,
                    ),
                )
            },
    ) {
        content()
    }
}
