//
//  RegistrarService.swift
//  Orange Cloud
//
//  Cloudflare Registrar（registrar-domains.read / .admin）。
//  用新版 /registrar/registrations —— 旧的 /registrar/domains 于 2026-09-27 停用。
//

import Foundation

struct RegistrarService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    /// 账号下已注册域名。该端点是**游标分页**（不是页码），cursor 为空串即到底。
    func registrations(accountId: String) async throws -> [DomainRegistration] {
        var all: [DomainRegistration] = []
        var cursor: String?
        // 兜底上限，防止服务端游标异常导致死循环
        for _ in 0..<20 {
            var items = [URLQueryItem(name: "per_page", value: "50")]
            if let cursor, !cursor.isEmpty {
                items.append(URLQueryItem(name: "cursor", value: cursor))
            }
            let response: CFAPIResponseArray<DomainRegistration> = try await client.get(
                "accounts/\(accountId)/registrar/registrations",
                queryItems: items
            )
            guard response.success else {
                throw response.toAPIError()
            }
            all.append(contentsOf: response.result ?? [])
            cursor = response.resultInfo?.cursor
            guard let cursor, !cursor.isEmpty else { break }
        }
        return all
    }

    /// 设置自动续费。返回的是异步 workflow 状态，调用方成功后应回读列表。
    func setAutoRenew(accountId: String, domainName: String, enabled: Bool) async throws {
        let response: CFAPIResponse<RegistrarWorkflowStatus> = try await client.patch(
            "accounts/\(accountId)/registrar/registrations/\(domainName)",
            body: RegistrationUpdate(autoRenew: enabled)
        )
        guard response.success else {
            throw response.toAPIError()
        }
    }
}

/// PATCH 的异步 workflow 结果，只取判定是否完成所需的字段
nonisolated struct RegistrarWorkflowStatus: Codable, Sendable {
    let completed: Bool?
    let state:     String?
}
