//
//  ZoneWidgets.swift
//  OrangeCloudWidgets
//
//  按域名配置的指标 Widget（长按可选择域名/指标），晨昏语言：
//  - ZoneStatWidget：小=天窗（大字 + 地平线弧）；中=山脊（指标地形 + 副指标）
//  - ZoneChartWidget：中=山脊请求地形；大=站点总览（地形 + 四指标行）
//  数据来自 App 刷新时写入的 App Group 快照。
//

import WidgetKit
import SwiftUI
import AppIntents

// （WidgetZoneEntity / ZoneWidgetMetric / 配置 Intent 在 Shared/WidgetConfigIntents.swift，两个 target 共享）

// MARK: - Entry / Provider

nonisolated struct ZoneWidgetEntry: TimelineEntry {
    let date: Date
    let zone: WidgetZoneMetrics?
    let metric: ZoneWidgetMetric
    var configuredName: String? = nil    // 所选域名（无数据时用于提示）
}

/// 显式选择时严格匹配——没有该域名的数据就返回 nil（显示提示），不静默回退到首个
nonisolated private func resolveZone(id: String?) -> WidgetZoneMetrics? {
    let zones = WidgetDataStore.loadZones()
    if let id {
        return zones.first { $0.id == id }
    }
    return zones.first
}

nonisolated private let sampleZone = WidgetZoneMetrics(
    id: "sample", name: "example.com",
    requests: 2_418_650, bytes: 184_000_000_000, threats: 38_200, uniques: 312_500,
    cacheHitRate: 94.2, requestsTrend: 12.4,
    requestsSeries: [42, 50, 47, 61, 58, 72, 90, 84, 95, 110, 104, 96, 88, 92, 81, 76, 70, 64, 58, 66, 61, 55, 50, 57],
    bytesSeries: [30, 41, 38, 52, 49, 60, 75, 70, 82, 95, 88, 80, 74, 78, 69, 64, 58, 52, 47, 55, 50, 45, 41, 47],
    updatedAt: .now
)

nonisolated struct ZoneStatProvider: AppIntentTimelineProvider {

    func placeholder(in context: Context) -> ZoneWidgetEntry {
        ZoneWidgetEntry(date: .now, zone: sampleZone, metric: .requests)
    }

    func snapshot(for configuration: ZoneStatConfigIntent, in context: Context) async -> ZoneWidgetEntry {
        ZoneWidgetEntry(
            date: .now,
            zone: resolveZone(id: configuration.zone?.id) ?? sampleZone,
            metric: configuration.resolvedMetric,
            configuredName: configuration.zone?.name
        )
    }

    func timeline(for configuration: ZoneStatConfigIntent, in context: Context) async -> Timeline<ZoneWidgetEntry> {
        let entry = ZoneWidgetEntry(
            date: .now,
            zone: await latestZone(for: configuration.zone),
            metric: configuration.resolvedMetric,
            configuredName: configuration.zone?.name
        )
        let next = Calendar.current.date(byAdding: .minute, value: 30, to: .now) ?? .now
        return Timeline(entries: [entry], policy: .after(next))
    }
}

/// 优先自取数（共享钥匙串的有效 token 直查 GraphQL），失败回退 App 写入的快照
nonisolated private func latestZone(for entity: WidgetZoneEntity?) async -> WidgetZoneMetrics? {
    let snapshot = resolveZone(id: entity?.id)
    guard let zoneId = entity?.id ?? snapshot?.id else { return snapshot }
    let name = entity?.name ?? snapshot?.name ?? zoneId
    if let fresh = await WidgetFetcher.freshZoneMetrics(zoneId: zoneId, name: name) {
        return fresh
    }
    return snapshot
}

nonisolated struct ZoneChartProvider: AppIntentTimelineProvider {

    func placeholder(in context: Context) -> ZoneWidgetEntry {
        ZoneWidgetEntry(date: .now, zone: sampleZone, metric: .requests)
    }

    func snapshot(for configuration: ZoneChartConfigIntent, in context: Context) async -> ZoneWidgetEntry {
        ZoneWidgetEntry(
            date: .now,
            zone: resolveZone(id: configuration.zone?.id) ?? sampleZone,
            metric: .requests,
            configuredName: configuration.zone?.name
        )
    }

    func timeline(for configuration: ZoneChartConfigIntent, in context: Context) async -> Timeline<ZoneWidgetEntry> {
        let entry = ZoneWidgetEntry(
            date: .now,
            zone: await latestZone(for: configuration.zone),
            metric: .requests,
            configuredName: configuration.zone?.name
        )
        let next = Calendar.current.date(byAdding: .minute, value: 30, to: .now) ?? .now
        return Timeline(entries: [entry], policy: .after(next))
    }
}

// MARK: - Widget 定义

struct ZoneStatWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "ZoneStatWidget", intent: ZoneStatConfigIntent.self, provider: ZoneStatProvider()) { entry in
            ZoneStatWidgetView(entry: entry)
                .daybreakContainer(date: entry.date)
        }
        .configurationDisplayName("域名指标")
        .description("单个域名的 24h 指标，可选请求/带宽/威胁/访客")
        .supportedFamilies([.systemSmall, .systemMedium, .accessoryInline, .accessoryCircular, .accessoryRectangular])
        .contentMarginsDisabled()
    }
}

struct ZoneChartWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "ZoneChartWidget", intent: ZoneChartConfigIntent.self, provider: ZoneChartProvider()) { entry in
            ZoneChartWidgetView(entry: entry)
                .daybreakContainer(date: entry.date)
        }
        .configurationDisplayName("请求地形")
        .description("域名 24h 请求地形；大尺寸含命中率与多指标总览")
        .supportedFamilies([.systemMedium, .systemLarge, .accessoryRectangular])
        .contentMarginsDisabled()
    }
}

// MARK: - 公共小组件

private struct TrendText: View {
    let trend: Double?

    var body: some View {
        if let trend, trend.isFinite, abs(trend) >= 0.05 {
            HStack(spacing: 1) {
                Image(systemName: trend >= 0 ? "arrow.up" : "arrow.down")
                    .font(.system(size: 9, weight: .bold))
                Text("\(abs(trend), format: .number.precision(.fractionLength(1)))%")
                    .font(.caption.weight(.semibold))
            }
            .foregroundStyle(trend >= 0 ? Color.green : Color.red)
        }
    }
}

private struct MicroStat: View {
    let title: String
    let value: String

    var body: some View {
        HStack(spacing: 4) {
            Text(title)
                .foregroundStyle(.secondary)
            Text(value)
                .fontWeight(.semibold)
                .monospacedDigit()
        }
        .font(.caption2)
    }
}

// MARK: - 域名指标视图

struct ZoneStatWidgetView: View {

    @Environment(\.widgetFamily) private var family
    let entry: ZoneWidgetEntry

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
        if let zone = entry.zone {
            switch family {
            case .systemMedium: mediumView(zone)
            default:            smallView(zone)
            }
        } else {
            WidgetEmptyHint(text: entry.configuredName.map { String(localized: "暂无 \($0) 数据\n打开 App 刷新") } ?? String(localized: "打开 App 同步数据"))
        }
    }

    // MARK: - 锁屏 accessory

    @ViewBuilder
    private var inlineView: some View {
        if let zone = entry.zone {
            Label {
                Text("\(zone.name) \(entry.metric.valueText(zone))")
            } icon: {
                Image(systemName: entry.metric.symbol)
            }
        } else {
            Label("Orange Cloud", systemImage: "cloud")
        }
    }

    @ViewBuilder
    private var circularView: some View {
        ZStack {
            AccessoryWidgetBackground()
            if let zone = entry.zone {
                VStack(spacing: -1) {
                    Image(systemName: entry.metric.symbol)
                        .font(.system(size: 10))
                        .foregroundStyle(.secondary)
                    Text(entry.metric.valueText(zone))
                        .font(.system(.callout, design: .rounded).weight(.semibold))
                        .minimumScaleFactor(0.4)
                        .lineLimit(1)
                }
                .padding(3)
            } else {
                Text("—").font(.title3)
            }
        }
    }

    @ViewBuilder
    private var rectangularView: some View {
        if let zone = entry.zone {
            VStack(alignment: .leading, spacing: 1) {
                Text(zone.name)
                    .font(.headline)
                    .widgetAccentable()
                    .lineLimit(1)
                HStack(alignment: .firstTextBaseline, spacing: 4) {
                    Text(entry.metric.valueText(zone))
                        .font(.system(.title3, design: .rounded).weight(.bold))
                        .monospacedDigit()
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                    if entry.metric == .requests {
                        TrendText(trend: zone.requestsTrend)
                    }
                }
                Text(entry.metric.label)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        } else {
            Text(entry.configuredName.map { String(localized: "暂无 \($0) 数据") } ?? String(localized: "打开 App 同步数据"))
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }

    /// 小（天窗）：单指标大字 + 地平线弧
    private func smallView(_ zone: WidgetZoneMetrics) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(zone.name)
                .font(.caption2.monospaced())
                .foregroundStyle(.secondary)
                .lineLimit(1)
            Spacer(minLength: 4)
            Text(entry.metric.valueText(zone))
                .font(.system(size: 30, weight: .bold, design: .rounded))
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.6)
                .contentTransition(.numericText())
            Text(entry.metric.label)
                .font(.caption2)
                .foregroundStyle(.secondary)
                .lineLimit(1)
            HorizonScene(date: entry.date)
                .frame(height: 36)
                .padding(.horizontal, -14)
        }
        .padding([.horizontal, .top], 14)
        .padding(.bottom, 8)
    }

    /// 中（山脊）：所选指标大字 + 两项副指标，24h 地形铺底
    private func mediumView(_ zone: WidgetZoneMetrics) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(zone.name)
                        .font(.caption2.monospaced())
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                    HStack(alignment: .firstTextBaseline, spacing: 6) {
                        Text(entry.metric.valueText(zone))
                            .font(.system(size: 26, weight: .bold, design: .rounded))
                            .monospacedDigit()
                            .lineLimit(1)
                            .minimumScaleFactor(0.6)
                            .contentTransition(.numericText())
                        if entry.metric == .requests {
                            TrendText(trend: zone.requestsTrend)
                        }
                    }
                    Text(entry.metric.label)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 3) {
                    ForEach(sideStats(zone), id: \.0) { stat in
                        MicroStat(title: stat.0, value: stat.1)
                    }
                }
            }
            .padding([.horizontal, .top], 14)
            RidgeScene(series: entry.metric.series(zone), date: entry.date)
                .frame(maxHeight: .infinity)
        }
    }

    /// 主指标之外挑两项放右上角
    private func sideStats(_ zone: WidgetZoneMetrics) -> [(String, String)] {
        let all: [(ZoneWidgetMetric, String, String)] = [
            (.requests,  String(localized: "请求"), zone.requests.formatted(.number.notation(.compactName))),
            (.threats,   String(localized: "拦截"), zone.threats.formatted(.number.notation(.compactName))),
            (.visitors,  String(localized: "访客"), zone.uniques.formatted(.number.notation(.compactName))),
            (.bandwidth, String(localized: "带宽"), Int64(zone.bytes).ocBytes),
        ]
        return Array(all.filter { $0.0 != entry.metric }.prefix(2).map { ($0.1, $0.2) })
    }
}

// MARK: - 请求地形 / 总览视图

struct ZoneChartWidgetView: View {

    @Environment(\.widgetFamily) private var family
    let entry: ZoneWidgetEntry

    @ViewBuilder
    var body: some View {
        switch family {
        case .accessoryRectangular: rectangularView
        default:                    systemBody
        }
    }

    @ViewBuilder
    private var systemBody: some View {
        if let zone = entry.zone {
            switch family {
            case .systemLarge: overviewView(zone)
            default:           chartView(zone)
            }
        } else {
            WidgetEmptyHint(text: entry.configuredName.map { String(localized: "暂无 \($0) 数据\n打开 App 刷新") } ?? String(localized: "打开 App 同步数据"))
        }
    }

    // MARK: - 锁屏 accessory（矩形迷你折线）

    @ViewBuilder
    private var rectangularView: some View {
        if let zone = entry.zone {
            VStack(alignment: .leading, spacing: 2) {
                HStack(alignment: .firstTextBaseline) {
                    Text(zone.name)
                        .font(.caption.weight(.semibold))
                        .lineLimit(1)
                    Spacer(minLength: 4)
                    Text(zone.requests.formatted(.number.notation(.compactName)))
                        .font(.caption.weight(.bold))
                        .monospacedDigit()
                }
                AccessorySparkline(series: zone.requestsSeries)
                    .widgetAccentable()
                    .frame(maxHeight: .infinity)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            Text(entry.configuredName.map { String(localized: "暂无 \($0) 数据") } ?? String(localized: "打开 App 同步数据"))
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }

    /// 中（山脊）：完整请求数 + 趋势，24h 地形铺底
    private func chartView(_ zone: WidgetZoneMetrics) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(zone.name)
                        .font(.caption2.monospaced())
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                    HStack(alignment: .firstTextBaseline, spacing: 6) {
                        Text(zone.requests.formatted())
                            .font(.system(size: 26, weight: .bold, design: .rounded))
                            .monospacedDigit()
                            .lineLimit(1)
                            .minimumScaleFactor(0.6)
                            .contentTransition(.numericText())
                        TrendText(trend: zone.requestsTrend)
                    }
                    Text("总请求 · 过去 24 小时")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 3) {
                    MicroStat(title: String(localized: "拦截"), value: zone.threats.formatted(.number.notation(.compactName)))
                    MicroStat(title: String(localized: "访客"), value: zone.uniques.formatted(.number.notation(.compactName)))
                }
            }
            .padding([.horizontal, .top], 14)
            RidgeScene(series: zone.requestsSeries, date: entry.date)
                .frame(maxHeight: .infinity)
        }
    }

    /// 大（山脊总览）：请求 + 趋势 + 地形 + 四指标行
    private func overviewView(_ zone: WidgetZoneMetrics) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(zone.name)
                        .font(.caption.monospaced())
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                    Spacer()
                    Text("更新于 \(zone.updatedAt, format: .dateTime.hour().minute())")
                        .font(.caption2)
                        .foregroundStyle(.tertiary)
                }
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text(zone.requests.formatted())
                        .font(.system(size: 32, weight: .bold, design: .rounded))
                        .monospacedDigit()
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                        .contentTransition(.numericText())
                    TrendText(trend: zone.requestsTrend)
                }
                Text("总请求 · 过去 24 小时")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            .padding([.horizontal, .top], 16)

            RidgeScene(series: zone.requestsSeries, date: entry.date)
                .frame(maxHeight: .infinity)

            HStack(spacing: 0) {
                if let hitRate = zone.cacheHitRate {
                    overviewStat(String(localized: "命中率"), "\(Int(hitRate))%")
                }
                overviewStat(String(localized: "带宽"), Int64(zone.bytes).ocBytes)
                overviewStat(String(localized: "威胁"), zone.threats.formatted(.number.notation(.compactName)))
                overviewStat(String(localized: "访客"), zone.uniques.formatted(.number.notation(.compactName)))
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(.primary.opacity(0.04))
        }
    }

    private func overviewStat(_ title: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(.caption2)
                .foregroundStyle(.secondary)
            Text(value)
                .font(.system(size: 16, weight: .bold, design: .rounded))
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
