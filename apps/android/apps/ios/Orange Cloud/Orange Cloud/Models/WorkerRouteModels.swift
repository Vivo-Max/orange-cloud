//
//  WorkerRouteModels.swift
//  Orange Cloud
//
//  Worker 的域名/路由模型。
//  GET/POST /accounts/{a}/workers/scripts/{n}/subdomain   workers.dev 子域开关
//  GET/PUT/DELETE /accounts/{a}/workers/domains            自定义域（按 service 过滤到本脚本）
//  GET/POST/PUT/DELETE /zones/{z}/workers/routes           Zone 路由（pattern → script）
//

import Foundation

// MARK: - workers.dev 子域

/// 脚本的 workers.dev 子域路由状态
nonisolated struct WorkerSubdomain: Codable, Sendable {
    let enabled:         Bool
    let previewsEnabled: Bool?

    enum CodingKeys: String, CodingKey {
        case enabled
        case previewsEnabled = "previews_enabled"
    }
}

/// 切换 workers.dev 子域（POST body）
nonisolated struct WorkerSubdomainInput: Codable, Sendable {
    let enabled: Bool
}

/// 账号级 workers.dev 子域前缀（GET /accounts/{a}/workers/subdomain → { subdomain }）。
/// 拼 <脚本名>.<前缀>.workers.dev 得完整访问地址；账号未注册子域时 subdomain 为 nil。
nonisolated struct WorkerAccountSubdomain: Codable, Sendable {
    let subdomain: String?
}

// MARK: - 自定义域

/// Worker 自定义域（账号级，service = 脚本名）
nonisolated struct WorkerCustomDomain: Codable, Identifiable, Hashable, Sendable {
    let id:          String
    let hostname:    String
    let service:     String?
    let zoneId:      String?
    let zoneName:    String?
    let environment: String?

    enum CodingKeys: String, CodingKey {
        case id, hostname, service, environment
        case zoneId   = "zone_id"
        case zoneName = "zone_name"
    }
}

/// 挂载自定义域（PUT .../domains）
nonisolated struct WorkerCustomDomainInput: Codable, Sendable {
    let hostname:    String
    let service:     String
    let zoneId:      String
    let environment: String

    init(hostname: String, service: String, zoneId: String, environment: String = "production") {
        self.hostname    = hostname
        self.service     = service
        self.zoneId      = zoneId
        self.environment = environment
    }

    enum CodingKeys: String, CodingKey {
        case hostname, service, environment
        case zoneId = "zone_id"
    }
}

// MARK: - Zone 路由

/// Zone 级 Worker 路由（GET /zones/{z}/workers/routes）
nonisolated struct WorkerRoute: Codable, Identifiable, Hashable, Sendable {
    let id:      String
    let pattern: String
    let script:  String?
}

/// 新建 / 更新路由（POST/PUT，body {pattern, script}）
nonisolated struct WorkerRouteInput: Codable, Sendable {
    let pattern: String
    let script:  String
}

/// 带 zone 上下文的路由（路由本身按 zone 查询，聚合展示/删除时需带 zone）
nonisolated struct ScopedWorkerRoute: Identifiable, Hashable, Sendable {
    let zoneId:   String
    let zoneName: String
    let route:    WorkerRoute

    var id: String { "\(zoneId)/\(route.id)" }
}
