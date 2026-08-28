package jiamin.chen.orangecloud.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Workers Builds —— Worker 的 CI 构建记录。
 *
 * 注意 status 与 outcome 是两个维度：status 只到 stopped 为止
 * （queued / initializing / running / stopped），成功还是失败要看 build_outcome。
 * 只看 status 会把「失败」显示成「已结束」，必须合起来判断。
 */
@Serializable
data class WorkerBuild(
    @SerialName("build_uuid") val buildUuid: String,
    /** queued / initializing / running / stopped */
    val status: String? = null,
    /** success / fail / skipped / cancelled / terminated（仅 stopped 后有值） */
    @SerialName("build_outcome") val buildOutcome: String? = null,
    @SerialName("created_on") val createdOn: String? = null,
    @SerialName("modified_on") val modifiedOn: String? = null,
    @SerialName("build_trigger_metadata") val triggerMetadata: BuildTriggerMetadata? = null,
) {
    /** 把 status + outcome 合成一个用户能看懂的状态 */
    val displayState: BuildDisplayState
        get() = if (status != "stopped") {
            when (status) {
                "queued" -> BuildDisplayState.QUEUED
                "initializing", "running" -> BuildDisplayState.RUNNING
                else -> BuildDisplayState.UNKNOWN
            }
        } else {
            when (buildOutcome) {
                "success" -> BuildDisplayState.SUCCESS
                "fail", "terminated" -> BuildDisplayState.FAILED
                "cancelled" -> BuildDisplayState.CANCELLED
                "skipped" -> BuildDisplayState.SKIPPED
                else -> BuildDisplayState.UNKNOWN
            }
        }
}

enum class BuildDisplayState {
    QUEUED, RUNNING, SUCCESS, FAILED, CANCELLED, SKIPPED, UNKNOWN;

    val isRunning: Boolean get() = this == QUEUED || this == RUNNING
}

@Serializable
data class BuildTriggerMetadata(
    val author: String? = null,
    val branch: String? = null,
    @SerialName("commit_hash") val commitHash: String? = null,
    @SerialName("build_command") val buildCommand: String? = null,
    @SerialName("build_trigger_source") val triggerSource: String? = null,
) {
    /** commit 只取前 7 位，和 git 短哈希习惯一致 */
    val shortCommit: String? get() = commitHash?.take(7)
}

@Serializable
data class BuildLogsPage(
    val lines: List<BuildLogLine>? = null,
    val cursor: String? = null,
)

@Serializable
data class BuildLogLine(
    val line: String? = null,
    val ts: String? = null,
)
