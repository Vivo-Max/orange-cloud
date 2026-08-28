package jiamin.chen.orangecloud.core.purchase

import android.app.Activity

/**
 * 计费网关抽象。play 风味用 Play Billing 真实实现，oss 风味为空实现（isPro 由 EntitlementStore 恒真）。
 * 真实实现 / DI 绑定放各自风味源集（src/play、src/oss），避免 oss classpath 引入 Billing 库。
 */
interface BillingGateway {
    /** App 启动时连接计费服务并查询既有购买，回填 EntitlementStore.isPro。 */
    fun connect()

    /** 拉起购买流程。play：Play Billing；direct：打开 Web 售卖页。planId ∈ {monthly, yearly, lifetime}。 */
    fun launchPurchase(activity: Activity, planId: String)

    /** 激活码兑换（仅 direct 风味实现；play / oss 返回 NOT_SUPPORTED）。 */
    suspend fun redeem(code: String): RedeemOutcome

    /** 反激活：释放本设备占用的名额（仅 direct 实现；play / oss 返回 false）。 */
    suspend fun deactivate(): Boolean

    companion object {
        const val PLAN_MONTHLY = "monthly"
        const val PLAN_YEARLY = "yearly"
        const val PLAN_LIFETIME = "lifetime"
    }
}

/** 激活码兑换结果（direct 风味）。 */
enum class RedeemOutcome { SUCCESS, INVALID, REVOKED, DEVICE_LIMIT, NETWORK_ERROR, NOT_SUPPORTED }
