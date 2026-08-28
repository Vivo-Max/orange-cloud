package jiamin.chen.orangecloud.data.repository

import jiamin.chen.orangecloud.core.network.CfApiClient
import jiamin.chen.orangecloud.data.model.URLScanAccepted
import jiamin.chen.orangecloud.data.model.URLScanResult
import jiamin.chen.orangecloud.data.model.URLScanSubmit
import javax.inject.Inject
import javax.inject.Singleton

/** URL Scanner v2（url-scanner.read / .write）。对应 iOS URLScannerService。 */
@Singleton
class URLScannerRepository @Inject constructor(
    private val api: CfApiClient,
) {
    /** 提交扫描。异步任务，返回 uuid 后需轮询结果。 */
    suspend fun submit(accountId: String, url: String): String? {
        val accepted: URLScanAccepted =
            api.post("accounts/$accountId/urlscanner/v2/scan", URLScanSubmit(url))
        // 不同版本回执里 uuid 可能落在 uuid 或 result 字段
        return accepted.uuid ?: accepted.result
    }

    /** 取报告。未就绪时接口 404 —— 调用方按「继续轮询」处理。 */
    suspend fun result(accountId: String, scanId: String): URLScanResult =
        api.get("accounts/$accountId/urlscanner/v2/result/$scanId")
}
