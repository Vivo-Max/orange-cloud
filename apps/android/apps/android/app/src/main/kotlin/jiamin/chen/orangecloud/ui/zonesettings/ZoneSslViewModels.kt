package jiamin.chen.orangecloud.ui.zonesettings

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.data.model.SslCertificatePack
import jiamin.chen.orangecloud.data.repository.SslRepository
import jiamin.chen.orangecloud.data.repository.ZoneSettingsRepository
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

/** SSL/TLS 加密模式（zone setting `ssl` 取值）。对应 iOS SSLMode。 */
enum class SslMode(val raw: String, @StringRes val titleRes: Int, @StringRes val blurbRes: Int) {
    OFF("off", R.string.ssl_mode_off, R.string.ssl_mode_off_blurb),
    FLEXIBLE("flexible", R.string.ssl_mode_flexible, R.string.ssl_mode_flexible_blurb),
    FULL("full", R.string.ssl_mode_full, R.string.ssl_mode_full_blurb),
    STRICT("strict", R.string.ssl_mode_strict, R.string.ssl_mode_strict_blurb);

    companion object {
        fun fromRaw(raw: String?): SslMode? = entries.firstOrNull { it.raw == raw }
    }
}

/** 最低 TLS 版本（zone setting `min_tls_version` 取值）。对应 iOS MinTLSVersion。 */
enum class MinTlsVersion(val raw: String) {
    V1_0("1.0"), V1_1("1.1"), V1_2("1.2"), V1_3("1.3");

    val title: String get() = "TLS $raw"

    companion object {
        fun fromRaw(raw: String?): MinTlsVersion? = entries.firstOrNull { it.raw == raw }
    }
}

// MARK: - SSL/TLS 加密设置（走 zone-settings，对应 iOS ZoneSSLViewModel）

data class ZoneSslUiState(
    val zoneName: String = "",
    val sslMode: SslMode = SslMode.FULL,
    val alwaysUseHttps: Boolean = false,
    val autoHttpsRewrites: Boolean = false,
    val minTls: MinTlsVersion = MinTlsVersion.V1_0,
    val tls13: Boolean = false,
    val loaded: Boolean = false,
    val isLoading: Boolean = false,
    val missingScope: Boolean = false,
    val canWrite: Boolean = false,
    /** 正在写入的 setting key（行内进度 / 临时禁用）。 */
    val updating: Set<String> = emptySet(),
)

@HiltViewModel
class ZoneSslViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ZoneSettingsRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val zoneId: String = checkNotNull(savedStateHandle["zoneId"])
    private val hasRead = authRepository.hasScope(Scopes.ZONE_SETTINGS_READ)
    private val canWrite = authRepository.hasScope(Scopes.ZONE_SETTINGS_WRITE)

    private val _uiState = MutableStateFlow(
        ZoneSslUiState(
            zoneName = savedStateHandle.get<String>("zoneName").orEmpty(),
            isLoading = hasRead,
            missingScope = !hasRead,
            canWrite = canWrite,
        ),
    )
    val uiState: StateFlow<ZoneSslUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<String?>(Channel.BUFFERED)
    val errors: Flow<String?> = eventChannel.receiveAsFlow()

    init {
        if (hasRead) load()
    }

    fun load() {
        if (!hasRead) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // 各项独立读取，单项失败不影响其它（缺单个设置不致整页空白）。
                val ssl = async { runCatching { repository.getSetting(zoneId, "ssl") }.getOrNull() }
                val ah = async { runCatching { repository.getSetting(zoneId, "always_use_https") }.getOrNull() }
                val ar = async { runCatching { repository.getSetting(zoneId, "automatic_https_rewrites") }.getOrNull() }
                val tls = async { runCatching { repository.getSetting(zoneId, "min_tls_version") }.getOrNull() }
                val t13 = async { runCatching { repository.getSetting(zoneId, "tls_1_3") }.getOrNull() }
                val sslV = ssl.await(); val ahV = ah.await(); val arV = ar.await()
                val tlsV = tls.await(); val t13V = t13.await()
                _uiState.update { st ->
                    st.copy(
                        sslMode = SslMode.fromRaw(sslV) ?: st.sslMode,
                        alwaysUseHttps = ahV?.let { it == "on" } ?: st.alwaysUseHttps,
                        autoHttpsRewrites = arV?.let { it == "on" } ?: st.autoHttpsRewrites,
                        minTls = MinTlsVersion.fromRaw(tlsV) ?: st.minTls,
                        tls13 = t13V?.let { it == "on" || it == "zrt" } ?: st.tls13,
                        loaded = sslV != null || ahV != null || tlsV != null,
                    )
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setSslMode(mode: SslMode) = update("ssl", mode.raw) { applied ->
        _uiState.update { it.copy(sslMode = SslMode.fromRaw(applied) ?: it.sslMode) }
    }

    fun setAlwaysUseHttps(on: Boolean) = update("always_use_https", if (on) "on" else "off") { applied ->
        _uiState.update { it.copy(alwaysUseHttps = applied == "on") }
    }

    fun setAutoHttpsRewrites(on: Boolean) = update("automatic_https_rewrites", if (on) "on" else "off") { applied ->
        _uiState.update { it.copy(autoHttpsRewrites = applied == "on") }
    }

    fun setMinTls(v: MinTlsVersion) = update("min_tls_version", v.raw) { applied ->
        _uiState.update { it.copy(minTls = MinTlsVersion.fromRaw(applied) ?: it.minTls) }
    }

    fun setTls13(on: Boolean) = update("tls_1_3", if (on) "on" else "off") { applied ->
        _uiState.update { it.copy(tls13 = applied == "on" || applied == "zrt") }
    }

    /** 写单项设置：同一 key 写入中再点忽略，成功后用服务端返回值回填。 */
    private fun update(setting: String, value: String, apply: (String) -> Unit) {
        if (!canWrite || setting in _uiState.value.updating) return
        _uiState.update { it.copy(updating = it.updating + setting) }
        viewModelScope.launch {
            try {
                apply(repository.setSetting(zoneId, setting, value))
            } catch (e: Exception) {
                eventChannel.send(e.message)
            } finally {
                _uiState.update { it.copy(updating = it.updating - setting) }
            }
        }
    }
}

// MARK: - SSL 证书（ssl-and-certificates，对应 iOS ZoneSSLCertsViewModel）

data class ZoneSslCertsUiState(
    val zoneName: String = "",
    val packs: List<SslCertificatePack> = emptyList(),
    val universalEnabled: Boolean = true,
    val universalLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val loaded: Boolean = false,
    val missingScope: Boolean = false,
    val canWrite: Boolean = false,
    val isTogglingUniversal: Boolean = false,
)

sealed interface SslCertsEvent {
    data object Deleted : SslCertsEvent
    data class Error(val message: String?) : SslCertsEvent
}

@HiltViewModel
class ZoneSslCertsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SslRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val zoneId: String = checkNotNull(savedStateHandle["zoneId"])
    private val hasRead = authRepository.hasScope(Scopes.SSL_CERTS_READ)
    private val canWrite = authRepository.hasScope(Scopes.SSL_CERTS_WRITE)

    private val _uiState = MutableStateFlow(
        ZoneSslCertsUiState(
            zoneName = savedStateHandle.get<String>("zoneName").orEmpty(),
            isLoading = hasRead,
            missingScope = !hasRead,
            canWrite = canWrite,
        ),
    )
    val uiState: StateFlow<ZoneSslCertsUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<SslCertsEvent>(Channel.BUFFERED)
    val events: Flow<SslCertsEvent> = eventChannel.receiveAsFlow()

    init {
        if (hasRead) load()
    }

    fun load() {
        if (!hasRead) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val packs = repository.certificatePacks(zoneId)
                _uiState.update { it.copy(packs = packs, loaded = true) }
            } catch (e: Exception) {
                eventChannel.send(SslCertsEvent.Error(e.message))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
            // Universal 状态是附加信息，读不到不影响证书列表。
            runCatching { repository.universalEnabled(zoneId) }.getOrNull()?.let { enabled ->
                _uiState.update { it.copy(universalEnabled = enabled, universalLoaded = true) }
            }
        }
    }

    fun setUniversal(enabled: Boolean) {
        if (!canWrite || _uiState.value.isTogglingUniversal) return
        _uiState.update { it.copy(isTogglingUniversal = true) }
        viewModelScope.launch {
            try {
                val applied = repository.setUniversal(zoneId, enabled)
                _uiState.update { it.copy(universalEnabled = applied) }
            } catch (e: Exception) {
                eventChannel.send(SslCertsEvent.Error(e.message))
            } finally {
                _uiState.update { it.copy(isTogglingUniversal = false) }
            }
        }
    }

    fun deletePack(pack: SslCertificatePack) {
        if (!canWrite || pack.isUniversal) return
        viewModelScope.launch {
            try {
                repository.deletePack(zoneId, pack.id)
                _uiState.update { it.copy(packs = it.packs.filterNot { p -> p.id == pack.id }) }
                eventChannel.send(SslCertsEvent.Deleted)
            } catch (e: Exception) {
                eventChannel.send(SslCertsEvent.Error(e.message))
                load()
            }
        }
    }
}
