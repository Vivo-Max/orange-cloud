//
//  DNSSettingsService.swift
//  Orange Cloud
//
//  Zone DNS 设置（zone-dns-settings.read/.write）与 DNSSEC（dns.read/.write）。
//

import Foundation

struct DNSSettingsService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    // MARK: - DNS 设置

    func settings(zoneId: String) async throws -> ZoneDNSSettings {
        let response: CFAPIResponse<ZoneDNSSettings> = try await client.get(
            "zones/\(zoneId)/dns_settings"
        )
        guard response.success, let result = response.result else {
            throw response.toAPIError()
        }
        return result
    }

    /// 合并语义：只发 update 里非 nil 的字段
    func updateSettings(zoneId: String, update: ZoneDNSSettingsUpdate) async throws -> ZoneDNSSettings {
        let response: CFAPIResponse<ZoneDNSSettings> = try await client.patch(
            "zones/\(zoneId)/dns_settings",
            body: update
        )
        guard response.success, let result = response.result else {
            throw response.toAPIError()
        }
        return result
    }

    // MARK: - DNSSEC

    func dnssec(zoneId: String) async throws -> ZoneDNSSEC {
        let response: CFAPIResponse<ZoneDNSSEC> = try await client.get("zones/\(zoneId)/dnssec")
        guard response.success, let result = response.result else {
            throw response.toAPIError()
        }
        return result
    }

    /// 启用传 "active"，停用传 "disabled"。
    /// 启用后状态先是 pending——必须由用户去注册商处添加 DS 记录才会转 active。
    func setDNSSEC(zoneId: String, enabled: Bool) async throws -> ZoneDNSSEC {
        let response: CFAPIResponse<ZoneDNSSEC> = try await client.patch(
            "zones/\(zoneId)/dnssec",
            body: ZoneDNSSECUpdate(status: enabled ? "active" : "disabled")
        )
        guard response.success, let result = response.result else {
            throw response.toAPIError()
        }
        return result
    }
}
