package jiamin.chen.orangecloud.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// MARK: - 边缘证书（GET /zones/{id}/ssl/certificate_packs?status=all，ssl-and-certificates.read）

/** 证书包（对应 iOS SSLCertificatePack）。Universal 包由 Cloudflare 托管，不可删。 */
@Serializable
data class SslCertificatePack(
    val id: String,
    val type: String? = null,
    val hosts: List<String>? = null,
    val status: String? = null,
    @SerialName("certificate_authority") val certificateAuthority: String? = null,
    val certificates: List<SslCertEntry>? = null,
) {
    val isUniversal: Boolean get() = type == "universal"

    /** 最近一张证书的到期日（ISO 字符串截到日）。 */
    val expiresOnDay: String? get() = certificates?.mapNotNull { it.expiresOn }?.minOrNull()?.take(10)

    val issuer: String? get() = certificates?.firstNotNullOfOrNull { it.issuer }
}

@Serializable
data class SslCertEntry(
    val id: String? = null,
    val issuer: String? = null,
    val status: String? = null,
    @SerialName("expires_on") val expiresOn: String? = null,
)

/** GET/PATCH /zones/{id}/ssl/universal/settings */
@Serializable
data class UniversalSslSettings(val enabled: Boolean? = null)
