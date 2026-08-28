package jiamin.chen.orangecloud.data.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Zone 流量分析（对应 iOS Models/AnalyticsModels.swift）。
 * 数据集：httpRequests1hGroups（24h，Time 标量）/ httpRequests1dGroups（7d·30d，Date 标量）。
 */
enum class AnalyticsTimeRange(val limit: Int) {
    LAST_24H(25),
    LAST_7D(7),
    LAST_30D(30);

    /** 24h 用小时级数据集，7d/30d 用天级（规避低套餐小时数据留存限制）。 */
    val usesHourlyGroups: Boolean get() = this == LAST_24H

    private val dayCount: Int get() = if (this == LAST_7D) 6 else 29

    /** 当前周期查询区间：小时级返回 ISO8601 datetime，天级返回 yyyy-MM-dd（UTC）。 */
    fun sinceUntil(now: Instant = Instant.now()): Pair<String, String> {
        if (usesHourlyGroups) {
            val sec = now.truncatedTo(ChronoUnit.SECONDS)
            return ISO_INSTANT.format(sec.minus(24, ChronoUnit.HOURS)) to ISO_INSTANT.format(sec)
        }
        val today = now.atZone(ZoneOffset.UTC).toLocalDate()
        val start = today.minusDays(dayCount.toLong())
        return start.format(DAY) to today.format(DAY)
    }

    companion object {
        val ISO_INSTANT: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT
        val DAY: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE  // yyyy-MM-dd

        fun parseDimension(datetime: String?, date: String?): Instant? {
            if (!datetime.isNullOrEmpty()) return runCatching { Instant.parse(datetime) }.getOrNull()
            if (!date.isNullOrEmpty()) {
                return runCatching {
                    LocalDate.parse(date, DAY).atStartOfDay(ZoneOffset.UTC).toInstant()
                }.getOrNull()
            }
            return null
        }
    }
}

// MARK: - GraphQL 变量 / 响应

@Serializable
data class ZoneAnalyticsVariables(
    val zoneTag: String,
    val since: String,
    val until: String,
)

@Serializable
data class ZoneAnalyticsData(val viewer: AnalyticsViewer)

@Serializable
data class AnalyticsViewer(val zones: List<AnalyticsZoneNode> = emptyList())

@Serializable
data class AnalyticsZoneNode(
    val httpRequests1hGroups: List<AnalyticsGroup>? = null,
    val httpRequests1dGroups: List<AnalyticsGroup>? = null,
) {
    val groups: List<AnalyticsGroup> get() = httpRequests1hGroups ?: httpRequests1dGroups ?: emptyList()
}

@Serializable
data class AnalyticsGroup(
    val dimensions: AnalyticsDimensions? = null,
    val sum: AnalyticsSum? = null,
    val uniq: AnalyticsUniq? = null,
)

@Serializable
data class AnalyticsDimensions(
    val datetime: String? = null,   // 1hGroups
    val date: String? = null,       // 1dGroups
)

@Serializable
data class AnalyticsSum(
    val requests: Int? = null,
    val bytes: Long? = null,
    val threats: Int? = null,
    val pageViews: Int? = null,
    val cachedRequests: Int? = null,
    val cachedBytes: Long? = null,
)

@Serializable
data class AnalyticsUniq(val uniques: Int? = null)

/** 两种 dimensions 归一化后的统一图表数据点。 */
data class TrafficDataPoint(
    val date: Instant,
    val requests: Int,
    val bytes: Long,
    val threats: Int,
    val pageViews: Int,
    val uniques: Int,
    val cachedRequests: Int,
)

// MARK: - 按国家/地区流量（clientCountryName 维度，对应 iOS ZoneTrafficMapView 数据）

@Serializable
data class ZoneCountryData(val viewer: CountryViewer)

@Serializable
data class CountryViewer(val zones: List<CountryZoneNode> = emptyList())

@Serializable
data class CountryZoneNode(
    val httpRequests1hGroups: List<CountryGroup>? = null,
    val httpRequests1dGroups: List<CountryGroup>? = null,
) {
    val groups: List<CountryGroup> get() = httpRequests1hGroups ?: httpRequests1dGroups ?: emptyList()
}

@Serializable
data class CountryGroup(
    val dimensions: CountryDimensions? = null,
    val sum: AnalyticsSum? = null,
)

@Serializable
data class CountryDimensions(val clientCountryName: String? = null)

/** 归一化的按国家/地区请求量。 */
data class CountryTraffic(val country: String, val requests: Long)

// MARK: - 单 Worker 指标（workersInvocationsAdaptive）

@Serializable
data class WorkerMetricsVariables(
    val accountTag: String,
    val scriptName: String,
    val since: String,
    val until: String,
)

@Serializable
data class WorkerMetricsData(val viewer: WorkerMetricsViewer)

@Serializable
data class WorkerMetricsViewer(val accounts: List<WorkerMetricsNode> = emptyList())

@Serializable
data class WorkerMetricsNode(
    val summary: List<WorkerMetricsGroup>? = null,
    val byStatus: List<WorkerStatusGroup>? = null,
)

@Serializable
data class WorkerMetricsGroup(
    val sum: WorkerMetricsSum? = null,
    val quantiles: WorkerQuantiles? = null,
)

@Serializable
data class WorkerMetricsSum(
    val requests: Long? = null,
    val errors: Long? = null,
    val subrequests: Long? = null,
)

@Serializable
data class WorkerQuantiles(
    val cpuTimeP50: Double? = null,   // 微秒
    val cpuTimeP99: Double? = null,
)

@Serializable
data class WorkerStatusGroup(
    val dimensions: WorkerStatusDimensions? = null,
    val sum: WorkerMetricsSum? = null,
)

@Serializable
data class WorkerStatusDimensions(val status: String? = null)

// 时间序列（趋势图）
@Serializable
data class WorkerSeriesData(val viewer: WorkerSeriesViewer)

@Serializable
data class WorkerSeriesViewer(val accounts: List<WorkerSeriesNode> = emptyList())

@Serializable
data class WorkerSeriesNode(val series: List<WorkerSeriesGroup>? = null)

@Serializable
data class WorkerSeriesGroup(
    val dimensions: WorkerSeriesDimensions? = null,
    val sum: WorkerMetricsSum? = null,
)

@Serializable
data class WorkerSeriesDimensions(
    val datetimeHour: String? = null,   // 24h
    val date: String? = null,           // 7d/30d
)

/** 聚合后的 Worker 指标。 */
data class WorkerMetrics(
    val requests: Long,
    val errors: Long,
    val subrequests: Long,
    val cpuP50Us: Double? = null,
    val cpuP99Us: Double? = null,
    val statusBreakdown: List<WorkerStatusCount> = emptyList(),
) {
    val errorRate: Double get() = if (requests > 0) errors.toDouble() / requests * 100 else 0.0
}

data class WorkerStatusCount(val status: String, val requests: Long)

/** 趋势图数据点（请求 + 错误）。 */
data class WorkerSeriesPoint(
    val date: Instant,
    val requests: Long,
    val errors: Long,
)

/** GraphQL 查询模板（对应 iOS AnalyticsQueries）。 */
object AnalyticsQueries {
    fun workerSummary(): String = """
        query (${'$'}accountTag: string!, ${'$'}scriptName: string!, ${'$'}since: Time!, ${'$'}until: Time!) {
          viewer {
            accounts(filter: { accountTag: ${'$'}accountTag }) {
              summary: workersInvocationsAdaptive(
                limit: 10000,
                filter: { scriptName: ${'$'}scriptName, datetime_geq: ${'$'}since, datetime_leq: ${'$'}until }
              ) {
                sum { requests errors subrequests }
                quantiles { cpuTimeP50 cpuTimeP99 }
              }
              byStatus: workersInvocationsAdaptive(
                limit: 100,
                filter: { scriptName: ${'$'}scriptName, datetime_geq: ${'$'}since, datetime_leq: ${'$'}until }
              ) {
                dimensions { status }
                sum { requests }
              }
            }
          }
        }
    """.trimIndent()

    /** Worker 调用趋势：24h 按小时（datetimeHour），7d/30d 按天（date）。 */
    fun workerSeries(daily: Boolean): String {
        val dimension = if (daily) "date" else "datetimeHour"
        return """
            query (${'$'}accountTag: string!, ${'$'}scriptName: string!, ${'$'}since: Time!, ${'$'}until: Time!) {
              viewer {
                accounts(filter: { accountTag: ${'$'}accountTag }) {
                  series: workersInvocationsAdaptive(
                    limit: 1000,
                    orderBy: [${dimension}_ASC],
                    filter: { scriptName: ${'$'}scriptName, datetime_geq: ${'$'}since, datetime_leq: ${'$'}until }
                  ) {
                    dimensions { $dimension }
                    sum { requests errors }
                  }
                }
              }
            }
        """.trimIndent()
    }

    fun zoneHourly(limit: Int): String = """
        query (${'$'}zoneTag: string!, ${'$'}since: Time!, ${'$'}until: Time!) {
          viewer {
            zones(filter: { zoneTag: ${'$'}zoneTag }) {
              httpRequests1hGroups(
                limit: $limit,
                orderBy: [datetime_ASC],
                filter: { datetime_geq: ${'$'}since, datetime_lt: ${'$'}until }
              ) {
                dimensions { datetime }
                sum  { requests bytes threats pageViews cachedRequests cachedBytes }
                uniq { uniques }
              }
            }
          }
        }
    """.trimIndent()

    /** 按国家/地区请求量（24h 小时级数据集，按请求降序）。 */
    fun zoneCountryHourly(limit: Int): String = """
        query (${'$'}zoneTag: string!, ${'$'}since: Time!, ${'$'}until: Time!) {
          viewer {
            zones(filter: { zoneTag: ${'$'}zoneTag }) {
              httpRequests1hGroups(
                limit: $limit,
                orderBy: [sum_requests_DESC],
                filter: { datetime_geq: ${'$'}since, datetime_lt: ${'$'}until }
              ) {
                dimensions { clientCountryName }
                sum { requests }
              }
            }
          }
        }
    """.trimIndent()

    /** 按国家/地区请求量（7d/30d 天级数据集，按请求降序）。 */
    fun zoneCountryDaily(limit: Int): String = """
        query (${'$'}zoneTag: string!, ${'$'}since: Date!, ${'$'}until: Date!) {
          viewer {
            zones(filter: { zoneTag: ${'$'}zoneTag }) {
              httpRequests1dGroups(
                limit: $limit,
                orderBy: [sum_requests_DESC],
                filter: { date_geq: ${'$'}since, date_leq: ${'$'}until }
              ) {
                dimensions { clientCountryName }
                sum { requests }
              }
            }
          }
        }
    """.trimIndent()

    fun zoneDaily(limit: Int): String = """
        query (${'$'}zoneTag: string!, ${'$'}since: Date!, ${'$'}until: Date!) {
          viewer {
            zones(filter: { zoneTag: ${'$'}zoneTag }) {
              httpRequests1dGroups(
                limit: $limit,
                orderBy: [date_ASC],
                filter: { date_geq: ${'$'}since, date_leq: ${'$'}until }
              ) {
                dimensions { date }
                sum  { requests bytes threats pageViews cachedRequests cachedBytes }
                uniq { uniques }
              }
            }
          }
        }
    """.trimIndent()
}
