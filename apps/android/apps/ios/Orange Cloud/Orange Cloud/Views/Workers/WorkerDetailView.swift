//
//  WorkerDetailView.swift
//  Orange Cloud
//
//  Workers 脚本详情：元数据 + 指标（请求/错误/CPU/状态分解/趋势图）+ 实时日志入口。
//

import SwiftUI
import Charts

struct WorkerDetailView: View {

    let script: CachedWorkerScript
    let session: SessionStore

    @Environment(AuthManager.self) private var auth
    @Environment(EntitlementStore.self) private var entitlements
    @State private var metricsViewModel: WorkerMetricsViewModel
    @State private var uploadViewModel: WorkerUploadViewModel
    @State private var showUpload = false
    @State private var uploadDenied = false
    @State private var editPaywallPresented = false

    init(script: CachedWorkerScript, session: SessionStore) {
        self.script = script
        self.session = session
        _metricsViewModel = State(initialValue: WorkerMetricsViewModel(
            analyticsService: session.analyticsService,
            accountId: script.accountId,
            scriptName: script.id
        ))
        _uploadViewModel = State(initialValue: WorkerUploadViewModel(
            service: session.workerService,
            accountId: script.accountId
        ))
    }

    private var canViewMetrics: Bool { auth.hasScope("account-analytics.read") }
    private var canWrite: Bool { auth.hasScope("workers-scripts.write") }

    var body: some View {
        List {
            Section("信息") {
                if let usageModel = script.usageModel {
                    LabeledContent("Usage Model", value: usageModel)
                }
                if !script.handlers.isEmpty {
                    LabeledContent("Handlers", value: script.handlers.joined(separator: ", "))
                }
                LabeledContent("Logpush", value: script.logpush ? String(localized: "开启") : String(localized: "关闭"))
                if let created = WorkerScript.parseDate(script.createdOn) {
                    LabeledContent("创建时间") {
                        Text(created, format: .dateTime.year().month().day().hour().minute())
                    }
                }
                if let modified = WorkerScript.parseDate(script.modifiedOn) {
                    LabeledContent("最近部署") {
                        Text(modified, format: .relative(presentation: .named))
                    }
                }
            }
            .glassRow()

            metricsSection
                .glassRow()

            Section("管理") {
                Button {
                    // 编辑（更新代码）收进 Pro：非 Pro 先弹付费墙，Pro 再走 scope 门控
                    if !entitlements.isPro {
                        editPaywallPresented = true
                    } else if canWrite {
                        showUpload = true
                    } else {
                        uploadDenied = true
                    }
                } label: {
                    HStack(spacing: 12) {
                        TintIcon(systemImage: "arrow.up.doc", color: .ocOrange)
                        Text("更新代码").foregroundStyle(.primary)
                        Spacer()
                        if entitlements.isPro {
                            Image(systemName: "chevron.right")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(.tertiary)
                        } else {
                            ProBadge()
                        }
                    }
                }
                ProGatedNavigationLink(
                    label: String(localized: "变量与密钥"),
                    systemImage: "key",
                    requiredScope: "workers-scripts.read",
                    feature: .workerSecrets
                ) {
                    WorkerSecretsView(accountId: script.accountId, scriptName: script.id, session: session)
                }
                ProGatedNavigationLink(
                    label: String(localized: "触发器"),
                    systemImage: "clock",
                    requiredScope: "workers-scripts.read",
                    feature: .workerTriggers
                ) {
                    WorkerTriggersView(accountId: script.accountId, scriptName: script.id, session: session)
                }
                PermissionGatedNavigationLink(
                    label: String(localized: "构建"),
                    systemImage: "hammer.circle",
                    requiredScope: "workers-ci.read",
                    tint: .teal,
                    showsChevron: true
                ) {
                    WorkerBuildsView(scriptName: script.id, session: session)
                }
                ProGatedNavigationLink(
                    label: String(localized: "部署历史"),
                    systemImage: "clock.arrow.circlepath",
                    requiredScope: "workers-scripts.read",
                    feature: .workerRoutes
                ) {
                    WorkerDeploymentsView(accountId: script.accountId, scriptName: script.id, session: session)
                }
                ProGatedNavigationLink(
                    label: String(localized: "域名"),
                    systemImage: "globe",
                    requiredScope: "workers-scripts.read",
                    feature: .workerRoutes
                ) {
                    WorkerRoutesView(accountId: script.accountId, scriptName: script.id, session: session)
                }
            }
            .glassRow()

            Section("调试") {
                ProGatedNavigationLink(
                    label: String(localized: "实时日志"),
                    systemImage: "text.alignleft",
                    requiredScope: "workers-tail.read",
                    feature: .workerTail
                ) {
                    WorkerTailView(accountId: script.accountId, scriptName: script.id, session: session)
                }
            }
            .glassRow()
        }
        .daybreakList()
        .navigationTitle(script.id)
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showUpload) {
            WorkerUploadView(mode: .replace(scriptName: script.id), viewModel: uploadViewModel) {}
        }
        .sheet(isPresented: $editPaywallPresented) {
            PaywallView(feature: .workerEdit)
        }
        .sensoryFeedback(.success, trigger: uploadViewModel.didUpload)
        .alert("权限不足", isPresented: $uploadDenied) {
            Button("好", role: .cancel) {}
        } message: {
            Text("当前授权未包含 Workers 写权限（workers-scripts.write）。\n请在设置中退出登录后重新授权以启用此功能。")
        }
        .task(id: metricsViewModel.range) {
            guard canViewMetrics else { return }
            await metricsViewModel.load()
        }
        .refreshable {
            guard canViewMetrics else { return }
            await metricsViewModel.refresh()
        }
    }

    // MARK: - 指标区

    @ViewBuilder
    private var metricsSection: some View {
        if !canViewMetrics {
            Section("指标") {
                Label("需要「流量分析」权限才能展示调用指标", systemImage: "lock")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        } else {
            Section {
                Picker("时间范围", selection: $metricsViewModel.range) {
                    ForEach(AnalyticsTimeRange.allCases) { range in
                        Text(range.label).tag(range)
                    }
                }
                .pickerStyle(.segmented)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets())

                if metricsViewModel.isLoading && metricsViewModel.metrics == nil {
                    Group {
                        SkeletonBlock(height: 130, cornerRadius: 12)
                            .padding(.vertical, 4)
                        ForEach(0..<3, id: \.self) { index in
                            HStack {
                                SkeletonBlock(width: 56 + CGFloat((index * 29) % 30), height: 11)
                                Spacer()
                                SkeletonBlock(width: 70, height: 11)
                            }
                        }
                    }
                    .skeletonPulse()
                } else if let metrics = metricsViewModel.metrics {
                    if !metricsViewModel.series.isEmpty {
                        seriesChart
                            .frame(height: 130)
                            .padding(.vertical, 4)
                    }

                    LabeledContent("请求") {
                        Text(metrics.requests.formatted())
                            .monospacedDigit()
                    }
                    LabeledContent("错误") {
                        HStack(spacing: 6) {
                            Text(metrics.errors.formatted())
                                .monospacedDigit()
                            if let rate = metrics.errorRate, metrics.errors > 0 {
                                Text(String(format: "(%.2f%%)", rate))
                                    .foregroundStyle(rate >= 1 ? .red : .secondary)
                            }
                        }
                    }
                    LabeledContent("子请求") {
                        Text(metrics.subrequests.formatted())
                            .monospacedDigit()
                    }
                    if let totalUs = metrics.cpuTotalUs {
                        LabeledContent("CPU 合计") {
                            Text(Int(totalUs / 1000).formatted(.number.notation(.compactName)) + " ms")
                                .monospacedDigit()
                        }
                    }
                    if let p50 = metrics.cpuP50Us {
                        LabeledContent("CPU 单次") {
                            Text(String(format: "P50 %.1f ms · P99 %.1f ms",
                                        p50 / 1000, (metrics.cpuP99Us ?? 0) / 1000))
                                .font(.subheadline)
                                .monospacedDigit()
                        }
                    }
                } else if metricsViewModel.accountAnalyticsUnavailable {
                    Label("此账号暂无账户级数据查询权限", systemImage: "chart.bar.xaxis")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                } else if let error = metricsViewModel.error {
                    Label(error, systemImage: "exclamationmark.triangle")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            } header: {
                Text("指标 · \(metricsViewModel.range.periodLabel)")
            }

            // 按调用状态分解
            if let metrics = metricsViewModel.metrics, !metrics.statusBreakdown.isEmpty {
                Section("调用状态") {
                    ForEach(metrics.statusBreakdown, id: \.status) { item in
                        HStack {
                            Circle()
                                .fill(statusColor(item.status))
                                .frame(width: 8, height: 8)
                            Text(WorkerInvocationStatus.label(item.status))
                            Spacer()
                            Text(item.requests.formatted())
                                .foregroundStyle(.secondary)
                                .monospacedDigit()
                        }
                    }
                }
            }
        }
    }

    private func statusColor(_ status: String) -> Color {
        if WorkerInvocationStatus.isHealthy(status) { return .green }
        if WorkerInvocationStatus.isNeutral(status) { return .gray }
        return .red
    }

    // MARK: - 趋势图（请求橙色面积线，错误红色线）

    private var seriesChart: some View {
        Chart {
            ForEach(metricsViewModel.series) { point in
                AreaMark(
                    x: .value("时间", point.date),
                    y: .value("请求", point.requests)
                )
                .interpolationMethod(.monotone)
                .foregroundStyle(
                    LinearGradient(
                        colors: [Color.ocOrange.opacity(0.25), Color.ocOrange.opacity(0.02)],
                        startPoint: .top, endPoint: .bottom
                    )
                )
                .accessibilityHidden(true)
                LineMark(
                    x: .value("时间", point.date),
                    y: .value("请求", point.requests)
                )
                .interpolationMethod(.monotone)
                .foregroundStyle(Color.ocOrange)
                .lineStyle(StrokeStyle(lineWidth: 2, lineCap: .round))
                .accessibilityLabel(Text(point.date, format: .dateTime.hour().minute()))
                .accessibilityValue(Text("\(point.requests) 次请求"))
            }

            if metricsViewModel.series.contains(where: { $0.errors > 0 }) {
                ForEach(metricsViewModel.series) { point in
                    LineMark(
                        x: .value("时间", point.date),
                        y: .value("错误", point.errors),
                        series: .value("指标", String(localized: "错误"))
                    )
                    .interpolationMethod(.monotone)
                    .foregroundStyle(.red)
                    .lineStyle(StrokeStyle(lineWidth: 1.5, lineCap: .round))
                    .accessibilityLabel(Text(point.date, format: .dateTime.hour().minute()))
                    .accessibilityValue(Text("\(point.errors) 个错误"))
                }
            }
        }
        .chartYAxis {
            AxisMarks(position: .trailing, values: .automatic(desiredCount: 3)) { value in
                AxisGridLine(stroke: StrokeStyle(lineWidth: 0.5, dash: [3, 3]))
                AxisValueLabel {
                    if let intValue = value.as(Int.self) {
                        Text(intValue.formatted(.number.notation(.compactName)))
                            .font(.caption2)
                    }
                }
            }
        }
        .chartXAxis(.hidden)
    }
}
