//
//  WorkerMetricsModels.swift
//  Orange Cloud
//
//  单个 Worker 的指标（workersInvocationsAdaptive，按 scriptName 过滤）。
//  摘要 / CPU 总量 / 时间序列分三个查询，新 schema 字段不可用时互不拖累。
//

import Foundation

// MARK: - 查询

nonisolated enum WorkerMetricsQueries {

    /// 摘要 + 按状态分解（全部为文档化字段，稳定）
    static let summary = """
    query ($accountTag: string!, $scriptName: string!, $since: Time!, $until: Time!) {
      viewer {
        accounts(filter: { accountTag: $accountTag }) {
          summary: workersInvocationsAdaptive(
            limit: 10000,
            filter: { scriptName: $scriptName, datetime_geq: $since, datetime_leq: $until }
          ) {
            sum { requests errors subrequests }
            quantiles { cpuTimeP50 cpuTimeP99 }
          }
          byStatus: workersInvocationsAdaptive(
            limit: 100,
            filter: { scriptName: $scriptName, datetime_geq: $since, datetime_leq: $until }
          ) {
            dimensions { status }
            sum { requests }
          }
        }
      }
    }
    """

    /// CPU 总耗时（较新字段，独立请求）
    static let cpuTotal = """
    query ($accountTag: string!, $scriptName: string!, $since: Time!, $until: Time!) {
      viewer {
        accounts(filter: { accountTag: $accountTag }) {
          summary: workersInvocationsAdaptive(
            limit: 10000,
            filter: { scriptName: $scriptName, datetime_geq: $since, datetime_leq: $until }
          ) {
            sum { cpuTimeUs }
          }
        }
      }
    }
    """

    /// 时间序列：24h 按小时（datetimeHour），7d/30d 按天（date）
    static func series(daily: Bool) -> String {
        let dimension = daily ? "date" : "datetimeHour"
        return """
        query ($accountTag: string!, $scriptName: string!, $since: Time!, $until: Time!) {
          viewer {
            accounts(filter: { accountTag: $accountTag }) {
              series: workersInvocationsAdaptive(
                limit: 1000,
                orderBy: [\(dimension)_ASC],
                filter: { scriptName: $scriptName, datetime_geq: $since, datetime_leq: $until }
              ) {
                dimensions { \(dimension) }
                sum { requests errors }
              }
            }
          }
        }
        """
    }
}

nonisolated struct WorkerMetricsVariables: Codable, Sendable {
    let accountTag: String
    let scriptName: String
    let since:      String
    let until:      String
}

// MARK: - 响应

nonisolated struct WorkerMetricsData: Codable, Sendable {
    let viewer: WorkerMetricsViewer
}

nonisolated struct WorkerMetricsViewer: Codable, Sendable {
    let accounts: [WorkerMetricsNode]
}

nonisolated struct WorkerMetricsNode: Codable, Sendable {
    let summary:  [WorkersUsageGroup]?      // 复用 AccountUsageModels 的 sum/quantiles 结构
    let byStatus: [WorkerStatusGroup]?
}

nonisolated struct WorkerStatusGroup: Codable, Sendable {
    let dimensions: WorkerStatusDimensions?
    let sum:        R2OpsSum?               // 复用 { requests }
}

nonisolated struct WorkerStatusDimensions: Codable, Sendable {
    let status: String?
}

nonisolated struct WorkerCpuData: Codable, Sendable {
    let viewer: WorkerCpuViewer
}

nonisolated struct WorkerCpuViewer: Codable, Sendable {
    let accounts: [WorkerCpuNode]
}

nonisolated struct WorkerCpuNode: Codable, Sendable {
    let summary: [WorkersCpuGroup]?         // 复用 { sum { cpuTimeUs } }
}

nonisolated struct WorkerSeriesData: Codable, Sendable {
    let viewer: WorkerSeriesViewer
}

nonisolated struct WorkerSeriesViewer: Codable, Sendable {
    let accounts: [WorkerSeriesNode]
}

nonisolated struct WorkerSeriesNode: Codable, Sendable {
    let series: [WorkerSeriesGroup]?
}

nonisolated struct WorkerSeriesGroup: Codable, Sendable {
    let dimensions: WorkerSeriesDimensions?
    let sum:        WorkerSeriesSum?
}

nonisolated struct WorkerSeriesDimensions: Codable, Sendable {
    let datetimeHour: String?
    let date:         String?
}

nonisolated struct WorkerSeriesSum: Codable, Sendable {
    let requests: Int?
    let errors:   Int?
}

// MARK: - 聚合结果

nonisolated struct WorkerMetrics: Sendable {
    let requests:    Int
    let errors:      Int
    let subrequests: Int
    let cpuP50Us:    Double?
    let cpuP99Us:    Double?
    var cpuTotalUs:  Double?
    let statusBreakdown: [(status: String, requests: Int)]

    var errorRate: Double? {
        guard requests > 0 else { return nil }
        return Double(errors) / Double(requests) * 100
    }
}

nonisolated struct WorkerSeriesPoint: Identifiable, Sendable {
    let date:     Date
    let requests: Int
    let errors:   Int

    var id: Date { date }
}

/// 调用状态 → 展示文案与颜色键
nonisolated enum WorkerInvocationStatus {

    static func label(_ status: String) -> String {
        switch status {
        case "success":              String(localized: "成功")
        case "scriptThrewException": String(localized: "脚本异常")
        case "exceededCpu":          String(localized: "超出 CPU 限制")
        case "exceededMemory":       String(localized: "超出内存限制")
        case "clientDisconnected":   String(localized: "客户端断开")
        case "canceled":             String(localized: "已取消")
        case "responseStreamDisconnected": String(localized: "响应流断开")
        default:                     status
        }
    }

    static func isHealthy(_ status: String) -> Bool {
        status == "success"
    }

    static func isNeutral(_ status: String) -> Bool {
        ["clientDisconnected", "canceled", "responseStreamDisconnected"].contains(status)
    }
}
