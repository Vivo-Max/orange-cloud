package jiamin.chen.orangecloud.data.repository

import jiamin.chen.orangecloud.core.network.CfApiClient
import jiamin.chen.orangecloud.data.model.HealthCheck
import jiamin.chen.orangecloud.data.model.HealthCheckSuspendUpdate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 独立健康检查（healthcheck.read / .write），对应 iOS HealthCheckService。
 * 免费套餐不可用（0 个）；Pro 10 / Business 50 / Enterprise 1000。
 */
@Singleton
class HealthCheckRepository @Inject constructor(
    private val api: CfApiClient,
) {
    suspend fun list(zoneId: String): List<HealthCheck> {
        val all = mutableListOf<HealthCheck>()
        var page = 1
        while (true) {
            val paged = api.getList<HealthCheck>(
                "zones/$zoneId/healthchecks",
                listOf("page" to page.toString(), "per_page" to "50"),
            )
            all += paged.items
            if (page >= (paged.info?.totalPages ?: 1)) break
            page++
        }
        return all
    }

    /** 暂停 / 恢复。暂停后 CF 不再向源站发送检查。 */
    suspend fun setSuspended(zoneId: String, checkId: String, suspended: Boolean): HealthCheck =
        api.patch("zones/$zoneId/healthchecks/$checkId", HealthCheckSuspendUpdate(suspended))

    suspend fun delete(zoneId: String, checkId: String) =
        api.delete("zones/$zoneId/healthchecks/$checkId")
}
