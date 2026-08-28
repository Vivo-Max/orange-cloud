//
//  ContentView.swift
//  Orange Cloud Watch Watch App
//
//  根视图：账号 24h 概览 + 域名列表 + 用量入口。数据来自 iPhone 经 WatchConnectivity
//  推送的快照（落 App Group），离线也可见；有有效 token 时详情页可实时刷新。
//

import SwiftUI

struct ContentView: View {

    @Environment(WatchBridge.self) private var bridge

    var body: some View {
        NavigationStack {
            ZStack {
                WatchSky()
                if bridge.zones.isEmpty {
                    EmptyStateView(hasToken: bridge.hasToken)
                } else {
                    ScrollView {
                        VStack(spacing: 8) {
                            OverviewCard(
                                totalRequests: bridge.totalRequests,
                                series: bridge.aggregatedSeries,
                                updatedAt: bridge.lastUpdated
                            )
                            ForEach(bridge.zones) { zone in
                                NavigationLink {
                                    ZoneDetailView(zone: zone)
                                } label: {
                                    ZoneRow(zone: zone)
                                }
                                .buttonStyle(.plain)
                            }
                            if let usage = bridge.usage, !usage.services.isEmpty {
                                NavigationLink {
                                    UsageView(usage: usage)
                                } label: {
                                    UsageLinkRow()
                                }
                                .buttonStyle(.plain)
                            } else if bridge.accountAnalyticsUnavailable {
                                AccountUsageUnavailableRow()
                            }
                        }
                        .padding(.horizontal, 4)
                        .padding(.bottom, 8)
                    }
                }
            }
            .navigationTitle(bridge.accountName.isEmpty ? String(localized: "橙云") : bridge.accountName)
        }
        .onAppear { bridge.requestFreshTokenIfNeeded() }
    }
}

// MARK: - 概览卡

private struct OverviewCard: View {
    let totalRequests: Int
    let series: [Int]
    let updatedAt: Date?

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("请求 · 全部域名 · 24h")
                .font(.caption2)
                .foregroundStyle(.secondary)
            Text(totalRequests.watchCompact)
                .font(.system(size: 30, weight: .bold, design: .rounded))
                .monospacedDigit()
                .minimumScaleFactor(0.6)
                .lineLimit(1)
                .contentTransition(.numericText())
            if series.count > 1 {
                WatchSparkline(series: series)
                    .frame(height: 22)
                    .padding(.top, 2)
            }
            if let updatedAt {
                Text("更新于 \(updatedAt, format: .dateTime.hour().minute())")
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(10)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 14))
    }
}

// MARK: - 域名行

private struct ZoneRow: View {
    let zone: WidgetZoneMetrics

    var body: some View {
        HStack(spacing: 6) {
            VStack(alignment: .leading, spacing: 1) {
                Text(zone.name)
                    .font(.footnote.weight(.medium))
                    .lineLimit(1)
                Text("\(zone.requests.watchCompact) 请求")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .monospacedDigit()
            }
            Spacer(minLength: 2)
            TrendBadge(trend: zone.requestsTrend)
            Image(systemName: "chevron.forward")
                .font(.caption2)
                .foregroundStyle(.tertiary)
        }
        .padding(.vertical, 7)
        .padding(.horizontal, 10)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 12))
    }
}

/// 账户级数据无权限（免费账号）：用量入口替换为只读提示，不可点
private struct AccountUsageUnavailableRow: View {
    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: "lock")
                .foregroundStyle(.secondary)
            VStack(alignment: .leading, spacing: 1) {
                Text("账户级用量不可用")
                    .font(.caption.weight(.medium))
                    .lineLimit(1)
                Text("通常需付费版账号")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            Spacer(minLength: 2)
        }
        .padding(.vertical, 8)
        .padding(.horizontal, 10)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 12))
    }
}

private struct UsageLinkRow: View {
    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: "gauge.medium")
                .foregroundStyle(Color.ocOrange)
            Text("用量")
                .font(.footnote.weight(.medium))
            Spacer()
            Image(systemName: "chevron.forward")
                .font(.caption2)
                .foregroundStyle(.tertiary)
        }
        .padding(.vertical, 8)
        .padding(.horizontal, 10)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 12))
    }
}

// MARK: - 复用小件

/// 趋势徽标（与前一个 24h 比的百分比）
struct TrendBadge: View {
    let trend: Double?

    var body: some View {
        if let trend, trend.isFinite, abs(trend) >= 0.05 {
            HStack(spacing: 1) {
                Image(systemName: trend >= 0 ? "arrow.up" : "arrow.down")
                    .font(.system(size: 8, weight: .bold))
                Text("\(abs(trend), format: .number.precision(.fractionLength(0)))%")
                    .font(.caption2.weight(.semibold))
                    .monospacedDigit()
            }
            .foregroundStyle(trend >= 0 ? Color.green : Color.red)
        }
    }
}

private struct EmptyStateView: View {
    let hasToken: Bool

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: "cloud")
                .font(.title2)
                .foregroundStyle(.secondary)
            Text(hasToken ? "打开 iPhone App 同步数据" : "请在 iPhone 上登录")
                .font(.caption2)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding()
    }
}
