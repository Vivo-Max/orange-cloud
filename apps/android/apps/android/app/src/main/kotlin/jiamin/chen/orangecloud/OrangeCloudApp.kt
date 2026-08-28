package jiamin.chen.orangecloud

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import dagger.hilt.android.HiltAndroidApp
import jiamin.chen.orangecloud.core.logging.AppLog
import jiamin.chen.orangecloud.core.logging.LogFileStore

@HiltAndroidApp
class OrangeCloudApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 统一日志门面文件落地（反馈附件用，仅缓存目录、不含任何令牌/密钥）。
        AppLog.install(LogFileStore(cacheDir))
        AppLog.app.info("App start ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        initFirebase()
    }

    /** 手动初始化 Firebase（从 BuildConfig 注入，不依赖 google-services.json）。
     *  FCM 配置未填（oss / 未配置）则跳过——推送随之降级，App 正常运行。 */
    private fun initFirebase() {
        if (BuildConfig.FCM_PROJECT_ID.isBlank() || FirebaseApp.getApps(this).isNotEmpty()) return
        runCatching {
            FirebaseApp.initializeApp(
                this,
                FirebaseOptions.Builder()
                    .setProjectId(BuildConfig.FCM_PROJECT_ID)
                    .setApplicationId(BuildConfig.FCM_APP_ID)
                    .setApiKey(BuildConfig.FCM_API_KEY)
                    .setGcmSenderId(BuildConfig.FCM_SENDER_ID)
                    .build(),
            )
        }.onFailure { AppLog.app.error("Firebase init failed: ${it.message}") }
    }
}
