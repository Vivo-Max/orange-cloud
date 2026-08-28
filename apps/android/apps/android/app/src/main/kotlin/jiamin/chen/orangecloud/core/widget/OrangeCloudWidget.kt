package jiamin.chen.orangecloud.core.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import jiamin.chen.orangecloud.MainActivity
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.design.SkyPhase
import java.time.LocalTime

/**
 * 晨昏「天窗」小组件（对应 iOS 主屏卡）：大字直落天色，展示当前账号 24h 请求合计。
 * 天色随刷新时刻 + 系统深浅走 [SkyPhase]；数据读 [WidgetSnapshotStore]（App 侧 Dashboard 写入）。
 */
class OrangeCloudWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSnapshotStore.read(context)
        val isNight = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val phase = SkyPhase.current(isNight, LocalTime.now().hour)
        provideContent { WidgetBody(snapshot, phase) }
    }
}

@Composable
private fun WidgetBody(snapshot: WidgetSnapshot, phase: SkyPhase) {
    val context = LocalContext.current
    val bg = phase.body.first()
    val textColor = if (phase.isDark) Color.White else Color(0xFF1A1A1A)
    val subColor = if (phase.isDark) Color(0xFFBDBDBD) else Color(0xFF6B6B6B)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(bg))
            .cornerRadius(20.dp)
            .padding(16.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
    ) {
        Text(
            context.getString(R.string.widget_today_requests),
            style = TextStyle(color = ColorProvider(subColor), fontSize = 12.sp),
        )
        Spacer(GlanceModifier.height(2.dp))
        Text(
            snapshot.todayRequests,
            style = TextStyle(color = ColorProvider(textColor), fontSize = 34.sp, fontWeight = FontWeight.Bold),
        )
        Spacer(GlanceModifier.defaultWeight())
        Text(
            context.getString(R.string.widget_footer, snapshot.zoneCount, snapshot.accountName),
            style = TextStyle(color = ColorProvider(subColor), fontSize = 12.sp),
            maxLines = 1,
        )
    }
}

class OrangeCloudWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OrangeCloudWidget()
}
