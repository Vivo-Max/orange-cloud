package jiamin.chen.orangecloud.data.repository

import jiamin.chen.orangecloud.core.network.CfApiClient
import jiamin.chen.orangecloud.data.model.TraceRequest
import jiamin.chen.orangecloud.data.model.TraceResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloudflare Trace（request-tracer.read）。全套餐可用，
 * 但**要求 Administrator / Super Administrator 角色**——非管理员成员会失败。
 */
@Singleton
class RequestTracerRepository @Inject constructor(
    private val api: CfApiClient,
) {
    suspend fun trace(accountId: String, method: String, url: String): TraceResult =
        api.post("accounts/$accountId/request-tracer/trace", TraceRequest(method, url))
}
