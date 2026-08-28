//
//  AccountService.swift
//  Orange Cloud
//

import Foundation

struct AccountService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    /// 获取当前用户有权访问的账号列表
    func listAccounts() async throws -> [Account] {
        let response: CFAPIResponseArray<Account> = try await client.get("accounts")
        guard response.success else {
            throw response.toAPIError()
        }
        return response.result ?? []
    }

    /// 账号订阅列表（best-effort：OAuth 目录无 billing scope，预期 403，调用方需降级）
    func listSubscriptions(accountId: String) async throws -> [AccountSubscription] {
        let response: CFAPIResponseArray<AccountSubscription> = try await client.get(
            "accounts/\(accountId)/subscriptions"
        )
        guard response.success else {
            throw response.toAPIError()
        }
        return response.result ?? []
    }
}
