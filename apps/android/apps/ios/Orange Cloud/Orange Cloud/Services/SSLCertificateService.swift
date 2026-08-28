//
//  SSLCertificateService.swift
//  Orange Cloud
//
//  边缘证书查询 + Universal SSL 开关 + 删除证书包（仅非 Universal）。
//  读 ssl-and-certificates.read，写 ssl-and-certificates.write。
//

import Foundation

struct SSLCertificateService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    /// 列出该 Zone 的证书包（含未激活的，status=all）
    func certificatePacks(zoneId: String) async throws -> [SSLCertificatePack] {
        let response: CFAPIResponse<[SSLCertificatePack]> = try await client.get(
            "zones/\(zoneId)/ssl/certificate_packs",
            queryItems: [URLQueryItem(name: "status", value: "all")]
        )
        guard response.success, let result = response.result else {
            throw response.toAPIError()
        }
        return result
    }

    /// 读 Universal SSL 是否启用
    func universalSSLEnabled(zoneId: String) async throws -> Bool {
        let response: CFAPIResponse<UniversalSSLSettings> = try await client.get(
            "zones/\(zoneId)/ssl/universal/settings"
        )
        guard response.success, let result = response.result else {
            throw response.toAPIError()
        }
        return result.enabled ?? false
    }

    /// 开关 Universal SSL，返回生效后的状态
    func setUniversalSSL(zoneId: String, enabled: Bool) async throws -> Bool {
        let response: CFAPIResponse<UniversalSSLSettings> = try await client.patch(
            "zones/\(zoneId)/ssl/universal/settings",
            body: UniversalSSLSettings(enabled: enabled)
        )
        guard response.success, let result = response.result else {
            throw response.toAPIError()
        }
        return result.enabled ?? enabled
    }

    /// 删除证书包（仅适用于高级 / 自定义包；Universal 包不可删）
    func deleteCertificatePack(zoneId: String, packId: String) async throws {
        try await client.delete("zones/\(zoneId)/ssl/certificate_packs/\(packId)")
    }
}
