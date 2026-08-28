package jiamin.chen.orangecloud.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.BuildConfig
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.AuthSessionMeta
import jiamin.chen.orangecloud.core.purchase.EntitlementStore
import jiamin.chen.orangecloud.core.system.AppAppearance
import jiamin.chen.orangecloud.core.system.AppPrefs
import jiamin.chen.orangecloud.data.repository.AccountStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val sessions: List<AuthSessionMeta> = emptyList(),
    val currentSessionId: String? = null,
    val isPro: Boolean = false,
    val appearance: AppAppearance = AppAppearance.SYSTEM,
    val notificationsEnabled: Boolean = false,
    val notifyZoneStatus: Boolean = true,
    val notifyWorkerErrors: Boolean = true,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountStore: AccountStore,
    private val appPrefs: AppPrefs,
    entitlementStore: EntitlementStore,
) : ViewModel() {

    val isOss: Boolean = BuildConfig.IS_OSS

    val uiState: StateFlow<SettingsUiState> =
        combine(
            authRepository.state,
            entitlementStore.isPro,
            appPrefs.appearance,
            combine(appPrefs.notificationsEnabled, appPrefs.notifyZoneStatus, appPrefs.notifyWorkerErrors) { master, zone, worker ->
                Triple(master, zone, worker)
            },
        ) { auth, isPro, appearance, notif ->
            SettingsUiState(
                sessions = auth.sessions,
                currentSessionId = auth.currentSessionId,
                isPro = isPro,
                appearance = appearance,
                notificationsEnabled = notif.first,
                notifyZoneStatus = notif.second,
                notifyWorkerErrors = notif.third,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun session(id: String): AuthSessionMeta? = authRepository.state.value.sessions.firstOrNull { it.id == id }

    fun switchSession(id: String) = authRepository.switchSession(id)

    /** 重命名登录身份的展示标签（下次账号列表加载不会回填覆盖为空标签的场景） */
    fun renameSession(id: String, label: String) = authRepository.updateSessionLabel(id, label)

    fun logout(sessionId: String) {
        viewModelScope.launch { authRepository.logout(sessionId) }
    }

    fun setAppearance(appearance: AppAppearance) {
        viewModelScope.launch { appPrefs.setAppearance(appearance) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { appPrefs.setNotificationsEnabled(enabled) }
    }

    fun setNotifyZoneStatus(enabled: Boolean) {
        viewModelScope.launch { appPrefs.setNotifyZoneStatus(enabled) }
    }

    fun setNotifyWorkerErrors(enabled: Boolean) {
        viewModelScope.launch { appPrefs.setNotifyWorkerErrors(enabled) }
    }
}
