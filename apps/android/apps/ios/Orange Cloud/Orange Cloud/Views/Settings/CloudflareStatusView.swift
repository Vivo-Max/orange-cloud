//
//  CloudflareStatusView.swift
//  Orange Cloud
//
//  Cloudflare 官方服务状态（cloudflarestatus.com）：
//  总体状态、进行中的事件、计划维护、受影响组件、近期事件历史。
//

import SwiftUI

struct CloudflareStatusView: View {

    @State private var viewModel = CloudflareStatusViewModel()

    var body: some View {
        Group {
            if viewModel.overall == nil && viewModel.isLoading {
                statusSkeleton
            } else if let overall = viewModel.overall {
                statusList(overall)
            } else {
                ContentUnavailableView {
                    Label("加载失败", systemImage: "wifi.exclamationmark")
                } description: {
                    Text(viewModel.error ?? "")
                } actions: {
                    Button("重试") {
                        Task { await viewModel.load() }
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(Color.ocOrangePressed)
                    .fontWeight(.bold)
                }
            }
        }
        .background { SkyBackground() }
        .navigationTitle("Cloudflare 状态")
        .navigationBarTitleDisplayMode(.inline)
        .task { await viewModel.load() }
    }

    // MARK: - 骨架（总体状态一行 + 产品服务若干行）

    private var statusSkeleton: some View {
        List {
            Section {
                SkeletonRow(icon: .circle(10), titleWidth: 170, subtitleWidth: nil)
            }
            .glassRow()
            Section {
                ForEach(0..<5, id: \.self) { index in
                    SkeletonRow(
                        icon: .circle(8),
                        titleWidth: 110 + CGFloat((index * 43) % 80),
                        subtitleWidth: nil,
                        trailingWidth: 40
                    )
                }
            } header: {
                SkeletonBlock(width: 60, height: 10)
            }
            .glassRow()
        }
        .daybreakList()
        .scrollDisabled(true)
        .skeletonPulse()
    }

    // MARK: - 列表

    private func statusList(_ overall: StatusPageOverall) -> some View {
        List {
            // ── 总体状态 ──
            Section {
                HStack(spacing: 12) {
                    StatusDot(status: dotStatus(for: overall.indicator), size: 10)
                    Text(overall.localizedText)
                        .font(.body.weight(.semibold))
                }
                .padding(.vertical, 4)
            }
            .glassRow()

            // ── 进行中的事件 ──
            if !viewModel.activeIncidents.isEmpty {
                Section("进行中的事件") {
                    ForEach(viewModel.activeIncidents) { incident in
                        incidentRow(incident)
                    }
                }
                .glassRow()
            }

            // ── 计划维护 ──
            if !viewModel.maintenances.isEmpty {
                Section("计划维护") {
                    ForEach(viewModel.maintenances) { maintenance in
                        incidentRow(maintenance)
                    }
                }
                .glassRow()
            }

            // ── 产品服务 ──
            Section("产品服务") {
                if viewModel.affectedProducts.isEmpty {
                    HStack(spacing: 12) {
                        TintIcon(systemImage: "checkmark.circle", color: .green, size: 28)
                        Text("\(viewModel.productTotal) 项产品服务全部正常")
                            .foregroundStyle(.secondary)
                    }
                } else {
                    ForEach(viewModel.affectedProducts) { component in
                        HStack(spacing: 12) {
                            StatusDot(status: dotStatus(forComponent: component.status))
                            Text(component.name)
                                .lineLimit(1)
                            Spacer()
                            Text(component.statusText)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            }
            .glassRow()

            // ── 边缘网络（按大区汇总，PoP 维护/重路由是常态，不逐个列）──
            if !viewModel.regions.isEmpty {
                Section("边缘网络") {
                    ForEach(viewModel.regions) { region in
                        HStack(spacing: 12) {
                            StatusDot(status: region.impacted == 0 ? "active" : "pending")
                            Text(region.localizedName)
                            Spacer()
                            if region.impacted == 0 {
                                Text("全部正常")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            } else {
                                Text("\(region.impacted) 个节点异常")
                                    .font(.caption)
                                    .foregroundStyle(.orange)
                            }
                        }
                    }
                }
                .glassRow()
            }

            // ── 近期事件 ──
            if !viewModel.recentIncidents.isEmpty {
                Section("最近事件") {
                    ForEach(viewModel.recentIncidents) { incident in
                        incidentRow(incident)
                    }
                }
                .glassRow()
            }

            // ── 完整状态页 ──
            Section {
                Link(destination: URL(string: "https://www.cloudflarestatus.com")!) {
                    HStack(spacing: 12) {
                        TintIcon(systemImage: "safari", color: .gray)
                        Text("在 cloudflarestatus.com 查看完整状态")
                            .foregroundStyle(.primary)
                        Spacer()
                        Image(systemName: "arrow.up.right")
                            .font(.caption)
                            .foregroundStyle(.tertiary)
                    }
                }
            }
            .glassRow()
        }
        .daybreakList()
        .refreshable { await viewModel.load() }
    }

    // MARK: - 行

    private func incidentRow(_ incident: StatusPageIncident) -> some View {
        NavigationLink {
            StatusIncidentDetailView(incident: incident)
        } label: {
            VStack(alignment: .leading, spacing: 3) {
                Text(incident.name)
                    .font(.callout.weight(.medium))
                    .lineLimit(2)
                HStack(spacing: 6) {
                    Text(incident.statusText)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(impactColor(incident.impact))
                    if let updated = WorkerScript.parseDate(incident.updatedAt) {
                        Text(updated, format: .relative(presentation: .named))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .padding(.vertical, 2)
        }
    }

    // MARK: - 颜色映射

    /// 总体 indicator → StatusDot 语义（active 绿 / pending 橙 / 其它红）
    private func dotStatus(for indicator: String) -> String {
        switch indicator {
        case "none":                 "active"
        case "minor", "maintenance": "pending"
        default:                     "paused"
        }
    }

    private func dotStatus(forComponent status: String) -> String {
        switch status {
        case "operational":                              "active"
        case "degraded_performance", "under_maintenance": "pending"
        default:                                         "paused"
        }
    }

    private func impactColor(_ impact: String) -> Color {
        switch impact {
        case "critical":    .red
        case "major":       .orange
        case "minor":       .yellow
        case "maintenance": .blue
        default:            .secondary
        }
    }
}

// MARK: - 事件详情（更新时间线）

struct StatusIncidentDetailView: View {

    let incident: StatusPageIncident

    var body: some View {
        List {
            Section {
                Text(incident.name)
                    .font(.headline)
                LabeledContent("状态", value: incident.statusText)
                LabeledContent("影响", value: incident.impactText)
                if let created = WorkerScript.parseDate(incident.createdAt) {
                    LabeledContent("开始时间") {
                        Text(created, format: .dateTime.year().month().day().hour().minute())
                    }
                }
                if let shortlink = incident.shortlink, let url = URL(string: shortlink) {
                    Link(destination: url) {
                        HStack(spacing: 12) {
                            Text("在状态页打开")
                                .foregroundStyle(.primary)
                            Spacer()
                            Image(systemName: "arrow.up.right")
                                .font(.caption)
                                .foregroundStyle(.tertiary)
                        }
                    }
                }
            }
            .glassRow()

            if let updates = incident.incidentUpdates, !updates.isEmpty {
                Section("更新记录") {
                    ForEach(updates) { update in
                        VStack(alignment: .leading, spacing: 4) {
                            HStack {
                                Text(update.statusText)
                                    .font(.caption.weight(.semibold))
                                    .foregroundStyle(Color.ocOrangeText)
                                Spacer()
                                if let date = WorkerScript.parseDate(update.displayAt) {
                                    Text(date, format: .dateTime.month().day().hour().minute())
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                }
                            }
                            Text(update.body)
                                .font(.callout)
                                .foregroundStyle(.primary)
                        }
                        .padding(.vertical, 2)
                    }
                }
                .glassRow()
            }
        }
        .daybreakList()
        .navigationTitle("事件详情")
        .navigationBarTitleDisplayMode(.inline)
    }
}
