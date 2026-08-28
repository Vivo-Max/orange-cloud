package jiamin.chen.orangecloud.ui.dnssettings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.data.model.ZoneDnsSettings
import jiamin.chen.orangecloud.data.model.ZoneDnsSettingsUpdate
import jiamin.chen.orangecloud.data.model.ZoneDnssec
import jiamin.chen.orangecloud.data.model.ZoneNameserversUpdate
import jiamin.chen.orangecloud.data.repository.DnsSettingsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DnsSettingsUiState(
    val zoneName: String = "",
    val settings: ZoneDnsSettings? = null,
    val dnssec: ZoneDnssec? = null,
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val canWriteSettings: Boolean = false,
    val canWriteDns: Boolean = false,
)

sealed interface DnsSettingsEvent {
    data class Error(val message: String?) : DnsSettingsEvent
}

@HiltViewModel
class DnsSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: DnsSettingsRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val zoneId: String = checkNotNull(savedStateHandle["zoneId"])
    private val canWriteSettings = authRepository.hasScope(Scopes.ZONE_DNS_SETTINGS_WRITE)
    private val canWriteDns = authRepository.hasScope(Scopes.DNS_WRITE)

    private val _uiState = MutableStateFlow(
        DnsSettingsUiState(
            zoneName = savedStateHandle.get<String>("zoneName").orEmpty(),
            canWriteSettings = canWriteSettings,
            canWriteDns = canWriteDns,
        ),
    )
    val uiState: StateFlow<DnsSettingsUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<DnsSettingsEvent>(Channel.BUFFERED)
    val events: Flow<DnsSettingsEvent> = eventChannel.receiveAsFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // 两条链路权限不同，任一失败不连累另一条
            val s = async { runCatching { repository.settings(zoneId) }.getOrNull() }
            val d = async { runCatching { repository.dnssec(zoneId) }.getOrNull() }
            val settings = s.await()
            val dnssec = d.await()
            _uiState.update { it.copy(settings = settings, dnssec = dnssec, isLoading = false) }
        }
    }

    private fun apply(update: ZoneDnsSettingsUpdate) {
        if (!canWriteSettings || _uiState.value.isMutating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true) }
            runCatching { repository.updateSettings(zoneId, update) }
                .onSuccess { s -> _uiState.update { it.copy(settings = s) } }
                .onFailure { eventChannel.send(DnsSettingsEvent.Error(it.message)); load() }
            _uiState.update { it.copy(isMutating = false) }
        }
    }

    fun setFlattenAllCnames(on: Boolean) = apply(ZoneDnsSettingsUpdate(flattenAllCnames = on))

    fun setMultiProvider(on: Boolean) = apply(ZoneDnsSettingsUpdate(multiProvider = on))

    fun setZoneMode(mode: String) = apply(ZoneDnsSettingsUpdate(zoneMode = mode))

    /** 高级名称服务器：用 nameservers.type 而非已弃用的 foundation_dns */
    fun setAdvancedNameservers(on: Boolean) = apply(
        ZoneDnsSettingsUpdate(
            nameservers = ZoneNameserversUpdate(if (on) "cloudflare.advanced" else "cloudflare.standard"),
        ),
    )

    fun setDnssec(on: Boolean) {
        if (!canWriteDns || _uiState.value.isMutating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true) }
            runCatching { repository.setDnssec(zoneId, on) }
                .onSuccess { d -> _uiState.update { it.copy(dnssec = d) } }
                .onFailure { eventChannel.send(DnsSettingsEvent.Error(it.message)); load() }
            _uiState.update { it.copy(isMutating = false) }
        }
    }
}
