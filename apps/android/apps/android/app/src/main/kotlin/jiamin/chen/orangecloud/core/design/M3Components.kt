package jiamin.chen.orangecloud.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jiamin.chen.orangecloud.core.design.theme.AvatarPalette
import jiamin.chen.orangecloud.core.design.theme.OcOrange

/* ============================================================
   M3 / Material You Expressive 共享组件（对齐 UI 稿 shared.jsx / material.jsx）
   ============================================================ */

/** 域名首字母（去 www. 前缀，大写）。 */
fun zoneInitial(domain: String): String =
    domain.removePrefix("www.").removePrefix("https://").removePrefix("http://")
        .firstOrNull()?.uppercase() ?: "?"

/** 按域名哈希出稳定底色（对齐 shared.jsx avatarBase）。 */
fun avatarColor(domain: String): Color {
    var h = 0L
    for (c in domain) h = (h * 31 + c.code) and 0xFFFFFFFFL
    return AvatarPalette[(h % AvatarPalette.size).toInt()]
}

private fun Color.darken(factor: Float): Color = Color(red * factor, green * factor, blue * factor, alpha)

/** 域名字母头像：哈希色 + 150° 渐变 + squircle（对齐 ZoneAvatar）。 */
@Composable
fun ZoneAvatar(domain: String, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val base = avatarColor(domain)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 30))
            .background(Brush.linearGradient(listOf(base, base.darken(0.72f)))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = zoneInitial(domain),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp,
        )
    }
}

/** 功能图标：色彩 16% 底 + 圆角方块（对齐 TintIcon）。 */
@Composable
fun TintIcon(icon: ImageVector, color: Color = OcOrange, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 28))
            .background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(size * 0.52f))
    }
}

/** 状态点 + 20% 辉光环（对齐 .and-dot box-shadow）。 */
@Composable
fun StatusDot(color: Color, size: Dp = 10.dp) {
    Box(
        modifier = Modifier.size(size + 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(size + 6.dp).clip(CircleShape).background(color.copy(alpha = 0.20f)))
        Box(Modifier.size(size).clip(CircleShape).background(color))
    }
}

/** 套餐徽章（按套餐着色，对齐 PlanBadge）。 */
@Composable
fun PlanBadge(plan: String) {
    val cs = MaterialTheme.colorScheme
    val (bg, fg) = when (plan.lowercase()) {
        "pro" -> cs.primary.copy(alpha = 0.18f) to cs.primary
        "business" -> Color(0xFF5856D6).copy(alpha = 0.18f) to Color(0xFF5856D6)
        "enterprise" -> Color(0xFFC99A1E).copy(alpha = 0.22f) to Color(0xFF9A7600)
        else -> cs.surfaceContainerHighest to cs.onSurfaceVariant
    }
    Box(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(plan, color = fg, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
    }
}

/** Dashboard 统计磁贴（对齐 dashboard.jsx StatTile）。 */
@Composable
fun StatTile(
    icon: ImageVector,
    value: String,
    label: String,
    sub: String,
    primary: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val bg = if (primary) cs.primaryContainer else cs.surfaceContainerHigh
    val iconTint = if (primary) cs.onPrimaryContainer else cs.primary
    val iconBg = if (primary) cs.primary.copy(alpha = 0.22f) else OcOrange.copy(alpha = 0.18f)
    val valueColor = if (primary) cs.onPrimaryContainer else cs.primary
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(bg)
            .heightIn(min = 116.dp)
            .padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(percent = 30)).background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(sub, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = cs.onSurfaceVariant)
        }
        Spacer(Modifier.weight(1f))
        Text(
            value,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.size(5.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = cs.onSurface)
    }
}
