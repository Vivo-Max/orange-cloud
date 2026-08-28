//
//  ToolboxModels.swift
//  Orange Cloud
//
//  免登录工具箱的数据模型（DNS / CIDR / HTTP / 证书 / WHOIS / GeoIP / CF trace）。
//  全部不依赖 CF 账号；纯值类型，标 nonisolated 以适配 MainActor 默认隔离。
//

import Foundation

// MARK: - DNS（DoH）

nonisolated enum DNSQueryType: String, CaseIterable, Identifiable, Sendable {
    case a = "A", aaaa = "AAAA", cname = "CNAME", mx = "MX", txt = "TXT", ns = "NS", caa = "CAA", soa = "SOA"
    var id: String { rawValue }
}

nonisolated struct DoHResponse: Decodable, Sendable {
    let status: Int
    let answer: [DoHAnswer]?
    enum CodingKeys: String, CodingKey { case status = "Status"; case answer = "Answer" }
}

nonisolated struct DoHAnswer: Decodable, Sendable {
    let name: String
    let type: Int
    let ttl: Int
    let data: String
    enum CodingKeys: String, CodingKey { case name, type, data; case ttl = "TTL" }
}

nonisolated struct DNSRecordResult: Identifiable, Sendable {
    let id = UUID()
    let name: String
    let typeName: String
    let ttl: Int
    let value: String
}

// MARK: - CIDR

nonisolated struct CIDRResult: Sendable {
    let family: String            // "IPv4" / "IPv6"
    let networkAddress: String
    let prefixLength: Int
    let netmask: String?          // 仅 IPv4
    let firstAddress: String
    let lastAddress: String
    let totalCount: String        // 地址总数（IPv6 以 2^k 表示）
    let usableHosts: String?      // 仅 IPv4
}

// MARK: - HTTP 请求器

nonisolated struct HTTPHeaderItem: Identifiable, Sendable {
    let id = UUID()
    let name: String
    let value: String
}

nonisolated struct HTTPProbeResult: Sendable {
    let statusCode: Int
    let statusText: String
    let headers: [HTTPHeaderItem]
    let durationMS: Int
    let finalURL: String
    let bodyByteCount: Int
    let bodyPreview: String
}

// MARK: - SSL 证书检查

nonisolated struct CertInfo: Sendable {
    let subjectCN: String
    let issuerCN: String
    let notBefore: Date?
    let notAfter: Date?
    let sanDNSNames: [String]
    let serialHex: String?
    let publicKeyBits: Int?
    let chainSubjects: [String]

    var isExpired: Bool {
        guard let notAfter else { return false }
        return notAfter < Date()
    }
    var daysRemaining: Int? {
        guard let notAfter else { return nil }
        return Calendar.current.dateComponents([.day], from: Date(), to: notAfter).day
    }
}

// MARK: - WHOIS（RDAP）

nonisolated struct WhoisInfo: Sendable {
    let domain: String
    let statuses: [String]
    let registrar: String?
    let created: Date?
    let updated: Date?
    let expires: Date?
    let nameservers: [String]
}

// MARK: - GeoIP（ipwho.is）

nonisolated struct GeoIPResult: Decodable, Sendable {
    let ip: String?
    let success: Bool
    let message: String?
    let type: String?
    let city: String?
    let region: String?
    let country: String?
    let countryCode: String?
    let latitude: Double?
    let longitude: Double?
    let connection: Connection?
    let timezone: Timezone?

    nonisolated struct Connection: Decodable, Sendable {
        let asn: Int?
        let org: String?
        let isp: String?
    }
    nonisolated struct Timezone: Decodable, Sendable {
        let id: String?
    }

    enum CodingKeys: String, CodingKey {
        case ip, success, message, type, city, region, country, latitude, longitude, connection, timezone
        case countryCode = "country_code"
    }
}

// MARK: - CF 数据中心 trace

nonisolated struct TraceField: Identifiable, Sendable {
    let id = UUID()
    let key: String
    let value: String
}

nonisolated struct CFTraceResult: Sendable {
    let fields: [TraceField]
    let raw: [String: String]

    var colo: String? { raw["colo"] }
    var ip: String? { raw["ip"] }
    var loc: String? { raw["loc"] }
    var tls: String? { raw["tls"] }
    var http: String? { raw["http"] }
    var warp: String? { raw["warp"] }
}
