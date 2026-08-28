package jiamin.chen.orangecloud.data.repository

import jiamin.chen.orangecloud.core.network.CfApiClient
import jiamin.chen.orangecloud.data.model.LoadBalancer
import jiamin.chen.orangecloud.data.model.LoadBalancerToggle
import jiamin.chen.orangecloud.data.model.LoadBalancerUpdate
import jiamin.chen.orangecloud.data.model.Monitor
import jiamin.chen.orangecloud.data.model.MonitorUpdate
import jiamin.chen.orangecloud.data.model.Pool
import jiamin.chen.orangecloud.data.model.PoolHealthResponse
import jiamin.chen.orangecloud.data.model.PoolUpdate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 负载均衡：Load Balancer（zone 级）+ Pool / Monitor（account 级）+ 池健康。
 * 对应 iOS LoadBalancerService。
 */
@Singleton
class LoadBalancerRepository @Inject constructor(
    private val api: CfApiClient,
) {
    // MARK: - Load Balancer（zone）

    suspend fun listLoadBalancers(zoneId: String): List<LoadBalancer> =
        api.get("zones/$zoneId/load_balancers")

    suspend fun setLoadBalancerEnabled(zoneId: String, lbId: String, enabled: Boolean): LoadBalancer =
        api.patch("zones/$zoneId/load_balancers/$lbId", LoadBalancerToggle(enabled))

    suspend fun createLoadBalancer(zoneId: String, body: LoadBalancerUpdate): LoadBalancer =
        api.post("zones/$zoneId/load_balancers", body)

    suspend fun updateLoadBalancer(zoneId: String, lbId: String, body: LoadBalancerUpdate): LoadBalancer =
        api.patch("zones/$zoneId/load_balancers/$lbId", body)

    suspend fun deleteLoadBalancer(zoneId: String, lbId: String) =
        api.delete("zones/$zoneId/load_balancers/$lbId")

    // MARK: - 源站池 Pool（account）

    suspend fun listPools(accountId: String): List<Pool> =
        api.get("accounts/$accountId/load_balancers/pools")

    suspend fun poolHealth(accountId: String, poolId: String): PoolHealthResponse =
        api.get("accounts/$accountId/load_balancers/pools/$poolId/health")

    suspend fun createPool(accountId: String, body: PoolUpdate): Pool =
        api.post("accounts/$accountId/load_balancers/pools", body)

    suspend fun updatePool(accountId: String, poolId: String, body: PoolUpdate): Pool =
        api.patch("accounts/$accountId/load_balancers/pools/$poolId", body)

    suspend fun deletePool(accountId: String, poolId: String) =
        api.delete("accounts/$accountId/load_balancers/pools/$poolId")

    // MARK: - 健康监测 Monitor（account）

    suspend fun listMonitors(accountId: String): List<Monitor> =
        api.get("accounts/$accountId/load_balancers/monitors")

    suspend fun createMonitor(accountId: String, body: MonitorUpdate): Monitor =
        api.post("accounts/$accountId/load_balancers/monitors", body)

    suspend fun updateMonitor(accountId: String, monitorId: String, body: MonitorUpdate): Monitor =
        api.patch("accounts/$accountId/load_balancers/monitors/$monitorId", body)

    suspend fun deleteMonitor(accountId: String, monitorId: String) =
        api.delete("accounts/$accountId/load_balancers/monitors/$monitorId")
}
