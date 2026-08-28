package jiamin.chen.orangecloud

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.OAuthConfig
import jiamin.chen.orangecloud.core.design.theme.OrangeCloudTheme
import jiamin.chen.orangecloud.core.purchase.BillingGateway
import jiamin.chen.orangecloud.core.purchase.RedeemOutcome
import jiamin.chen.orangecloud.core.system.AppAppearance
import jiamin.chen.orangecloud.core.system.AppPrefs
import jiamin.chen.orangecloud.ui.root.OrangeCloudRoot
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var billingGateway: BillingGateway

    @Inject
    lateinit var appPrefs: AppPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        billingGateway.connect()
        handleOAuthRedirect(intent)
        handleRedeemRedirect(intent)
        setContent {
            val appearance by appPrefs.appearance.collectAsStateWithLifecycle(initialValue = AppAppearance.SYSTEM)
            val darkTheme = when (appearance) {
                AppAppearance.LIGHT -> false
                AppAppearance.DARK -> true
                AppAppearance.SYSTEM -> isSystemInDarkTheme()
            }
            OrangeCloudTheme(darkTheme = darkTheme) {
                // 状态栏 / 导航栏图标色随生效主题（而非系统），与晨昏天景对比一致
                val view = LocalView.current
                SideEffect {
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                }
                OrangeCloudRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthRedirect(intent)
        handleRedeemRedirect(intent)
    }

    /** 接住 Web 后端 302 跳回的 orangecloud://oauth/callback，交给 AuthRepository 验 state + 换 token。 */
    private fun handleOAuthRedirect(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != OAuthConfig.CALLBACK_SCHEME || data.host != OAuthConfig.CALLBACK_HOST) return
        lifecycleScope.launch { authRepository.handleRedirect(data) }
    }

    /** direct 风味：接住 Web 成功页的 orangecloud://redeem?code=...，兑换并提示结果（play/oss 的清单不含此 filter）。 */
    private fun handleRedeemRedirect(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "orangecloud" || data.host != "redeem") return
        val code = data.getQueryParameter("code")?.takeIf { it.isNotBlank() } ?: return
        lifecycleScope.launch {
            val msg = when (billingGateway.redeem(code)) {
                RedeemOutcome.SUCCESS -> R.string.redeem_ok
                RedeemOutcome.INVALID -> R.string.redeem_invalid
                RedeemOutcome.REVOKED -> R.string.redeem_revoked
                RedeemOutcome.DEVICE_LIMIT -> R.string.redeem_device_limit
                else -> R.string.redeem_network
            }
            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
        }
    }
}
