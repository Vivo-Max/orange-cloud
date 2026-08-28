//
//  BotManagementModels.swift
//  Orange Cloud
//
//  Zone 级机器人管控配置（GET/PUT /zones/{id}/bot_management）。
//
//  接口只有一个，但响应 result 是四种套餐形态的 oneOf
//  （Bot Fight Mode / SBFM Definitely / SBFM Likely / BM Enterprise）。
//  四者都 allOf 引用同一个 base_config，本文件只建模 base_config 里与
//  AI / 爬虫相关的字段，其余套餐专属字段（sbfm_* / fight_mode 等）一律不碰——
//  因此全部字段可选，任何套餐的响应都能安全解码。
//
//  写入用 PUT 但是**合并语义**：官方文档的示例就是只发要改的字段
//  （如 {"fight_mode": false}），base_config 无 required 字段。
//  所以这里每次只发一个字段，既不会覆盖别的设置，也不会把 GET 回来的
//  只读字段（using_latest_model / stale_zone_configuration）回写过去。
//

import Foundation

nonisolated struct BotManagementConfig: Codable, Sendable {
    /// 拦截 AI 抓取与爬虫：block（全站）/ only_on_ad_pages（仅带广告页）/ disabled（放行）
    let aiBotsProtection:      String?
    /// 用链接迷宫惩罚 AI 爬虫（AI Labyrinth）：enabled / disabled
    let crawlerProtection:     String?
    /// 拦内容机器人（低 bot score，排除已验证安全类别）：block / disabled
    let contentBotsProtection: String?
    /// Robots 访问控制许可证变体：off / policy_only
    let cfRobotsVariant:       String?
    /// 启用 Cloudflare 托管 robots.txt（会前置到既有 robots.txt 之前）
    let isRobotsTxtManaged:    Bool?

    enum CodingKeys: String, CodingKey {
        case aiBotsProtection      = "ai_bots_protection"
        case crawlerProtection     = "crawler_protection"
        case contentBotsProtection = "content_bots_protection"
        case cfRobotsVariant       = "cf_robots_variant"
        case isRobotsTxtManaged    = "is_robots_txt_managed"
    }
}

/// AI 爬虫处置档位。三档取值直接对应 ai_bots_protection 的枚举。
nonisolated enum AIBotsProtection: String, CaseIterable, Identifiable, Sendable {
    case disabled
    case onlyOnAdPages = "only_on_ad_pages"
    case block

    var id: String { rawValue }

    /// 未知取值（CF 日后加档）按放行显示，不猜测语义
    init(apiValue: String?) {
        self = AIBotsProtection(rawValue: apiValue ?? "") ?? .disabled
    }

    var label: String {
        switch self {
        case .disabled:      String(localized: "放行")
        case .onlyOnAdPages: String(localized: "仅拦广告页")
        case .block:         String(localized: "全站拦截")
        }
    }
}

// MARK: - 写入体（每次只发一个字段，见文件头说明）

nonisolated struct BotManagementUpdate<Value: Codable & Sendable>: Codable, Sendable {
    let key:   BotManagementField
    let value: Value

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: BotManagementField.self)
        try container.encode(value, forKey: key)
    }

    init(key: BotManagementField, value: Value) {
        self.key = key
        self.value = value
    }

    init(from decoder: Decoder) throws {
        throw DecodingError.dataCorrupted(
            .init(codingPath: [], debugDescription: "BotManagementUpdate 只用于编码")
        )
    }
}

nonisolated enum BotManagementField: String, CodingKey, Sendable {
    case aiBotsProtection      = "ai_bots_protection"
    case crawlerProtection     = "crawler_protection"
    case contentBotsProtection = "content_bots_protection"
    case cfRobotsVariant       = "cf_robots_variant"
    case isRobotsTxtManaged    = "is_robots_txt_managed"
}
