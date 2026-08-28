package jiamin.chen.orangecloud.ui.zonesettings

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
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

/** 缓存级别（zone setting `cache_level` 取值）。对应 iOS CacheLevel。 */
enum class CacheLevel(val raw: String, @StringRes val titleRes: Int) {
    BASIC("basic", R.string.perf_cache_basic),
    SIMPLIFIED("simplified", R.string.perf_cache_simplified),
    AGGRESSIVE("aggressive", R.string.perf_cache_aggressive);

    companion object {
        fun fromRaw(raw: String?): CacheLevel = entries.firstOrNull { it.raw == raw } ?: AGGRESSIVE
    }
}

/** 一个网络优化开关：setting id + 显示名。按 Cloudflare 仪表盘顺序。 */
data class PerfToggle(val id: String, @StringRes val labelRes: Int)

data class ZonePerformanceUiState(
    val zoneName: String = "",
    /** setting id → 当前值（"on"/"off" 或 cache_level 取值）。读不到的项不在 map 中。 */
    val values: Map<String, String> = emptyMap(),
    val loaded: Boolean = false,
    val isLoading: Boolean = false,
    val missingScope: Boolean = false,
    val canWrite: Boolean = false,
    val updating: Set<String> = emptySet(),
) {
    fun isOn(id: String): Boolean = values[id] == "on"
    val cacheLevel: CacheLevel get() = CacheLevel.fromRaw(values["cache_level"])
}

/**
 * 「性能与缓存」面板：网络优化开关 + 缓存控制。全部走 zone-settings（对应 iOS ZonePerformanceViewModel）。
 */
@HiltViewModel
class ZonePerformanceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ZoneSettingsRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val zoneId: String = checkNotNull(savedStateHandle["zoneId"])
    private val hasRead = authRepository.hasScope(Scopes.ZONE_SETTINGS_READ)
    private val canWrite = authRepository.hasScope(Scopes.ZONE_SETTINGS_WRITE)

    private val _uiState = MutableStateFlow(
        ZonePerformanceUiState(
            zoneName = savedStateHandle.get<String>("zoneName").orEmpty(),
            isLoading = hasRead,
            missingScope = !hasRead,
            canWrite = canWrite,
        ),
    )
    val uiState: StateFlow<ZonePerformanceUiState> = _uiState.asStateFlow()

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
                // 每项独立读取，单项失败（如某些套餐无该设置）不影响其它。
                val ids = NETWORK_TOGGLES.map { it.id } + listOf("cache_level", "always_online", "sort_query_string_for_cache")
                val results = ids.map { id ->
                    id to async { runCatching { repository.getSetting(zoneId, id) }.getOrNull() }
                }
                val acc = buildMap {
                    results.forEach { (id, deferred) -> deferred.await()?.let { put(id, it) } }
                }
                if (acc.isNotEmpty()) {
                    _uiState.update { it.copy(values = acc, loaded = true) }
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setToggle(id: String, on: Boolean) = update(id, if (on) "on" else "off")

    fun setCacheLevel(level: CacheLevel) = update("cache_level", level.raw)

    private fun update(id: String, value: String) {
        if (!canWrite || id in _uiState.value.updating) return
        _uiState.update { it.copy(updating = it.updating + id) }
        viewModelScope.launch {
            try {
                val applied = repository.setSetting(zoneId, id, value)
                _uiState.update { it.copy(values = it.values + (id to applied)) }
            } catch (e: Exception) {
                eventChannel.send(e.message)
            } finally {
                _uiState.update { it.copy(updating = it.updating - id) }
            }
        }
    }

    companion object {
        val NETWORK_TOGGLES = listOf(
            PerfToggle("brotli", R.string.perf_brotli),
            PerfToggle("http2", R.string.perf_http2),
            PerfToggle("http3", R.string.perf_http3),
            PerfToggle("0rtt", R.string.perf_0rtt),
            PerfToggle("early_hints", R.string.perf_early_hints),
            PerfToggle("websockets", R.string.perf_websockets),
            PerfToggle("ipv6", R.string.perf_ipv6),
        )
    }
}
