//
//  AlertCenterCard.swift
//  Orange Cloud
//
//  概览页「告警中心」：最多 5 条，严重度用不同配色圆点；全部正常时显示「暂无告警」。
//  规则在 Core/Dashboard/DashboardAlerts.swift（纯函数、不发请求），本文件只负责呈现。
//

import SwiftUI

struct AlertCenterCard: View {

    let alerts: [DashboardAlert]
    /// 点击一条告警：由概览页栈根的 navdest 承接（值式路由，勿改 eager NavigationLink）
    let onSelect: (DashboardResourceRoute) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            header
            content
        }
    }

    @ViewBuilder
    private var header: some View {
        HStack {
            Text("告警")
                .font(.title3.bold())
            Spacer()
            if !alerts.isEmpty {
                Text("\(alerts.count)")
                    .font(.caption.weight(.semibold).monospacedDigit())
                    .foregroundStyle(.secondary)
            }
        }
    }

    @ViewBuilder
    private var content: some View {
        if alerts.isEmpty {
            emptyIsland
        } else {
            VStack(spacing: 0) {
                ForEach(alerts) { alert in
                    row(alert)
                    if alert.id != alerts.last?.id {
                        Divider().padding(.leading, 30)
                    }
                }
            }
            .glassIsland()
        }
    }

    private var emptyIsland: some View {
        HStack(spacing: 10) {
            SeverityDot(severity: .ok)
            Text("暂无告警，一切正常")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Spacer(minLength: 0)
        }
        .padding(.horizontal, OCLayout.islandPadding)
        .padding(.vertical, 14)
        .glassIsland(cornerRadius: OCLayout.chipRadius)
    }

    @ViewBuilder
    private func row(_ alert: DashboardAlert) -> some View {
        if let route = alert.route {
            Button {
                onSelect(route)
            } label: {
                AlertRow(alert: alert, showsChevron: true)
            }
            .buttonStyle(.plain)
        } else {
            AlertRow(alert: alert, showsChevron: false)
        }
    }
}

/// 单条告警行（圆点 + 标题 + 说明）
private struct AlertRow: View {

    let alert: DashboardAlert
    let showsChevron: Bool

    var body: some View {
        HStack(spacing: 10) {
            SeverityDot(severity: alert.severity)
            VStack(alignment: .leading, spacing: 2) {
                Text(alert.title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)
                    .lineLimit(1)
                Text(alert.detail)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
            Spacer(minLength: 8)
            if showsChevron {
                Image(systemName: "chevron.right")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.tertiary)
            }
        }
        .padding(.horizontal, OCLayout.islandPadding)
        .padding(.vertical, 10)
        .contentShape(Rectangle())
    }
}

/// 严重度圆点：critical 红 / warn 橙 / info 蓝 / ok 绿；
/// 开启「不用颜色区分」时换成带形状的符号，不只靠颜色传达。
private struct SeverityDot: View {

    let severity: DashboardAlert.Severity

    @Environment(\.accessibilityDifferentiateWithoutColor) private var differentiateWithoutColor

    private var color: Color {
        switch severity {
        case .critical: .red
        case .warn:     .ocOrange
        case .info:     .blue
        case .ok:       .green
        }
    }

    private var glyph: String {
        switch severity {
        case .critical: "exclamationmark.circle.fill"
        case .warn:     "exclamationmark.triangle.fill"
        case .info:     "info.circle.fill"
        case .ok:       "checkmark.circle.fill"
        }
    }

    private var label: String {
        switch severity {
        case .critical: String(localized: "严重")
        case .warn:     String(localized: "警告")
        case .info:     String(localized: "提示")
        case .ok:       String(localized: "正常")
        }
    }

    var body: some View {
        Group {
            if differentiateWithoutColor {
                Image(systemName: glyph)
                    .font(.system(size: 12))
                    .foregroundStyle(color)
            } else {
                Circle()
                    .fill(color)
                    .frame(width: 8, height: 8)
                    .background(
                        Circle()
                            .fill(color.opacity(0.13))
                            .frame(width: 14, height: 14)
                    )
            }
        }
        .frame(width: 16)
        .accessibilityLabel(label)
    }
}
