//
//  SSLCertificateModels.swift
//  Orange Cloud
//
//  边缘证书展示（只读）。GET /zones/{id}/ssl/certificate_packs?status=all
//  权限：ssl-and-certificates.read
//

import Foundation

nonisolated struct SSLCertificatePack: Codable, Identifiable, Sendable {
    let id: String
    let type: String?
    let hosts: [String]?
    let status: String?
    let certificateAuthority: String?
    let certificates: [SSLCertEntry]?

    enum CodingKeys: String, CodingKey {
        case id, type, hosts, status, certificates
        case certificateAuthority = "certificate_authority"
    }

    var typeLabel: String {
        switch type {
        case "universal":                          String(localized: "通用 SSL")
        case "advanced":                           String(localized: "高级证书")
        case "sni_custom", "legacy_custom", "mh_custom", "keyless": String(localized: "自定义证书")
        case "total_tls":                          "Total TLS"
        default:                                   type ?? "—"
        }
    }

    var statusLabel: String {
        switch status {
        case "active":              String(localized: "已签发")
        case "pending_validation":  String(localized: "待验证")
        case "initializing":        String(localized: "初始化中")
        case "expired":             String(localized: "已过期")
        default:                    status ?? "—"
        }
    }

    /// 最近一张证书的到期日（ISO 字符串截取到日）
    var expiresOnDay: String? {
        guard let raw = certificates?.compactMap(\.expiresOn).sorted().first else { return nil }
        return String(raw.prefix(10))
    }

    var issuer: String? { certificates?.compactMap(\.issuer).first }

    /// Universal 包由 Cloudflare 自动托管，不可删除
    var isUniversal: Bool { type == "universal" }
}

/// GET/PATCH /zones/{id}/ssl/universal/settings
nonisolated struct UniversalSSLSettings: Codable, Sendable {
    let enabled: Bool?
}

nonisolated struct SSLCertEntry: Codable, Sendable {
    let id: String?
    let issuer: String?
    let status: String?
    let expiresOn: String?

    enum CodingKeys: String, CodingKey {
        case id, issuer, status
        case expiresOn = "expires_on"
    }
}
