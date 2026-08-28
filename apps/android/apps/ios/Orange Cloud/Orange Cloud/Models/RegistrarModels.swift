//
//  RegistrarModels.swift
//  Orange Cloud
//
//  Cloudflare Registrar —— 在 Cloudflare 注册的域名：到期日 / 自动续费 / 转移锁。
//
//  ⚠️ 必须用新版 API。旧的 /accounts/{id}/registrar/domains 系列于 2026-06-29 弃用、
//  **2026-09-27 停用**；新版在 /accounts/{id}/registrar/registrations 下。
//
//  两个由接口决定的边界，UI 必须如实反映：
//  · PATCH **只支持 auto_renew**（规范原文：currently supports updating auto_renew only），
//    转移锁 locked 是只读的 —— 不做成开关
//  · PATCH 返回的是**异步 workflow 状态**而非更新后的注册对象，故写入后需回读列表
//

import Foundation

nonisolated struct DomainRegistration: Codable, Identifiable, Hashable, Sendable {
    let domainName:  String
    /// 到期时间。registration_pending 期间可能为 null
    let expiresAt:   String?
    let createdAt:   String?
    /// 自动续费：开启即授权 Cloudflare 在到期前 30 天内扣默认支付方式
    let autoRenew:   Bool?
    /// 是否锁定转移。**只读**，新版 API 不支持经此端点修改
    let locked:      Bool?
    /// false / redaction
    let privacyMode: String?
    /// active / registration_pending / expired / suspended / redemption_period
    let status:      String?

    var id: String { domainName }

    enum CodingKeys: String, CodingKey {
        case locked, status
        case domainName  = "domain_name"
        case expiresAt   = "expires_at"
        case createdAt   = "created_at"
        case autoRenew   = "auto_renew"
        case privacyMode = "privacy_mode"
    }

    var statusText: String {
        switch status {
        case "active":               String(localized: "正常")
        case "registration_pending": String(localized: "注册中")
        case "expired":              String(localized: "已过期")
        case "suspended":            String(localized: "已暂停")
        case "redemption_period":    String(localized: "赎回期")
        default:                     status ?? String(localized: "未知")
        }
    }

    var expiryDate: Date? {
        guard let expiresAt else { return nil }
        return ISO8601DateFormatter.registrarParser.date(from: expiresAt)
    }

    /// 距到期天数；负数表示已过期
    var daysUntilExpiry: Int? {
        guard let expiryDate else { return nil }
        return Calendar.current.dateComponents([.day], from: Date(), to: expiryDate).day
    }

    /// 30 天内到期且未开自动续费——需要用户尽快处理
    var needsAttention: Bool {
        guard let days = daysUntilExpiry else { return false }
        return days <= 30 && autoRenew != true
    }
}

/// PATCH 体。接口目前只认 auto_renew。
nonisolated struct RegistrationUpdate: Codable, Sendable {
    let autoRenew: Bool

    enum CodingKeys: String, CodingKey {
        case autoRenew = "auto_renew"
    }
}

nonisolated extension ISO8601DateFormatter {
    /// Registrar 的时间戳带小数秒，默认解析器接不住
    static let registrarParser: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }()
}
