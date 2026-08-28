package jiamin.chen.orangecloud.ui.zones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.data.model.Zone
import jiamin.chen.orangecloud.data.repository.ZoneRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 域名详情：从 Room 缓存观察单个域名（状态 / 套餐 / Name Servers）。
 * 列表页与 Dashboard 两处都已写入缓存，详情直接读，不再额外发网络。
 * zoneId 由屏幕 bind 传入（双栏 pane 与单栏 nav 两种入口共用一个 VM）。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ZoneDetailViewModel @Inject constructor(
    private val zoneRepository: ZoneRepository,
) : ViewModel() {

    private val zoneId = MutableStateFlow<String?>(null)

    val zone: StateFlow<Zone?> = zoneId
        .flatMapLatest { id -> if (id == null) flowOf(null) else zoneRepository.observeZone(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun bind(id: String) {
        if (zoneId.value != id) zoneId.value = id
    }
}
