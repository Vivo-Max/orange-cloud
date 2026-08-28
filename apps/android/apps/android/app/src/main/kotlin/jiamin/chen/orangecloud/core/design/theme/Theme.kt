package jiamin.chen.orangecloud.core.design.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * 当前生效的暗色状态（== 传给 OrangeCloudTheme 的 darkTheme）。
 * 晨昏天景 / onSky 等品牌层必须读这里，**不要直接用 isSystemInDarkTheme()**——
 * 否则应用内手动切换外观（与系统不一致）时，天色与 MaterialTheme 会发散，导致字色错乱。
 */
val LocalIsDark = staticCompositionLocalOf { false }

/**
 * Material 3 / Material You Expressive，暖橙固定调色板（对齐 UI 稿）。
 *
 * - dynamicColor 默认**关**：UI 稿是固定品牌调色板，开动态取色会被壁纸覆盖。
 *   如需 Material You，传 dynamicColor = true（仅 Android 12+）。
 * - 明暗由 darkTheme 决定（外观偏好：跟随系统 / 浅 / 深），并经 LocalIsDark 下发全局。
 */
@Composable
fun OrangeCloudTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColors
        else -> LightColors
    }
    CompositionLocalProvider(LocalIsDark provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
