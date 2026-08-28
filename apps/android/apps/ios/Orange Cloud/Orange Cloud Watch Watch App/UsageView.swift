//
//  UsageView.swift
//  Orange Cloud Watch Watch App
//
//  账号用量：按服务（Workers/R2/D1/KV）分组，每项额度做线性仪表。
//  数据来自 iPhone 桥接的用量快照。
//

import SwiftUI

struct UsageView: View {

    let usage: WidgetUsageData

    var body: some View {
        ZStack {
            WatchSky()
            ScrollView {
                VStack(spacing: 8) {
                    ForEach(usage.services) { service in
                        ServiceCard(service: service)
                    }
                }
                .padding(.horizontal, 6)
                .padding(.bottom, 8)
            }
        }
        .navigationTitle("用量")
    }
}

private struct ServiceCard: View {
    let service: WidgetUsageService

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(service.name)
                .font(.footnote.weight(.semibold))
            ForEach(Array(service.rows.prefix(3).enumerated()), id: \.offset) { _, row in
                UsageRowView(row: row)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(10)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 14))
    }
}

private struct UsageRowView: View {
    let row: WidgetUsageRow

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack {
                Text(row.title)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                Spacer(minLength: 2)
                Text(row.quota.map { "\(row.valueText) / \($0.watchCompact)" } ?? row.valueText)
                    .font(.caption2.weight(.semibold))
                    .monospacedDigit()
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
            }
            if let quota = row.quota, quota > 0 {
                let ratio = min(Double(row.used) / Double(quota), 1)
                Gauge(value: ratio) { EmptyView() }
                    .gaugeStyle(.accessoryLinearCapacity)
                    .tint(barColor(ratio))
            }
        }
    }

    private func barColor(_ ratio: Double) -> Color {
        if ratio >= 0.9 { return .red }
        if ratio >= 0.7 { return .orange }
        return .ocOrange
    }
}
