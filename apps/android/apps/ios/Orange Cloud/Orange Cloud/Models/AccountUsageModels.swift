//
//  AccountUsageModels.swift
//  Orange Cloud
//
//  账号用量（Dashboard）：Workers 调用（主查询）+ R2 操作/存储（独立查询）。
//  数据集：workersInvocationsAdaptive / r2OperationsAdaptiveGroups / r2StorageAdaptiveGroups。
//  R2 拆为独立查询：账号未启用 R2 / token 无 R2 数据集权限时不拖垮 Workers 主用量（issue #4）。
//

import Foundation

// MARK: - 查询

nonisolated enum AccountUsageQuery {

    /// month/today 两个窗口（仅 Workers 调用）。R2 见独立的 R2UsageQuery——
    /// 账号未启用 R2 / token 无 R2 数据集权限时 R2 字段会让整条 GraphQL 报错，
    /// 与 CPU/D1/KV 同策略拆开，保证 Workers 用量始终能加载（issue #4）。
    static let text = """
    query ($accountTag: string!, $monthStart: Time!, $todayStart: Time!, $now: Time!) {
      viewer {
        accounts(filter: { accountTag: $accountTag }) {
          month: workersInvocationsAdaptive(
            limit: 10000,
            filter: { datetime_geq: $monthStart, datetime_leq: $now }
          ) {
            sum { requests errors subrequests }
            quantiles { cpuTimeP50 cpuTimeP99 }
          }
          today: workersInvocationsAdaptive(
            limit: 10000,
            filter: { datetime_geq: $todayStart, datetime_leq: $now }
          ) {
            sum { requests errors subrequests }
          }
        }
      }
    }
    """
}

/// R2 用量独立查询：月操作分类 + 当前存储。窗口变量复用 AccountUsageVariables。
/// 账号未启用 R2 或 token 无 R2 数据集权限时单独失败，不影响 Workers 主用量（issue #4）。
nonisolated enum R2UsageQuery {

    static let text = """
    query ($accountTag: string!, $monthStart: Time!, $todayStart: Time!, $now: Time!) {
      viewer {
        accounts(filter: { accountTag: $accountTag }) {
          r2Ops: r2OperationsAdaptiveGroups(
            limit: 10000,
            filter: { datetime_geq: $monthStart, datetime_leq: $now }
          ) {
            dimensions { actionType bucketName }
            sum { requests }
          }
          r2Storage: r2StorageAdaptiveGroups(
            limit: 1000,
            filter: { datetime_geq: $todayStart, datetime_leq: $now }
          ) {
            dimensions { bucketName }
            max { payloadSize metadataSize objectCount }
          }
        }
      }
    }
    """
}

nonisolated struct AccountUsageVariables: Codable, Sendable {
    let accountTag: String
    let monthStart: String
    let todayStart: String
    let now:        String
}

/// CPU 总耗时单独查询：sum.cpuTimeUs 是较新的 schema 字段，
/// 独立请求保证它在个别账号不可用时不拖垮主用量查询。
nonisolated enum WorkersCpuQuery {

    static let text = """
    query ($accountTag: string!, $monthStart: Time!, $todayStart: Time!, $now: Time!) {
      viewer {
        accounts(filter: { accountTag: $accountTag }) {
          month: workersInvocationsAdaptive(
            limit: 10000,
            filter: { datetime_geq: $monthStart, datetime_leq: $now }
          ) {
            sum { cpuTimeUs }
          }
          today: workersInvocationsAdaptive(
            limit: 10000,
            filter: { datetime_geq: $todayStart, datetime_leq: $now }
          ) {
            sum { cpuTimeUs }
          }
        }
      }
    }
    """
}

nonisolated struct WorkersCpuData: Codable, Sendable {
    let viewer: WorkersCpuViewer
}

nonisolated struct WorkersCpuViewer: Codable, Sendable {
    let accounts: [WorkersCpuNode]
}

nonisolated struct WorkersCpuNode: Codable, Sendable {
    let month: [WorkersCpuGroup]?
    let today: [WorkersCpuGroup]?
}

nonisolated struct WorkersCpuGroup: Codable, Sendable {
    let sum: WorkersCpuSum?
}

nonisolated struct WorkersCpuSum: Codable, Sendable {
    let cpuTimeUs: Double?
}

// MARK: - 响应

nonisolated struct AccountUsageData: Codable, Sendable {
    let viewer: AccountUsageViewer
}

nonisolated struct AccountUsageViewer: Codable, Sendable {
    let accounts: [AccountUsageNode]
}

nonisolated struct AccountUsageNode: Codable, Sendable {
    let month: [WorkersUsageGroup]?
    let today: [WorkersUsageGroup]?
}

// MARK: - R2 用量响应（独立查询，复用 R2OpsGroup / R2StorageGroup）

nonisolated struct R2UsageData: Codable, Sendable {
    let viewer: R2UsageViewer
}

nonisolated struct R2UsageViewer: Codable, Sendable {
    let accounts: [R2UsageNode]
}

nonisolated struct R2UsageNode: Codable, Sendable {
    let r2Ops:     [R2OpsGroup]?
    let r2Storage: [R2StorageGroup]?
}

nonisolated struct WorkersUsageGroup: Codable, Sendable {
    let sum:       WorkersUsageSum?
    let quantiles: WorkersUsageQuantiles?
}

nonisolated struct WorkersUsageSum: Codable, Sendable {
    let requests:    Int?
    let errors:      Int?
    let subrequests: Int?
}

nonisolated struct WorkersUsageQuantiles: Codable, Sendable {
    let cpuTimeP50: Double?    // 微秒
    let cpuTimeP99: Double?
}

nonisolated struct R2OpsGroup: Codable, Sendable {
    let dimensions: R2OpsDimensions?
    let sum:        R2OpsSum?
}

nonisolated struct R2OpsDimensions: Codable, Sendable {
    let actionType: String?
    let bucketName: String?     // 按桶用量需要；账号级聚合忽略它，互不影响
}

nonisolated struct R2OpsSum: Codable, Sendable {
    let requests: Int?
}

nonisolated struct R2StorageGroup: Codable, Sendable {
    let dimensions: R2StorageDimensions?
    let max:        R2StorageMax?
}

nonisolated struct R2StorageDimensions: Codable, Sendable {
    let bucketName: String?
}

nonisolated struct R2StorageMax: Codable, Sendable {
    let payloadSize:  Int?
    let metadataSize: Int?
    let objectCount:  Int?
}

// MARK: - D1 用量（独立查询；date 维度只支持 Date 标量过滤）

nonisolated enum D1UsageQuery {

    static let text = """
    query ($accountTag: string!, $periodStart: Date!, $todayStart: Date!, $until: Date!) {
      viewer {
        accounts(filter: { accountTag: $accountTag }) {
          period: d1AnalyticsAdaptiveGroups(
            limit: 10000,
            filter: { date_geq: $periodStart, date_leq: $until }
          ) {
            sum { rowsRead rowsWritten readQueries writeQueries }
          }
          today: d1AnalyticsAdaptiveGroups(
            limit: 10000,
            filter: { date_geq: $todayStart, date_leq: $until }
          ) {
            sum { rowsRead rowsWritten readQueries writeQueries }
          }
        }
      }
    }
    """
}

nonisolated struct D1UsageVariables: Codable, Sendable {
    let accountTag:  String
    let periodStart: String   // yyyy-MM-dd（UTC）
    let todayStart:  String
    let until:       String
}

nonisolated struct D1UsageData: Codable, Sendable {
    let viewer: D1UsageViewer
}

nonisolated struct D1UsageViewer: Codable, Sendable {
    let accounts: [D1UsageNode]
}

nonisolated struct D1UsageNode: Codable, Sendable {
    let period: [D1UsageGroup]?
    let today:  [D1UsageGroup]?
}

nonisolated struct D1UsageGroup: Codable, Sendable {
    let sum: D1UsageSum?
}

nonisolated struct D1UsageSum: Codable, Sendable {
    let rowsRead:     Int?
    let rowsWritten:  Int?
    let readQueries:  Int?
    let writeQueries: Int?
}

/// D1 用量聚合
nonisolated struct D1Usage: Codable, Sendable {
    let rowsReadToday:     Int
    let rowsWrittenToday:  Int
    let rowsReadPeriod:    Int
    let rowsWrittenPeriod: Int
}

// MARK: - Worker 错误（通知检测用：单窗口 sum errors）

nonisolated enum WorkersErrorsQuery {

    static let text = """
    query ($accountTag: string!, $since: Time!, $until: Time!) {
      viewer {
        accounts(filter: { accountTag: $accountTag }) {
          window: workersInvocationsAdaptive(
            limit: 10000,
            filter: { datetime_geq: $since, datetime_leq: $until }
          ) {
            sum { errors }
          }
        }
      }
    }
    """
}

nonisolated struct WorkersErrorsVariables: Codable, Sendable {
    let accountTag: String
    let since:      String
    let until:      String
}

nonisolated struct WorkersErrorsData: Codable, Sendable {
    let viewer: WorkersErrorsViewer
}

nonisolated struct WorkersErrorsViewer: Codable, Sendable {
    let accounts: [WorkersErrorsNode]
}

nonisolated struct WorkersErrorsNode: Codable, Sendable {
    let window: [WorkersUsageGroup]?
}

// MARK: - KV 用量（独立查询；按 actionType 分组，date 标量过滤）

nonisolated enum KVUsageQuery {

    static let operations = """
    query ($accountTag: string!, $periodStart: Date!, $todayStart: Date!, $until: Date!) {
      viewer {
        accounts(filter: { accountTag: $accountTag }) {
          period: kvOperationsAdaptiveGroups(
            limit: 10000,
            filter: { date_geq: $periodStart, date_leq: $until }
          ) {
            dimensions { actionType }
            sum { requests }
          }
          today: kvOperationsAdaptiveGroups(
            limit: 10000,
            filter: { date_geq: $todayStart, date_leq: $until }
          ) {
            dimensions { actionType }
            sum { requests }
          }
        }
      }
    }
    """

    static let storage = """
    query ($accountTag: string!, $periodStart: Date!, $todayStart: Date!, $until: Date!) {
      viewer {
        accounts(filter: { accountTag: $accountTag }) {
          storage: kvStorageAdaptiveGroups(
            limit: 1000,
            filter: { date_geq: $todayStart, date_leq: $until }
          ) {
            dimensions { namespaceId }
            max { byteCount keyCount }
          }
        }
      }
    }
    """
}

nonisolated struct KVUsageData: Codable, Sendable {
    let viewer: KVUsageViewer
}

nonisolated struct KVUsageViewer: Codable, Sendable {
    let accounts: [KVUsageNode]
}

nonisolated struct KVUsageNode: Codable, Sendable {
    let period:  [KVOpsGroup]?
    let today:   [KVOpsGroup]?
    let storage: [KVStorageGroup]?
}

nonisolated struct KVOpsGroup: Codable, Sendable {
    let dimensions: KVOpsDimensions?
    let sum:        R2OpsSum?           // 复用 { requests }
}

nonisolated struct KVOpsDimensions: Codable, Sendable {
    let actionType: String?             // "read" | "write" | "delete" | "list"
}

nonisolated struct KVStorageGroup: Codable, Sendable {
    let dimensions: KVStorageDimensions?
    let max:        KVStorageMax?
}

nonisolated struct KVStorageDimensions: Codable, Sendable {
    let namespaceId: String?
}

nonisolated struct KVStorageMax: Codable, Sendable {
    let byteCount: Int?
    let keyCount:  Int?
}

/// KV 用量聚合
nonisolated struct KVUsage: Codable, Sendable {
    let readsToday:   Int
    let writesToday:  Int
    let readsPeriod:  Int
    let writesPeriod: Int
}

// MARK: - 聚合结果

nonisolated struct AccountUsage: Codable, Sendable {
    let workersRequestsToday: Int
    let workersRequestsMonth: Int
    let workersErrorsMonth:   Int
    let cpuP50Us:             Double?   // 微秒（单次分位）
    let cpuP99Us:             Double?
    // CPU 总耗时（独立查询合并，schema 不支持时为 nil → 回退分位展示）
    var cpuTimeMonthUs:       Double?
    var cpuTimeTodayUs:       Double?
    // R2（独立查询合并，账号无 R2 / 无权限时保持 0，不影响 Workers 用量展示）
    var r2ClassAMonth:        Int = 0
    var r2ClassBMonth:        Int = 0
    // 存储优先用 REST /r2/metrics 覆盖（与 Dashboard 同源），GraphQL 采样兜底
    var r2StorageBytes:       Int = 0
    var r2ObjectCount:        Int = 0
    // D1（独立查询合并，不可用时为 nil → 行隐藏）
    var d1Usage:              D1Usage? = nil
    var d1StorageBytes:       Int? = nil
    // KV（独立查询合并）
    var kvUsage:              KVUsage? = nil
    var kvStorageBytes:       Int? = nil
}

// MARK: - R2 操作分类（来自 R2 计价文档）

nonisolated enum R2OperationClass {

    static let classA: Set<String> = [
        "ListBuckets", "PutBucket", "ListObjects", "PutObject", "CopyObject",
        "CompleteMultipartUpload", "CreateMultipartUpload", "UploadPart",
        "UploadPartCopy", "ListMultipartUploads", "ListParts",
        "PutBucketEncryption", "PutBucketCors", "PutBucketLifecycleConfiguration",
        "LifecycleStorageTierTransition",
    ]

    static let classB: Set<String> = [
        "HeadBucket", "HeadObject", "GetObject", "UsageSummary",
        "GetBucketEncryption", "GetBucketLocation", "GetBucketCors",
        "GetBucketLifecycleConfiguration",
    ]
    // 其余（DeleteObject / DeleteBucket / AbortMultipartUpload 等）免费，不计入
}
