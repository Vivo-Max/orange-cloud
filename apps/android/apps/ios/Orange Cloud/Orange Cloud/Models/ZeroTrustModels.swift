//
//  ZeroTrustModels.swift
//  Orange Cloud
//
//  Zero Trust 只读：Access 应用（access.read）+ Gateway 策略（teams.read）。
//  GET /accounts/{id}/access/apps、GET /accounts/{id}/gateway/rules
//

import Foundation

/// Access 应用。列表只给概要；编辑前用 getApp 取详情（含 policies 的完整 include 规则）。
/// 自托管应用可绑**多个**公共主机名：现行字段是 `destinations`（含 public/private 目标），
/// 旧字段 `self_hosted_domains` 已弃用但 API 仍返回（含全部 public 目标的 URI）；只读 `domain`
/// 只是其中第一个，**不能只取它**否则多主机名只显示第一个（issue：网页端绑 3 个子域，客户端只显示 1 个）。
nonisolated struct AccessApp: Codable, Identifiable, Sendable {
    let id:                String
    let name:              String?
    let domain:            String?
    let selfHostedDomains: [String]?
    let destinations:      [AccessDestination]?
    let type:              String?
    let sessionDuration:   String?
    let policies:          [AccessPolicy]?

    enum CodingKeys: String, CodingKey {
        case id, name, domain, type, policies, destinations
        case selfHostedDomains = "self_hosted_domains"
        case sessionDuration = "session_duration"
    }

    /// 全部公共主机名（保序去重，domain 优先）。destinations 的 public 目标 ∪ self_hosted_domains ∪ domain，
    /// 三处取并集兼容新老应用形态；只要其一有值就能拿全。
    var publicHostnames: [String] {
        var seen = Set<String>()
        var out: [String] = []
        func add(_ value: String?) {
            guard let value, !value.isEmpty, seen.insert(value).inserted else { return }
            out.append(value)
        }
        add(domain)
        for dest in destinations ?? [] where (dest.type ?? "public") == "public" { add(dest.uri) }
        for host in selfHostedDomains ?? [] { add(host) }
        return out
    }

    /// 写回时需原样保留的非 public 目标（如私有网络目标），避免编辑公共主机名时把它们误删
    var nonPublicDestinations: [AccessDestination] {
        (destinations ?? []).filter { ($0.type ?? "public") != "public" }
    }

    /// 应用类型可读名
    var typeLabel: String {
        switch type ?? "" {
        case "self_hosted":  String(localized: "自托管")
        case "saas":         "SaaS"
        case "ssh":          "SSH"
        case "vnc":          "VNC"
        case "app_launcher": String(localized: "应用启动台")
        case "warp":         "WARP"
        case "bookmark":     String(localized: "书签")
        case "dash_sso":     "Dash SSO"
        case "":             String(localized: "应用")
        case let other:      other
        }
    }
}

/// Access 应用的目标（`destinations` 取代 `self_hosted_domains`）：
/// type=public 是公共主机名，type=private 是私有网络目标（IP/私有主机名）。
nonisolated struct AccessDestination: Codable, Sendable {
    let type: String?
    let uri:  String?
}

/// Access 策略（可复用资源；应用通过 id 引用）
nonisolated struct AccessPolicy: Codable, Identifiable, Sendable {
    let id:       String?
    let name:     String?
    let decision: String?
    let include:  [AccessRule]?
    let exclude:  [AccessRule]?
    let require:  [AccessRule]?

    /// 仅含本 App 可视化编辑的 include 规则、且无 exclude/require 时，才允许在此改规则
    var isSimplyEditable: Bool {
        (exclude?.isEmpty ?? true) && (require?.isEmpty ?? true)
            && (include?.allSatisfy { $0.isKnown } ?? false)
            && (include?.isEmpty == false)
    }
}

/// include/exclude/require 里的单条规则。JSON 是单键对象（如 {"email":{"email":"x"}}）。
/// 已知类型可视化编辑；未知类型保留原样不在此暴露（避免编辑丢数据）。
nonisolated struct AccessRule: Codable, Sendable, Identifiable {
    var id = UUID()
    var kind:  String      // everyone | email | email_domain | ip | geo | 其它(只读保留)
    var value: String      // email→email；email_domain→domain；ip→ip；geo→country_code

    init(kind: String, value: String) {
        self.kind = kind
        self.value = value
    }

    var isKnown: Bool { ["everyone", "email", "email_domain", "ip", "geo"].contains(kind) }

    private struct DynKey: CodingKey {
        var stringValue: String
        init(_ s: String) { stringValue = s }
        init?(stringValue: String) { self.stringValue = stringValue }
        var intValue: Int? { nil }
        init?(intValue: Int) { nil }
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: DynKey.self)
        guard let key = c.allKeys.first else { kind = "unknown"; value = ""; return }
        kind = key.stringValue
        let inner = try? c.decode([String: String].self, forKey: key)
        switch kind {
        case "everyone":     value = ""
        case "email":        value = inner?["email"] ?? ""
        case "email_domain": value = inner?["domain"] ?? ""
        case "ip":           value = inner?["ip"] ?? ""
        case "geo":          value = inner?["country_code"] ?? ""
        default:             value = ""
        }
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: DynKey.self)
        switch kind {
        case "email":        try c.encode(["email": value], forKey: DynKey("email"))
        case "email_domain": try c.encode(["domain": value], forKey: DynKey("email_domain"))
        case "ip":           try c.encode(["ip": value], forKey: DynKey("ip"))
        case "geo":          try c.encode(["country_code": value], forKey: DynKey("geo"))
        default:             try c.encode([String: String](), forKey: DynKey("everyone"))
        }
    }
}

/// 可视化规则类型（创建 / 编辑时可选）
nonisolated enum AccessRuleKind: String, CaseIterable, Identifiable, Sendable {
    case everyone, email, email_domain, ip, geo
    var id: String { rawValue }
    var label: String {
        switch self {
        case .everyone:     String(localized: "所有人")
        case .email:        String(localized: "邮箱")
        case .email_domain: String(localized: "邮箱域名")
        case .ip:           String(localized: "IP 范围")
        case .geo:          String(localized: "国家/地区")
        }
    }
    var placeholder: String {
        switch self {
        case .everyone:     ""
        case .email:        "user@example.com"
        case .email_domain: "example.com"
        case .ip:           "203.0.113.0/24"
        case .geo:          "US"
        }
    }
    var needsValue: Bool { self != .everyone }
}

nonisolated enum AccessDecision: String, CaseIterable, Identifiable, Sendable {
    case allow, deny, bypass
    var id: String { rawValue }
    var label: String {
        switch self {
        case .allow:  String(localized: "允许")
        case .deny:   String(localized: "拒绝")
        case .bypass: String(localized: "绕过")
        }
    }
}

// MARK: - Access 写入

nonisolated struct AccessPolicyInput: Codable, Sendable {
    let name:     String
    let decision: String
    let include:  [AccessRule]
}

nonisolated struct AccessAppInput: Codable, Sendable {
    let name:            String
    let domain:          String                       // 主主机名（第一个），保留兼容
    let destinations:    [AccessDestinationInput]?    // 全部目标（多公共主机名 + 保留的私有目标）
    let type:            String
    let sessionDuration: String?
    let policies:        [String]      // 引用的策略 ID

    enum CodingKeys: String, CodingKey {
        case name, domain, type, policies, destinations
        case sessionDuration = "session_duration"
    }
}

/// 写回用的单个目标。PUT 是全量替换：编辑公共主机名时必须把**全部**主机名（及原有私有目标）一起回写，
/// 否则会把未提交的主机名删掉（此前只回写 domain 单值 → 多主机名应用一保存就被删到只剩一个）。
nonisolated struct AccessDestinationInput: Codable, Sendable {
    let type: String
    let uri:  String
}

/// Access 会话时长预设
nonisolated enum AccessSessionDuration: String, CaseIterable, Identifiable, Sendable {
    case m30 = "30m", h1 = "1h", h6 = "6h", h24 = "24h", week = "168h", month = "730h"
    var id: String { rawValue }
    var label: String {
        switch self {
        case .m30:   String(localized: "30 分钟")
        case .h1:    String(localized: "1 小时")
        case .h6:    String(localized: "6 小时")
        case .h24:   String(localized: "24 小时")
        case .week:  String(localized: "1 周")
        case .month: String(localized: "1 个月")
        }
    }
}

/// Gateway 策略（DNS / HTTP / Network）。列表端点返回全字段（含 traffic 等表达式），供编辑预填。
nonisolated struct GatewayRule: Codable, Identifiable, Sendable {
    let id:            String
    let name:          String?
    let description:   String?
    let action:        String?
    let enabled:       Bool?
    let precedence:    Int?
    let filters:       [String]?
    let traffic:       String?
    let identity:      String?
    let devicePosture: String?
    let ruleSettings:  JSONValue?

    enum CodingKeys: String, CodingKey {
        case id, name, description, action, enabled, precedence, filters, traffic, identity
        case devicePosture = "device_posture"
        case ruleSettings  = "rule_settings"
    }

    var isEnabled: Bool { enabled ?? false }

    /// 策略类型（取 filters 首项），编辑表单据此切换选择器与动作集
    var filterType: GatewayFilterType {
        GatewayFilterType(rawValue: filters?.first ?? "") ?? .dns
    }

    /// 策略类型徽章（来自 filters）
    var kindLabel: String {
        guard let f = filters?.first else { return "Gateway" }
        switch f {
        case "dns":            return "DNS"
        case "http":           return "HTTP"
        case "l4":             return String(localized: "网络")
        case "egress":         return String(localized: "出口")
        case "resolver":       return String(localized: "解析器")
        default:               return f.uppercased()
        }
    }

    /// 动作可读名（常见值，其余原样）
    var actionLabel: String {
        switch action {
        case "allow":           String(localized: "允许")
        case "block":           String(localized: "阻止")
        case "isolate":         String(localized: "隔离")
        case "override":        String(localized: "覆盖")
        case "safesearch":      String(localized: "安全搜索")
        case "off":             String(localized: "关闭")
        case "on":              String(localized: "开启")
        case "do_not_inspect":  String(localized: "不检查")
        case "noscan":          String(localized: "不扫描")
        case let other?:        other
        case nil:               "—"
        }
    }
}

// MARK: - Gateway 写入

/// POST/PUT /gateway/rules 请求体。可选字段为 nil 时编码省略；rule_settings 用 JSONValue 透传，
/// 编辑时回写原值以保留我们未建模的设置（block_page / override 目标等）。
nonisolated struct GatewayRuleInput: Codable, Sendable {
    let name:          String
    let description:   String?
    let action:        String
    let enabled:       Bool
    let filters:       [String]
    let traffic:       String?
    let identity:      String?
    let devicePosture: String?
    let precedence:    Int?
    let ruleSettings:  JSONValue?

    enum CodingKeys: String, CodingKey {
        case name, description, action, enabled, filters, traffic, identity, precedence
        case devicePosture = "device_posture"
        case ruleSettings  = "rule_settings"
    }

    /// 从既有规则构造（编辑 / 启停回写用），可覆盖个别字段
    init(from rule: GatewayRule, enabledOverride: Bool? = nil) {
        name          = rule.name ?? ""
        description   = rule.description
        action        = rule.action ?? "block"
        enabled       = enabledOverride ?? rule.isEnabled
        filters       = rule.filters ?? ["dns"]
        traffic       = rule.traffic
        identity      = rule.identity
        devicePosture = rule.devicePosture
        precedence    = rule.precedence
        ruleSettings  = rule.ruleSettings
    }

    init(
        name: String, description: String?, action: String, enabled: Bool,
        filters: [String], traffic: String?, identity: String?,
        devicePosture: String?, precedence: Int?, ruleSettings: JSONValue?
    ) {
        self.name = name; self.description = description; self.action = action
        self.enabled = enabled; self.filters = filters; self.traffic = traffic
        self.identity = identity; self.devicePosture = devicePosture
        self.precedence = precedence; self.ruleSettings = ruleSettings
    }
}

/// Gateway 策略类型（filters 首项）
nonisolated enum GatewayFilterType: String, CaseIterable, Identifiable, Sendable {
    case dns, http, l4

    var id: String { rawValue }

    var label: String {
        switch self {
        case .dns:  "DNS"
        case .http: "HTTP"
        case .l4:   String(localized: "网络")
        }
    }

    /// 该类型可选的动作（API 值 → 显示名）。仅收录无需额外 rule_settings 即可创建的动作。
    var actions: [(value: String, label: String)] {
        switch self {
        case .dns:
            [("allow", String(localized: "允许")),
             ("block", String(localized: "阻止")),
             ("safesearch", String(localized: "安全搜索")),
             ("ytrestricted", String(localized: "YouTube 受限"))]
        case .http:
            [("allow", String(localized: "允许")),
             ("block", String(localized: "阻止")),
             ("isolate", String(localized: "隔离")),
             ("off", String(localized: "不检查")),
             ("noscan", String(localized: "不扫描"))]
        case .l4:
            [("allow", String(localized: "允许")),
             ("block", String(localized: "阻止"))]
        }
    }

    var defaultAction: String { self == .l4 ? "block" : "block" }
}

/// 表达式编辑器的选择器调色板：点一下插入对应片段（已含 any()/[*] 与引号等正确语法）
nonisolated struct GatewaySelector: Identifiable, Sendable {
    let label:   String
    let snippet: String
    var id: String { label }
}

nonisolated enum GatewayExpressionCatalog {
    static func selectors(for type: GatewayFilterType) -> [GatewaySelector] {
        switch type {
        case .dns:
            [GatewaySelector(label: String(localized: "域名"),     snippet: "any(dns.domains[*] == \"example.com\")"),
             GatewaySelector(label: String(localized: "主机"),     snippet: "dns.fqdn == \"example.com\""),
             GatewaySelector(label: String(localized: "内容分类"), snippet: "any(dns.content_category[*] in {1})"),
             GatewaySelector(label: String(localized: "安全分类"), snippet: "any(dns.security_category[*] in {1})"),
             GatewaySelector(label: String(localized: "记录类型"), snippet: "dns.query_rtype == \"AAAA\""),
             GatewaySelector(label: String(localized: "源 IP"),    snippet: "dns.src_ip == \"1.1.1.1\""),
             GatewaySelector(label: String(localized: "用户邮箱"), snippet: "identity.email == \"user@example.com\"")]
        case .http:
            [GatewaySelector(label: String(localized: "域名"),     snippet: "any(http.request.domains[*] == \"example.com\")"),
             GatewaySelector(label: String(localized: "主机"),     snippet: "http.request.host == \"example.com\""),
             GatewaySelector(label: String(localized: "URI 路径"), snippet: "http.request.uri.path == \"/path\""),
             GatewaySelector(label: String(localized: "请求方法"), snippet: "http.request.method == \"POST\""),
             GatewaySelector(label: String(localized: "内容分类"), snippet: "any(http.request.uri.content_category[*] in {1})"),
             GatewaySelector(label: String(localized: "安全分类"), snippet: "any(http.request.uri.security_category[*] in {1})"),
             GatewaySelector(label: String(localized: "应用"),     snippet: "any(app.ids[*] in {1})"),
             GatewaySelector(label: String(localized: "用户邮箱"), snippet: "identity.email == \"user@example.com\"")]
        case .l4:
            [GatewaySelector(label: String(localized: "目标 IP"),   snippet: "net.dst.ip == \"1.2.3.4\""),
             GatewaySelector(label: String(localized: "目标端口"),  snippet: "net.dst.port == 443"),
             GatewaySelector(label: String(localized: "协议"),      snippet: "net.protocol == \"tcp\""),
             GatewaySelector(label: "SNI",                          snippet: "net.sni.host == \"example.com\""),
             GatewaySelector(label: String(localized: "目标国家"),  snippet: "net.dst.geo.country == \"US\""),
             GatewaySelector(label: String(localized: "源 IP"),     snippet: "net.src.ip == \"1.2.3.4\"")]
        }
    }

    /// 编辑器底部的语法提示
    static let syntaxHint = String(localized: "用 and / or 连接多个条件；字符串加双引号；集合用 {1 2 3}；数组字段用 any(字段[*] == 值)。保存时 Cloudflare 会校验并规范化表达式。")
}
