//
//  R2SQLService.swift
//  Orange Cloud
//
//  R2 SQL：对 R2 Data Catalog（Iceberg）表跑只读分析查询。
//
//  查询端点在独立主机 api.sql.cloudflarestorage.com（**不是** client/v4）：
//  POST /api/v1/accounts/{account_id}/r2-sql/query/{bucket_name}  body {"query": "..."}
//  鉴权是 Cloudflare Bearer token（需 r2-catalog-sql.read + r2-catalog.read + workers-r2.read）。
//  该主机对 OAuth token 的接受度未见官方明说，401/403 时由 ViewModel 给出友好指引。
//
//  响应格式官方未列 schema，按宽容策略解析：在常见容器键（result/data/rows…）下
//  找「对象数组」当作行集，列序优先取首行键序。**计费提醒**：按扫描量计费
//  （$0.0025/GB，10GB/月免费，单次查询最少计 10MB），调用方 UI 需带提示。
//

import Foundation

struct R2SQLService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    /// 执行一条 R2 SQL 查询，返回展平后的行集
    func query(accountId: String, bucketName: String, sql: String) async throws -> R2SQLResult {
        guard let url = URL(string:
            "https://api.sql.cloudflarestorage.com/api/v1/accounts/\(accountId)/r2-sql/query/\(bucketName)"
        ) else {
            throw APIError.networkError(URLError(.badURL))
        }
        let data = try await client.postExternalJSON(url: url, body: R2SQLQueryRequest(query: sql))
        return try Self.parse(data)
    }

    // MARK: - 宽容解析

    /// 从响应 JSON 里挖出行集：顶层或 result/data 容器下的第一个「对象数组」。
    static func parse(_ data: Data) throws -> R2SQLResult {
        let object: Any
        do {
            object = try JSONSerialization.jsonObject(with: data)
        } catch {
            throw APIError.decodingError(error)
        }

        // 顶层直接就是数组
        if let rows = object as? [[String: Any]] {
            return flatten(rows)
        }

        guard let dict = object as? [String: Any] else {
            throw APIError.decodingError(DecodingError.dataCorrupted(
                .init(codingPath: [], debugDescription: "R2 SQL 响应不是 JSON 对象")
            ))
        }

        // CF 信封形态：success=false 时抛出首个错误
        if let success = dict["success"] as? Bool, !success {
            let errors = dict["errors"] as? [[String: Any]]
            let message = errors?.first?["message"] as? String ?? String(localized: "查询失败")
            let code = errors?.first?["code"] as? Int ?? 0
            throw APIError.cloudflareError(code: code, message: message)
        }

        // 常见容器键逐层找对象数组（最多两层，避免误挖嵌套数据）
        let containerKeys = ["result", "data", "rows", "results", "records"]
        var candidates: [Any] = [dict]
        for key in containerKeys {
            if let nested = dict[key] { candidates.append(nested) }
            if let inner = dict["result"] as? [String: Any], let nested = inner[key] {
                candidates.append(nested)
            }
        }
        for candidate in candidates {
            if let rows = candidate as? [[String: Any]] {
                return flatten(rows)
            }
        }
        // 无行集但也没报错：当作执行成功、零行
        return R2SQLResult(columns: [], rows: [])
    }

    /// 对象数组 → 列名 + 字符串矩阵（列序优先按首行键序，缺失键补空）
    private static func flatten(_ rows: [[String: Any]]) -> R2SQLResult {
        guard !rows.isEmpty else { return R2SQLResult(columns: [], rows: []) }
        // 首行键序不可得（字典无序），统一按字母序，稳定可预期
        var columnSet = Set<String>()
        for row in rows { columnSet.formUnion(row.keys) }
        let columns = columnSet.sorted()
        let matrix = rows.map { row in
            columns.map { display(row[$0]) }
        }
        return R2SQLResult(columns: columns, rows: matrix)
    }

    private static func display(_ value: Any?) -> String {
        switch value {
        case nil, is NSNull:      return "NULL"
        case let s as String:     return s
        case let n as NSNumber:
            return CFGetTypeID(n) == CFBooleanGetTypeID() ? (n.boolValue ? "true" : "false") : n.stringValue
        case let other:
            // 嵌套对象/数组压成紧凑 JSON
            if let data = try? JSONSerialization.data(withJSONObject: other, options: [.fragmentsAllowed]),
               let text = String(data: data, encoding: .utf8) {
                return text
            }
            return String(describing: other)
        }
    }
}

// MARK: - 模型

nonisolated struct R2SQLQueryRequest: Codable, Sendable {
    let query: String
}

/// 展平后的查询结果（值统一字符串化用于展示 / 导出）
nonisolated struct R2SQLResult: Sendable {
    let columns: [String]
    let rows:    [[String]]
}
