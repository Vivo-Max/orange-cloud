//
//  D1Service.swift
//  Orange Cloud
//

import Foundation

struct D1Service {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    /// 数据库列表（页码分页）
    func listDatabases(accountId: String) async throws -> [D1Database] {
        var databases: [D1Database] = []
        var page = 1
        while true {
            let response: CFAPIResponseArray<D1Database> = try await client.get(
                "accounts/\(accountId)/d1/database",
                queryItems: [
                    URLQueryItem(name: "page",     value: String(page)),
                    URLQueryItem(name: "per_page", value: "100"),
                ]
            )
            guard response.success else {
                throw response.toAPIError()
            }
            databases.append(contentsOf: response.result ?? [])
            let totalPages = response.resultInfo?.totalPages ?? 1
            guard page < totalPages else { break }
            page += 1
        }
        return databases
    }

    /// 数据库详情。列表端点不返回 file_size / num_tables 的真实值（常年 0），
    /// 这两个字段以详情端点为准。
    func getDatabase(accountId: String, databaseId: String) async throws -> D1Database {
        let response: CFAPIResponse<D1Database> = try await client.get(
            "accounts/\(accountId)/d1/database/\(databaseId)"
        )
        guard response.success, let result = response.result else {
            throw response.toAPIError()
        }
        return result
    }

    /// 创建数据库（POST 返回新建的 D1Database）。locationHint 为空走自动放置。
    func createDatabase(accountId: String, name: String, locationHint: String? = nil) async throws -> D1Database {
        let response: CFAPIResponse<D1Database> = try await client.post(
            "accounts/\(accountId)/d1/database",
            body: D1CreateRequest(name: name, primaryLocationHint: locationHint)
        )
        guard response.success, let result = response.result else {
            throw response.toAPIError()
        }
        return result
    }

    /// 删除数据库（DELETE，连同全部表与数据，不可恢复）。
    func deleteDatabase(accountId: String, databaseId: String) async throws {
        try await client.delete("accounts/\(accountId)/d1/database/\(databaseId)")
    }

    /// 执行 SQL（result 是 [D1QueryResult]，每条语句一个结果）。params 为参数化占位符值。
    func query(
        accountId: String,
        databaseId: String,
        sql: String,
        params: [String]? = nil
    ) async throws -> [D1QueryResult] {
        let response: CFAPIResponse<[D1QueryResult]> = try await client.post(
            "accounts/\(accountId)/d1/database/\(databaseId)/query",
            body: D1QueryRequest(sql: sql, params: params)
        )
        guard response.success, let results = response.result else {
            throw response.toAPIError()
        }
        return results
    }
}
