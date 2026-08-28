package jiamin.chen.orangecloud.core.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import jiamin.chen.orangecloud.core.logging.AppLog
import jiamin.chen.orangecloud.core.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * FCM 接收服务：令牌轮换时重注册；收到消息时展示通知 + 落收件箱。
 * Firebase 未初始化时系统不会调用本服务（无害）。
 */
@AndroidEntryPoint
class OcMessagingService : FirebaseMessagingService() {

    @Inject lateinit var repository: PushRepository
    @Inject lateinit var notifier: PushNotifier
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onNewToken(token: String) {
        scope.launch { repository.onTokenRefreshed(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        // 脱敏：只记形态不记内容（title/body 是用户消息）
        AppLog.app.info("fcm message received hasNotification=${message.notification != null} dataKeys=${data.keys.size}")
        // TODO E2E：data["ciphertext"]/["iv"] 存在时在此用 Keystore AES 解密后再展示/落库。
        val title = message.notification?.title ?: data["title"]
        val body = message.notification?.body ?: data["body"]
        val group = data["group"]
        val url = data["url"]
        notifier.show(title, body, url)
        scope.launch { repository.record(title, body, group, url) }
    }
}
