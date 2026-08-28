package jiamin.chen.orangecloud.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Cloudflare Alerting / Notifications（account 级 alerting/v3 端点，对应 iOS CFAlertingModels）。
// 读侧字段尽量可选、容错解码。

@Serializable
data class CFAvailableAlert(
    val type: String,
    @SerialName("display_name") val displayName: String? = null,
    val name: String? = null,
    val description: String? = null,
) {
    val label: String get() = displayName ?: name ?: type
}

@Serializable
data class CFWebhookDestination(
    val id: String,
    val name: String? = null,
    val url: String? = null,
    val type: String? = null,
)

@Serializable
data class CFWebhookCreate(val name: String, val url: String)

@Serializable
data class CFMechanismRef(val id: String)

@Serializable
data class CFAlertMechanisms(
    val webhooks: List<CFMechanismRef>? = null,
    val email: List<CFMechanismRef>? = null,
)

@Serializable
data class CFAlertPolicy(
    val id: String,
    val name: String? = null,
    @SerialName("alert_type") val alertType: String? = null,
    val enabled: Boolean? = null,
    val mechanisms: CFAlertMechanisms? = null,
)

@Serializable
data class CFAlertPolicyCreate(
    val name: String,
    @SerialName("alert_type") val alertType: String,
    val enabled: Boolean,
    val mechanisms: CFAlertMechanisms,
    val filters: Map<String, List<String>>,
)

@Serializable
data class CFAlertingId(val id: String)
