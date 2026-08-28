//
//  ZoneDetailView.swift
//  Orange Cloud Watch Watch App
//
//  单个域名的 24h 详情：请求 hero + 趋势 + 折线 + 命中率/威胁/访客/带宽。
//  进入时若有有效 token 则实时刷新该 Zone，否则用桥接快照。
//

import SwiftUI

struct ZoneDetailView: View {

    @Environment(WatchBridge.self) private var bridge
    private let zone: WidgetZoneMetrics
    @State private var current: WidgetZoneMetrics

    init(zone: WidgetZoneMetrics) {
        self.zone = zone
        _current = State(initialValue: zone)
    }

    var body: some View {
        ZStack {
            WatchSky()
            ScrollView {
                VStack(alignment: .leading, spacing: 8) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(current.requests.watchCompact)
                            .font(.system(size: 28, weight: .bold, design: .rounded))
                            .monospacedDigit()
                            .minimumScaleFactor(0.6)
                            .lineLimit(1)
                            .contentTransition(.numericText())
                        HStack(spacing: 4) {
                            Text("请求 · 24h")
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                            TrendBadge(trend: current.requestsTrend)
                        }
                    }
                    if current.requestsSeries.count > 1 {
                        WatchSparkline(series: current.requestsSeries)
                            .frame(height: 28)
                    }
                    metricGrid
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 6)
                .padding(.bottom, 8)
            }
        }
        .navigationTitle(current.name)
        .task {
            await bridge.refreshZone(id: zone.id, name: zone.name)
            if let updated = bridge.zones.first(where: { $0.id == zone.id }) {
                current = updated
            }
        }
    }

    private var metricGrid: some View {
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 6) {
            MetricTile(title: String(localized: "命中率"), value: current.cacheHitRate.map { "\(Int($0))%" } ?? "—")
            MetricTile(title: String(localized: "威胁"),   value: current.threats.watchCompact)
            MetricTile(title: String(localized: "访客"),   value: current.uniques.watchCompact)
            MetricTile(title: String(localized: "带宽"),   value: Int64(current.bytes).ocBytes)
        }
    }
}

private struct MetricTile: View {
    let title: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 1) {
            Text(title)
                .font(.caption2)
                .foregroundStyle(.secondary)
            Text(value)
                .font(.system(.footnote, design: .rounded).weight(.semibold))
                .monospacedDigit()
                .minimumScaleFactor(0.6)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(8)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 10))
    }
}
