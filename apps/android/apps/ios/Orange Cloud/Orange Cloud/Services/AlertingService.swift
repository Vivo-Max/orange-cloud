//
//  AlertingService.swift
//  Orange Cloud
//
//  Cloudflare Alerting / Notifications（account 级）。读 notifications.read，写 notifications.write。
//

import Foundation

struct AlertingService {

    private let client: CFAPIClient

    init(client: CFAPIClient) { self.client = client }

    /// 可用告警类型（result 是 {分类名: [告警]}）
    func availableAlerts(accountId: String) async throws -> [String: [CFAvailableAlert]] {
        let response: CFAPIResponse<[String: [CFAvailableAlert]]> = try await client.get(
            "accounts/\(accountId)/alerting/v3/available_alerts"
        )
        guard response.success else { throw response.toAPIError() }
        return response.result ?? [:]
    }

    // MARK: Webhook destinations

    func webhooks(accountId: String) async throws -> [CFWebhookDestination] {
        let response: CFAPIResponseArray<CFWebhookDestination> = try await client.get(
            "accounts/\(accountId)/alerting/v3/destinations/webhooks"
        )
        guard response.success else { throw response.toAPIError() }
        return response.result ?? []
    }

    func createWebhook(accountId: String, name: String, url: String) async throws -> String {
        let response: CFAPIResponse<CFAlertingIDResult> = try await client.post(
            "accounts/\(accountId)/alerting/v3/destinations/webhooks",
            body: CFWebhookCreate(name: name, url: url)
        )
        guard response.success, let id = response.result?.id else { throw response.toAPIError() }
        return id
    }

    func deleteWebhook(accountId: String, id: String) async throws {
        try await client.delete("accounts/\(accountId)/alerting/v3/destinations/webhooks/\(id)")
    }

    // MARK: Policies

    func policies(accountId: String) async throws -> [CFAlertPolicy] {
        let response: CFAPIResponseArray<CFAlertPolicy> = try await client.get(
            "accounts/\(accountId)/alerting/v3/policies"
        )
        guard response.success else { throw response.toAPIError() }
        return response.result ?? []
    }

    func createPolicy(accountId: String, name: String, alertType: String, webhookId: String) async throws -> String {
        let body = CFAlertPolicyCreate(
            name: name,
            alertType: alertType,
            enabled: true,
            mechanisms: CFAlertMechanisms(webhooks: [CFMechanismRef(id: webhookId)], email: nil),
            filters: [:]
        )
        let response: CFAPIResponse<CFAlertingIDResult> = try await client.post(
            "accounts/\(accountId)/alerting/v3/policies", body: body
        )
        guard response.success, let id = response.result?.id else { throw response.toAPIError() }
        return id
    }

    func deletePolicy(accountId: String, id: String) async throws {
        try await client.delete("accounts/\(accountId)/alerting/v3/policies/\(id)")
    }
}
