//
//  UsageWidget.swift
//  OrangeCloudWidgets
//
//  账号用量 Widget（长按可选择服务）：每个服务展示对应的状态条。
//  小=该服务首要指标额度环；中=该服务全部指标进度条。
//  额度口径与 App 内用量区一致（App 刷新时写入快照）。
//

import WidgetKit
import SwiftUI
import AppIntents
import os

nonisolated private let usageLog = Logger(subsystem: "jiamin.chen.orange-cloud.widgets", category: "UsageWidget")

// （UsageServiceOption / UsageConfigIntent 在 Shared/WidgetConfigIntents.swift，两个 target 共享）

// MARK: - Entry / Provider

nonisolated struct UsageWidgetEntry: TimelineEntry {
    let date: Date
    let service: WidgetUsageService?
    let missingName: String?     // 所选服务无数据时的名称（用于提示文案）
    var unavailable: Bool = false   // 账户级数据无权限（免费账号）→ 显示专用提示而非「打开 App 同步」
}

/// 画廊/占位用的示例数据——跟随所选服务，避免看起来像"参数没生效"
nonisolated private func sampleService(for id: String) -> WidgetUsageService {
    switch id {
    case "r2":
        WidgetUsageService(id: "r2", name: "R2", rows: [
            WidgetUsageRow(title: String(localized: "存储"), used: 2_400_000_000, quota: 10_000_000_000, valueText: "2.4 GB"),
            WidgetUsageRow(title: String(localized: "A 类操作 · 本月"), used: 120_000, quota: 1_000_000, valueText: "120K"),
            WidgetUsageRow(title: String(localized: "B 类操作 · 本月"), used: 1_800_000, quota: 10_000_000, valueText: "1.8M"),
        ])
    case "d1":
        WidgetUsageService(id: "d1", name: "D1", rows: [
            WidgetUsageRow(title: String(localized: "行读取 · 今日"), used: 820_000, quota: 5_000_000, valueText: "820K"),
            WidgetUsageRow(title: String(localized: "行写入 · 今日"), used: 12_000, quota: 100_000, valueText: "12K"),
        ])
    case "kv":
        WidgetUsageService(id: "kv", name: "KV", rows: [
            WidgetUsageRow(title: String(localized: "读取 · 今日"), used: 12_400, quota: 100_000, valueText: "12.4K"),
            WidgetUsageRow(title: String(localized: "写入 · 今日"), used: 320, quota: 1_000, valueText: "320"),
        ])
    default:
        WidgetUsageService(id: "workers", name: "Workers", rows: [
            WidgetUsageRow(title: String(localized: "请求 · 今日"), used: 42_300, quota: 100_000, valueText: "42.3K"),
        ])
    }
}

/// 严格按所选服务匹配——没有数据就返回 nil（显示同步提示），不再静默回退到首个服务
nonisolated private func resolveService(_ id: String, accountId: String?) -> WidgetUsageService? {
    WidgetDataStore.loadUsage(accountId: accountId)?.services.first { $0.id == id }
}

nonisolated struct UsageWidgetProvider: AppIntentTimelineProvider {

    func placeholder(in context: Context) -> UsageWidgetEntry {
        UsageWidgetEntry(date: .now, service: sampleService(for: "workers"), missingName: nil)
    }

    func snapshot(for configuration: UsageConfigIntent, in context: Context) async -> UsageWidgetEntry {
        usageLog.info("snapshot: config.service=\(configuration.serviceId, privacy: .public)")
        let accountId = configuration.account?.id ?? WidgetSnapshot.currentAccountId()
        return UsageWidgetEntry(
            date: .now,
            service: resolveService(configuration.serviceId, accountId: accountId) ?? sampleService(for: configuration.serviceId),
            missingName: nil
        )
    }

    func timeline(for configuration: UsageConfigIntent, in context: Context) async -> Timeline<UsageWidgetEntry> {
        usageLog.info("timeline: config.service=\(configuration.serviceId, privacy: .public)")
        let accountId = configuration.account?.id ?? WidgetSnapshot.currentAccountId()
        // 自取数优先（共享钥匙串 token 直查所选服务），失败回退 App 写入的快照
        let fresh = await UsageFetcher.freshService(
            configuration.serviceId,
            accountId: configuration.account?.id,
            sessionId: configuration.account?.sessionId
        )
        let service = fresh ?? resolveService(configuration.serviceId, accountId: accountId)
        let unavailable = service == nil && !WidgetDataStore.loadAccountAnalyticsAvailable()
        usageLog.info("timeline: fresh=\(fresh?.id ?? "nil", privacy: .public) resolved=\(service?.id ?? "nil", privacy: .public) unavailable=\(unavailable, privacy: .public)")
        let entry = UsageWidgetEntry(
            date: .now,
            service: service,
            missingName: service == nil ? (configuration.service?.name ?? "Workers") : nil,
            unavailable: unavailable
        )
        let next = Calendar.current.date(byAdding: .minute, value: 30, to: .now) ?? .now
        return Timeline(entries: [entry], policy: .after(next))
    }
}

// MARK: - Widget

struct UsageWidget: Widget {

    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "UsageWidget", intent: UsageConfigIntent.self, provider: UsageWidgetProvider()) { entry in
            UsageWidgetView(entry: entry)
                .daybreakContainer(date: entry.date)
        }
        .configurationDisplayName("账号用量")
        .description("Workers / R2 / D1 / KV 的额度使用情况，可按服务选择")
        .supportedFamilies([.systemSmall, .systemMedium, .accessoryInline, .accessoryCircular, .accessoryRectangular])
        .contentMarginsDisabled()
    }
}

struct UsageWidgetView: View {

    @Environment(\.widgetFamily) private var family
    let entry: UsageWidgetEntry

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
        if let service = entry.service, !service.rows.isEmpty {
            switch family {
            case .systemMedium: mediumView(service)
            default:            smallView(service)
            }
        } else if entry.unavailable {
            WidgetEmptyHint(text: String(localized: "账户级用量不可用"))
        } else {
            WidgetEmptyHint(text: entry.missingName.map { String(localized: "暂无 \($0) 用量数据\n打开 App 刷新") }
                ?? String(localized: "打开 App 同步用量"))
        }
    }

    // MARK: - 锁屏 accessory

    /// 首要指标的额度占比（无额度返回 nil）
    private func ratio(_ row: WidgetUsageRow) -> Double? {
        row.quota.map { min(Double(row.used) / Double(max($0, 1)), 1) }
    }

    @ViewBuilder
    private var inlineView: some View {
        if let service = entry.service, let row = service.rows.first {
            Label {
                if let percent = ratio(row).map({ Int($0 * 100) }) {
                    Text("\(service.name) \(percent)% · \(row.title)")
                } else {
                    Text("\(service.name) \(row.valueText) · \(row.title)")
                }
            } icon: {
                Image(systemName: "gauge.medium")
            }
        } else if entry.unavailable {
            Label(String(localized: "账户级用量不可用"), systemImage: "lock")
        } else {
            Label("Orange Cloud", systemImage: "gauge.medium")
        }
    }

    @ViewBuilder
    private var circularView: some View {
        if let service = entry.service, let row = service.rows.first, let value = ratio(row) {
            Gauge(value: value) {
                Text(service.name)
            } currentValueLabel: {
                Text("\(Int(value * 100))")
                    .minimumScaleFactor(0.5)
            }
            .gaugeStyle(.accessoryCircularCapacity)
            .tint(barColor(value))
        } else {
            ZStack {
                AccessoryWidgetBackground()
                VStack(spacing: -1) {
                    Text(entry.service?.rows.first?.valueText ?? "—")
                        .font(.system(.callout, design: .rounded).weight(.semibold))
                        .minimumScaleFactor(0.4)
                        .lineLimit(1)
                    Text(entry.service?.name ?? "")
                        .font(.system(size: 9))
                        .foregroundStyle(.secondary)
                }
                .padding(3)
            }
        }
    }

    @ViewBuilder
    private var rectangularView: some View {
        if let service = entry.service, let row = service.rows.first {
            VStack(alignment: .leading, spacing: 2) {
                HStack {
                    Text("\(service.name) 用量")
                        .font(.headline)
                        .widgetAccentable()
                        .lineLimit(1)
                    Spacer(minLength: 4)
                    if let percent = ratio(row).map({ Int($0 * 100) }) {
                        Text("\(percent)%")
                            .font(.caption)
                            .monospacedDigit()
                    }
                }
                if let value = ratio(row) {
                    Gauge(value: value) { EmptyView() }
                        .gaugeStyle(.accessoryLinearCapacity)
                        .tint(barColor(value))
                }
                Text(row.quota.map { "\(row.valueText) / \($0.formatted(.number.notation(.compactName)))" } ?? row.valueText)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .monospacedDigit()
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        } else if entry.unavailable {
            Text("账户级用量不可用")
                .font(.caption2)
                .foregroundStyle(.secondary)
        } else {
            Text(entry.missingName.map { String(localized: "暂无 \($0) 用量数据") } ?? String(localized: "打开 App 同步用量"))
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }

    /// 小（天窗）：首要指标百分比大字，太阳在地平线弧上走到额度位置
    private func smallView(_ service: WidgetUsageService) -> some View {
        let row = service.rows[0]
        let ratio = row.quota.map { min(Double(row.used) / Double(max($0, 1)), 1) }
        return VStack(alignment: .leading, spacing: 2) {
            Text("\(service.name) · \(row.title)")
                .font(.caption2)
                .foregroundStyle(.secondary)
                .lineLimit(1)
            Spacer(minLength: 4)
            Text(ratio.map { "\(Int($0 * 100))%" } ?? row.valueText)
                .font(.system(size: 30, weight: .bold, design: .rounded))
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.6)
                .contentTransition(.numericText())
            Text(row.quota.map { "\(row.valueText) / \($0.formatted(.number.notation(.compactName)))" } ?? row.valueText)
                .font(.caption2)
                .foregroundStyle(.secondary)
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            HorizonScene(
                date: entry.date,
                gaugeRatio: ratio,
                gaugeTint: ratio.map(barColor) ?? .ocOrange
            )
            .frame(height: 36)
            .padding(.horizontal, -14)
        }
        .padding([.horizontal, .top], 14)
        .padding(.bottom, 8)
    }

    /// 中：该服务全部指标进度条（天空打底，轨道中性、警示渐进保留）
    private func mediumView(_ service: WidgetUsageService) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("\(service.name) 用量")
                    .font(.caption.weight(.semibold))
                Spacer()
                Text("今日 · 本账期")
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
            }
            ForEach(Array(service.rows.prefix(3).enumerated()), id: \.offset) { _, row in
                usageBar(row)
            }
        }
        .padding(14)
    }

    private func usageBar(_ row: WidgetUsageRow) -> some View {
        VStack(spacing: 2) {
            HStack {
                Text(row.title)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                Spacer()
                Text(row.quota.map { "\(row.valueText) / \($0.formatted(.number.notation(.compactName)))" } ?? row.valueText)
                    .font(.caption2.weight(.semibold))
                    .monospacedDigit()
            }
            if let quota = row.quota, quota > 0 {
                let ratio = min(Double(row.used) / Double(quota), 1)
                GeometryReader { proxy in
                    ZStack(alignment: .leading) {
                        Capsule().fill(Color.primary.opacity(0.08))
                        Capsule()
                            .fill(barColor(ratio))
                            .frame(width: max(proxy.size.width * ratio, 3))
                    }
                }
                .frame(height: 4)
            }
        }
    }

    private func barColor(_ ratio: Double) -> Color {
        if ratio >= 0.9 { return .red }
        if ratio >= 0.7 { return .orange }
        return .ocOrange
    }
}
