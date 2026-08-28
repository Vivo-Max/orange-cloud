//
//  ZoneService.swift
//  Orange Cloud
//

import Foundation

struct ZoneService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    /// 拉取账号下全部 Zone（自动翻页）
    func listZones(accountId: String) async throws -> [Zone] {
        var zones: [Zone] = []
        var page = 1
        while true {
            let response: CFAPIResponseArray<Zone> = try await client.get(
                "zones",
                queryItems: [
                    URLQueryItem(name: "account.id", value: accountId),
                    URLQueryItem(name: "page",       value: String(page)),
                    URLQueryItem(name: "per_page",   value: "50"),
                ]
            )
            guard response.success else {
                throw response.toAPIError()
            }
            zones.append(contentsOf: response.result ?? [])
            let totalPages = response.resultInfo?.totalPages ?? 1
            guard page < totalPages else { break }
            page += 1
        }
        return zones
    }

    func getZone(zoneId: String) async throws -> Zone {
        let response: CFAPIResponse<Zone> = try await client.get("zones/\(zoneId)")
        guard response.success, let zone = response.result else {
            throw response.toAPIError()
        }
        return zone
    }

    /// 新建 Zone（添加域名）。type 默认 "full"——Cloudflare 作权威 DNS，
    /// 响应返回分配的 name_servers，状态为 pending，待用户在注册商处更换 NS 后激活。
    func createZone(name: String, accountId: String, type: String = "full") async throws -> Zone {
        let response: CFAPIResponse<Zone> = try await client.post(
            "zones",
            body: CreateZoneRequest(name: name, type: type, account: .init(id: accountId))
        )
        guard response.success, let zone = response.result else {
            throw response.toAPIError()
        }
        return zone
    }

    /// 暂停 / 恢复 Cloudflare 代理（Dashboard 的「Pause Cloudflare on Site」）。
    /// 暂停后 DNS 仍由 Cloudflare 解析，但流量不再经过 Cloudflare——WAF、缓存、
    /// 源站 IP 隐藏一并失效；生效约需 5 分钟。需要 zone.write。
    func setPaused(zoneId: String, paused: Bool) async throws -> Zone {
        let response: CFAPIResponse<Zone> = try await client.patch(
            "zones/\(zoneId)",
            body: PauseZoneRequest(paused: paused)
        )
        guard response.success, let zone = response.result else {
            throw response.toAPIError()
        }
        return zone
    }
}
