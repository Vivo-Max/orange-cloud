//
//  TurnstileModels.swift
//  Orange Cloud
//
//  Turnstile 人机验证组件（账号级 /accounts/{id}/challenges/widgets，
//  OAuth scope challenge-widgets.read / .write）。
//
//  端点与字段以 api-schemas OpenAPI 为准（2026-08 核对）：
//  · widget 主键是 sitekey；secret 仅 list/get/create/rotate 响应携带
//  · mode 枚举：managed / non-interactive / invisible
//  · domains 最多 10 条（主机名或 IP，含子域生效）；空数组 = 允许任意主机名
//  · region（world / china）创建后不可改；offlabel / ephemeral_id 是 ENT 专属，
//    App 只透传显示、不提供编辑（免费/付费账号写它必 403）
//

import Foundation

nonisolated struct TurnstileWidget: Codable, Identifiable, Hashable, Sendable {
    let sitekey:        String
    let name:           String
    let mode:           String
    let domains:        [String]
    /// 服务端校验用密钥。出于最小暴露，详情页默认遮蔽、点按显示/复制。
    let secret:         String?
    let botFightMode:   Bool?
    let clearanceLevel: String?
    let region:         String?
    let offlabel:       Bool?
    let ephemeralId:    Bool?
    let createdOn:      String?
    let modifiedOn:     String?

    var id: String { sitekey }

    enum CodingKeys: String, CodingKey {
        case sitekey, name, mode, domains, secret, region, offlabel
        case botFightMode   = "bot_fight_mode"
        case clearanceLevel = "clearance_level"
        case ephemeralId    = "ephemeral_id"
        case createdOn      = "created_on"
        case modifiedOn     = "modified_on"
    }
}

/// 创建 / 更新载荷（PUT 必填 name + mode + domains；未提及字段服务端保持原值，
/// 但 bot_fight_mode / clearance_level 我们开放编辑，故显式带上）
nonisolated struct TurnstileWidgetInput: Codable, Sendable {
    let name:           String
    let mode:           String
    let domains:        [String]
    let botFightMode:   Bool?
    /// 仅创建时有效（world / china），创建后不可改
    let region:         String?
    let clearanceLevel: String?

    enum CodingKeys: String, CodingKey {
        case name, mode, domains, region
        case botFightMode   = "bot_fight_mode"
        case clearanceLevel = "clearance_level"
    }
}

/// 轮换密钥请求体。invalidate_immediately = false 时旧密钥保留 2 小时宽限。
nonisolated struct TurnstileRotateRequest: Codable, Sendable {
    let invalidateImmediately: Bool

    enum CodingKeys: String, CodingKey {
        case invalidateImmediately = "invalidate_immediately"
    }
}

/// Widget 模式。清障强度递增：invisible（无感）→ non-interactive（可见不点选）→ managed（必要时勾选）。
nonisolated enum TurnstileMode: String, CaseIterable, Identifiable, Sendable {
    case managed
    case nonInteractive = "non-interactive"
    case invisible

    var id: String { rawValue }

    /// 未知取值（CF 日后加模式）按 managed 显示
    init(apiValue: String) {
        self = TurnstileMode(rawValue: apiValue) ?? .managed
    }

    var label: String {
        switch self {
        case .managed:        String(localized: "托管（推荐）")
        case .nonInteractive: String(localized: "非交互")
        case .invisible:      String(localized: "隐形")
        }
    }

    var detail: String {
        switch self {
        case .managed:        String(localized: "由 Cloudflare 判断是否要求访客勾选复选框")
        case .nonInteractive: String(localized: "显示验证进度但不需要访客操作")
        case .invisible:      String(localized: "完全不可见，静默完成验证")
        }
    }
}
