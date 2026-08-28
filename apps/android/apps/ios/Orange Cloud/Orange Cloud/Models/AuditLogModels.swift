//
//  AuditLogModels.swift
//  Orange Cloud
//
//  账号审计日志（Audit Logs v2）。
//  GET /accounts/{id}/logs/audit?since=&before=&cursor=&limit=&direction=
//  仅需 account-settings.read（账号模块必选 scope），无需新增授权。
//

import Foundation

/// 单条审计日志条目（v2 字段，全部可选——不同产品/动作填充的字段不一）
nonisolated struct AuditLogEntry: Codable, Sendable {
    let id:       String?
    let account:  AuditLogScopeRef?
    let action:   AuditLogAction?
    let actor:    AuditLogActor?
    let raw:      AuditLogRaw?
    let resource: AuditLogResource?
    let zone:     AuditLogScopeRef?

    /// action.time 解析为 Date（ISO8601 / RFC3339）。
    /// 用值类型 ISO8601FormatStyle（Sendable），避免静态 DateFormatter 的并发安全问题。
    var timestamp: Date? {
        guard let t = action?.time else { return nil }
        if let d = try? Date.ISO8601FormatStyle(includingFractionalSeconds: true).parse(t) {
            return d
        }
        return try? Date.ISO8601FormatStyle().parse(t)
    }

    /// action.result == "success" → true；明确失败 → false；缺省 → nil
    var succeeded: Bool? {
        guard let r = action?.result?.lowercased(), !r.isEmpty else { return nil }
        return r == "success"
    }
}

nonisolated struct AuditLogScopeRef: Codable, Sendable {
    let id:   String?
    let name: String?
}

nonisolated struct AuditLogAction: Codable, Sendable {
    let description: String?
    let result:      String?
    let time:        String?
    let type:        String?
}

nonisolated struct AuditLogActor: Codable, Sendable {
    let id:        String?
    let context:   String?   // api_key / api_token / dash / oauth / origin_ca_key
    let email:     String?
    let ipAddress: String?
    let type:      String?   // account / cloudflare_admin / system / user

    enum CodingKeys: String, CodingKey {
        case id, context, email, type
        case ipAddress = "ip_address"
    }
}

nonisolated struct AuditLogRaw: Codable, Sendable {
    let method:     String?
    let statusCode: Int?
    let uri:        String?

    enum CodingKeys: String, CodingKey {
        case method, uri
        case statusCode = "status_code"
    }
}

nonisolated struct AuditLogResource: Codable, Sendable {
    let id:      String?
    let product: String?
    let type:    String?
}

/// result_info 里我们只关心 cursor。
/// v2 的 result_info.count 是字符串——这里干脆不声明它，Codable 会忽略未知键，
/// 从根上绕开它与通用 ResultInfo(count: Int) 的类型冲突。
nonisolated struct AuditResultInfo: Codable, Sendable {
    let cursor: String?
    /// 仅资源变更历史返回：exact / approximate / unavailable。
    /// approximate 表示没拿到资源 URI、靠其它字段近似匹配，结果可能混入无关条目——必须告诉用户。
    let historyStatus: String?

    enum CodingKeys: String, CodingKey {
        case cursor
        case historyStatus = "history_status"
    }
}

/// 审计日志分页响应信封（合成 Codable；errors 设为可选以容忍 null/缺省）。
nonisolated struct AuditLogPage: Codable, Sendable {
    let result:     [AuditLogEntry]?
    let success:    Bool
    let errors:     [CFAPIError]?
    let resultInfo: AuditResultInfo?

    enum CodingKeys: String, CodingKey {
        case result, success, errors
        case resultInfo = "result_info"
    }

    var cursor: String? { resultInfo?.cursor }

    func toAPIError() -> APIError {
        let err = errors?.first
        return .cloudflareError(code: err?.code ?? 0,
                                message: err?.message ?? String(localized: "未知错误"))
    }
}

/// ForEach 用的稳定身份包装（条目 id 可能缺失或重复）
nonisolated struct IdentifiedAuditEntry: Identifiable, Sendable {
    let id = UUID()
    let entry: AuditLogEntry
}
