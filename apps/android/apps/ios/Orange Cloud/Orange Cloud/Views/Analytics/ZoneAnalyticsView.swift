//
//  ZoneAnalyticsView.swift
//  Orange Cloud
//
//  Zone 流量分析（内嵌在域名详情页第一层级）：
//  总请求大数字卡 + 面积图、缓存命中率环形仪表、2 列小卡（带宽/威胁/访客/PV，迷你走势）。
//  所有趋势为与前一等长周期的环比。
//

import SwiftUI
import Charts

struct ZoneAnalyticsSection: View {

    /// 宿主页持有（@State）并传入，宿主的下拉刷新与本区共用同一实例
    let viewModel: ZoneAnalyticsViewModel
    @Environment(EntitlementStore.self) private var entitlements
    @State private var selectedDate: Date?
    @State private var rangePaywallPresented = false
    @State private var summaryPaywallPresented = false
    // 总请求大数字：保留 40pt 视觉基线，同时随动态字体缩放
    @ScaledMetric(relativeTo: .largeTitle) private var heroNumberSize: CGFloat = 40

    var body: some View {
        VStack(spacing: 14) {
            rangePicker

            if viewModel.error != nil && !viewModel.isLoading {
                RefreshFailedBanner { Task { await viewModel.load() } }
            }

            if viewModel.isLoading && viewModel.points.isEmpty {
                analyticsSkeleton
            } else if viewModel.points.isEmpty {
                // 失败时不再误报「暂无数据」，由上方红色提示说明
                if viewModel.error == nil {
                    ContentUnavailableView {
                        Label("暂无数据", systemImage: "chart.xyaxis.line")
                    } description: {
                        Text("所选时间范围内没有流量数据")
                    }
                    .frame(minHeight: 200)
                }
            } else {
                aiSummaryCard
                requestsHeroCard
                cacheHitCard
                smallCardGrid
                ZoneTrafficMapCard(viewModel: viewModel)
            }
        }
        .task {
            // 订阅回落后把残留的 7d/30d 选择拉回免费档
            if !entitlements.isPro && viewModel.selectedRange != .last24h {
                viewModel.selectedRange = .last24h
            }
            await viewModel.load()
        }
        .sheet(isPresented: $rangePaywallPresented) {
            PaywallView(feature: .analyticsRange)
        }
        .sheet(isPresented: $summaryPaywallPresented) {
            PaywallView(feature: .aiInsights)
        }
        .onChange(of: viewModel.selectedRange) {
            selectedDate = nil
            viewModel.clearInsight()
            Task { await viewModel.load() }
        }
    }

    // MARK: - 智能摘要卡（设备端 AI，只读，Pro）

    /// 仅在设备真正支持设备端模型时出现；免费层展示预告 → 付费墙，Pro 现场生成。
    @ViewBuilder
    private var aiSummaryCard: some View {
        if AnalyticsAssistant.isReady {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Label("智能摘要", systemImage: "sparkles")
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(.secondary)
                    Spacer()
                    if !entitlements.isPro { ProBadge() }
                }

                if entitlements.isPro {
                    proSummaryContent
                } else {
                    lockedSummaryTeaser
                }
            }
            .padding()
            .frame(maxWidth: .infinity, alignment: .leading)
            .glassIsland()
        }
    }

    @ViewBuilder
    private var proSummaryContent: some View {
        if let insight = viewModel.insight {
            VStack(alignment: .leading, spacing: 8) {
                if !insight.summary.isEmpty {
                    Text(insight.summary)
                        .font(.callout.weight(.medium))
                        .fixedSize(horizontal: false, vertical: true)
                }
                ForEach(Array(insight.highlights.enumerated()), id: \.offset) { _, line in
                    Label {
                        Text(line)
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                            .fixedSize(horizontal: false, vertical: true)
                    } icon: {
                        Image(systemName: "circle.fill")
                            .font(.system(size: 5))
                            .foregroundStyle(Color.ocOrange)
                    }
                }
                Button {
                    Task { await viewModel.generateInsight() }
                } label: {
                    HStack(spacing: 4) {
                        if viewModel.isSummarizing {
                            ProgressView().controlSize(.mini)
                        } else {
                            Image(systemName: "arrow.clockwise")
                        }
                        Text(viewModel.isSummarizing ? String(localized: "分析中…") : String(localized: "重新生成"))
                    }
                    .font(.caption.weight(.medium))
                }
                .buttonStyle(.borderless)
                .tint(Color.ocOrangeText)
                .disabled(viewModel.isSummarizing)
                .padding(.top, 2)
            }
            .transition(.opacity)
        } else {
            Button {
                Task { await viewModel.generateInsight() }
            } label: {
                HStack(spacing: 8) {
                    if viewModel.isSummarizing {
                        ProgressView().controlSize(.small)
                        Text("分析中…")
                    } else {
                        Image(systemName: "sparkles")
                        Text("生成本期摘要")
                    }
                }
                .frame(maxWidth: .infinity)
                .font(.callout.weight(.medium))
            }
            .buttonStyle(.bordered)
            .tint(Color.ocOrange)
            .disabled(viewModel.isSummarizing)

            if let error = viewModel.summaryError {
                Text(error)
                    .font(.footnote)
                    .foregroundStyle(.red)
            }
            Text("在设备上离线生成，仅基于当前所选时间范围的数据。")
                .font(.caption2)
                .foregroundStyle(.tertiary)
        }
    }

    private var lockedSummaryTeaser: some View {
        Button {
            summaryPaywallPresented = true
        } label: {
            VStack(alignment: .leading, spacing: 6) {
                Text("用一句话读懂本期流量：增长、异常与主要来源，设备端离线生成。")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
                Text("升级 Pro 解锁")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color.ocOrangeText)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .buttonStyle(.plain)
    }

    // MARK: - 骨架（总请求卡 + 命中率卡 + 2×2 小卡，与真实布局同形状）

    private var analyticsSkeleton: some View {
        VStack(spacing: 14) {
            // 总请求卡
            VStack(alignment: .leading, spacing: 10) {
                SkeletonBlock(width: 110, height: 10)
                SkeletonBlock(width: 150, height: 32, cornerRadius: 8)
                SkeletonBlock(height: 150, cornerRadius: 12)
            }
            .padding()
            .frame(maxWidth: .infinity, alignment: .leading)
            .glassIsland()

            // 缓存命中率卡
            HStack(spacing: 18) {
                Circle()
                    .stroke(.quaternary, lineWidth: 10)
                    .frame(width: 76, height: 76)
                VStack(alignment: .leading, spacing: 8) {
                    SkeletonBlock(width: 70, height: 10)
                    SkeletonBlock(width: 110, height: 22, cornerRadius: 7)
                    SkeletonBlock(width: 120, height: 9)
                }
                Spacer()
            }
            .padding()
            .frame(maxWidth: .infinity, alignment: .leading)
            .glassIsland()

            // 小卡网格
            LazyVGrid(columns: [GridItem(.flexible(), spacing: 14), GridItem(.flexible())], spacing: 14) {
                ForEach(0..<4, id: \.self) { index in
                    VStack(alignment: .leading, spacing: 8) {
                        SkeletonBlock(width: 50 + CGFloat((index * 19) % 30), height: 10)
                        SkeletonBlock(width: 80, height: 20, cornerRadius: 6)
                        SkeletonBlock(width: 64, height: 16)
                            .frame(maxWidth: .infinity, alignment: .trailing)
                    }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .glassIsland()
                }
            }
        }
        .skeletonPulse()
    }

    // MARK: - 时间范围

    private var rangePicker: some View {
        Picker("时间范围", selection: Binding(
            get: { viewModel.selectedRange },
            set: { newRange in
                // 7d/30d 是 Pro 功能：未解锁时弹付费墙并停留在 24h
                if newRange != .last24h && !entitlements.isPro {
                    rangePaywallPresented = true
                } else {
                    viewModel.selectedRange = newRange
                }
            }
        )) {
            ForEach(AnalyticsTimeRange.allCases) { range in
                Text(range.label).tag(range)
            }
        }
        .pickerStyle(.segmented)
    }

    // MARK: - 总请求卡

    private var requestsHeroCard: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text("总请求 · \(viewModel.selectedRange.periodLabel)")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                Spacer()
                TrendBadge(delta: viewModel.requestsTrend)
            }

            Text(viewModel.totalRequests.formatted())
                .font(.system(size: heroNumberSize, weight: .bold, design: .rounded))
                .contentTransition(.numericText())

            requestsChart
                .frame(height: 150)
                .padding(.top, 8)
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .glassIsland()
    }

    private var selectedPoint: TrafficDataPoint? {
        guard let selectedDate, !viewModel.points.isEmpty else { return nil }
        return viewModel.points.min {
            abs($0.date.timeIntervalSince(selectedDate)) < abs($1.date.timeIntervalSince(selectedDate))
        }
    }

    private var requestsChart: some View {
        Chart {
            ForEach(viewModel.points) { point in
                AreaMark(
                    x: .value("时间", point.date),
                    y: .value("请求", point.requests)
                )
                .interpolationMethod(.monotone)
                .foregroundStyle(
                    LinearGradient(
                        colors: [Color.ocOrange.opacity(0.28), Color.ocOrange.opacity(0.02)],
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
                .lineStyle(StrokeStyle(lineWidth: 2.5, lineCap: .round))
                // 让读屏可逐点浏览：标签报时刻，值报请求数（也驱动 Audio Graph）
                .accessibilityLabel(axisLabel(for: point.date))
                .accessibilityValue(Text("\(point.requests) 次请求"))
            }

            // 末点高亮（"现在"）
            if let last = viewModel.points.last {
                PointMark(
                    x: .value("时间", last.date),
                    y: .value("请求", last.requests)
                )
                .symbolSize(60)
                .foregroundStyle(Color.ocOrange)
                .accessibilityHidden(true)
            }

            // 扫览
            if let selected = selectedPoint {
                RuleMark(x: .value("选中", selected.date))
                    .foregroundStyle(.secondary.opacity(0.4))
                    .accessibilityHidden(true)
                    .annotation(position: .top, overflowResolution: .init(x: .fit(to: .chart))) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(axisLabel(for: selected.date))
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                            Text(selected.requests.formatted())
                                .font(.caption.bold())
                                .foregroundStyle(.primary)
                        }
                        .padding(.horizontal, 8)
                        .padding(.vertical, 5)
                        // 实色背景：material 在 annotation 渲染上下文中会发黑
                        .background(Color(.systemBackground), in: RoundedRectangle(cornerRadius: 8))
                        .overlay(
                            RoundedRectangle(cornerRadius: 8)
                                .strokeBorder(Color(.separator).opacity(0.5), lineWidth: 0.5)
                        )
                        .shadow(color: .black.opacity(0.08), radius: 4, y: 2)
                    }
            }
        }
        .chartXSelection(value: $selectedDate)
        .chartYAxis {
            AxisMarks(position: .trailing, values: .automatic(desiredCount: 4)) { value in
                AxisGridLine(stroke: StrokeStyle(lineWidth: 0.5, dash: [3, 3]))
                AxisValueLabel {
                    if let intValue = value.as(Int.self) {
                        Text(intValue.formatted(.number.notation(.compactName)))
                            .font(.caption2)
                            .foregroundStyle(.tertiary)
                    }
                }
            }
        }
        .chartXAxis {
            // 从实际数据点取标签，保证与数据对齐且密度可控
            AxisMarks(values: xAxisValues) { value in
                AxisValueLabel(anchor: .top) {
                    if let date = value.as(Date.self) {
                        Text(axisLabel(for: date))
                            .font(.caption2)
                    }
                }
            }
        }
    }

    /// X 轴标签取样：24h 每 6 小时、7d 隔天、30d 每周
    private var xAxisValues: [Date] {
        let points = viewModel.points
        guard points.count > 1 else { return points.map(\.date) }
        let step = switch viewModel.selectedRange {
        case .last24h: 6
        case .last7d:  2
        case .last30d: 7
        }
        return Swift.stride(from: 0, to: points.count, by: step).map { points[$0].date }
    }

    // 固定 24 小时制：FormatStyle 会跟随系统 12 小时制，下午 4 点显示成 "04:00" 导致标签重复
    private static let hourLabelFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        return formatter
    }()

    private static let dayLabelFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "M/d"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(identifier: "UTC")   // 天级数据桶是 UTC 日期
        return formatter
    }()

    private func axisLabel(for date: Date) -> String {
        viewModel.selectedRange.usesHourlyGroups
            ? Self.hourLabelFormatter.string(from: date)
            : Self.dayLabelFormatter.string(from: date)
    }

    // MARK: - 缓存命中率卡

    private var cacheHitCard: some View {
        HStack(spacing: 18) {
            RingGauge(percent: viewModel.cacheHitRate ?? 0)
                .accessibilityHidden(true)   // 命中率数字已在右侧文字呈现

            VStack(alignment: .leading, spacing: 3) {
                Text("缓存命中率")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text(viewModel.cacheHitRate.map { String(format: "%.1f%%", $0) } ?? "—")
                        .font(.system(.title, design: .rounded, weight: .bold))
                        .contentTransition(.numericText())
                    TrendBadge(delta: viewModel.cacheHitTrendPt, unit: "pt")
                }
                Text("边缘命中 / 全部请求")
                    .font(.caption)
                    .foregroundStyle(.tertiary)
            }
            Spacer()
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .glassIsland()
    }

    // MARK: - 小卡网格

    private var smallCardGrid: some View {
        LazyVGrid(columns: [GridItem(.flexible(), spacing: 14), GridItem(.flexible())], spacing: 14) {
            SmallStatCard(
                title: String(localized: "带宽"),
                value: Int64(viewModel.totalBytes).ocBytes,
                trend: viewModel.bytesTrend,
                sparkValues: viewModel.points.map { Double($0.bytes) },
                sparkColor: .ocOrange
            )
            SmallStatCard(
                title: String(localized: "拦截威胁"),
                value: viewModel.totalThreats.formatted(.number.notation(.compactName)),
                trend: viewModel.threatsTrend,
                positiveIsGood: false,    // 威胁减少是好事
                sparkValues: viewModel.points.map { Double($0.threats) },
                sparkColor: .red
            )
            SmallStatCard(
                title: String(localized: "独立访客"),
                value: viewModel.totalUniques.formatted(.number.notation(.compactName)),
                trend: viewModel.uniquesTrend,
                sparkValues: viewModel.points.map { Double($0.uniques) },
                sparkColor: .blue
            )
            SmallStatCard(
                title: String(localized: "页面浏览"),
                value: viewModel.totalPageViews.formatted(.number.notation(.compactName)),
                trend: nil,
                sparkValues: viewModel.points.map { Double($0.pageViews) },
                sparkColor: .green
            )
        }
    }
}

// MARK: - 趋势徽章

struct TrendBadge: View {

    let delta: Double?
    var unit: String = "%"
    var positiveIsGood: Bool = true

    var body: some View {
        if let delta, delta.isFinite, abs(delta) >= 0.05 {
            HStack(spacing: 2) {
                Image(systemName: delta >= 0 ? "arrow.up" : "arrow.down")
                    .font(.caption2.weight(.bold))
                Text("\(abs(delta), format: .number.precision(.fractionLength(1)))\(unit)")
                    .font(.footnote.weight(.semibold))
            }
            .foregroundStyle((delta >= 0) == positiveIsGood ? Color.green : Color.red)
            // 方向不只靠颜色：读屏报「上升/下降 + 数值」，箭头形状本身也已区分
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(delta >= 0 ? "上升" : "下降")
            .accessibilityValue(Text(verbatim: "\(abs(delta).formatted(.number.precision(.fractionLength(1))))\(unit)"))
        }
    }
}

// MARK: - 环形仪表

struct RingGauge: View {

    let percent: Double    // 0–100
    var size: CGFloat = 76

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color.ocOrange.opacity(0.15), lineWidth: 10)
            Circle()
                .trim(from: 0, to: min(max(percent / 100, 0), 1))
                .stroke(Color.ocOrange, style: StrokeStyle(lineWidth: 10, lineCap: .round))
                .rotationEffect(.degrees(-90))
            Text(percent > 0 ? String(format: "%.1f", percent) : "—")
                .font(.system(.headline, design: .rounded, weight: .bold))
        }
        .frame(width: size, height: size)
        .animation(.smooth, value: percent)
    }
}

// MARK: - 小指标卡（迷你走势）

struct SmallStatCard: View {

    let title: String
    let value: String
    let trend: Double?
    var positiveIsGood: Bool = true
    let sparkValues: [Double]
    let sparkColor: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.footnote)
                .foregroundStyle(.secondary)
            Text(value)
                .font(.system(.title2, design: .rounded, weight: .bold))
                .contentTransition(.numericText())
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            HStack(alignment: .bottom) {
                TrendBadge(delta: trend, positiveIsGood: positiveIsGood)
                Spacer()
                Sparkline(values: sparkValues, color: sparkColor)
                    .frame(width: 64, height: 26)
                    .accessibilityHidden(true)   // 装饰性迷你走势，数值已在上方文字
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .glassIsland()
    }
}

// MARK: - 迷你走势线

struct Sparkline: View {

    let values: [Double]
    let color: Color

    var body: some View {
        if values.count > 1, values.contains(where: { $0 > 0 }) {
            Chart(Array(values.enumerated()), id: \.offset) { index, value in
                LineMark(
                    x: .value("i", index),
                    y: .value("v", value)
                )
                .interpolationMethod(.monotone)
                .foregroundStyle(color)
                .lineStyle(StrokeStyle(lineWidth: 1.5, lineCap: .round))
            }
            .chartXAxis(.hidden)
            .chartYAxis(.hidden)
            .chartLegend(.hidden)
        } else {
            // 全零/单点时画一条基线
            Rectangle()
                .fill(color.opacity(0.3))
                .frame(height: 1.5)
                .frame(maxHeight: .infinity, alignment: .bottom)
        }
    }
}

#Preview {
    // mock 数据走查样式
    let now = Date()
    let mock = (0..<24).map { hour in
        TrafficDataPoint(
            date: now.addingTimeInterval(TimeInterval(hour - 24) * 3600),
            requests: Int.random(in: 60_000...140_000),
            bytes: Int.random(in: 4_000_000_000...10_000_000_000),
            threats: Int.random(in: 800...2400),
            pageViews: Int.random(in: 30_000...90_000),
            uniques: Int.random(in: 8_000...22_000),
            cachedRequests: Int.random(in: 50_000...130_000)
        )
    }
    return ScrollView {
        VStack(spacing: 14) {
            SmallStatCard(title: String(localized: "带宽"), value: "184.2 GB", trend: 8.1,
                          sparkValues: mock.map { Double($0.bytes) }, sparkColor: .ocOrange)
            RingGauge(percent: 94.2)
        }
        .padding()
    }
    .background(Color(.systemGroupedBackground))
}
