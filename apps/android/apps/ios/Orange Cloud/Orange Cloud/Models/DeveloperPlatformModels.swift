//
//  DeveloperPlatformModels.swift
//  Orange Cloud
//
//  开发者平台模块数据模型：Queues / AI Gateway / Durable Objects / Workers AI。
//  端点核对自 Cloudflare 官方 API 文档。
//

import Foundation

// MARK: - Queues（queues.read / .write）

/// GET /accounts/{id}/queues
nonisolated struct CFQueue: Codable, Identifiable, Sendable {
    let queueId:    String
    let queueName:  String?
    let createdOn:  String?
    let modifiedOn: String?
    let producers:  [CFQueueEndpoint]?
    let consumers:  [CFQueueEndpoint]?
    let producersTotalCount: Int?
    let consumersTotalCount: Int?
    let settings:   CFQueueSettings?

    var id: String { queueId }
    var name: String { queueName ?? queueId }

    enum CodingKeys: String, CodingKey {
        case producers, consumers, settings
        case queueId    = "queue_id"
        case queueName  = "queue_name"
        case createdOn  = "created_on"
        case modifiedOn = "modified_on"
        case producersTotalCount = "producers_total_count"
        case consumersTotalCount = "consumers_total_count"
    }
}

/// 队列投递设置（保留期 / 投递延迟 / 暂停态）
nonisolated struct CFQueueSettings: Codable, Sendable {
    let deliveryDelay:           Int?
    let deliveryPaused:          Bool?
    let messageRetentionPeriod:  Int?

    enum CodingKeys: String, CodingKey {
        case deliveryDelay          = "delivery_delay"
        case deliveryPaused         = "delivery_paused"
        case messageRetentionPeriod = "message_retention_period"
    }
}

nonisolated struct CFQueueEndpoint: Codable, Sendable {
    let type:            String?
    let script:          String?
    let settings:        CFQueueConsumerSettings?
    let deadLetterQueue: String?

    enum CodingKeys: String, CodingKey {
        case type, script, settings
        case deadLetterQueue = "dead_letter_queue"
    }
}

/// 消费者绑定的批处理 / 重试设置（仅详情展示，全可选）
nonisolated struct CFQueueConsumerSettings: Codable, Sendable {
    let batchSize:       Int?
    let maxRetries:      Int?
    let maxWaitTimeMs:   Int?
    let maxConcurrency:  Int?
    let retryDelay:      Int?

    enum CodingKeys: String, CodingKey {
        case batchSize      = "batch_size"
        case maxRetries     = "max_retries"
        case maxWaitTimeMs  = "max_wait_time_ms"
        case maxConcurrency = "max_concurrency"
        case retryDelay     = "retry_delay"
    }
}

/// POST /accounts/{id}/queues
nonisolated struct CFQueueCreate: Codable, Sendable {
    let queueName: String
    enum CodingKeys: String, CodingKey { case queueName = "queue_name" }
}

/// PUT /accounts/{id}/queues/{queue_id}（全字段可选，omit 即不改；合成 Encodable 对 nil 走 encodeIfPresent）
nonisolated struct CFQueueUpdate: Codable, Sendable {
    var queueName: String? = nil
    var settings:  CFQueueSettingsPatch? = nil

    enum CodingKeys: String, CodingKey {
        case queueName = "queue_name"
        case settings
    }
}

nonisolated struct CFQueueSettingsPatch: Codable, Sendable {
    var deliveryDelay:          Int?  = nil
    var deliveryPaused:         Bool? = nil
    var messageRetentionPeriod: Int?  = nil

    enum CodingKeys: String, CodingKey {
        case deliveryDelay          = "delivery_delay"
        case deliveryPaused         = "delivery_paused"
        case messageRetentionPeriod = "message_retention_period"
    }
}

/// POST /accounts/{id}/queues/{queue_id}/purge（清空全部消息，不可撤销）
nonisolated struct CFQueuePurge: Codable, Sendable {
    let deleteMessagesPermanently: Bool
    enum CodingKeys: String, CodingKey { case deleteMessagesPermanently = "delete_messages_permanently" }
}

// MARK: - AI Gateway（aig.read / .write）

/// GET /accounts/{id}/ai-gateway/gateways
nonisolated struct AIGateway: Codable, Identifiable, Sendable {
    let id:                     String
    let cacheTtl:               Int?
    let collectLogs:            Bool?
    let rateLimitingInterval:   Int?
    let rateLimitingLimit:      Int?
    let cacheInvalidateOnUpdate: Bool?
    let createdOn:              String?
    let modifiedOn:             String?

    enum CodingKeys: String, CodingKey {
        case id
        case cacheTtl                = "cache_ttl"
        case collectLogs             = "collect_logs"
        case rateLimitingInterval    = "rate_limiting_interval"
        case rateLimitingLimit       = "rate_limiting_limit"
        case cacheInvalidateOnUpdate = "cache_invalidate_on_update"
        case createdOn               = "created_on"
        case modifiedOn              = "modified_on"
    }
}

/// POST /accounts/{id}/ai-gateway/gateways（全字段必填）
nonisolated struct AIGatewayCreate: Codable, Sendable {
    let id:                      String
    let cacheInvalidateOnUpdate: Bool
    let cacheTtl:                Int
    let collectLogs:             Bool
    let rateLimitingInterval:    Int
    let rateLimitingLimit:       Int

    enum CodingKeys: String, CodingKey {
        case id
        case cacheInvalidateOnUpdate = "cache_invalidate_on_update"
        case cacheTtl                = "cache_ttl"
        case collectLogs             = "collect_logs"
        case rateLimitingInterval    = "rate_limiting_interval"
        case rateLimitingLimit       = "rate_limiting_limit"
    }
}

// MARK: - Durable Objects（只读，workers-scripts.read）

/// GET /accounts/{id}/workers/durable_objects/namespaces
nonisolated struct DurableObjectNamespace: Codable, Identifiable, Sendable {
    let id:         String
    let name:       String?
    let className:  String?
    let script:     String?
    let useSqlite:  Bool?

    enum CodingKeys: String, CodingKey {
        case id, name, script
        case className = "class"
        case useSqlite = "use_sqlite"
    }
}

/// GET /accounts/{id}/workers/durable_objects/namespaces/{id}/objects（游标分页，只读）
/// 字段名以官方文档为 `hasStoredData`，但 CF 多数端点是 snake_case——两种都试，避免解码失败。
nonisolated struct DurableObjectInstance: Codable, Identifiable, Sendable {
    let id:            String
    let hasStoredData: Bool?

    enum CodingKeys: String, CodingKey {
        case id
        case hasStoredData
        case hasStoredDataSnake = "has_stored_data"
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(String.self, forKey: .id)
        if let v = try? c.decode(Bool.self, forKey: .hasStoredData) {
            hasStoredData = v
        } else if let v = try? c.decode(Bool.self, forKey: .hasStoredDataSnake) {
            hasStoredData = v
        } else {
            hasStoredData = nil
        }
    }

    // 额外 CodingKey（snake 兜底）使 Encodable 无法自动合成，显式给出（实际只解码、不编码）
    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id, forKey: .id)
        try c.encodeIfPresent(hasStoredData, forKey: .hasStoredData)
    }
}

// MARK: - Workers AI（只读模型目录，ai.read）

/// GET /accounts/{id}/ai/models/search
nonisolated struct AIModel: Codable, Identifiable, Sendable {
    let id:          String
    let name:        String?
    let description: String?
    let task:        AITask?

    /// 模型短名（去掉 @cf/ 前缀的最后一段）
    var shortName: String { (name ?? id).split(separator: "/").last.map(String.init) ?? (name ?? id) }
    var taskName:  String { task?.name ?? "" }

    /// 文本生成类模型（chat / instruct）
    var isTextGen:  Bool { taskName == "Text Generation" }
    /// 文生图类模型（Cloudflare 目录里 task name 为 `Text-to-Image`）
    var isImageGen: Bool { taskName == "Text-to-Image" }
}

nonisolated struct AITask: Codable, Sendable {
    let id:   String?
    let name: String?
}

// MARK: - Workers AI 文本生成试运行（POST /accounts/{id}/ai/run/{model}，需 ai.read + ai.write）

nonisolated struct AIChatMessage: Codable, Sendable {
    let role:    String
    let content: String
}

/// 文生图请求体（各模型通用的最小集：只发 prompt，其余走模型默认值）
nonisolated struct AIImageGenRequest: Codable, Sendable {
    let prompt: String
}

/// 部分文生图模型不返回二进制，而是返回 CF 标准信封 + base64 图片（如 flux 系列）
nonisolated struct AIImageGenResult: Codable, Sendable {
    let image: String?
}

/// 单轮对话请求体（instruct / chat 类模型通用）
nonisolated struct AITextGenRequest: Codable, Sendable {
    let messages: [AIChatMessage]
}

/// run 返回标准信封的 result：文本生成在 `response` 字段
nonisolated struct AITextGenResult: Codable, Sendable {
    let response: String?
}

// MARK: - Hyperdrive（query-cache.read / .write）

/// GET /accounts/{id}/hyperdrive/configs（password 为写专用，响应永不返回）
nonisolated struct HyperdriveConfig: Codable, Identifiable, Sendable {
    let id:     String
    let name:   String?
    let origin: HyperdriveOrigin?
    let caching: HyperdriveCaching?
    let originConnectionLimit: Int?

    var displayName: String { name ?? id }

    enum CodingKeys: String, CodingKey {
        case id, name, origin, caching
        case originConnectionLimit = "origin_connection_limit"
    }
}

nonisolated struct HyperdriveOrigin: Codable, Sendable {
    let scheme:   String?
    let host:     String?
    let port:     Int?
    let database: String?
    let user:     String?

    var summary: String {
        let s = scheme ?? "postgres"
        let h = host ?? "—"
        let db = database.map { "/\($0)" } ?? ""
        return "\(s)://\(h)\(db)"
    }
}

nonisolated struct HyperdriveCaching: Codable, Sendable {
    let disabled:             Bool?
    let maxAge:               Int?
    let staleWhileRevalidate: Int?

    enum CodingKeys: String, CodingKey {
        case disabled
        case maxAge               = "max_age"
        case staleWhileRevalidate = "stale_while_revalidate"
    }
}

/// POST /accounts/{id}/hyperdrive/configs
nonisolated struct HyperdriveCreate: Codable, Sendable {
    let name:   String
    let origin: Origin

    nonisolated struct Origin: Codable, Sendable {
        let scheme:   String
        let host:     String
        let port:     Int
        let database: String
        let user:     String
        let password: String
    }
}

/// PATCH /accounts/{id}/hyperdrive/configs/{id}（全字段可选，omit 即不改）
nonisolated struct HyperdrivePatch: Codable, Sendable {
    var name:                  String? = nil
    var caching:               HyperdriveCachingPatch? = nil
    var origin:                HyperdriveCreate.Origin? = nil
    var originConnectionLimit: Int? = nil

    enum CodingKeys: String, CodingKey {
        case name, caching, origin
        case originConnectionLimit = "origin_connection_limit"
    }
}

nonisolated struct HyperdriveCachingPatch: Codable, Sendable {
    var disabled:             Bool? = nil
    var maxAge:               Int?  = nil
    var staleWhileRevalidate: Int?  = nil

    enum CodingKeys: String, CodingKey {
        case disabled
        case maxAge               = "max_age"
        case staleWhileRevalidate = "stale_while_revalidate"
    }
}

nonisolated enum HyperdriveScheme: String, CaseIterable, Identifiable, Sendable {
    case postgres, mysql
    var id: String { rawValue }
    var label: String { self == .postgres ? "PostgreSQL" : "MySQL" }
    var defaultPort: Int { self == .postgres ? 5432 : 3306 }
}
