//
//  TunnelModels.swift
//  Orange Cloud
//
//  Cloudflare Tunnel（cfd_tunnel）。GET /accounts/{id}/cfd_tunnel
//
//  ⚠️ 活跃连接的取数方式已变更（CF 2026-07-09 公告）：
//  list/get 响应内嵌的 connections 数组将于 **2026-10-05** 移除，
//  改由专用端点 GET /cfd_tunnel/{id}/connections 提供。
//  该端点返回的是 **Client 数组**（每个 client 内嵌 conns），与旧的扁平数组结构不同，
//  故新增 TunnelClient 建模并用 flattenedConnections 摊平回 TunnelConnection。
//  Tunnel.connections 仅作过渡期占位（字段消失后恒为 nil），勿再作为唯一数据源。
//

import Foundation

nonisolated struct Tunnel: Codable, Identifiable, Hashable, Sendable {
    let id:            String
    let name:          String
    let status:        String?            // "inactive" | "degraded" | "healthy" | "down"
    let createdAt:     String?
    let connsActiveAt: String?
    let tunType:       String?            // "cfd_tunnel" | "warp_connector" ...
    let remoteConfig:  Bool?
    /// 过渡期字段：CF 将于 2026-10-05 从 list/get 响应移除。
    /// 只用来在详情页首帧占位，真实数据以 TunnelService.connections(...) 为准。
    let connections:   [TunnelConnection]?

    enum CodingKeys: String, CodingKey {
        case id, name, status, connections
        case createdAt     = "created_at"
        case connsActiveAt = "conns_active_at"
        case tunType       = "tun_type"
        case remoteConfig  = "remote_config"
    }

    var statusText: String {
        switch status {
        case "healthy":  String(localized: "运行中")
        case "degraded": String(localized: "降级")
        case "down":     String(localized: "离线")
        case "inactive": String(localized: "未激活")
        default:         status ?? String(localized: "未知")
        }
    }
}

nonisolated struct TunnelConnection: Codable, Hashable, Sendable {
    let id:            String?
    let coloName:      String?
    let originIp:      String?
    let openedAt:      String?
    let clientVersion: String?
    /// 仅专用端点返回；旧的内嵌数组没有这两个字段。
    let clientId:      String?
    let uuid:          String?

    enum CodingKeys: String, CodingKey {
        case id, uuid
        case coloName      = "colo_name"
        case originIp      = "origin_ip"
        case openedAt      = "opened_at"
        case clientVersion = "client_version"
        case clientId      = "client_id"
    }
}

/// GET /accounts/{id}/cfd_tunnel/{id}/connections 的 result 元素。
/// 一个 client 即一个 cloudflared 进程，其 conns 是它维持的各条连接。
nonisolated struct TunnelClient: Codable, Hashable, Sendable {
    let id:            String?
    let arch:          String?
    let version:       String?
    let runAt:         String?
    let configVersion: Int?
    let features:      [String]?
    let conns:         [TunnelConnection]?

    enum CodingKeys: String, CodingKey {
        case id, arch, version, features, conns
        case runAt         = "run_at"
        case configVersion = "config_version"
    }
}

extension Array where Element == TunnelClient {
    /// 摊平成 UI 直接消费的连接列表。
    /// client_version 在 conns 里可能缺省，回退用 client 自身的 version 补齐。
    var flattenedConnections: [TunnelConnection] {
        flatMap { client in
            (client.conns ?? []).map { conn in
                TunnelConnection(
                    id:            conn.id,
                    coloName:      conn.coloName,
                    originIp:      conn.originIp,
                    openedAt:      conn.openedAt,
                    clientVersion: conn.clientVersion ?? client.version,
                    clientId:      conn.clientId ?? client.id,
                    uuid:          conn.uuid
                )
            }
        }
    }
}

// MARK: - 新建

/// POST /accounts/{id}/cfd_tunnel 请求体。固定远程托管（config_src=cloudflare），
/// 只有远程托管隧道的 ingress 配置能经 API 管理。
nonisolated struct CreateTunnelRequest: Codable, Sendable {
    let name:      String
    let configSrc: String

    init(name: String, configSrc: String = "cloudflare") {
        self.name = name
        self.configSrc = configSrc
    }

    enum CodingKeys: String, CodingKey {
        case name
        case configSrc = "config_src"
    }
}

// MARK: - 配置（公共主机名 / ingress）

/// GET/PUT /configurations 的 result 外壳。
nonisolated struct TunnelConfigResult: Codable, Sendable {
    let tunnelId: String?
    let config:   TunnelConfig?

    enum CodingKeys: String, CodingKey {
        case config
        case tunnelId = "tunnel_id"
    }
}

/// 隧道配置。整组 PUT，未建模的高级字段（originRequest）以 TunnelJSONValue 原样保留，避免回写丢失。
nonisolated struct TunnelConfig: Codable, Sendable {
    var ingress:       [IngressRule]?
    var warpRouting:   WarpRouting?
    var originRequest: TunnelJSONValue?        // 全局 originRequest，原样透传

    enum CodingKeys: String, CodingKey {
        case ingress, originRequest
        case warpRouting = "warp-routing"
    }
}

/// 单条 ingress 规则。catch-all（末尾兜底）只有 service、无 hostname。
nonisolated struct IngressRule: Codable, Sendable {
    var hostname:      String?
    var service:       String
    var path:          String?
    var originRequest: TunnelJSONValue?        // 规则级 originRequest，原样透传

    init(hostname: String? = nil, service: String, path: String? = nil, originRequest: TunnelJSONValue? = nil) {
        self.hostname = hostname
        self.service = service
        self.path = path
        self.originRequest = originRequest
    }

    /// catch-all 兜底规则：无 hostname，把其余流量返回 404。
    static let catchAll = IngressRule(service: "http_status:404")

    /// 是否为兜底规则（无 hostname，或 service 是 http_status 形态）。UI 列表里隐藏它。
    var isCatchAll: Bool {
        (hostname?.isEmpty ?? true) || service.hasPrefix("http_status:")
    }

    /// 从 service 字符串识别协议种类（用于编辑表单回填）。
    var serviceKind: IngressServiceKind {
        IngressServiceKind.allCases.first { service.hasPrefix($0.scheme) } ?? .other
    }

    /// 去掉协议前缀后的目标（host:port），.other 时为整串。
    var serviceTarget: String {
        let kind = serviceKind
        return kind == .other ? service : String(service.dropFirst(kind.scheme.count))
    }
}

/// 公共主机名表单支持的服务协议。
nonisolated enum IngressServiceKind: String, CaseIterable, Identifiable, Sendable {
    case http, https, tcp, ssh, rdp, other

    var id: String { rawValue }

    /// 协议前缀（.other 无前缀，用户填整串）
    var scheme: String {
        switch self {
        case .http:  "http://"
        case .https: "https://"
        case .tcp:   "tcp://"
        case .ssh:   "ssh://"
        case .rdp:   "rdp://"
        case .other: ""
        }
    }

    var label: String {
        switch self {
        case .http:  "HTTP"
        case .https: "HTTPS"
        case .tcp:   "TCP"
        case .ssh:   "SSH"
        case .rdp:   "RDP"
        case .other: String(localized: "其他")
        }
    }

    /// 该协议的默认目标占位
    var targetPlaceholder: String {
        switch self {
        case .http, .https: "localhost:8000"
        case .tcp:          "localhost:5432"
        case .ssh:          "localhost:22"
        case .rdp:          "localhost:3389"
        case .other:        "unix:/path/to.sock"
        }
    }
}

nonisolated struct WarpRouting: Codable, Sendable {
    var enabled: Bool
}

/// PUT /configurations 请求体：{ "config": { … } }
nonisolated struct TunnelConfigUpdate: Codable, Sendable {
    let config: TunnelConfig
}

// MARK: - 任意 JSON 透传

/// 未建模字段（如 originRequest）的原样保留容器，保证整组 PUT 不丢失既有高级配置。
nonisolated enum TunnelJSONValue: Codable, Sendable {
    case string(String)
    case int(Int)
    case double(Double)
    case bool(Bool)
    case object([String: TunnelJSONValue])
    case array([TunnelJSONValue])
    case null

    init(from decoder: Decoder) throws {
        let c = try decoder.singleValueContainer()
        if c.decodeNil() {
            self = .null
        } else if let b = try? c.decode(Bool.self) {
            self = .bool(b)
        } else if let i = try? c.decode(Int.self) {
            self = .int(i)
        } else if let d = try? c.decode(Double.self) {
            self = .double(d)
        } else if let s = try? c.decode(String.self) {
            self = .string(s)
        } else if let a = try? c.decode([TunnelJSONValue].self) {
            self = .array(a)
        } else if let o = try? c.decode([String: TunnelJSONValue].self) {
            self = .object(o)
        } else {
            throw DecodingError.dataCorruptedError(in: c, debugDescription: "Unsupported JSON value")
        }
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.singleValueContainer()
        switch self {
        case .string(let s): try c.encode(s)
        case .int(let i):    try c.encode(i)
        case .double(let d): try c.encode(d)
        case .bool(let b):   try c.encode(b)
        case .object(let o): try c.encode(o)
        case .array(let a):  try c.encode(a)
        case .null:          try c.encodeNil()
        }
    }
}
