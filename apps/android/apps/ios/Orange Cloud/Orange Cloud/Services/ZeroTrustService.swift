//
//  ZeroTrustService.swift
//  Orange Cloud
//
//  Zero Trust 只读：Access 应用列表 + Gateway 策略列表（账号级）。
//

import Foundation

struct ZeroTrustService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    /// Access 应用列表（access.read）
    func accessApps(accountId: String) async throws -> [AccessApp] {
        let response: CFAPIResponseArray<AccessApp> = try await client.get(
            "accounts/\(accountId)/access/apps"
        )
        guard response.success else {
            throw response.toAPIError()
        }
        return response.result ?? []
    }

    /// 单个 Access 应用详情（含完整 policies 规则，编辑前取）
    func accessApp(accountId: String, appId: String) async throws -> AccessApp {
        let response: CFAPIResponse<AccessApp> = try await client.get(
            "accounts/\(accountId)/access/apps/\(appId)"
        )
        guard response.success, let app = response.result else { throw response.toAPIError() }
        return app
    }

    // MARK: - Access 写入（access.write）

    func createAccessPolicy(accountId: String, body: AccessPolicyInput) async throws -> AccessPolicy {
        let response: CFAPIResponse<AccessPolicy> = try await client.post(
            "accounts/\(accountId)/access/policies", body: body
        )
        guard response.success, let policy = response.result else { throw response.toAPIError() }
        return policy
    }

    /// 更新可复用策略（PATCH 替换 name/decision/include）
    func updateAccessPolicy(accountId: String, policyId: String, body: AccessPolicyInput) async throws {
        let response: CFAPIResponse<AccessPolicy> = try await client.patch(
            "accounts/\(accountId)/access/policies/\(policyId)", body: body
        )
        guard response.success else { throw response.toAPIError() }
    }

    func deleteAccessPolicy(accountId: String, policyId: String) async throws {
        try await client.delete("accounts/\(accountId)/access/policies/\(policyId)")
    }

    func createAccessApp(accountId: String, body: AccessAppInput) async throws -> AccessApp {
        let response: CFAPIResponse<AccessApp> = try await client.post(
            "accounts/\(accountId)/access/apps", body: body
        )
        guard response.success, let app = response.result else { throw response.toAPIError() }
        return app
    }

    func updateAccessApp(accountId: String, appId: String, body: AccessAppInput) async throws -> AccessApp {
        let response: CFAPIResponse<AccessApp> = try await client.put(
            "accounts/\(accountId)/access/apps/\(appId)", body: body
        )
        guard response.success, let app = response.result else { throw response.toAPIError() }
        return app
    }

    func deleteAccessApp(accountId: String, appId: String) async throws {
        try await client.delete("accounts/\(accountId)/access/apps/\(appId)")
    }

    /// Gateway 策略列表（teams.read）
    func gatewayRules(accountId: String) async throws -> [GatewayRule] {
        let response: CFAPIResponseArray<GatewayRule> = try await client.get(
            "accounts/\(accountId)/gateway/rules"
        )
        guard response.success else {
            throw response.toAPIError()
        }
        return response.result ?? []
    }

    /// 新建 Gateway 策略（teams.write）
    func createGatewayRule(accountId: String, body: GatewayRuleInput) async throws -> GatewayRule {
        let response: CFAPIResponse<GatewayRule> = try await client.post(
            "accounts/\(accountId)/gateway/rules", body: body
        )
        guard response.success, let rule = response.result else { throw response.toAPIError() }
        return rule
    }

    /// 更新 Gateway 策略（PUT 全量替换，teams.write）
    func updateGatewayRule(accountId: String, ruleId: String, body: GatewayRuleInput) async throws -> GatewayRule {
        let response: CFAPIResponse<GatewayRule> = try await client.put(
            "accounts/\(accountId)/gateway/rules/\(ruleId)", body: body
        )
        guard response.success, let rule = response.result else { throw response.toAPIError() }
        return rule
    }

    /// 删除 Gateway 策略（teams.write）
    func deleteGatewayRule(accountId: String, ruleId: String) async throws {
        try await client.delete("accounts/\(accountId)/gateway/rules/\(ruleId)")
    }
}
