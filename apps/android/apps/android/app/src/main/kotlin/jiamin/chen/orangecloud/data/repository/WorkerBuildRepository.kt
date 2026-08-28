package jiamin.chen.orangecloud.data.repository

import jiamin.chen.orangecloud.core.network.CfApiClient
import jiamin.chen.orangecloud.data.model.BuildLogLine
import jiamin.chen.orangecloud.data.model.BuildLogsPage
import jiamin.chen.orangecloud.data.model.WorkerBuild
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private class CancelBuildBody

/** Workers Builds（workers-ci.read / .write）。对应 iOS WorkerBuildService。 */
@Singleton
class WorkerBuildRepository @Inject constructor(
    private val api: CfApiClient,
) {
    /** 某个 Worker 的构建记录。未接 CI 的 Worker 会 404，调用方按「没有构建」处理。 */
    suspend fun builds(accountId: String, scriptId: String): List<WorkerBuild> =
        api.getList<WorkerBuild>(
            "accounts/$accountId/builds/workers/$scriptId/builds",
            listOf("per_page" to "20"),
        ).items

    suspend fun logs(accountId: String, buildUuid: String): List<BuildLogLine> =
        api.get<BuildLogsPage>("accounts/$accountId/builds/builds/$buildUuid/logs").lines.orEmpty()

    /** 取消进行中的构建（workers-ci.write） */
    suspend fun cancel(accountId: String, buildUuid: String): WorkerBuild =
        api.put("accounts/$accountId/builds/builds/$buildUuid/cancel", CancelBuildBody())
}
