//
//  LoadBalancerModels.swift
//  Orange Cloud
//
//  负载均衡：Load Balancer（zone 级）+ 源站池 Pool / 健康监测 Monitor（account 级）。
//  读 load-balancers.read / load-balancing-monitors-and-pools.read，写对应 .write。
//  字段名核对自 Cloudflare 官方 SDK（cloudflare-python types/load_balancers）。
//
//  写入用独立的 *Update 结构，只带要改的字段（PATCH 顶层合并，省略字段不变）——
//  故未开放的高级字段（region_pools / load_shedding 等）编辑时自动保留。Pool 的 origins
//  是整组字段，编辑时把现有 origin 完整回写（含 header/port/vnet）避免丢配置。
//

import Foundation

// MARK: - Load Balancer（zone）

nonisolated struct LoadBalancer: Codable, Identifiable, Sendable {
    let id:              String
    var name:            String?
    var enabled:         Bool?
    var ttl:             Int?
    var proxied:         Bool?
    var defaultPools:    [String]?
    var fallbackPool:    String?
    var steeringPolicy:  String?
    var sessionAffinity: String?

    enum CodingKeys: String, CodingKey {
        case id, name, enabled, ttl, proxied
        case defaultPools    = "default_pools"
        case fallbackPool    = "fallback_pool"
        case steeringPolicy  = "steering_policy"
        case sessionAffinity = "session_affinity"
    }

    var steeringLabel: String { LBSteeringPolicy(rawValue: steeringPolicy ?? "")?.label ?? (steeringPolicy ?? "—") }
}

/// 创建 / 编辑负载均衡器的载荷
nonisolated struct LoadBalancerUpdate: Codable, Sendable {
    var name:            String? = nil
    var enabled:         Bool?   = nil
    var ttl:             Int?    = nil
    var proxied:         Bool?   = nil
    var defaultPools:    [String]? = nil
    var fallbackPool:    String? = nil
    var steeringPolicy:  String? = nil
    var sessionAffinity: String? = nil

    enum CodingKeys: String, CodingKey {
        case name, enabled, ttl, proxied
        case defaultPools    = "default_pools"
        case fallbackPool    = "fallback_pool"
        case steeringPolicy  = "steering_policy"
        case sessionAffinity = "session_affinity"
    }
}

// MARK: - 源站池 Pool（account）

nonisolated struct Pool: Codable, Identifiable, Sendable {
    let id:                String
    var name:              String?
    var enabled:           Bool?
    var description:       String?
    var monitor:           String?
    var notificationEmail: String?
    var minimumOrigins:    Int?
    var origins:           [Origin]?

    enum CodingKeys: String, CodingKey {
        case id, name, enabled, description, monitor, origins
        case notificationEmail = "notification_email"
        case minimumOrigins    = "minimum_origins"
    }

    var enabledOriginsCount: Int { (origins ?? []).filter { $0.enabled ?? true }.count }
    var originsCount: Int { origins?.count ?? 0 }
}

nonisolated struct Origin: Codable, Identifiable, Sendable {
    var name:             String? = nil
    var address:          String? = nil
    var enabled:          Bool?   = nil
    var weight:           Double? = nil
    var port:             Int?    = nil
    var header:           [String: [String]]? = nil
    var virtualNetworkId: String? = nil
    var disabledAt:       String? = nil      // 只读

    var id: String { name ?? address ?? UUID().uuidString }

    enum CodingKeys: String, CodingKey {
        case name, address, enabled, weight, port, header
        case virtualNetworkId = "virtual_network_id"
        case disabledAt       = "disabled_at"
    }
}

/// 写回 origin（去掉只读 disabled_at；header/port/vnet 原样保留）
nonisolated struct OriginInput: Codable, Sendable {
    var name:             String?
    var address:          String?
    var enabled:          Bool?
    var weight:           Double?
    var port:             Int?
    var header:           [String: [String]]?
    var virtualNetworkId: String?

    enum CodingKeys: String, CodingKey {
        case name, address, enabled, weight, port, header
        case virtualNetworkId = "virtual_network_id"
    }

    init(from origin: Origin) {
        name = origin.name
        address = origin.address
        enabled = origin.enabled
        weight = origin.weight
        port = origin.port
        header = origin.header
        virtualNetworkId = origin.virtualNetworkId
    }
}

nonisolated struct PoolUpdate: Codable, Sendable {
    var name:              String? = nil
    var enabled:           Bool?   = nil
    var description:       String? = nil
    var monitor:           String? = nil
    var notificationEmail: String? = nil
    var origins:           [OriginInput]? = nil

    enum CodingKeys: String, CodingKey {
        case name, enabled, description, monitor, origins
        case notificationEmail = "notification_email"
    }
}

// MARK: - 源站池健康

nonisolated struct PoolHealthResponse: Codable, Sendable {
    let poolId:    String?
    let popHealth: [String: PoolPopHealth]?

    enum CodingKeys: String, CodingKey {
        case poolId    = "pool_id"
        case popHealth = "pop_health"
    }

    var healthyCount: Int { (popHealth?.values.filter { $0.healthy == true }.count) ?? 0 }
    var totalCount:   Int { popHealth?.count ?? 0 }
}

nonisolated struct PoolPopHealth: Codable, Sendable {
    let healthy: Bool?
}

// MARK: - 健康监测 Monitor（account）

nonisolated struct Monitor: Codable, Identifiable, Sendable {
    let id:              String
    var type:            String?
    var method:          String?
    var path:            String?
    var expectedCodes:   String?
    var expectedBody:    String?
    var interval:        Int?
    var timeout:         Int?
    var retries:         Int?
    var port:            Int?
    var followRedirects: Bool?
    var allowInsecure:   Bool?
    var description:     String?

    enum CodingKeys: String, CodingKey {
        case id, type, method, path, interval, timeout, retries, port, description
        case expectedCodes   = "expected_codes"
        case expectedBody    = "expected_body"
        case followRedirects = "follow_redirects"
        case allowInsecure   = "allow_insecure"
    }

    var typeLabel: String { (type ?? "").uppercased() }
    /// 列表副标题：HTTP 类显示 方法+路径，其它显示端口
    var summary: String {
        if type == "http" || type == "https" {
            return "\(method ?? "GET") \(path ?? "/")"
        }
        return port.map { String(localized: "端口 \($0)") } ?? (type ?? "")
    }
}

nonisolated struct MonitorUpdate: Codable, Sendable {
    var type:            String? = nil
    var method:          String? = nil
    var path:            String? = nil
    var expectedCodes:   String? = nil
    var expectedBody:    String? = nil
    var interval:        Int?    = nil
    var timeout:         Int?    = nil
    var retries:         Int?    = nil
    var port:            Int?    = nil
    var followRedirects: Bool?   = nil
    var allowInsecure:   Bool?   = nil
    var description:     String? = nil

    enum CodingKeys: String, CodingKey {
        case type, method, path, interval, timeout, retries, port, description
        case expectedCodes   = "expected_codes"
        case expectedBody    = "expected_body"
        case followRedirects = "follow_redirects"
        case allowInsecure   = "allow_insecure"
    }
}

// MARK: - 枚举

nonisolated enum LBSteeringPolicy: String, CaseIterable, Identifiable, Sendable {
    case off                      = "off"
    case geo                      = "geo"
    case random                   = "random"
    case dynamicLatency           = "dynamic_latency"
    case proximity                = "proximity"
    case leastOutstandingRequests = "least_outstanding_requests"
    case leastConnections         = "least_connections"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .off:                      String(localized: "默认（按池顺序故障转移）")
        case .geo:                      String(localized: "地理位置")
        case .random:                   String(localized: "随机")
        case .dynamicLatency:           String(localized: "动态延迟")
        case .proximity:                String(localized: "就近")
        case .leastOutstandingRequests: String(localized: "最少未完成请求")
        case .leastConnections:         String(localized: "最少连接")
        }
    }
}

nonisolated enum LBSessionAffinity: String, CaseIterable, Identifiable, Sendable {
    case none, cookie
    case ipCookie = "ip_cookie"
    case header

    var id: String { rawValue }

    var label: String {
        switch self {
        case .none:     String(localized: "不启用")
        case .cookie:   String(localized: "Cookie")
        case .ipCookie: String(localized: "IP + Cookie")
        case .header:   String(localized: "请求头")
        }
    }
}

nonisolated enum MonitorType: String, CaseIterable, Identifiable, Sendable {
    case http, https, tcp
    case udpIcmp  = "udp_icmp"
    case icmpPing = "icmp_ping"
    case smtp

    var id: String { rawValue }
    var label: String { rawValue.uppercased() }
    var isHTTP: Bool { self == .http || self == .https }
}

nonisolated enum MonitorMethod: String, CaseIterable, Identifiable, Sendable {
    case get = "GET"
    case head = "HEAD"
    var id: String { rawValue }
    var label: String { rawValue }
}
