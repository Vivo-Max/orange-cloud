//
//  EmailRoutingService.swift
//  Orange Cloud
//
//  Email Routing：域名级设置/规则（email-routing-rule.*）+ 账号级目的地址（email-routing-address.*）。
//

import Foundation

struct EmailRoutingService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    // MARK: - 设置（域名级）

    /// 读 Email Routing 总设置（开关 + 状态）
    func settings(zoneId: String) async throws -> EmailRoutingSettings {
        let response: CFAPIResponse<EmailRoutingSettings> = try await client.get(
            "zones/\(zoneId)/email/routing"
        )
        guard response.success, let result = response.result else {
            throw response.toAPIError()
        }
        return result
    }

    /// 开启 / 关闭 Email Routing（空 body POST 到 enable/disable）
    func setEnabled(zoneId: String, enabled: Bool) async throws {
        let action = enabled ? "enable" : "disable"
        let response: CFAPIResponse<EmailRoutingSettings> = try await client.post(
            "zones/\(zoneId)/email/routing/\(action)",
            body: [String: String]()
        )
        guard response.success else {
            throw response.toAPIError()
        }
    }

    // MARK: - 规则（域名级）

    /// 列出路由规则（不含 catch-all）
    func rules(zoneId: String) async throws -> [EmailRoutingRule] {
        let response: CFAPIResponseArray<EmailRoutingRule> = try await client.get(
            "zones/\(zoneId)/email/routing/rules"
        )
        guard response.success else {
            throw response.toAPIError()
        }
        return response.result ?? []
    }

    /// 新建规则
    func createRule(zoneId: String, input: EmailRoutingRuleInput) async throws -> EmailRoutingRule {
        let response: CFAPIResponse<EmailRoutingRule> = try await client.post(
            "zones/\(zoneId)/email/routing/rules",
            body: input
        )
        guard response.success, let result = response.result else {
            throw response.toAPIError()
        }
        return result
    }

    /// 全量更新规则（PUT 需带完整 matchers/actions）
    func updateRule(zoneId: String, ruleId: String, input: EmailRoutingRuleInput) async throws -> EmailRoutingRule {
        let response: CFAPIResponse<EmailRoutingRule> = try await client.put(
            "zones/\(zoneId)/email/routing/rules/\(ruleId)",
            body: input
        )
        guard response.success, let result = response.result else {
            throw response.toAPIError()
        }
        return result
    }

    /// 删除规则
    func deleteRule(zoneId: String, ruleId: String) async throws {
        try await client.delete("zones/\(zoneId)/email/routing/rules/\(ruleId)")
    }

    // MARK: - 目的地址（账号级）

    /// 列出账号下全部目的地址（含未验证）
    func addresses(accountId: String) async throws -> [EmailDestinationAddress] {
        let response: CFAPIResponseArray<EmailDestinationAddress> = try await client.get(
            "accounts/\(accountId)/email/routing/addresses"
        )
        guard response.success else {
            throw response.toAPIError()
        }
        return response.result ?? []
    }

    /// 新增目的地址（提交后 Cloudflare 向该邮箱发验证信）
    func createAddress(accountId: String, email: String) async throws -> EmailDestinationAddress {
        let response: CFAPIResponse<EmailDestinationAddress> = try await client.post(
            "accounts/\(accountId)/email/routing/addresses",
            body: EmailDestinationCreate(email: email)
        )
        guard response.success, let result = response.result else {
            throw response.toAPIError()
        }
        return result
    }

    /// 删除目的地址
    func deleteAddress(accountId: String, addressId: String) async throws {
        try await client.delete("accounts/\(accountId)/email/routing/addresses/\(addressId)")
    }

    // MARK: - 抑制列表（email-routing-suppression.read / .write）

    /// 被抑制的收件地址。响应信封含 success/errors，与常规一致（另有 page/per_page 平级字段，忽略即可）。
    func suppressions(zoneId: String) async throws -> [EmailSuppression] {
        let response: CFAPIResponseArray<EmailSuppression> = try await client.get(
            "zones/\(zoneId)/email/routing/suppression",
            queryItems: [URLQueryItem(name: "per_page", value: "100")]
        )
        guard response.success else {
            throw response.toAPIError()
        }
        return response.result ?? []
    }

    /// 手动抑制一个地址（此后不再向它转发）
    func addSuppression(zoneId: String, email: String) async throws {
        let response: CFAPIResponse<EmailSuppression> = try await client.post(
            "zones/\(zoneId)/email/routing/suppression",
            body: EmailSuppressionCreate(email: email)
        )
        guard response.success else {
            throw response.toAPIError()
        }
    }

    /// 解除抑制
    func deleteSuppression(zoneId: String, id: String) async throws {
        try await client.delete("zones/\(zoneId)/email/routing/suppression/\(id)")
    }

}
