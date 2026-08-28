package jiamin.chen.orangecloud.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/* ============================================================
   免登录开发者工具箱的数据模型（对应 iOS Models/ToolboxModels.kt）。
   分两类：① 外部 API 的线缆 DTO（@Serializable）；② UI 用的展示结果类型。
   工具全部无需 CF 账号，走 ToolboxRepository 的 auth-free OkHttp。
   ============================================================ */

// ---------- DNS over HTTPS（cloudflare-dns.com，Accept: application/dns-json）----------

@Serializable
data class DohResponse(
    @SerialName("Status") val status: Int = 0,
    @SerialName("TC") val truncated: Boolean = false,
    @SerialName("Answer") val answer: List<DohAnswer> = emptyList(),
    @SerialName("Authority") val authority: List<DohAnswer> = emptyList(),
)

@Serializable
data class DohAnswer(
    val name: String = "",
    val type: Int = 0,
    @SerialName("TTL") val ttl: Int = 0,
    val data: String = "",
)

// ---------- GeoIP（ipwho.is，无 key）----------

@Serializable
data class GeoIpResponse(
    val success: Boolean = false,
    val message: String? = null,
    val ip: String = "",
    val type: String? = null,
    val country: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    val region: String? = null,
    val city: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val flag: GeoIpFlag? = null,
    val connection: GeoIpConnection? = null,
    val timezone: GeoIpTimezone? = null,
)

@Serializable data class GeoIpFlag(val emoji: String? = null)
@Serializable data class GeoIpConnection(val asn: Long? = null, val org: String? = null, val isp: String? = null, val domain: String? = null)
@Serializable data class GeoIpTimezone(val id: String? = null, val utc: String? = null)

// ---------- RDAP（WHOIS 的 HTTP/JSON 形态）----------

/** IANA bootstrap：services = [ [[tld…], [rdapBase…]], … ] */
@Serializable
data class RdapBootstrap(val services: List<List<List<String>>> = emptyList())

@Serializable
data class RdapDomain(
    val handle: String? = null,
    val ldhName: String? = null,
    val unicodeName: String? = null,
    val status: List<String> = emptyList(),
    val events: List<RdapEvent> = emptyList(),
    val entities: List<RdapEntity> = emptyList(),
    val nameservers: List<RdapNameserver> = emptyList(),
)

@Serializable data class RdapEvent(val eventAction: String? = null, val eventDate: String? = null)
@Serializable data class RdapNameserver(val ldhName: String? = null)

@Serializable
data class RdapEntity(
    val handle: String? = null,
    val roles: List<String> = emptyList(),
    // vCard 是异构 JSON 数组（["vcard", [[...]]]），用 JsonElement 容纳、按需走 vcard fn 提取。
    val vcardArray: JsonElement? = null,
)

// ============================================================
//  UI 展示结果（Repository 把上面 DTO/握手/抓包归一成这些）
// ============================================================

/** 通用「标签 : 值」展示行。 */
data class ToolField(val label: String, val value: String, val mono: Boolean = false)

/** DNS 查询结果：每条记录一行。 */
data class DnsLookupResult(val records: List<DnsRecordLine>, val authorityNote: String?)
data class DnsRecordLine(val type: String, val name: String, val ttl: Int, val data: String)

/** HTTP 探测结果。 */
data class HttpProbeResult(
    val statusLine: String,
    val statusCode: Int,
    val elapsedMs: Long,
    val finalUrl: String,
    val headers: List<ToolField>,
    val bodyPreview: String,
    val redirected: Boolean,
)

/** SSL 证书检查结果（叶子证书 + 链长）。 */
data class CertInspectResult(
    val subject: String,
    val issuer: String,
    val notBefore: String,
    val notAfter: String,
    val daysRemaining: Long,
    val expired: Boolean,
    val sigAlg: String,
    val keyDescription: String,
    val sans: List<String>,
    val chainLength: Int,
    val trusted: Boolean,
)

/** CIDR 计算结果（纯本地）。 */
data class CidrResult(
    val network: String,
    val broadcast: String?,
    val firstHost: String,
    val lastHost: String,
    val prefix: Int,
    val totalAddresses: String,
    val usableHosts: String,
    val isV6: Boolean,
)
