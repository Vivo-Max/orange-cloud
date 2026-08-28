package jiamin.chen.orangecloud.ui.network

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.data.model.Tunnel
import jiamin.chen.orangecloud.data.repository.AccountStore
import jiamin.chen.orangecloud.data.repository.SecurityRepository
import jiamin.chen.orangecloud.ui.storage.StorageListViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TunnelListEvent {
    data class Created(val tunnel: Tunnel) : TunnelListEvent
    data object Deleted : TunnelListEvent
    data class Error(val message: String?) : TunnelListEvent
}

@HiltViewModel
class TunnelListViewModel @Inject constructor(
    private val accountStore: AccountStore,
    private val securityRepository: SecurityRepository,
    authRepository: AuthRepository,
) : StorageListViewModel<Tunnel>(accountStore, authRepository.hasScope(Scopes.TUNNEL_READ)) {

    /** argotunnel.write 才可新建 / 删除隧道。 */
    val canWrite: Boolean = authRepository.hasScope(Scopes.TUNNEL_WRITE)

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val eventChannel = Channel<TunnelListEvent>(Channel.BUFFERED)
    val events: Flow<TunnelListEvent> = eventChannel.receiveAsFlow()

    override suspend fun fetch(accountId: String) = securityRepository.listTunnels(accountId)
    init { load() }

    /** 新建远程托管隧道，成功后插到列表顶端并发出 Created（供随后展示连接令牌）。 */
    fun createTunnel(name: String) {
        if (!canWrite || _isSaving.value) return
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                val tunnel = securityRepository.createTunnel(accountId, name)
                state.update { it.copy(items = listOf(tunnel) + it.items) }
                eventChannel.send(TunnelListEvent.Created(tunnel))
            } catch (e: Exception) {
                eventChannel.send(TunnelListEvent.Error(e.message))
            } finally {
                _isSaving.value = false
            }
        }
    }

    /** 删除隧道（先清理连接）。 */
    fun deleteTunnel(tunnel: Tunnel) {
        if (!canWrite) return
        viewModelScope.launch {
            try {
                val accountId = accountStore.selectedAccountId.value ?: error("no account")
                securityRepository.deleteTunnel(accountId, tunnel.id)
                state.update { it.copy(items = it.items.filterNot { t -> t.id == tunnel.id }) }
                eventChannel.send(TunnelListEvent.Deleted)
            } catch (e: Exception) {
                eventChannel.send(TunnelListEvent.Error(e.message))
            }
        }
    }
}
