package jiamin.chen.orangecloud.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Zone 级 DNS 设置（/zones/{id}/dns_settings）与 DNSSEC（/zones/{id}/dnssec）。
 *
 * 注意历史弃用，别用错端点：
 * · zone settings 里的 cname_flattening（2025-06-08 弃用）已迁到这里的 flatten_all_cnames
 * · 账户设置的 default_nameservers 等（2025-03-14 弃用）同样迁来
 * · foundation_dns（2026-07-27 弃用，**sunset 2026-11-23**）已由 nameservers.type =
 *   "cloudflare.advanced" 取代 —— 本文件只用后者，不建模前者
 */
@Serializable
data class ZoneDnsSettings(
    /** 展平 zone 内全部 CNAME（顶点 CNAME 因 DNS 限制始终展平） */
    @SerialName("flatten_all_cnames") val flattenAllCnames: Boolean? = null,
    /** 多提供商 DNS：存在非 Cloudflare NS 记录时仍激活该 zone */
    @SerialName("multi_provider") val multiProvider: Boolean? = null,
    @SerialName("secondary_overrides") val secondaryOverrides: Boolean? = null,
    /** NS 记录 TTL，30–86400 秒 */
    @SerialName("ns_ttl") val nsTtl: Double? = null,
    /** standard / cdn_only / dns_only */
    @SerialName("zone_mode") val zoneMode: String? = null,
    val nameservers: ZoneNameservers? = null,
)

@Serializable
data class ZoneNameservers(
    /** cloudflare.standard / cloudflare.advanced / custom.* */
    val type: String? = null,
    @SerialName("ns_set") val nsSet: Int? = null,
)

/** PATCH 体：合并语义，null 项在 explicitNulls=false 下不会被序列化 */
@Serializable
data class ZoneDnsSettingsUpdate(
    @SerialName("flatten_all_cnames") val flattenAllCnames: Boolean? = null,
    @SerialName("multi_provider") val multiProvider: Boolean? = null,
    @SerialName("secondary_overrides") val secondaryOverrides: Boolean? = null,
    @SerialName("ns_ttl") val nsTtl: Double? = null,
    @SerialName("zone_mode") val zoneMode: String? = null,
    val nameservers: ZoneNameserversUpdate? = null,
)

@Serializable
data class ZoneNameserversUpdate(val type: String)

@Serializable
data class ZoneDnssec(
    /** active / pending / disabled / pending-disabled / error */
    val status: String? = null,
    /** 完整 DS 记录——用户要把它粘到域名注册商处，DNSSEC 才真正生效 */
    val ds: String? = null,
    val digest: String? = null,
    @SerialName("digest_type") val digestType: String? = null,
    val algorithm: String? = null,
    @SerialName("key_tag") val keyTag: Double? = null,
) {
    val isEnabled: Boolean get() = status == "active" || status == "pending"
}

@Serializable
data class ZoneDnssecUpdate(val status: String)
