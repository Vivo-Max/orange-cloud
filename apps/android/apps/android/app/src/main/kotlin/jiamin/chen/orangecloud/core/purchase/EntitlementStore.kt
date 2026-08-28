package jiamin.chen.orangecloud.core.purchase

import jiamin.chen.orangecloud.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pro 授权状态（对应 iOS EntitlementStore）。isPro = 活跃订阅 ∨ 持有买断。
 * - `oss` 风味：`IS_OSS` 编译为 true，isPro 恒真（开源自编译全解锁，无 Billing 依赖）。
 * - `play` 风味：初始 false，由 Play Billing 查询订阅/买断后回填（Billing 接入见下方 TODO，
 *   wiring 放 play 风味源集 `src/play/`，避免 oss classpath 引入 Billing 库）。
 *
 * 六处闸门统一读 [isPro]；非 Pro 时展示 Paywall。
 */
@Singleton
class EntitlementStore @Inject constructor() {
    private val _isPro = MutableStateFlow(BuildConfig.IS_OSS)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    /** 由 Billing 层（play 风味）回填订阅/买断结果。 */
    fun setPro(value: Boolean) {
        _isPro.value = value || BuildConfig.IS_OSS
    }

    // TODO(play): BillingManager(play 源集) queryPurchasesAsync(SUBS+INAPP) → setPro(...)；
    // 商品 ID：jiamin.chen.orange_cloud.pro.monthly/.yearly/.lifetime。
}
