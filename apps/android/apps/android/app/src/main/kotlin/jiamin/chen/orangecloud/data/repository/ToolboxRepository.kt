package jiamin.chen.orangecloud.data.repository

import jiamin.chen.orangecloud.core.network.ApiError
import jiamin.chen.orangecloud.data.model.CertInspectResult
import jiamin.chen.orangecloud.data.model.CidrResult
import jiamin.chen.orangecloud.data.model.DnsLookupResult
import jiamin.chen.orangecloud.data.model.DnsRecordLine
import jiamin.chen.orangecloud.data.model.DohResponse
import jiamin.chen.orangecloud.data.model.GeoIpResponse
import jiamin.chen.orangecloud.data.model.HttpProbeResult
import jiamin.chen.orangecloud.data.model.RdapBootstrap
import jiamin.chen.orangecloud.data.model.RdapDomain
import jiamin.chen.orangecloud.data.model.RdapEntity
import jiamin.chen.orangecloud.data.model.ToolField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.math.BigInteger
import java.net.IDN
import java.net.InetAddress
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/** WHOIS：该 TLD 无 RDAP 服务器时抛出，UI 给降级提示。 */
class RdapUnsupportedException(val tld: String) : Exception("No RDAP server for .$tld")

/**
 * 免登录工具箱的网络层（对应 iOS Services/ToolboxServices.kt）。
 * 全部走 auth-free 的 OkHttpClient（不注入 CF token），纯公共 HTTPS 端点。
 */
@Singleton
class ToolboxRepository @Inject constructor(
    private val httpClient: OkHttpClient,
    private val json: Json,
) {
    private val dateFmt: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())
    private val dateTimeFmt: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

    @Volatile private var rdapBootstrap: RdapBootstrap? = null

    // ---------- DNS 查询（DoH JSON）----------

    suspend fun dnsLookup(name: String, type: String): DnsLookupResult {
        val q = name.trim().removePrefix("https://").removePrefix("http://").substringBefore('/')
        if (q.isEmpty()) throw ApiError.Network(IOException("empty name"))
        val url = "https://cloudflare-dns.com/dns-query".toHttpUrl().newBuilder()
            .addQueryParameter("name", q)
            .addQueryParameter("type", type)
            .build()
        val req = Request.Builder().url(url).header("Accept", "application/dns-json").get().build()
        val resp = decode(DohResponse.serializer(), execute(req))
        val records = resp.answer.map { DnsRecordLine(dnsTypeName(it.type), it.name, it.ttl, it.data) }
        val note = if (records.isEmpty()) resp.authority.firstOrNull()?.data else null
        return DnsLookupResult(records, note)
    }

    // ---------- GeoIP（ipwho.is，无 key）----------

    /** ip 为空 = 查本机出口 IP。 */
    suspend fun geoIp(ip: String): GeoIpResponse {
        val target = ip.trim()
        val url = if (target.isEmpty()) "https://ipwho.is/" else "https://ipwho.is/$target"
        return decode(GeoIpResponse.serializer(), execute(req(url)))
    }

    // ---------- Cloudflare colo trace（cdn-cgi/trace）----------

    suspend fun cfTrace(host: String): List<ToolField> {
        val h = normalizeHost(host).ifEmpty { "1.1.1.1" }
        val text = execute(req("https://$h/cdn-cgi/trace")).decodeToString()
        return text.lineSequence().mapNotNull { line ->
            val idx = line.indexOf('=')
            if (idx <= 0) null else ToolField(line.substring(0, idx), line.substring(idx + 1), mono = true)
        }.toList()
    }

    // ---------- HTTP 探测（仅 https）----------

    suspend fun httpProbe(rawUrl: String, method: String): HttpProbeResult {
        var u = rawUrl.trim()
        if (!u.startsWith("http://", true) && !u.startsWith("https://", true)) u = "https://$u"
        val httpUrl = u.toHttpUrlOrNull() ?: throw ApiError.Network(IOException("bad url"))
        if (httpUrl.scheme != "https") throw ApiError.Network(IOException("https only"))
        val request = Request.Builder().url(httpUrl).method(method, null).build()
        val start = System.currentTimeMillis()
        return withContext(Dispatchers.IO) {
            try {
                httpClient.newCall(request).execute().use { resp ->
                    val elapsed = System.currentTimeMillis() - start
                    val bodyText = if (method == "HEAD") "" else
                        runCatching { resp.peekBody(64 * 1024).string() }.getOrDefault("")
                    HttpProbeResult(
                        statusLine = "${resp.protocol.toString().uppercase()} ${resp.code} ${resp.message}".trim(),
                        statusCode = resp.code,
                        elapsedMs = elapsed,
                        finalUrl = resp.request.url.toString(),
                        headers = resp.headers.map { ToolField(it.first, it.second, mono = true) },
                        bodyPreview = bodyText.take(4000),
                        redirected = resp.priorResponse != null || resp.request.url != httpUrl,
                    )
                }
            } catch (e: IOException) {
                throw ApiError.Network(e)
            }
        }
    }

    // ---------- WHOIS（RDAP bootstrap → /domain/{name}）----------

    suspend fun whois(domain: String): RdapDomain {
        val name = normalizeHost(domain).lowercase()
        val tld = name.substringAfterLast('.', "")
        if (tld.isEmpty() || !name.contains('.')) throw ApiError.Network(IOException("bad domain"))
        val base = rdapBaseFor(tld) ?: throw RdapUnsupportedException(tld)
        val asciiName = runCatching { IDN.toASCII(name) }.getOrDefault(name)
        val url = base.trimEnd('/') + "/domain/" + asciiName
        val req = Request.Builder().url(url).header("Accept", "application/rdap+json").get().build()
        return decode(RdapDomain.serializer(), execute(req))
    }

    private suspend fun rdapBaseFor(tld: String): String? {
        val boot = rdapBootstrap ?: decode(
            RdapBootstrap.serializer(),
            execute(req("https://data.iana.org/rdap/dns.json")),
        ).also { rdapBootstrap = it }
        for (svc in boot.services) {
            val tlds = svc.getOrNull(0) ?: continue
            val bases = svc.getOrNull(1) ?: continue
            if (tlds.any { it.equals(tld, ignoreCase = true) }) {
                return bases.firstOrNull { it.startsWith("https") } ?: bases.firstOrNull()
            }
        }
        return null
    }

    /** 从 RDAP entity 的 vCard 数组取展示名（fn 属性）。 */
    fun entityName(entity: RdapEntity): String? {
        val arr = entity.vcardArray as? JsonArray ?: return null
        val props = arr.getOrNull(1) as? JsonArray ?: return null
        for (p in props) {
            val pa = p as? JsonArray ?: continue
            if ((pa.getOrNull(0) as? JsonPrimitive)?.contentOrNull == "fn") {
                return (pa.getOrNull(3) as? JsonPrimitive)?.contentOrNull
            }
        }
        return null
    }

    fun formatRdapDate(iso: String?): String? = iso?.let {
        runCatching { dateTimeFmt.format(Instant.parse(it)) }.getOrDefault(it)
    }

    // ---------- SSL 证书检查 ----------

    suspend fun sslInspect(host: String): CertInspectResult {
        val (h, port) = hostPort(host, 443)
        if (h.isEmpty()) throw ApiError.Network(IOException("bad host"))
        // 宽松 client：不校验信任 / 主机名，过期或不受信证书也要能读取（对齐 iOS「先抓后判」）。
        val permissive = buildPermissiveClient()
        val request = Request.Builder().url("https://$h:$port/").head().build()
        val chain = withContext(Dispatchers.IO) {
            try {
                permissive.newCall(request).execute().use { resp ->
                    resp.handshake?.peerCertificates.orEmpty().filterIsInstance<X509Certificate>()
                }
            } catch (e: IOException) {
                throw ApiError.Network(e)
            }
        }
        val leaf = chain.firstOrNull() ?: throw ApiError.Network(IOException("no certificate"))
        val now = Instant.now()
        val notAfter = leaf.notAfter.toInstant()
        val expired = now.isAfter(notAfter)
        val days = ChronoUnit.DAYS.between(now, notAfter)
        val sans = runCatching {
            leaf.subjectAlternativeNames?.mapNotNull { it.getOrNull(1)?.toString() }.orEmpty()
        }.getOrDefault(emptyList())
        val keyDesc = runCatching {
            val alg = leaf.publicKey.algorithm
            val bits = (leaf.publicKey as? java.security.interfaces.RSAPublicKey)?.modulus?.bitLength()
                ?: (leaf.publicKey as? java.security.interfaces.ECPublicKey)?.params?.curve?.field?.fieldSize
            if (bits != null) "$alg $bits-bit" else alg
        }.getOrDefault(leaf.publicKey.algorithm)
        return CertInspectResult(
            subject = leaf.subjectX500Principal.name.let(::cnOf),
            issuer = leaf.issuerX500Principal.name.let(::cnOf),
            notBefore = dateFmt.format(leaf.notBefore.toInstant()),
            notAfter = dateFmt.format(notAfter),
            daysRemaining = days,
            expired = expired,
            sigAlg = leaf.sigAlgName,
            keyDescription = keyDesc,
            sans = sans,
            chainLength = chain.size,
            trusted = validateTrust(chain, leaf),
        )
    }

    /** 用系统默认信任库校验链到根（不校验主机名，作为「受信」指示）。 */
    private fun validateTrust(chain: List<X509Certificate>, leaf: X509Certificate): Boolean = runCatching {
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(null as KeyStore?)
        val tm = tmf.trustManagers.filterIsInstance<X509TrustManager>().first()
        tm.checkServerTrusted(chain.toTypedArray(), leaf.publicKey.algorithm)
        true
    }.getOrDefault(false)

    private fun buildPermissiveClient(): OkHttpClient {
        val trustAll = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
        val ctx = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trustAll), java.security.SecureRandom()) }
        return httpClient.newBuilder()
            .sslSocketFactory(ctx.socketFactory, trustAll)
            .hostnameVerifier { _, _ -> true }
            .followRedirects(false)
            .build()
    }

    // ---------- CIDR 计算（纯本地，无网络）----------

    fun computeCidr(input: String): CidrResult {
        val parts = input.trim().split('/')
        require(parts.size == 2) { "expected ip/prefix" }
        val addr = InetAddress.getByName(parts[0].trim())
        val prefix = parts[1].trim().toInt()
        val bytes = addr.address
        val isV6 = bytes.size == 16
        val bits = if (isV6) 128 else 32
        require(prefix in 0..bits) { "prefix out of range" }

        val ipInt = BigInteger(1, bytes)
        val hostBits = bits - prefix
        val allOnes = BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE)
        val hostMask = BigInteger.ONE.shiftLeft(hostBits).subtract(BigInteger.ONE) // hostBits=0 → 0
        val mask = allOnes.andNot(hostMask)
        val network = ipInt.and(mask)
        val total = BigInteger.ONE.shiftLeft(hostBits)
        val lastAddr = network.add(total).subtract(BigInteger.ONE)

        val networkStr = bigIntToIp(network, isV6)
        val broadcast = if (isV6) null else bigIntToIp(lastAddr, isV6)
        val firstHost: BigInteger
        val lastHost: BigInteger
        val usable: String
        if (isV6) {
            firstHost = network
            lastHost = lastAddr
            usable = total.toString()
        } else {
            if (prefix >= 31) {
                firstHost = network; lastHost = lastAddr; usable = total.toString()
            } else {
                firstHost = network.add(BigInteger.ONE)
                lastHost = lastAddr.subtract(BigInteger.ONE)
                usable = total.subtract(BigInteger.valueOf(2)).toString()
            }
        }
        return CidrResult(
            network = "$networkStr/$prefix",
            broadcast = broadcast,
            firstHost = bigIntToIp(firstHost, isV6),
            lastHost = bigIntToIp(lastHost, isV6),
            prefix = prefix,
            totalAddresses = total.toString(),
            usableHosts = usable,
            isV6 = isV6,
        )
    }

    private fun bigIntToIp(value: BigInteger, isV6: Boolean): String {
        val len = if (isV6) 16 else 4
        var raw = value.toByteArray()
        // 去掉 BigInteger 可能的符号前导 0 或补齐到定长
        raw = when {
            raw.size == len -> raw
            raw.size > len -> raw.copyOfRange(raw.size - len, raw.size)
            else -> ByteArray(len - raw.size) + raw
        }
        return InetAddress.getByAddress(raw).hostAddress ?: value.toString()
    }

    // ---------- 内部 ----------

    private fun req(url: String): Request = Request.Builder().url(url).get().build()

    private suspend fun execute(request: Request): ByteArray = withContext(Dispatchers.IO) {
        try {
            httpClient.newCall(request).execute().use { resp ->
                val bytes = resp.body?.bytes() ?: ByteArray(0)
                if (resp.code !in 200..299) throw ApiError.Http(resp.code)
                bytes
            }
        } catch (e: IOException) {
            throw ApiError.Network(e)
        }
    }

    private fun <T> decode(serializer: KSerializer<T>, bytes: ByteArray): T = try {
        json.decodeFromString(serializer, bytes.decodeToString())
    } catch (e: Exception) {
        throw ApiError.Decoding(e)
    }

    private fun normalizeHost(input: String): String =
        input.trim().removePrefix("https://").removePrefix("http://").substringBefore('/').trim()

    private fun hostPort(input: String, default: Int): Pair<String, Int> {
        val h = normalizeHost(input)
        // 仅当恰好一个冒号时视作 host:port（避开 IPv6 字面量）
        val first = h.indexOf(':')
        val last = h.lastIndexOf(':')
        return if (first > 0 && first == last) {
            h.substring(0, first) to (h.substring(first + 1).toIntOrNull() ?: default)
        } else {
            h to default
        }
    }

    private fun cnOf(dn: String): String {
        val cn = dn.split(',').map { it.trim() }.firstOrNull { it.startsWith("CN=", true) }
        return cn?.removePrefix("CN=")?.removePrefix("cn=") ?: dn
    }

    private fun dnsTypeName(type: Int): String = when (type) {
        1 -> "A"; 2 -> "NS"; 5 -> "CNAME"; 6 -> "SOA"; 12 -> "PTR"; 15 -> "MX"
        16 -> "TXT"; 28 -> "AAAA"; 33 -> "SRV"; 257 -> "CAA"; 43 -> "DS"; 48 -> "DNSKEY"
        else -> "TYPE$type"
    }
}
