package jiamin.chen.orangecloud.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** GET/PATCH /zones/{id}/settings/{setting} 的 result。value 形态因设置而异（字符串）。 */
@Serializable
data class ZoneSetting(
    val id: String? = null,
    val value: String,
)

@Serializable
data class ZoneSettingUpdate(val value: String)

@Serializable
data class PurgeRequest(
    @SerialName("purge_everything") val purgeEverything: Boolean,
)

/** 按 URL 单文件清缓存（单次最多 30 个 URL；2025-04 起所有套餐可用）。 */
@Serializable
data class PurgeFilesRequest(val files: List<String>)

@Serializable
data class PurgeResult(val id: String? = null)
