//
//  AuditLogService.swift
//  Orange Cloud
//
//  账号审计日志（Audit Logs v2）只读查询，游标分页。
//  since / before 为必填时间窗；direction=desc 取最近在前。
//

import Foundation

struct AuditLogService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    /// 拉取一页审计日志。cursor 为空取首页，非空续接下一页。
    func list(
        accountId: String,
        since: Date,
        before: Date,
        cursor: String?,
        limit: Int = 50
    ) async throws -> AuditLogPage {
        var query: [URLQueryItem] = [
            .init(name: "since",     value: since.ISO8601Format()),
            .init(name: "before",    value: before.ISO8601Format()),
            .init(name: "limit",     value: String(limit)),
            .init(name: "direction", value: "desc"),
        ]
        if let cursor, !cursor.isEmpty {
            query.append(.init(name: "cursor", value: cursor))
        }
        let page: AuditLogPage = try await client.get(
            "accounts/\(accountId)/logs/audit",
            queryItems: query
        )
        guard page.success else {
            throw page.toAPIError()
        }
        return page
    }
    // MARK: - 资源变更历史（2026-07-27 上线）

    /// 某条审计日志所对应资源的完整变更序列。
    ///
    /// 接口先用 id + action_time 定位源日志、从中推出资源标识，再回查同一资源的其它日志，
    /// 所以 **action_time 必须传**（用于收窄查找窗口），否则定位不到。
    ///
    /// result_info.history_status 表示识别质量，要如实透传给用户：
    /// · exact —— 用资源 URI 精确识别
    /// · approximate —— 没有资源 URI，靠其它字段近似匹配，可能混入无关条目
    /// · unavailable —— 源日志信息不足，返回空列表（不是「没有变更」）
    func resourceHistory(
        accountId: String,
        entryId: String,
        actionTime: Date,
        since: Date,
        before: Date,
        limit: Int = 50
    ) async throws -> AuditLogPage {
        let query: [URLQueryItem] = [
            .init(name: "action_time", value: actionTime.ISO8601Format()),
            .init(name: "since",       value: since.ISO8601Format()),
            .init(name: "before",      value: before.ISO8601Format()),
            .init(name: "limit",       value: String(limit)),
            .init(name: "direction",   value: "desc"),
        ]
        let page: AuditLogPage = try await client.get(
            "accounts/\(accountId)/logs/audit/\(entryId)/history",
            queryItems: query
        )
        guard page.success else {
            throw page.toAPIError()
        }
        return page
    }

}
