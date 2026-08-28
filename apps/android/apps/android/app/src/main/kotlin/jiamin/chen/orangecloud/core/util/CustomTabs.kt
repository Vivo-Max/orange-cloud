package jiamin.chen.orangecloud.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

/**
 * 打开授权页（对应 iOS ASWebAuthenticationSession）。
 * ephemeral（添加账号 / freshLogin）时走无痕 WebView（[WebAuthActivity]），
 * 避免 Custom Tab 与系统浏览器共享 Cookie 复用上一个登录态；常规登录仍用 Custom Tab。
 */
fun Context.launchCustomTab(uri: Uri, ephemeral: Boolean = false) {
    if (ephemeral) {
        startActivity(Intent(this, WebAuthActivity::class.java).apply { data = uri })
    } else {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(this, uri)
    }
}
