//
//  HealthCheckService.swift
//  Orange Cloud
//
//  独立健康检查（healthcheck.read / .write）。免费版不可用。
//

import Foundation

struct HealthCheckService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    /// 该 Zone 的全部健康检查（页码分页）
    func list(zoneId: String) async throws -> [HealthCheck] {
        var checks: [HealthCheck] = []
        var page = 1
        while true {
            let response: CFAPIResponseArray<HealthCheck> = try await client.get(
                "zones/\(zoneId)/healthchecks",
                queryItems: [
                    URLQueryItem(name: "page",     value: String(page)),
                    URLQueryItem(name: "per_page", value: "50"),
                ]
            )
            guard response.success else {
                throw response.toAPIError()
            }
            checks.append(contentsOf: response.result ?? [])
            let totalPages = response.resultInfo?.totalPages ?? 1
            guard page < totalPages else { break }
            page += 1
        }
        return checks
    }

    /// 暂停 / 恢复。暂停后 CF 不再向源站发送检查。
    func setSuspended(zoneId: String, checkId: String, suspended: Bool) async throws -> HealthCheck {
        let response: CFAPIResponse<HealthCheck> = try await client.patch(
            "zones/\(zoneId)/healthchecks/\(checkId)",
            body: HealthCheckSuspendUpdate(suspended: suspended)
        )
        guard response.success, let result = response.result else {
            throw response.toAPIError()
        }
        return result
    }

    func delete(zoneId: String, checkId: String) async throws {
        try await client.delete("zones/\(zoneId)/healthchecks/\(checkId)")
    }
}
