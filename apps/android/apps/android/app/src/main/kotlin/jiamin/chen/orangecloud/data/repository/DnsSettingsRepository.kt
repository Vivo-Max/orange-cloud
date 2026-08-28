package jiamin.chen.orangecloud.data.repository

import jiamin.chen.orangecloud.core.network.CfApiClient
import jiamin.chen.orangecloud.data.model.ZoneDnsSettings
import jiamin.chen.orangecloud.data.model.ZoneDnsSettingsUpdate
import jiamin.chen.orangecloud.data.model.ZoneDnssec
import jiamin.chen.orangecloud.data.model.ZoneDnssecUpdate
import javax.inject.Inject
import javax.inject.Singleton

/** Zone DNS 设置（zone-dns-settings.*）与 DNSSEC（dns.*）。对应 iOS DNSSettingsService。 */
@Singleton
class DnsSettingsRepository @Inject constructor(
    private val api: CfApiClient,
) {
    suspend fun settings(zoneId: String): ZoneDnsSettings =
        api.get("zones/$zoneId/dns_settings")

    /** 合并语义：只发非 null 字段 */
    suspend fun updateSettings(zoneId: String, update: ZoneDnsSettingsUpdate): ZoneDnsSettings =
        api.patch("zones/$zoneId/dns_settings", update)

    suspend fun dnssec(zoneId: String): ZoneDnssec =
        api.get("zones/$zoneId/dnssec")

    /**
     * 启用传 active，停用传 disabled。
     * 启用后状态先是 pending——必须由用户去注册商处添加 DS 记录才会转 active。
     */
    suspend fun setDnssec(zoneId: String, enabled: Boolean): ZoneDnssec =
        api.patch("zones/$zoneId/dnssec", ZoneDnssecUpdate(if (enabled) "active" else "disabled"))
}
