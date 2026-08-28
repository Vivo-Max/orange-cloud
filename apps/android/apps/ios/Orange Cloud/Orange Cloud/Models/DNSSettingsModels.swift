//
//  DNSSettingsModels.swift
//  Orange Cloud
//
//  Zone 级 DNS 设置（/zones/{id}/dns_settings）与 DNSSEC（/zones/{id}/dnssec）。
//
//  注意历史弃用，别用错端点：
//  · zone settings 里的 cname_flattening（2025-06-08 弃用）已迁到这里的 flatten_all_cnames
//  · 账户设置的 default_nameservers / use_account_custom_ns_by_default（2025-03-14 弃用）同样迁来
//  · 2024-09-13 起独立 DNS 设置端点合并为统一的 DNS Settings API
//  · foundation_dns 布尔字段（2026-07-27 弃用，**sunset 2026-11-23**）已由
//    nameservers.type = "cloudflare.advanced" 取代 —— 本文件只用后者，不建模前者
//

import Foundation

// MARK: - DNS 设置

nonisolated struct ZoneDNSSettings: Codable, Sendable {
    /// 展平 zone 内全部 CNAME（顶点 CNAME 因 DNS 限制始终展平）
    let flattenAllCnames:   Bool?
    /// 多提供商 DNS：存在非 Cloudflare NS 记录时仍激活该 zone
    let multiProvider:      Bool?
    let secondaryOverrides: Bool?
    /// NS 记录 TTL，30–86400 秒
    let nsTtl:              Double?
    /// standard / cdn_only / dns_only
    let zoneMode:           String?
    let nameservers:        ZoneNameservers?

    enum CodingKeys: String, CodingKey {
        case nameservers
        case flattenAllCnames   = "flatten_all_cnames"
        case multiProvider      = "multi_provider"
        case secondaryOverrides = "secondary_overrides"
        case nsTtl              = "ns_ttl"
        case zoneMode           = "zone_mode"
    }
}

nonisolated struct ZoneNameservers: Codable, Sendable {
    /// cloudflare.standard / cloudflare.advanced / custom.account / custom.tenant / custom.zone
    let type:  String?
    /// 名称服务器集编号，1–5
    let nsSet: Int?

    enum CodingKeys: String, CodingKey {
        case type
        case nsSet = "ns_set"
    }
}

/// PATCH 体：只带要改的字段（接口为合并语义）
nonisolated struct ZoneDNSSettingsUpdate: Codable, Sendable {
    var flattenAllCnames:   Bool?
    var multiProvider:      Bool?
    var secondaryOverrides: Bool?
    var nsTtl:              Double?
    var zoneMode:           String?
    var nameservers:        ZoneNameserversUpdate?

    enum CodingKeys: String, CodingKey {
        case nameservers
        case flattenAllCnames   = "flatten_all_cnames"
        case multiProvider      = "multi_provider"
        case secondaryOverrides = "secondary_overrides"
        case nsTtl              = "ns_ttl"
        case zoneMode           = "zone_mode"
    }
}

nonisolated struct ZoneNameserversUpdate: Codable, Sendable {
    let type: String
}

nonisolated enum ZoneMode: String, CaseIterable, Identifiable, Sendable {
    case standard, cdnOnly = "cdn_only", dnsOnly = "dns_only"

    var id: String { rawValue }

    init(apiValue: String?) {
        self = ZoneMode(rawValue: apiValue ?? "") ?? .standard
    }

    var label: String {
        switch self {
        case .standard: String(localized: "标准")
        case .cdnOnly:  String(localized: "仅 CDN")
        case .dnsOnly:  String(localized: "仅 DNS")
        }
    }
}

// MARK: - DNSSEC

nonisolated struct ZoneDNSSEC: Codable, Sendable {
    /// active / pending / disabled / pending-disabled / error
    let status:          String?
    /// 完整 DS 记录——用户要把它粘到域名注册商处，DNSSEC 才真正生效
    let ds:              String?
    let digest:          String?
    let digestType:      String?
    let digestAlgorithm: String?
    let algorithm:       String?
    let keyTag:          Double?
    let flags:           Double?
    let publicKey:       String?
    let modifiedOn:      String?

    enum CodingKeys: String, CodingKey {
        case status, ds, digest, algorithm, flags
        case digestType      = "digest_type"
        case digestAlgorithm = "digest_algorithm"
        case keyTag          = "key_tag"
        case publicKey       = "public_key"
        case modifiedOn      = "modified_on"
    }

    var isEnabled: Bool { status == "active" || status == "pending" }

    var statusText: String {
        switch status {
        case "active":           String(localized: "已启用")
        case "pending":          String(localized: "待在注册商处添加 DS 记录")
        case "pending-disabled": String(localized: "正在停用")
        case "error":            String(localized: "出错")
        default:                 String(localized: "未启用")
        }
    }
}

nonisolated struct ZoneDNSSECUpdate: Codable, Sendable {
    let status: String
}
