package jiamin.chen.orangecloud.data.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneOffset

/**
 * 账号用量（Dashboard 用量模块，对应 iOS Models/AccountUsageModels.swift）。
 *
 * 每个数据集拆成独立查询：账号未启用某服务 / token 无该数据集权限时单独失败，
 * 不拖垮其余用量（对齐 iOS issue #4 的拆查询策略）。R2 复用 [R2UsageData]/[R2UsageVariables]。
 */

// MARK: - 聚合结果（@Serializable 供落盘缓存，对齐 iOS UsageCache）

/** D1 用量聚合（行读/写：今日 + 周期）。 */
@Serializable
data class D1Usage(
    val rowsReadToday: Long,
    val rowsWrittenToday: Long,
    val rowsReadPeriod: Long,
    val rowsWrittenPeriod: Long,
)

/** KV 用量聚合（读/写：今日 + 周期）。 */
@Serializable
data class KVUsage(
    val readsToday: Long,
    val writesToday: Long,
    val readsPeriod: Long,
    val writesPeriod: Long,
)

/** 账号用量总表：Workers 主查询 + R2/CPU/D1/KV 独立查询合并（缺失项保持默认/ null）。 */
@Serializable
data class AccountUsage(
    val workersRequestsToday: Long = 0,
    val workersRequestsMonth: Long = 0,
    val workersErrorsMonth: Long = 0,
    val cpuP50Us: Double? = null,      // 微秒（单次分位）
    val cpuP99Us: Double? = null,
    val cpuTimeMonthUs: Double? = null,
    val cpuTimeTodayUs: Double? = null,
    val r2ClassAMonth: Long = 0,
    val r2ClassBMonth: Long = 0,
    val r2StorageBytes: Long = 0,
    val r2ObjectCount: Long = 0,
    val d1Usage: D1Usage? = null,
    val d1StorageBytes: Long? = null,
    val kvUsage: KVUsage? = null,
    val kvStorageBytes: Long? = null,
)

// MARK: - 计费周期

/** 用量周期起点：billingDay=1 即自然月起点；否则最近一次账单日（UTC 锚定）。 */
object BillingCycle {
    fun periodStart(billingDay: Int, now: Instant = Instant.now()): Instant {
        val day = billingDay.coerceIn(1, 28)
        val today = now.atZone(ZoneOffset.UTC).toLocalDate()
        val anchor = if (today.dayOfMonth >= day) today.withDayOfMonth(day)
        else today.minusMonths(1).withDayOfMonth(day)
        return anchor.atStartOfDay(ZoneOffset.UTC).toInstant()
    }
}

// MARK: - R2 操作分类（读类计 Class B，其余计 Class A，与 StorageRepository 口径一致）

val R2_CLASS_B_ACTIONS: Set<String> = setOf(
    "GetObject", "HeadObject", "HeadBucket", "UsageSummary",
    "GetBucketEncryption", "GetBucketLocation", "GetBucketCors", "GetBucketLifecycleConfiguration",
)

// MARK: - Workers 账号用量（month/today 两窗口）

@Serializable
data class AccountUsageVariables(
    val accountTag: String,
    val monthStart: String,
    val todayStart: String,
    val now: String,
)

@Serializable
data class AccountUsageData(val viewer: AccountUsageViewer)

@Serializable
data class AccountUsageViewer(val accounts: List<AccountUsageNode> = emptyList())

@Serializable
data class AccountUsageNode(
    val month: List<WorkersUsageGroup>? = null,
    val today: List<WorkersUsageGroup>? = null,
)

@Serializable
data class WorkersUsageGroup(
    val sum: WorkersUsageSum? = null,
    val quantiles: WorkerQuantiles? = null,   // 复用 AnalyticsModels 的 cpuTimeP50/P99
)

@Serializable
data class WorkersUsageSum(
    val requests: Long? = null,
    val errors: Long? = null,
    val subrequests: Long? = null,
)

// MARK: - Workers CPU 总耗时（较新字段，独立查询）

@Serializable
data class WorkersCpuData(val viewer: WorkersCpuViewer)

@Serializable
data class WorkersCpuViewer(val accounts: List<WorkersCpuNode> = emptyList())

@Serializable
data class WorkersCpuNode(
    val month: List<WorkersCpuGroup>? = null,
    val today: List<WorkersCpuGroup>? = null,
)

@Serializable
data class WorkersCpuGroup(val sum: WorkersCpuSum? = null)

@Serializable
data class WorkersCpuSum(val cpuTimeUs: Double? = null)

// MARK: - D1 用量（date 标量窗口）

@Serializable
data class D1UsageVariables(
    val accountTag: String,
    val periodStart: String,   // yyyy-MM-dd（UTC）
    val todayStart: String,
    val until: String,
)

@Serializable
data class D1UsageData(val viewer: D1UsageViewer)

@Serializable
data class D1UsageViewer(val accounts: List<D1UsageNode> = emptyList())

@Serializable
data class D1UsageNode(
    val period: List<D1UsageGroup>? = null,
    val today: List<D1UsageGroup>? = null,
)

@Serializable
data class D1UsageGroup(val sum: D1UsageSum? = null)

@Serializable
data class D1UsageSum(
    val rowsRead: Long? = null,
    val rowsWritten: Long? = null,
    val readQueries: Long? = null,
    val writeQueries: Long? = null,
)

// MARK: - KV 用量（按 actionType 分组 + 存储，date 标量窗口，复用 D1UsageVariables）

@Serializable
data class KVUsageData(val viewer: KVUsageViewer)

@Serializable
data class KVUsageViewer(val accounts: List<KVUsageNode> = emptyList())

@Serializable
data class KVUsageNode(
    val period: List<KVOpsGroup>? = null,
    val today: List<KVOpsGroup>? = null,
    val storage: List<KVStorageGroup>? = null,
)

@Serializable
data class KVOpsGroup(
    val dimensions: KVOpsDim? = null,
    val sum: KVOpsSum? = null,
)

@Serializable
data class KVOpsDim(val actionType: String? = null)   // "read" | "write" | "delete" | "list"

@Serializable
data class KVOpsSum(val requests: Long? = null)

@Serializable
data class KVStorageGroup(
    val dimensions: KVStorageDim? = null,
    val max: KVStorageMax? = null,
)

@Serializable
data class KVStorageDim(val namespaceId: String? = null)

@Serializable
data class KVStorageMax(val byteCount: Long? = null, val keyCount: Long? = null)

// MARK: - GraphQL 查询模板

object AccountUsageQueries {

    /** Workers 调用（month/today），含单次 CPU 分位。 */
    val WORKERS = """
        query (${'$'}accountTag: string!, ${'$'}monthStart: Time!, ${'$'}todayStart: Time!, ${'$'}now: Time!) {
          viewer {
            accounts(filter: { accountTag: ${'$'}accountTag }) {
              month: workersInvocationsAdaptive(limit: 10000, filter: { datetime_geq: ${'$'}monthStart, datetime_leq: ${'$'}now }) {
                sum { requests errors subrequests }
                quantiles { cpuTimeP50 cpuTimeP99 }
              }
              today: workersInvocationsAdaptive(limit: 10000, filter: { datetime_geq: ${'$'}todayStart, datetime_leq: ${'$'}now }) {
                sum { requests errors subrequests }
              }
            }
          }
        }
    """.trimIndent()

    /** Workers CPU 总耗时（month/today），sum.cpuTimeUs 是较新 schema 字段。 */
    val WORKERS_CPU = """
        query (${'$'}accountTag: string!, ${'$'}monthStart: Time!, ${'$'}todayStart: Time!, ${'$'}now: Time!) {
          viewer {
            accounts(filter: { accountTag: ${'$'}accountTag }) {
              month: workersInvocationsAdaptive(limit: 10000, filter: { datetime_geq: ${'$'}monthStart, datetime_leq: ${'$'}now }) { sum { cpuTimeUs } }
              today: workersInvocationsAdaptive(limit: 10000, filter: { datetime_geq: ${'$'}todayStart, datetime_leq: ${'$'}now }) { sum { cpuTimeUs } }
            }
          }
        }
    """.trimIndent()

    /** R2 操作分类 + 当前存储（账号级聚合，忽略 bucketName 维度）。 */
    val R2 = """
        query (${'$'}accountTag: string!, ${'$'}monthStart: Time!, ${'$'}todayStart: Time!, ${'$'}now: Time!) {
          viewer {
            accounts(filter: { accountTag: ${'$'}accountTag }) {
              r2Ops: r2OperationsAdaptiveGroups(limit: 10000, filter: { datetime_geq: ${'$'}monthStart, datetime_leq: ${'$'}now }) {
                dimensions { actionType bucketName }
                sum { requests }
              }
              r2Storage: r2StorageAdaptiveGroups(limit: 1000, filter: { datetime_geq: ${'$'}todayStart, datetime_leq: ${'$'}now }) {
                dimensions { bucketName }
                max { payloadSize metadataSize objectCount }
              }
            }
          }
        }
    """.trimIndent()

    /** D1 行读/写（period/today）。 */
    val D1 = """
        query (${'$'}accountTag: string!, ${'$'}periodStart: Date!, ${'$'}todayStart: Date!, ${'$'}until: Date!) {
          viewer {
            accounts(filter: { accountTag: ${'$'}accountTag }) {
              period: d1AnalyticsAdaptiveGroups(limit: 10000, filter: { date_geq: ${'$'}periodStart, date_leq: ${'$'}until }) {
                sum { rowsRead rowsWritten readQueries writeQueries }
              }
              today: d1AnalyticsAdaptiveGroups(limit: 10000, filter: { date_geq: ${'$'}todayStart, date_leq: ${'$'}until }) {
                sum { rowsRead rowsWritten readQueries writeQueries }
              }
            }
          }
        }
    """.trimIndent()

    /** KV 操作按 actionType 分组（period/today）。 */
    val KV_OPERATIONS = """
        query (${'$'}accountTag: string!, ${'$'}periodStart: Date!, ${'$'}todayStart: Date!, ${'$'}until: Date!) {
          viewer {
            accounts(filter: { accountTag: ${'$'}accountTag }) {
              period: kvOperationsAdaptiveGroups(limit: 10000, filter: { date_geq: ${'$'}periodStart, date_leq: ${'$'}until }) {
                dimensions { actionType }
                sum { requests }
              }
              today: kvOperationsAdaptiveGroups(limit: 10000, filter: { date_geq: ${'$'}todayStart, date_leq: ${'$'}until }) {
                dimensions { actionType }
                sum { requests }
              }
            }
          }
        }
    """.trimIndent()

    /** KV 当前存储（各 namespace 当日 max byteCount）。 */
    val KV_STORAGE = """
        query (${'$'}accountTag: string!, ${'$'}periodStart: Date!, ${'$'}todayStart: Date!, ${'$'}until: Date!) {
          viewer {
            accounts(filter: { accountTag: ${'$'}accountTag }) {
              storage: kvStorageAdaptiveGroups(limit: 1000, filter: { date_geq: ${'$'}todayStart, date_leq: ${'$'}until }) {
                dimensions { namespaceId }
                max { byteCount keyCount }
              }
            }
          }
        }
    """.trimIndent()
}
