//
//  AIGatewayService.swift
//  Orange Cloud
//
//  Cloudflare AI Gateway（account 级）。读 aig.read，写 aig.write。
//

import Foundation

struct AIGatewayService {

    private let client: CFAPIClient

    init(client: CFAPIClient) { self.client = client }

    func list(accountId: String) async throws -> [AIGateway] {
        let response: CFAPIResponseArray<AIGateway> = try await client.get(
            "accounts/\(accountId)/ai-gateway/gateways",
            queryItems: [URLQueryItem(name: "per_page", value: "50")]
        )
        guard response.success else { throw response.toAPIError() }
        return response.result ?? []
    }

    func create(accountId: String, body: AIGatewayCreate) async throws -> AIGateway {
        let response: CFAPIResponse<AIGateway> = try await client.post(
            "accounts/\(accountId)/ai-gateway/gateways", body: body
        )
        guard response.success, let gateway = response.result else { throw response.toAPIError() }
        return gateway
    }

    func delete(accountId: String, gatewayId: String) async throws {
        try await client.delete("accounts/\(accountId)/ai-gateway/gateways/\(gatewayId)")
    }
}
