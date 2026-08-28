package jiamin.chen.orangecloud.core.system

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import jiamin.chen.orangecloud.MainActivity
import jiamin.chen.orangecloud.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Workers tail 实时日志通知（对应 iOS tail Live Activity）。
 * 连接期间常驻通知显示事件数 + 最新行；Android 16（API 36）请求促升为实况通知，低版本常规常驻。
 */
@Singleton
class TailNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = NotificationManagerCompat.from(context)

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.tail_title), NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }
    }

    private fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun update(scriptName: String, eventCount: Int, lastLine: String, connected: Boolean) {
        if (!hasPermission()) return
        ensureChannel()
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("orangecloud://open/workers")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val statusText = context.getString(if (connected) R.string.tail_connected else R.string.tail_disconnected)
        val contentText = lastLine.ifBlank { context.getString(R.string.tail_events, eventCount) }

        // Android 16（API 36）促升为「实况通知」（Live Update，状态栏常驻 chip + 短文）；
        // NotificationCompat 暂未透出促升 API，故 36+ 直接用平台 Notification.Builder。低版本走常驻通知。
        val notification = if (Build.VERSION.SDK_INT >= 36 && connected) {
            buildPromoted(scriptName, contentText, statusText, eventCount, pending)
        } else {
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(scriptName)
                .setContentText(contentText)
                .setSubText(statusText)
                .setOngoing(connected)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(pending)
                .build()
        }
        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // 权限在 hasPermission() 检查后被撤销的窗口期
        }
    }

    /** API 36 实况通知：setShortCriticalText 状态栏短文（实时事件数）+ FLAG_PROMOTED_ONGOING 促升。 */
    @RequiresApi(36)
    private fun buildPromoted(
        scriptName: String,
        contentText: String,
        statusText: String,
        eventCount: Int,
        pending: PendingIntent,
    ): Notification {
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(scriptName)
            .setContentText(contentText)
            .setSubText(statusText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(pending)
            .setShortCriticalText(eventCount.toString())
            .build()
        // 促升 API 仅暴露为标志位（Builder 无 requestPromotedOngoing 方法）。
        notification.flags = notification.flags or Notification.FLAG_PROMOTED_ONGOING
        return notification
    }

    fun cancel() {
        manager.cancel(NOTIFICATION_ID)
    }

    private companion object {
        const val CHANNEL_ID = "tail"
        const val NOTIFICATION_ID = 4201
    }
}
