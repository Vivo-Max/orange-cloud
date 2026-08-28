package jiamin.chen.orangecloud.core.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import jiamin.chen.orangecloud.core.auth.OAuthConfig

/**
 * 无痕 WebView 授权页（对应 iOS `prefersEphemeralWebBrowserSession`）。
 *
 * 添加第二个账号（freshLogin）时使用：Custom Tab 与系统浏览器共享 Cookie，会自动复用
 * 上一个 Cloudflare 登录态；`prompt=login` 被 Cloudflare 忽略、Chrome 不给第三方开无痕标签。
 * 旧方案「dash.cloudflare.com/logout?to=授权页」能落到登录表单，但副作用是把用户系统浏览器里的
 * Cloudflare 登录态也登出了。独立 WebView + 进出清 Cookie/缓存 = 真无痕，且不碰系统浏览器。
 * （借鉴 fork a422015028 的方案并按仓库规范重写。）
 *
 * 注意：默认 WebView UA 带 "Version/4.0" 标识，Cloudflare 登录页会将其判为嵌入式环境而拦截，
 * 须从 UA 中移除；授权回调（90dd.adsl8.workers.dev 中转 302 回 orangecloud://oauth/callback）在
 * shouldOverrideUrlLoading 截获后转投给主 Activity 的既有 deep link 处理。
 */
class WebAuthActivity : Activity() {

    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 进场先清 Cookie：上一次授权（或上一个账号）的 dash 会话绝不带进本次登录
        CookieManager.getInstance().apply {
            removeAllCookies(null)
            flush()
        }

        val view = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            // Cloudflare 会拦截带 "Version/4.0" 标识的嵌入式 WebView，从系统 UA 中移除该段
            settings.userAgentString = settings.userAgentString.replace("Version/4.0 ", "")
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    if (uri.scheme == OAuthConfig.CALLBACK_SCHEME && uri.host == OAuthConfig.CALLBACK_HOST) {
                        // 转投主 Activity 的 orangecloud:// deep link 处理（换 token / 新增身份）
                        startActivity(Intent(Intent.ACTION_VIEW, uri).apply { `package` = packageName })
                        finish()
                        return true
                    }
                    return false
                }
            }
            clearCache(true)
            clearHistory()
            clearFormData()
        }
        webView = view

        // 全面屏（edge-to-edge）下让内容避开系统栏，Android 16+ 默认沉浸不再遮挡登录表单
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }

        setContentView(view)
        intent.data?.let { view.loadUrl(it.toString()) }
    }

    /** 离场彻底销毁：Cookie/缓存/历史全清，多账号连续登录互不污染 */
    override fun onDestroy() {
        CookieManager.getInstance().apply {
            removeAllCookies(null)
            flush()
        }
        webView?.apply {
            loadUrl("about:blank")
            stopLoading()
            clearCache(true)
            removeAllViews()
            destroy()
        }
        webView = null
        super.onDestroy()
    }
}
