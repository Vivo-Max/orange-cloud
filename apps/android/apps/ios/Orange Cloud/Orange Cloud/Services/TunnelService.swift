//
//  TunnelService.swift
//  Orange Cloud
//

import Foundation

struct TunnelService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    /// 账号下全部 Tunnel（排除已删除，页码分页）
    func listTunnels(accountId: String) async throws -> [Tunnel] {
        var tunnels: [Tunnel] = []
        var page = 1
        while true {
            let response: CFAPIResponseArray<Tunnel> = try await client.get(
                "accounts/\(accountId)/cfd_tunnel",
                queryItems: [
                    URLQueryItem(name: "is_deleted", value: "false"),
                    URLQueryItem(name: "page",       value: String(page)),
                    URLQueryItem(name: "per_page",   value: "100"),
                ]
            )
            guard response.success else {
                throw response.toAPIError()
            }
            tunnels.append(contentsOf: response.result ?? [])
            let totalPages = response.resultInfo?.totalPages ?? 1
            guard page < totalPages else { break }
            page += 1
        }
        return tunnels
    }

    // MARK: - 生命周期（argotunnel.write）

    /// 新建远程托管隧道（config_src=cloudflare）
    func createTunnel(accountId: String, name: String) async throws -> Tunnel {
        let response: CFAPIResponse<Tunnel> = try await client.post(
            "accounts/\(accountId)/cfd_tunnel",
            body: CreateTunnelRequest(name: name)
        )
        guard response.success, let tunnel = response.result else {
            throw response.toAPIError()
        }
        return tunnel
    }

    /// 隧道连接令牌（result 是裸 base64 字符串），用于 `cloudflared tunnel run --token`
    func tunnelToken(accountId: String, tunnelId: String) async throws -> String {
        let response: CFAPIResponse<String> = try await client.get(
            "accounts/\(accountId)/cfd_tunnel/\(tunnelId)/token"
        )
        guard response.success, let token = response.result else {
            throw response.toAPIError()
        }
        return token
    }

    // MARK: - 活跃连接

    /// 隧道的活跃连接。
    ///
    /// 专用端点，取代 list/get 响应里内嵌的 connections 数组（CF 将于 2026-10-05 移除该字段）。
    /// result 是 Client 数组（一个 cloudflared 进程一项），此处摊平为连接列表供 UI 直接消费。
    func connections(accountId: String, tunnelId: String) async throws -> [TunnelConnection] {
        let response: CFAPIResponseArray<TunnelClient> = try await client.get(
            "accounts/\(accountId)/cfd_tunnel/\(tunnelId)/connections"
        )
        guard response.success else {
            throw response.toAPIError()
        }
        return (response.result ?? []).flattenedConnections
    }

    /// 清理失活连接（删除隧道前先调用；活跃的 cloudflared 仍会重连）
    func deleteConnections(accountId: String, tunnelId: String) async throws {
        try await client.delete("accounts/\(accountId)/cfd_tunnel/\(tunnelId)/connections")
    }

    /// 删除隧道：先清理连接再删；若仍有活跃连接，CF 业务错误原样透出。
    func deleteTunnel(accountId: String, tunnelId: String) async throws {
        try? await deleteConnections(accountId: accountId, tunnelId: tunnelId)
        try await client.delete("accounts/\(accountId)/cfd_tunnel/\(tunnelId)")
    }

    // MARK: - 配置（公共主机名 / ingress，仅远程托管）

    /// 读隧道配置。新建后尚无配置时返回 nil（404 或 config 为空均按"无配置"处理）。
    func configuration(accountId: String, tunnelId: String) async throws -> TunnelConfig? {
        do {
            let response: CFAPIResponse<TunnelConfigResult> = try await client.get(
                "accounts/\(accountId)/cfd_tunnel/\(tunnelId)/configurations"
            )
            guard response.success else {
                throw response.toAPIError()
            }
            return response.result?.config
        } catch APIError.notFound {
            return nil
        }
    }

    /// 整组回写配置（catch-all 守在末尾由调用方保证），返回更新后的配置。
    func updateConfiguration(
        accountId: String,
        tunnelId: String,
        config: TunnelConfig
    ) async throws -> TunnelConfig {
        let response: CFAPIResponse<TunnelConfigResult> = try await client.put(
            "accounts/\(accountId)/cfd_tunnel/\(tunnelId)/configurations",
            body: TunnelConfigUpdate(config: config)
        )
        guard response.success, let result = response.result?.config else {
            throw response.toAPIError()
        }
        return result
    }
}
