//
//  CacheRuleModels.swift
//  Orange Cloud
//
//  Cache Rules（Rulesets API，phase http_request_cache_settings，动作 set_cache_settings）。
//  读 cache-settings.read，写 cache-settings.write。结构同 Transform Rules（entrypoint + 规则数组）。
//
//  action_parameters 我们只开放常用字段（缓存资格 / 边缘·浏览器 TTL / serve stale / 强 ETag /
//  源站错误页透传）。含高级设置（自定义缓存键、Cache Reserve、读超时、源站 Cache-Control、
//  额外可缓存端口）的规则在编辑器里只读，避免 PATCH 覆盖时丢配置；status_code_ttl 不可编辑但会原样保留。
//

import Foundation

nonisolated struct CacheRuleset: Codable, Sendable {
    let id:    String
    let name:  String?
    let phase: String?
    let rules: [CacheRule]?
}

nonisolated struct CacheRule: Codable, Identifiable, Sendable {
    let id:          String
    let expression:  String?
    let description: String?
    let enabled:     Bool?
    let action:      String?       // 恒 "set_cache_settings"
    let actionParameters: CacheActionParameters?

    enum CodingKeys: String, CodingKey {
        case id, expression, description, enabled, action
        case actionParameters = "action_parameters"
    }

    /// 列表行一句话摘要
    var summary: String {
        guard let p = actionParameters else { return String(localized: "默认缓存设置") }
        if p.cache == false { return String(localized: "绕过缓存") }
        var parts: [String] = [String(localized: "可缓存")]
        if let edge = p.edgeTtl { parts.append(String(localized: "边缘：\(edge.modeLabel)")) }
        if let browser = p.browserTtl { parts.append(String(localized: "浏览器：\(browser.modeLabel)")) }
        return parts.joined(separator: " · ")
    }
}

// MARK: - action_parameters

nonisolated struct CacheActionParameters: Codable, Sendable {
    // —— 我们开放编辑的字段（带默认值，便于编辑器用 CacheActionParameters() 起手逐项填）——
    var cache: Bool? = nil
    var edgeTtl: CacheEdgeTTL? = nil
    var browserTtl: CacheBrowserTTL? = nil
    var serveStale: CacheServeStale? = nil
    var respectStrongEtags: Bool? = nil
    var originErrorPagePassthru: Bool? = nil
    // —— 仅解码、用于「是否含高级设置」探测（编辑器对这些规则只读，不回写以免丢配置）——
    var cacheKey: CacheKeyProbe? = nil
    var cacheReserve: CacheReserveProbe? = nil
    var readTimeout: Int? = nil
    var originCacheControl: Bool? = nil
    var additionalCacheablePorts: [Int]? = nil
    var vary: VaryProbe? = nil             // 2026-07-02 新增：按 Vary 响应头缓存多版本

    enum CodingKeys: String, CodingKey {
        case cache
        case edgeTtl = "edge_ttl"
        case browserTtl = "browser_ttl"
        case serveStale = "serve_stale"
        case respectStrongEtags = "respect_strong_etags"
        case originErrorPagePassthru = "origin_error_page_passthru"
        case cacheKey = "cache_key"
        case cacheReserve = "cache_reserve"
        case readTimeout = "read_timeout"
        case originCacheControl = "origin_cache_control"
        case additionalCacheablePorts = "additional_cacheable_ports"
        case vary
    }

    /// 含我们未开放的高级设置 → 编辑器只读（status_code_ttl 不计入，编辑时会原样保留）
    var hasAdvancedSettings: Bool {
        cacheKey != nil || cacheReserve != nil || readTimeout != nil
            || originCacheControl != nil || (additionalCacheablePorts?.isEmpty == false)
            || vary != nil
    }
}

/// 仅探测存在性的占位（不还原内部结构，故含此设置的规则在 App 内只读）
nonisolated struct CacheKeyProbe: Codable, Sendable {}
nonisolated struct CacheReserveProbe: Codable, Sendable {}
nonisolated struct VaryProbe: Codable, Sendable {}

nonisolated struct CacheEdgeTTL: Codable, Sendable {
    var mode: String                       // respect_origin | override_origin | bypass_by_default
    var defaultTtl: Int?                    // override_origin 时必填，单位秒
    var statusCodeTtl: [CacheStatusCodeTTL]?   // 不可编辑，但编辑时原样保留

    enum CodingKeys: String, CodingKey {
        case mode
        case defaultTtl = "default"
        case statusCodeTtl = "status_code_ttl"
    }

    var modeLabel: String { CacheTTLMode(rawValue: mode)?.label ?? mode }
}

nonisolated struct CacheBrowserTTL: Codable, Sendable {
    var mode: String                       // respect_origin | override_origin | bypass_by_default
    var defaultTtl: Int?

    enum CodingKeys: String, CodingKey {
        case mode
        case defaultTtl = "default"
    }

    var modeLabel: String { CacheTTLMode(rawValue: mode)?.label ?? mode }
}

nonisolated struct CacheStatusCodeTTL: Codable, Sendable {
    var statusCodeRange: CacheStatusCodeRange?
    var statusCode: Int?
    var value: Int

    enum CodingKeys: String, CodingKey {
        case statusCodeRange = "status_code_range"
        case statusCode = "status_code"
        case value
    }
}

nonisolated struct CacheStatusCodeRange: Codable, Sendable {
    var from: Int?
    var to:   Int?
}

nonisolated struct CacheServeStale: Codable, Sendable {
    var disableStaleWhileUpdating: Bool
    enum CodingKeys: String, CodingKey {
        case disableStaleWhileUpdating = "disable_stale_while_updating"
    }
}

// MARK: - TTL 模式

nonisolated enum CacheTTLMode: String, CaseIterable, Identifiable, Sendable {
    case respectOrigin   = "respect_origin"
    case overrideOrigin  = "override_origin"
    case bypassByDefault = "bypass_by_default"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .respectOrigin:   String(localized: "遵循源站")
        case .overrideOrigin:  String(localized: "覆盖为固定值")
        case .bypassByDefault: String(localized: "源站无指令则不缓存")
        }
    }
}

/// 缓存资格（cache 字段）
nonisolated enum CacheEligibility: String, CaseIterable, Identifiable, Sendable {
    case eligible, bypass
    var id: String { rawValue }
    var label: String {
        switch self {
        case .eligible: String(localized: "可缓存")
        case .bypass:   String(localized: "绕过缓存")
        }
    }
}

// MARK: - 写入载荷（POST rules / PATCH rule / PUT entrypoint 共用）

nonisolated struct CacheRuleCreate: Codable, Sendable {
    let action:           String        // 恒 "set_cache_settings"
    let expression:       String
    let description:      String?
    let enabled:          Bool
    let actionParameters: CacheActionParameters?

    enum CodingKeys: String, CodingKey {
        case action, expression, description, enabled
        case actionParameters = "action_parameters"
    }
}

nonisolated struct CacheRuleToggle: Codable, Sendable {
    let enabled: Bool
}

nonisolated struct CacheEntrypointUpdate: Codable, Sendable {
    let rules: [CacheRuleCreate]
}

// MARK: - 表达式常用字段

/// 缓存规则表达式的常用字段，供编辑器点按插入。
/// 含 2026-07-16 起缓存规则新支持的 bot management 分数与 ASN 匹配字段。
nonisolated struct CacheRuleField: Identifiable, Sendable {
    let label: String
    let token: String

    var id: String { token }

    static let common: [CacheRuleField] = [
        .init(label: String(localized: "路径"),   token: "http.request.uri.path"),
        .init(label: String(localized: "主机"),   token: "http.host"),
        .init(label: String(localized: "查询串"), token: "http.request.uri.query"),
        .init(label: String(localized: "方法"),   token: "http.request.method"),
        .init(label: "Cookie",                   token: "http.cookie"),
        .init(label: String(localized: "国家"),   token: "ip.src.country"),
        // 2026-07-16 新增：缓存规则现在也能按 bot 分数、JA3/JA4 指纹与来源 ASN 匹配
        // （bot 字段需 Bot Management 订阅；ASN 全套餐可用）
        .init(label: String(localized: "Bot 分数"),     token: "cf.bot_management.score"),
        .init(label: String(localized: "已验证机器人"), token: "cf.bot_management.verified_bot"),
        .init(label: String(localized: "JA3 指纹"),     token: "cf.bot_management.ja3_hash"),
        .init(label: String(localized: "JA4 指纹"),     token: "cf.bot_management.ja4"),
        .init(label: "ASN",                            token: "ip.src.asnum"),
    ]
}
