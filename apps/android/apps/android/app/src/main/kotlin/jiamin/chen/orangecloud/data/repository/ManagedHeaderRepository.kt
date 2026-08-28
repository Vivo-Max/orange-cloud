package jiamin.chen.orangecloud.data.repository

import jiamin.chen.orangecloud.core.network.CfApiClient
import jiamin.chen.orangecloud.data.model.ManagedTransformToggle
import jiamin.chen.orangecloud.data.model.ManagedTransforms
import jiamin.chen.orangecloud.data.model.ManagedTransformsPatch
import javax.inject.Inject
import javax.inject.Singleton

/** 托管请求/响应头（managed-headers.read / .write）。对应 iOS ManagedHeaderService。 */
@Singleton
class ManagedHeaderRepository @Inject constructor(
    private val api: CfApiClient,
) {
    suspend fun transforms(zoneId: String): ManagedTransforms =
        api.get("zones/$zoneId/managed_headers")

    /** 开关单条。isRequest 决定改哪一节——两节的 id 空间独立。 */
    suspend fun setEnabled(
        zoneId: String,
        id: String,
        enabled: Boolean,
        isRequest: Boolean,
    ): ManagedTransforms {
        val toggle = listOf(ManagedTransformToggle(id, enabled))
        val body = if (isRequest) {
            ManagedTransformsPatch(requestHeaders = toggle)
        } else {
            ManagedTransformsPatch(responseHeaders = toggle)
        }
        return api.patch("zones/$zoneId/managed_headers", body)
    }
}
