//
//  RequestTracerService.swift
//  Orange Cloud
//
//  Cloudflare Trace（request-tracer.read）。全套餐可用，
//  但**要求 Administrator / Super Administrator 角色**——非管理员成员会失败。
//

import Foundation

struct RequestTracerService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    func trace(
        accountId: String,
        method: String,
        url: String,
        protocolVersion: String = "HTTP/1.1"
    ) async throws -> TraceResult {
        let response: CFAPIResponse<TraceResult> = try await client.post(
            "accounts/\(accountId)/request-tracer/trace",
            body: TraceRequest(method: method, url: url, protocolVersion: protocolVersion)
        )
        guard response.success, let result = response.result else {
            throw response.toAPIError()
        }
        return result
    }
}
