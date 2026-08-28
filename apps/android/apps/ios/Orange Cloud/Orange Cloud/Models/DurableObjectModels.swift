
// MARK: - 内存用量（GraphQL durableObjectsPeriodicGroups）

/// DO 的 V8 isolate 内存分位数（字节）。
///
/// 注意口径：**按 isolate 计，不是按单个 Durable Object**。一个 isolate 可能同时承载
/// 同类的多个 DO 与外围 Worker 代码，图上永远是整个 isolate 的内存。
/// 每个 isolate 受 128 MB 限制。
nonisolated struct DurableObjectMemory: Sendable {
    let p50:  Double
    let p90:  Double
    let p99:  Double
    let p999: Double

    /// 128 MB isolate 上限，用于算占比
    static let isolateLimitBytes: Double = 128 * 1024 * 1024

    var p99Ratio: Double { min(p99 / Self.isolateLimitBytes, 1) }
}

// GraphQL 解码结构

nonisolated struct DOMemoryData: Codable, Sendable {
    let viewer: DOMemoryViewer
}

nonisolated struct DOMemoryViewer: Codable, Sendable {
    let accounts: [DOMemoryAccount]
}

nonisolated struct DOMemoryAccount: Codable, Sendable {
    let periodic: [DOMemoryGroup]?

    enum CodingKeys: String, CodingKey {
        case periodic = "durableObjectsPeriodicGroups"
    }
}

nonisolated struct DOMemoryGroup: Codable, Sendable {
    let quantiles: DOMemoryQuantiles?
}

nonisolated struct DOMemoryQuantiles: Codable, Sendable {
    let p50:  Double?
    let p90:  Double?
    let p99:  Double?
    let p999: Double?

    enum CodingKeys: String, CodingKey {
        case p50  = "memoryUsageBytesP50"
        case p90  = "memoryUsageBytesP90"
        case p99  = "memoryUsageBytesP99"
        case p999 = "memoryUsageBytesP999"
    }
}

nonisolated enum DOMemoryQuery {
    /// 按 namespace 聚合的内存分位数。scriptName 维度用来过滤到具体 namespace。
    static let text = """
    query DOMemory($accountTag: String!, $namespaceId: String!, $since: Time!, $until: Time!) {
      viewer {
        accounts(filter: { accountTag: $accountTag }) {
          durableObjectsPeriodicGroups(
            limit: 1
            filter: { datetime_geq: $since, datetime_leq: $until, namespaceId: $namespaceId }
          ) {
            quantiles {
              memoryUsageBytesP50
              memoryUsageBytesP90
              memoryUsageBytesP99
              memoryUsageBytesP999
            }
          }
        }
      }
    }
    """
}

nonisolated struct DOMemoryVariables: Codable, Sendable {
    let accountTag:  String
    let namespaceId: String
    let since:       String
    let until:       String
}
