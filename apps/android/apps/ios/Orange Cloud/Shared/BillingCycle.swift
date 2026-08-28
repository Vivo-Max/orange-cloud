//
//  BillingCycle.swift
//  Orange Cloud（主 App 与 Widget Extension 共享）
//
//  账单周期计算：给定账单日（1–28），求最近一次周期起点（UTC）。
//  Cloudflare 的用量额度按订阅锚定日重置（如账单日 16 → 5/16–6/16），不是自然月。
//

import Foundation

nonisolated enum BillingCycle {

    static func periodStart(billingDay: Int, now: Date = .now) -> Date {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC")!
        let day = min(max(billingDay, 1), 28)

        var components = calendar.dateComponents([.year, .month], from: now)
        components.day = day
        guard let thisMonth = calendar.date(from: components) else { return now }

        if thisMonth <= now {
            return thisMonth
        }
        // 今天还没到本月账单日 → 周期起点在上个月
        return calendar.date(byAdding: .month, value: -1, to: thisMonth) ?? thisMonth
    }
}
