package jiamin.chen.orangecloud.core.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用内安装：下载 APK 并流式写入 [PackageInstaller] 会话后提交，由系统弹出安装确认界面。
 *
 * 仅 `direct` 风味调用（其 manifest 声明 `REQUEST_INSTALL_PACKAGES` 权限并注册 [InstallResultReceiver]）。
 * 任一步失败返回 `false`，调用方回退到浏览器下载。
 *
 * 注意：下载的 APK 必须与已安装的 `direct` 包**同签名密钥**，否则系统会因签名不符拒绝原地更新。
 */
@Singleton
class ApkInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val http = OkHttpClient()

    suspend fun downloadAndInstall(url: String): Boolean = withContext(Dispatchers.IO) {
        val installer = context.packageManager.packageInstaller
        var sessionId = -1
        try {
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            sessionId = installer.createSession(params)
            installer.openSession(sessionId).use { session ->
                http.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                    if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                    val body = resp.body ?: throw IOException("empty body")
                    session.openWrite(WRITE_NAME, 0, body.contentLength()).use { out ->
                        body.byteStream().copyTo(out)
                        session.fsync(out)
                    }
                }
                val pending = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    Intent(context, InstallResultReceiver::class.java)
                        .setAction(InstallResultReceiver.ACTION_STATUS)
                        .setPackage(context.packageName),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                )
                session.commit(pending.intentSender)
            }
            true
        } catch (_: Exception) {
            if (sessionId != -1) runCatching { installer.abandonSession(sessionId) }
            false
        }
    }

    companion object {
        private const val WRITE_NAME = "orange_cloud_update.apk"
    }
}
