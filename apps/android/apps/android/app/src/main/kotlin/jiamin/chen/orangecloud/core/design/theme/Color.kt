package jiamin.chen.orangecloud.core.design.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/* ============================================================
   Orange Cloud — Material 3 / Material You Expressive
   暖色 tonal 令牌，从 Cloudflare Orange #F48120 派生（对齐 UI 稿 m3-tokens.css）。
   primary 是深橙 #9C4500；亮橙 #F48120 只留给代理云 / 头像 / 强调。
   ============================================================ */

// 品牌亮橙（代理云 / TintIcon 强调 / 头像兜底）
val OcOrange = Color(0xFFF48120)
val OcOrangeBright = Color(0xFFFF9438)

// 语义成功色（状态点：健康 / 已代理 / active）
val OcSuccess = Color(0xFF2E6B3E)
val OcSuccessDark = Color(0xFF98D7A2)

/** 头像哈希色板（对齐 shared.jsx AND_AV_PALETTE）。 */
val AvatarPalette = listOf(
    Color(0xFFE8743B), Color(0xFF3D86E0), Color(0xFF1F9D5B), Color(0xFF9B59C9), Color(0xFFE0508C),
    Color(0xFFC99A1E), Color(0xFF2BAFA6), Color(0xFF5B6CE0), Color(0xFFD85C5C), Color(0xFF4F7C9C),
)

val LightColors = lightColorScheme(
    primary = Color(0xFF9C4500),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC7),
    onPrimaryContainer = Color(0xFF351200),
    inversePrimary = Color(0xFFFFB68A),
    secondary = Color(0xFF765848),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBC7),
    onSecondaryContainer = Color(0xFF2B160A),
    tertiary = Color(0xFF66611C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEEE793),
    onTertiaryContainer = Color(0xFF1F1D00),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF221A15),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF221A15),
    surfaceVariant = Color(0xFFF0DFD5),
    onSurfaceVariant = Color(0xFF52443B),
    surfaceTint = Color(0xFF9C4500),
    outline = Color(0xFF85746A),
    outlineVariant = Color(0xFFD7C2B6),
    inverseSurface = Color(0xFF382E29),
    inverseOnSurface = Color(0xFFFFEDE4),
    scrim = Color.Black,
    surfaceBright = Color(0xFFFFF8F5),
    surfaceDim = Color(0xFFE9D7CD),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF1EA),
    surfaceContainer = Color(0xFFFCEBE1),
    surfaceContainerHigh = Color(0xFFF6E5DB),
    surfaceContainerHighest = Color(0xFFF0DFD5),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB68A),
    onPrimary = Color(0xFF532200),
    primaryContainer = Color(0xFF763300),
    onPrimaryContainer = Color(0xFFFFDBC7),
    inversePrimary = Color(0xFF9C4500),
    secondary = Color(0xFFE6BEAA),
    onSecondary = Color(0xFF432B1C),
    secondaryContainer = Color(0xFF5C4131),
    onSecondaryContainer = Color(0xFFFFDBC7),
    tertiary = Color(0xFFD1CB79),
    onTertiary = Color(0xFF363300),
    tertiaryContainer = Color(0xFF4E4A05),
    onTertiaryContainer = Color(0xFFEEE793),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A120D),
    onBackground = Color(0xFFF0DFD5),
    surface = Color(0xFF1A120D),
    onSurface = Color(0xFFF0DFD5),
    surfaceVariant = Color(0xFF52443B),
    onSurfaceVariant = Color(0xFFD7C2B6),
    surfaceTint = Color(0xFFFFB68A),
    outline = Color(0xFF9F8D82),
    outlineVariant = Color(0xFF52443B),
    inverseSurface = Color(0xFFF0DFD5),
    inverseOnSurface = Color(0xFF382E29),
    scrim = Color.Black,
    surfaceBright = Color(0xFF42372F),
    surfaceDim = Color(0xFF1A120D),
    surfaceContainerLowest = Color(0xFF140C08),
    surfaceContainerLow = Color(0xFF221A15),
    surfaceContainer = Color(0xFF271E18),
    surfaceContainerHigh = Color(0xFF312822),
    surfaceContainerHighest = Color(0xFF3C332C),
)
