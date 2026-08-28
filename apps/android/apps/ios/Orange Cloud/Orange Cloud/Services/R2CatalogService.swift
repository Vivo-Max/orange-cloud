//
//  R2CatalogService.swift
//  Orange Cloud
//
//  R2 Data Catalog（r2-catalog.read / .write）。
//

import Foundation

struct R2CatalogService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    /// 单桶的目录详情。未启用时接口 404，按「未启用」处理而非报错。
    func catalog(accountId: String, bucketName: String) async throws -> R2Catalog? {
        do {
            let response: CFAPIResponse<R2Catalog> = try await client.get(
                "accounts/\(accountId)/r2-catalog/\(bucketName)"
            )
            guard response.success else {
                throw response.toAPIError()
            }
            return response.result
        } catch APIError.notFound {
            return nil
        }
    }

    /// 启用为 Iceberg 目录。**这是计费动作**（$9/百万次目录操作），调用方需先确认。
    func enable(accountId: String, bucketName: String) async throws -> R2Catalog? {
        let response: CFAPIResponse<R2Catalog> = try await client.post(
            "accounts/\(accountId)/r2-catalog/\(bucketName)/enable",
            body: EmptyBody()
        )
        guard response.success else {
            throw response.toAPIError()
        }
        return response.result
    }

    /// 停用目录。数据保留，只是不再作为 Iceberg 目录对外提供。
    func disable(accountId: String, bucketName: String) async throws {
        let response: CFAPIResponse<R2Catalog> = try await client.post(
            "accounts/\(accountId)/r2-catalog/\(bucketName)/disable",
            body: EmptyBody()
        )
        guard response.success else {
            throw response.toAPIError()
        }
    }

    func namespaces(accountId: String, bucketName: String) async throws -> [R2CatalogNamespace] {
        let response: CFAPIResponse<R2CatalogNamespaceList> = try await client.get(
            "accounts/\(accountId)/r2-catalog/\(bucketName)/namespaces",
            queryItems: [URLQueryItem(name: "page_size", value: "100")]
        )
        guard response.success else {
            throw response.toAPIError()
        }
        return response.result?.namespaces ?? []
    }

    /// 某命名空间下的表清单（嵌套命名空间按 Iceberg 惯例以 . 连接进路径）
    func tables(accountId: String, bucketName: String, namespace: String) async throws -> [R2CatalogTableIdentifier] {
        let response: CFAPIResponse<R2CatalogTableList> = try await client.get(
            "accounts/\(accountId)/r2-catalog/\(bucketName)/namespaces/\(namespace)/tables",
            queryItems: [URLQueryItem(name: "page_size", value: "100")]
        )
        guard response.success else {
            throw response.toAPIError()
        }
        return response.result?.identifiers ?? []
    }
}

/// enable / disable 是无体 POST，但 CFAPIClient.post 要求 Encodable body
nonisolated struct EmptyBody: Codable, Sendable {}
