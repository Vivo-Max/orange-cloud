package jiamin.chen.orangecloud.data.repository

import jiamin.chen.orangecloud.core.network.CfApiClient
import jiamin.chen.orangecloud.data.model.SslCertificatePack
import jiamin.chen.orangecloud.data.model.UniversalSslSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 边缘证书查询 + Universal SSL 开关 + 删除证书包（对应 iOS SSLCertificateService）。
 * 读 ssl-and-certificates.read，写 ssl-and-certificates.write。
 * SSL/TLS 加密设置（ssl 模式 / min_tls 等）走 [ZoneSettingsRepository]，scope 是 zone-settings。
 */
@Singleton
class SslRepository @Inject constructor(
    private val api: CfApiClient,
) {
    /** 列出该 Zone 的证书包（含未激活的，status=all）。 */
    suspend fun certificatePacks(zoneId: String): List<SslCertificatePack> =
        api.get("zones/$zoneId/ssl/certificate_packs", listOf("status" to "all"))

    /** 读 Universal SSL 是否启用。 */
    suspend fun universalEnabled(zoneId: String): Boolean =
        api.get<UniversalSslSettings>("zones/$zoneId/ssl/universal/settings").enabled ?: false

    /** 开关 Universal SSL，返回生效后的状态。 */
    suspend fun setUniversal(zoneId: String, enabled: Boolean): Boolean =
        api.patch<UniversalSslSettings, UniversalSslSettings>(
            "zones/$zoneId/ssl/universal/settings",
            UniversalSslSettings(enabled),
        ).enabled ?: enabled

    /** 删除证书包（仅高级 / 自定义包；Universal 不可删）。 */
    suspend fun deletePack(zoneId: String, packId: String) =
        api.delete("zones/$zoneId/ssl/certificate_packs/$packId")
}
