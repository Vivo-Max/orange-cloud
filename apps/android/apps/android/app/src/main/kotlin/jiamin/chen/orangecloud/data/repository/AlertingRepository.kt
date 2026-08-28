package jiamin.chen.orangecloud.data.repository

import jiamin.chen.orangecloud.core.network.CfApiClient
import jiamin.chen.orangecloud.data.model.CFAlertMechanisms
import jiamin.chen.orangecloud.data.model.CFAlertPolicy
import jiamin.chen.orangecloud.data.model.CFAlertPolicyCreate
import jiamin.chen.orangecloud.data.model.CFAlertingId
import jiamin.chen.orangecloud.data.model.CFAvailableAlert
import jiamin.chen.orangecloud.data.model.CFMechanismRef
import jiamin.chen.orangecloud.data.model.CFWebhookCreate
import jiamin.chen.orangecloud.data.model.CFWebhookDestination
import javax.inject.Inject
import javax.inject.Singleton

// Cloudflare 告警（account 级 alerting/v3 端点，对应 iOS AlertingService）。
// 把所选告警类型建/删指向推送端点 webhook 的策略。需 notifications.read/.write。
@Singleton
class AlertingRepository @Inject constructor(
    private val api: CfApiClient,
) {
    /** result 形如 {分类: [告警]}。 */
    suspend fun availableAlerts(accountId: String): Map<String, List<CFAvailableAlert>> {
        return api.get<Map<String, List<CFAvailableAlert>>>("accounts/$accountId/alerting/v3/available_alerts")
    }

    suspend fun webhooks(accountId: String): List<CFWebhookDestination> {
        return api.get<List<CFWebhookDestination>>("accounts/$accountId/alerting/v3/destinations/webhooks")
    }

    suspend fun createWebhook(accountId: String, name: String, url: String): String {
        val result = api.post<CFAlertingId, CFWebhookCreate>(
            "accounts/$accountId/alerting/v3/destinations/webhooks",
            CFWebhookCreate(name, url),
        )
        return result.id
    }

    suspend fun deleteWebhook(accountId: String, id: String) {
        api.delete("accounts/$accountId/alerting/v3/destinations/webhooks/$id")
    }

    suspend fun policies(accountId: String): List<CFAlertPolicy> {
        return api.get<List<CFAlertPolicy>>("accounts/$accountId/alerting/v3/policies")
    }

    suspend fun createPolicy(accountId: String, name: String, alertType: String, webhookId: String): String {
        val result = api.post<CFAlertingId, CFAlertPolicyCreate>(
            "accounts/$accountId/alerting/v3/policies",
            CFAlertPolicyCreate(
                name = name,
                alertType = alertType,
                enabled = true,
                mechanisms = CFAlertMechanisms(webhooks = listOf(CFMechanismRef(webhookId)), email = null),
                filters = emptyMap(),
            ),
        )
        return result.id
    }

    suspend fun deletePolicy(accountId: String, id: String) {
        api.delete("accounts/$accountId/alerting/v3/policies/$id")
    }
}
