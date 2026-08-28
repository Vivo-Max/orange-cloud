//
//  LoadBalancerService.swift
//  Orange Cloud
//
//  负载均衡 CRUD：Load Balancer（zone 级）+ Pool / Monitor（account 级）+ 池健康。
//  端点核对自 Cloudflare 官方 SDK（cloudflare-python resources/load_balancers）。
//

import Foundation

struct LoadBalancerService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    // MARK: - Load Balancer（zone）

    func listLoadBalancers(zoneId: String) async throws -> [LoadBalancer] {
        let response: CFAPIResponse<[LoadBalancer]> = try await client.get(
            "zones/\(zoneId)/load_balancers"
        )
        guard response.success, let result = response.result else { throw response.toAPIError() }
        return result
    }

    func createLoadBalancer(zoneId: String, body: LoadBalancerUpdate) async throws -> LoadBalancer {
        let response: CFAPIResponse<LoadBalancer> = try await client.post(
            "zones/\(zoneId)/load_balancers", body: body
        )
        guard response.success, let result = response.result else { throw response.toAPIError() }
        return result
    }

    func updateLoadBalancer(zoneId: String, lbId: String, body: LoadBalancerUpdate) async throws -> LoadBalancer {
        let response: CFAPIResponse<LoadBalancer> = try await client.patch(
            "zones/\(zoneId)/load_balancers/\(lbId)", body: body
        )
        guard response.success, let result = response.result else { throw response.toAPIError() }
        return result
    }

    func deleteLoadBalancer(zoneId: String, lbId: String) async throws {
        try await client.delete("zones/\(zoneId)/load_balancers/\(lbId)")
    }

    // MARK: - 源站池 Pool（account）

    func listPools(accountId: String) async throws -> [Pool] {
        let response: CFAPIResponse<[Pool]> = try await client.get(
            "accounts/\(accountId)/load_balancers/pools"
        )
        guard response.success, let result = response.result else { throw response.toAPIError() }
        return result
    }

    func createPool(accountId: String, body: PoolUpdate) async throws -> Pool {
        let response: CFAPIResponse<Pool> = try await client.post(
            "accounts/\(accountId)/load_balancers/pools", body: body
        )
        guard response.success, let result = response.result else { throw response.toAPIError() }
        return result
    }

    func updatePool(accountId: String, poolId: String, body: PoolUpdate) async throws -> Pool {
        let response: CFAPIResponse<Pool> = try await client.patch(
            "accounts/\(accountId)/load_balancers/pools/\(poolId)", body: body
        )
        guard response.success, let result = response.result else { throw response.toAPIError() }
        return result
    }

    func deletePool(accountId: String, poolId: String) async throws {
        try await client.delete("accounts/\(accountId)/load_balancers/pools/\(poolId)")
    }

    func poolHealth(accountId: String, poolId: String) async throws -> PoolHealthResponse {
        let response: CFAPIResponse<PoolHealthResponse> = try await client.get(
            "accounts/\(accountId)/load_balancers/pools/\(poolId)/health"
        )
        guard response.success, let result = response.result else { throw response.toAPIError() }
        return result
    }

    // MARK: - 健康监测 Monitor（account）

    func listMonitors(accountId: String) async throws -> [Monitor] {
        let response: CFAPIResponse<[Monitor]> = try await client.get(
            "accounts/\(accountId)/load_balancers/monitors"
        )
        guard response.success, let result = response.result else { throw response.toAPIError() }
        return result
    }

    func createMonitor(accountId: String, body: MonitorUpdate) async throws -> Monitor {
        let response: CFAPIResponse<Monitor> = try await client.post(
            "accounts/\(accountId)/load_balancers/monitors", body: body
        )
        guard response.success, let result = response.result else { throw response.toAPIError() }
        return result
    }

    func updateMonitor(accountId: String, monitorId: String, body: MonitorUpdate) async throws -> Monitor {
        let response: CFAPIResponse<Monitor> = try await client.patch(
            "accounts/\(accountId)/load_balancers/monitors/\(monitorId)", body: body
        )
        guard response.success, let result = response.result else { throw response.toAPIError() }
        return result
    }

    func deleteMonitor(accountId: String, monitorId: String) async throws {
        try await client.delete("accounts/\(accountId)/load_balancers/monitors/\(monitorId)")
    }
}
