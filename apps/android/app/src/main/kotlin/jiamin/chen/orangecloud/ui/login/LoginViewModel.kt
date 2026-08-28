package jiamin.chen.orangecloud.ui.login

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.OAuthRedirectException
import jiamin.chen.orangecloud.core.auth.DeviceGate
import jiamin.chen.orangecloud.core.auth.PermissionCatalog
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 授权页启动事件：ephemeral（freshLogin 添加账号）时用无痕 WebView，其余用 Custom Tab */
data class AuthLaunch(val uri: Uri, val ephemeral: Boolean)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceGate: DeviceGate,
) : ViewModel() {

    private val launchChannel = Channel<AuthLaunch>(Channel.BUFFERED)
    /** 一次性事件：屏幕收到后按 ephemeral 分流打开授权页 */
    val launchAuthTab = launchChannel.receiveAsFlow()

    val redirectError: StateFlow<String?> = authRepository.state
        .map { it.redirectError }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun login(
        selectedIds: Set<String> = PermissionCatalog.defaultSelectedIds,
        freshLogin: Boolean = false,
    ) {
        launchAuth(PermissionCatalog.scopeString(selectedIds), freshLogin)
    }

    /** 按模块读/写级别登录（授权屏用）。levels[id]=true 读写，false 只读。 */
    fun loginWithLevels(levels: Map<String, Boolean>, freshLogin: Boolean = false) {
        launchAuth(PermissionCatalog.scopeString(levels), freshLogin)
    }

    private fun launchAuth(scopeString: String, freshLogin: Boolean) {
        viewModelScope.launch {
            authRepository.clearRedirectError()
            // 设备门禁：被封禁的设备不发起授权，只报通用登录失败（用户无法察觉封禁）
            if (deviceGate.isBanned()) {
                authRepository.reportRedirectError("login_failed")
                return@launch
            }
            runCatching { authRepository.buildAuthorizationUri(scopeString) }
                .onSuccess { launchChannel.send(AuthLaunch(it, ephemeral = freshLogin)) }
                .onFailure { error ->
                    val reason = (error as? OAuthRedirectException)?.reason ?: "missing_oauth_config"
                    authRepository.reportRedirectError(reason)
                }
        }
    }
}
  }
    }
}
