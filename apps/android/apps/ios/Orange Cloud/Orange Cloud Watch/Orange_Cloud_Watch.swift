//
//  Orange_Cloud_Watch.swift
//  Orange Cloud Watch（表盘 complication）
//
//  账号总览表盘组件：全账号 24h 请求合计。数据读 App Group 快照
//  （由 watch App 接收 iPhone 推送后写入），离线也可显示。
//

import WidgetKit
import SwiftUI

nonisolated struct WatchComplicationEntry: TimelineEntry {
    let date: Date
    let totalRequests: Int
    let series: [Int]
    let topZone: String?
}

nonisolated struct WatchComplicationProvider: TimelineProvider {

    func placeholder(in context: Context) -> WatchComplicationEntry {
        WatchComplicationEntry(date: .now, totalRequests: 2_418_650, series: Self.sample, topZone: "example.com")
    }

    func getSnapshot(in context: Context, completion: @escaping (WatchComplicationEntry) -> Void) {
        completion(currentEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<WatchComplicationEntry>) -> Void) {
        let next = Calendar.current.date(byAdding: .minute, value: 30, to: .now) ?? .now
        completion(Timeline(entries: [currentEntry()], policy: .after(next)))
    }

    private func currentEntry() -> WatchComplicationEntry {
        let zones = WidgetDataStore.loadZones()
        let total = zones.reduce(0) { $0 + $1.requests }
        let top = zones.max(by: { $0.requests < $1.requests })?.name
        let length = zones.map(\.requestsSeries.count).max() ?? 0
        var summed: [Int] = []
        if length > 1 {
            summed = [Int](repeating: 0, count: length)
            for zone in zones {
                let offset = length - zone.requestsSeries.count
                for (index, value) in zone.requestsSeries.enumerated() where offset + index < length {
                    summed[offset + index] += value
                }
            }
        }
        return WatchComplicationEntry(date: .now, totalRequests: total, series: summed, topZone: top)
    }

    private static let sample = [42, 50, 47, 61, 58, 72, 90, 84, 95, 110, 104, 96, 88, 92, 81, 76, 70, 64, 58, 66, 61, 55, 50, 57]
}

struct AccountComplication: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "AccountComplication", provider: WatchComplicationProvider()) { entry in
            ComplicationView(entry: entry)
                .containerBackground(.clear, for: .widget)
        }
        .configurationDisplayName("账号总览")
        .description("全账号 24 小时请求合计")
        .supportedFamilies([.accessoryCircular, .accessoryRectangular, .accessoryInline, .accessoryCorner])
    }
}

struct ComplicationView: View {

    @Environment(\.widgetFamily) private var family
    let entry: WatchComplicationEntry

    private var compact: String {
        entry.totalRequests > 0 ? entry.totalRequests.formatted(.number.notation(.compactName)) : "—"
    }

    var body: some View {
        switch family {
        case .accessoryInline:
            Label("\(compact) 请求", systemImage: "chart.bar.fill")

        case .accessoryCircular:
            ZStack {
                AccessoryWidgetBackground()
                VStack(spacing: -1) {
                    Text(compact)
                        .font(.system(.title3, design: .rounded).weight(.semibold))
                        .minimumScaleFactor(0.5)
                        .lineLimit(1)
                    Text("请求")
                        .font(.system(size: 9))
                        .foregroundStyle(.secondary)
                }
                .padding(2)
            }

        case .accessoryCorner:
            Text(compact)
                .font(.system(.title3, design: .rounded).weight(.semibold))
                .minimumScaleFactor(0.5)
                .widgetLabel("请求 · 24h")

        case .accessoryRectangular:
            HStack(spacing: 6) {
                VStack(alignment: .leading, spacing: 1) {
                    Text("请求 · 24h")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                    Text(compact)
                        .font(.system(.title3, design: .rounded).weight(.bold))
                        .minimumScaleFactor(0.6)
                        .lineLimit(1)
                        .widgetAccentable()
                    if let topZone = entry.topZone {
                        Text(topZone)
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                    }
                }
                if entry.series.count > 1 {
                    ComplicationSparkline(series: entry.series)
                        .frame(width: 38)
                        .widgetAccentable()
                }
            }

        default:
            Text(compact)
        }
    }
}

/// 表盘用迷你折线（单色，vibrant 下用前景色描边）
nonisolated struct ComplicationSparkline: View {
    var series: [Int]

    var body: some View {
        GeometryReader { geo in
            let points = normalized(in: geo.size)
            if points.count > 1 {
                Path { path in
                    path.move(to: points[0])
                    for point in points.dropFirst() { path.addLine(to: point) }
                }
                .stroke(.foreground, style: StrokeStyle(lineWidth: 1.6, lineCap: .round, lineJoin: .round))
            }
        }
        .accessibilityHidden(true)
    }

    private func normalized(in size: CGSize) -> [CGPoint] {
        guard series.count > 1 else { return [] }
        let maxValue = series.max() ?? 1
        let minValue = series.min() ?? 0
        let span = CGFloat(max(maxValue - minValue, 1))
        let lastIndex = CGFloat(series.count - 1)
        return series.enumerated().map { index, value in
            CGPoint(x: CGFloat(index) / lastIndex * size.width,
                    y: size.height - CGFloat(value - minValue) / span * size.height)
        }
    }
}
