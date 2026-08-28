package jiamin.chen.orangecloud.data.model

import kotlinx.serialization.Serializable

/**
 * Cloudflare URL Scanner（**v2**，别用 v1）。
 * 扫描是异步的：提交后返回 uuid，结果需轮询，未就绪时接口 404。
 */
@Serializable
data class URLScanSubmit(val url: String)

@Serializable
data class URLScanAccepted(
    val uuid: String? = null,
    val api: String? = null,
    val message: String? = null,
    val result: String? = null,
)

@Serializable
data class URLScanResult(
    val task: URLScanTask? = null,
    val page: URLScanPage? = null,
    val verdicts: URLScanVerdicts? = null,
)

@Serializable
data class URLScanTask(
    val uuid: String? = null,
    val url: String? = null,
    val effectiveUrl: String? = null,
    val status: String? = null,
)

@Serializable
data class URLScanPage(
    val url: String? = null,
    val domain: String? = null,
    val ip: String? = null,
    val country: String? = null,
    val asn: String? = null,
    val asnname: String? = null,
    val server: String? = null,
    val statusCode: Int? = null,
)

@Serializable
data class URLScanVerdicts(val overall: URLScanVerdict? = null)

@Serializable
data class URLScanVerdict(
    val malicious: Boolean? = null,
    val categories: List<URLScanCategory>? = null,
)

@Serializable
data class URLScanCategory(val id: Int? = null, val name: String? = null)
