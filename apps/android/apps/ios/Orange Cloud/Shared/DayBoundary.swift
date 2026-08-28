//
//  DayBoundary.swift
//  Orange Cloud（主 App 与 Widget Extension 共享）
//
//  用量「今日」窗口的日界口径：UTC（默认，与 Cloudflare 免费额度重置一致）或本地时间。
//  存 App Group UserDefaults，设置页修改，主 App 用量查询与 Widget 共同读取。
//  注意：仅对 datetime 过滤的查询（Workers 请求/CPU、R2 操作）生效；
//  D1/KV 走 GraphQL date 维度，Cloudflare 按 UTC 天聚合，无法表达本地日界，始终 UTC。
//

import Foundation

nonisolated enum DayBoundary: String, CaseIterable, Identifiable, Sendable {

    case utc
    case local

    var id: String { rawValue }

    var label: String {
        switch self {
        case .utc:   "UTC"
        case .local: String(localized: "本地时间")
        }
    }

    /// 计算「今日零点」用的日历
    var calendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = self == .utc ? TimeZone(identifier: "UTC")! : TimeZone.current
        return calendar
    }

    static let storageKey = "usageDayBoundary"

    /// 当前口径（App Group，主 App 与 Widget 一致），未设置时默认 UTC
    static var current: DayBoundary {
        guard let defaults = UserDefaults(suiteName: WidgetSnapshot.appGroupID),
              let raw = defaults.string(forKey: storageKey),
              let value = DayBoundary(rawValue: raw) else { return .utc }
        return value
    }
}
