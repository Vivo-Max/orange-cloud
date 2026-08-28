//
//  ZoneStatusWidget.swift
//  OrangeCloudWidgets
//
//  账号总览 Widget（晨昏 · 小=天窗 / 中=山脊）：
//  全账号 24h 请求合计 + 域名数/活跃数。请求数据由各 Zone 快照聚合，
//  域名计数来自 Zone 列表快照。kind 沿用 "ZoneStatusWidget"，保住已放置的卡片。
//

import WidgetKit
import SwiftUI
import AppIntents

nonisolated struct AccountOverviewEntry: TimelineEntry {
    let date: Date
    let snapshot: WidgetSnapshot?
    let totalRequests: Int      // 所选账号域名 24h 合计（无 Zone 快照时为 0）
    let series: [Int]           // 各域名逐小时求和（尾对齐）
}

/// 把某账号各 Zone 的 24h 序列按「距今相同小时」对齐后求和
nonisolated private func aggregateZones(accountId: String?) -> (total: Int, series: [Int]) {
    let zones = WidgetDataStore.loadZones(accountId: accountId)
    guard !zones.isEmpty else { return (0, []) }
    let total = zones.reduce(0) { $0 + $1.requests }
    let length = zones.map(\.requestsSeries.count).max() ?? 0
    guard length > 1 else { return (total, []) }
    var summed = [Int](repeating: 0, count: length)
    for zone in zones {
        let offset = length - zone.requestsSeries.count
        for (index, value) in zone.requestsSeries.enumerated() {
            summed[offset + index] += value
        }
    }
    return (total, summed)
}

nonisolated private let sampleSeries = [42, 50, 47, 61, 58, 72, 90, 84, 95, 110, 104, 96, 88, 92, 81, 76, 70, 64, 58, 66, 61, 55, 50, 57]

nonisolated struct ZoneStatusProvider: AppIntentTimelineProvider {

    func placeholder(in context: Context) -> AccountOverviewEntry {
        AccountOverviewEntry(
            date: .now,
            snapshot: WidgetSnapshot(accountName: "My Account", totalZones: 5, activeZones: 4, updatedAt: .now),
            totalRequests: 2_418_650,
            series: sampleSeries
        )
    }

    func snapshot(for configuration: AccountOverviewConfigIntent, in context: Context) async -> AccountOverviewEntry {
        currentEntry(for: configuration)
    }

    func timeline(for configuration: AccountOverviewConfigIntent, in context: Context) async -> Timeline<AccountOverviewEntry> {
        // App 刷新数据时会主动 reload；这里兜底每小时重读一次（天色相位也随之走动）
        let next = Calendar.current.date(byAdding: .hour, value: 1, to: .now) ?? .now
        return Timeline(entries: [currentEntry(for: configuration)], policy: .after(next))
    }

    private func currentEntry(for configuration: AccountOverviewConfigIntent) -> AccountOverviewEntry {
        // 配置固定某账号则用它，否则回退当前账号
        let accountId = configuration.account?.id ?? WidgetSnapshot.currentAccountId()
        let aggregate = aggregateZones(accountId: accountId)
        return AccountOverviewEntry(
            date: .now,
            snapshot: WidgetSnapshot.load(accountId: accountId),
            totalRequests: aggregate.total,
            series: aggregate.series
        )
    }
}

struct ZoneStatusWidget: Widget {

    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "ZoneStatusWidget", intent: AccountOverviewConfigIntent.self, provider: ZoneStatusProvider()) { entry in
            AccountOverviewWidgetView(entry: entry)
                .daybreakContainer(date: entry.date)
        }
        .configurationDisplayName("账号总览")
        .description("某个账号 24 小时请求与域名运行状态，长按可选择账号")
        .supportedFamilies([.systemSmall, .systemMedium, .accessoryInline, .accessoryCircular, .accessoryRectangular])
        .contentMarginsDisabled()
    }
}

struct AccountOverviewWidgetView: View {

    @Environment(\.widgetFamily) private var family
    let entry: AccountOverviewEntry

    @ViewBuilder
    var body: some View {
        switch family {
        case .accessoryInline:      inlineView
        case .accessoryCircular:    circularView
        case .accessoryRectangular: rectangularView
        default:                    systemBody
        }
    }

    @ViewBuilder
    private var systemBody: some View {
        if let snapshot = entry.snapshot {
            switch family {
            case .systemMedium: mediumView(snapshot)
            default:            smallView(snapshot)
            }
        } else {
            WidgetEmptyHint(text: String(localized: "打开 App 同步数据"))
        }
    }

    // MARK: - 锁屏 accessory

    private var compactRequests: String {
        entry.totalRequests > 0
            ? entry.totalRequests.formatted(.number.notation(.compactName))
            : "—"
    }

    private var inlineView: some View {
        Label {
            Text("\(compactRequests) 请求")
        } icon: {
            Image(systemName: "chart.bar.fill")
        }
    }

    private var circularView: some View {
        ZStack {
            AccessoryWidgetBackground()
            VStack(spacing: -1) {
                Text(compactRequests)
                    .font(.system(.title3, design: .rounded).weight(.semibold))
                    .minimumScaleFactor(0.5)
                    .lineLimit(1)
                Text("请求")
                    .font(.system(size: 9))
                    .foregroundStyle(.secondary)
            }
            .padding(3)
        }
    }

    private var rectangularView: some View {
        VStack(alignment: .leading, spacing: 1) {
            Text(entry.snapshot?.accountName ?? "Orange Cloud")
                .font(.headline)
                .widgetAccentable()
                .lineLimit(1)
            Text(compactRequests)
                .font(.system(.title2, design: .rounded).weight(.bold))
                .monospacedDigit()
            if let snapshot = entry.snapshot {
                Text("\(snapshot.activeZones)/\(snapshot.totalZones) 活跃 · 24h")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .monospacedDigit()
            } else {
                Text("请求 · 全部域名 · 24h")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
    }

    /// 小（天窗）：合计请求大字 + 地平线弧
    private func smallView(_ snapshot: WidgetSnapshot) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack {
                Text(snapshot.accountName)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                Spacer()
                if entry.totalRequests > 0 {
                    Text("\(snapshot.activeZones)/\(snapshot.totalZones)")
                        .font(.caption2.weight(.semibold))
                        .foregroundStyle(.secondary)
                        .monospacedDigit()
                }
            }
            Spacer(minLength: 4)
            if entry.totalRequests > 0 {
                Text(entry.totalRequests.formatted(.number.notation(.compactName)))
                    .font(.system(size: 30, weight: .bold, design: .rounded))
                    .monospacedDigit()
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
                    .contentTransition(.numericText())
                Text("请求 · 全部域名 · 24h")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            } else {
                Text("\(snapshot.activeZones)/\(snapshot.totalZones)")
                    .font(.system(size: 30, weight: .bold, design: .rounded))
                    .monospacedDigit()
                Text("活跃域名")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            HorizonScene(date: entry.date)
                .frame(height: 36)
                .padding(.horizontal, -14)
        }
        .padding([.horizontal, .top], 14)
        .padding(.bottom, 8)
    }

    /// 中（山脊）：合计请求 + 域名计数，24h 地形铺底
    private func mediumView(_ snapshot: WidgetSnapshot) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(snapshot.accountName)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                    Text(entry.totalRequests > 0
                         ? entry.totalRequests.formatted(.number.notation(.compactName))
                         : "\(snapshot.activeZones)/\(snapshot.totalZones)")
                        .font(.system(size: 28, weight: .bold, design: .rounded))
                        .monospacedDigit()
                        .contentTransition(.numericText())
                    Text(entry.totalRequests > 0 ? String(localized: "请求 · 全部域名 · 24h") : String(localized: "活跃域名"))
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 3) {
                    Text("\(snapshot.totalZones) 域名")
                        .font(.caption2.weight(.semibold))
                        .monospacedDigit()
                    Text("\(snapshot.activeZones) 活跃")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .monospacedDigit()
                    Text(snapshot.updatedAt, format: .dateTime.hour().minute())
                        .font(.caption2)
                        .foregroundStyle(.tertiary)
                }
            }
            .padding([.horizontal, .top], 14)
            if entry.series.count > 1 {
                RidgeScene(series: entry.series, date: entry.date)
                    .frame(maxHeight: .infinity)
            } else {
                Spacer(minLength: 0)
                HorizonScene(date: entry.date)
                    .frame(height: 36)
            }
        }
    }
}
