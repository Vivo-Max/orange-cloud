//
//  WorkerTailModels.swift
//  Orange Cloud
//
//  Workers 实时日志（tail）相关模型。
//  - TailSession 来自 REST（snake_case）
//  - TailTraceItem 来自 trace-v1 WebSocket 帧（camelCase！与 REST 不同）
//

import Foundation

/// POST /accounts/{id}/workers/scripts/{name}/tails 的返回
nonisolated struct TailSession: Codable, Sendable {
    let id:        String
    let url:       String          // 预签名 wss://，连接无需 Bearer
    let expiresAt: String?

    enum CodingKeys: String, CodingKey {
        case id, url
        case expiresAt = "expires_at"
    }
}

/// trace-v1 单条事件（字段为 camelCase，不要加 snake_case CodingKeys）
nonisolated struct TailTraceItem: Codable, Sendable {
    let outcome:        String?          // "ok" | "exception" | "exceededCpu" ...
    let scriptName:     String?
    let eventTimestamp: Int?             // 毫秒
    let event:          TailEventInfo?
    let logs:           [TailLog]?
    let exceptions:     [TailException]?
}

/// 触发事件信息：HTTP 请求或 cron
nonisolated struct TailEventInfo: Codable, Sendable {
    let request: TailRequestInfo?
    let cron:    String?
}

nonisolated struct TailRequestInfo: Codable, Sendable {
    let url:    String?
    let method: String?
}

nonisolated struct TailLog: Codable, Sendable {
    let level:     String                // "log" | "warn" | "error" | "debug" | "info"
    let timestamp: Int?
    let message:   [JSONValue]?          // console.log 的参数是任意 JSON 数组
}

nonisolated struct TailException: Codable, Sendable {
    let name:      String?
    let message:   String?
    let timestamp: Int?
}

// MARK: - 任意 JSON 值

/// console.log 参数可以是任意 JSON，用轻量枚举承载并提供展示文本
nonisolated indirect enum JSONValue: Codable, Sendable {
    case string(String)
    case number(Double)
    case bool(Bool)
    case null
    case array([JSONValue])
    case object([String: JSONValue])

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if container.decodeNil() {
            self = .null
        } else if let value = try? container.decode(Bool.self) {
            self = .bool(value)
        } else if let value = try? container.decode(Double.self) {
            self = .number(value)
        } else if let value = try? container.decode(String.self) {
            self = .string(value)
        } else if let value = try? container.decode([JSONValue].self) {
            self = .array(value)
        } else if let value = try? container.decode([String: JSONValue].self) {
            self = .object(value)
        } else {
            throw DecodingError.dataCorruptedError(in: container, debugDescription: "无法识别的 JSON 值")
        }
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        switch self {
        case .string(let value): try container.encode(value)
        case .number(let value): try container.encode(value)
        case .bool(let value):   try container.encode(value)
        case .null:              try container.encodeNil()
        case .array(let value):  try container.encode(value)
        case .object(let value): try container.encode(value)
        }
    }

    /// 日志行展示文本
    var displayText: String {
        switch self {
        case .string(let value):
            return value
        case .number(let value):
            return value.truncatingRemainder(dividingBy: 1) == 0
                ? String(Int(value))
                : String(value)
        case .bool(let value):
            return value ? "true" : "false"
        case .null:
            return "null"
        case .array(let values):
            return "[" + values.map(\.displayText).joined(separator: ", ") + "]"
        case .object(let dict):
            let pairs = dict.sorted { $0.key < $1.key }
                .map { "\($0.key): \($0.value.displayText)" }
            return "{" + pairs.joined(separator: ", ") + "}"
        }
    }
}
