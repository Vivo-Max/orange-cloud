//
//  WorkerTailView.swift
//  Orange Cloud
//
//  实时日志控制台：连接状态条、等宽日志行、自动滚底、暂停/清屏。
//

import SwiftUI
import TipKit
import UIKit

struct WorkerTailView: View {

    @State private var viewModel: WorkerTailViewModel
    /// 复制成功的触觉反馈触发器
    @State private var copyTick = 0
    /// 点击行后展示的详情（sheet 而非 push：日志流页面 push 会打断实时滚动）
    @State private var detailLine: WorkerTailViewModel.LogLine?
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.colorScheme) private var colorScheme

    init(accountId: String, scriptName: String, session: SessionStore) {
        _viewModel = State(initialValue: WorkerTailViewModel(
            service: session.workerTailService,
            accountId: accountId,
            scriptName: scriptName
        ))
    }

    var body: some View {
        VStack(spacing: 0) {
            header
            Divider()
            logConsole
        }
        .sensoryFeedback(.success, trigger: copyTick)
        .sheet(item: $detailLine) { line in
            LogLineDetailSheet(line: line) { copy(line) }
        }
        .navigationTitle("实时日志")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItemGroup(placement: .topBarTrailing) {
                Button(
                    viewModel.isPaused ? String(localized: "继续") : String(localized: "暂停"),
                    systemImage: viewModel.isPaused ? "play.fill" : "pause.fill"
                ) {
                    viewModel.togglePause()
                }
                .safePopoverTip(TailPauseTip())
                Button("清屏", systemImage: "xmark.bin") {
                    viewModel.clear()
                }
                .disabled(viewModel.lines.isEmpty)
            }
        }
        .task {
            await viewModel.start()
        }
        .onDisappear {
            Task { await viewModel.stop() }
        }
        .onChange(of: scenePhase) { _, phase in
            // tail 连接进后台必断：置灰 Live Activity，回前台再复活重连
            switch phase {
            case .background: viewModel.enterBackground()
            case .active:     viewModel.enterForeground()
            default:          break
            }
        }
    }

    // MARK: - 顶部：连接状态 + 筛选

    /// 状态条与筛选条共用一层材质背景（静态、不随列表滚动，不产生逐行模糊节点）
    private var header: some View {
        VStack(spacing: 8) {
            statusBar
            filterBar
        }
        .padding(.horizontal)
        .padding(.vertical, 8)
        .background(.regularMaterial)
    }

    private var statusBar: some View {
        HStack(spacing: 8) {
            Circle()
                .fill(statusColor)
                .frame(width: 8, height: 8)
            Text(statusText)
                .font(.footnote)
                .foregroundStyle(.secondary)
            Spacer()
            if case .disconnected = viewModel.state {
                Button("重新连接") {
                    Task { await viewModel.start() }
                }
                .font(.footnote)
                .buttonStyle(.bordered)
                .controlSize(.mini)
                .tint(Color.ocOrange)
            }
            if viewModel.isPaused {
                // 暂停只冻结视图，事件仍在入 buffer；点一下即恢复并滚到底
                Button {
                    viewModel.togglePause()
                } label: {
                    Label(
                        viewModel.pausedNewCount > 0
                            ? String(localized: "已暂停 · 新增 \(viewModel.pausedNewCount) 条")
                            : String(localized: "已暂停"),
                        systemImage: "pause.fill"
                    )
                    .font(.caption)
                    .foregroundStyle(Color.ocOrangeText)
                }
                .buttonStyle(.plain)
                .accessibilityHint("恢复实时滚动")
            }
        }
    }

    // MARK: - 筛选条（普通 TextField，不用 .searchable）

    private var filterBar: some View {
        HStack(spacing: 8) {
            searchField
            levelMenu
        }
    }

    private var searchField: some View {
        HStack(spacing: 6) {
            Image(systemName: "magnifyingglass")
                .font(.footnote)
                .foregroundStyle(.secondary)
            TextField("搜索日志", text: $viewModel.searchText)
                .font(.footnote)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.done)
            if !viewModel.searchText.isEmpty {
                Button {
                    viewModel.searchText = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("清除搜索")
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(OCGlass.fill(for: colorScheme), in: .rect(cornerRadius: 9))
    }

    private var levelMenu: some View {
        Menu {
            Picker("级别", selection: $viewModel.levelFilter) {
                ForEach(WorkerTailViewModel.LevelFilter.allCases) { level in
                    Text(level.title).tag(level)
                }
            }
            .pickerStyle(.inline)
        } label: {
            Label(viewModel.levelFilter.title, systemImage: "line.3.horizontal.decrease.circle")
                .font(.footnote)
                .foregroundStyle(viewModel.levelFilter == .all ? Color.secondary : Color.ocOrangeText)
        }
        .accessibilityLabel("按级别筛选")
    }

    private var statusColor: Color {
        switch viewModel.state {
        case .connected:    .green
        case .connecting:   .orange
        case .idle:         .gray
        case .disconnected: .red
        }
    }

    private var statusText: String {
        switch viewModel.state {
        case .idle:                      String(localized: "未连接")
        case .connecting:                String(localized: "连接中…")
        case .connected:                 String(localized: "已连接，等待事件")
        case .disconnected(let reason):  reason ?? String(localized: "连接已断开")
        }
    }

    // MARK: - 日志控制台

    private var logConsole: some View {
        ScrollViewReader { proxy in
            ScrollView {
                logContent
            }
            .background { SkyBackground() }
            .onChange(of: viewModel.lines.count) {
                scrollToBottom(proxy)
            }
            // 恢复时补一次滚动：暂停期间新行已入 buffer，但 count 不再变化时不会触发上面那条
            .onChange(of: viewModel.isPaused) {
                scrollToBottom(proxy)
            }
        }
    }

    @ViewBuilder
    private var logContent: some View {
        if viewModel.lines.isEmpty {
            emptyHint
        } else if viewModel.visibleLines.isEmpty {
            noMatchHint
        } else {
            logList
        }
    }

    private var logList: some View {
        LazyVStack(alignment: .leading, spacing: 4) {
            ForEach(viewModel.visibleLines) { line in
                LogLineRow(
                    line: line,
                    onCopy: { copy(line) },
                    onOpen: { detailLine = line }
                )
                .id(line.id)
            }
        }
        .padding(12)
        // 日志正文（时间戳 + 请求路径/JSON）始终 LTR，避免在阿拉伯语等 RTL 下被镜像
        .environment(\.layoutDirection, .leftToRight)
    }

    private func scrollToBottom(_ proxy: ScrollViewProxy) {
        guard !viewModel.isPaused, let last = viewModel.visibleLines.last else { return }
        withAnimation(.easeOut(duration: 0.15)) {
            proxy.scrollTo(last.id, anchor: .bottom)
        }
    }

    private func copy(_ line: WorkerTailViewModel.LogLine) {
        UIPasteboard.general.string = line.text
        copyTick += 1
    }

    private var emptyHint: some View {
        ContentUnavailableView {
            Label("等待事件", systemImage: "dot.radiowaves.left.and.right")
        } description: {
            Text("向这个 Worker 发起一次请求，日志会实时出现在这里")
        }
        .padding(.top, 60)
    }

    /// 筛选后无结果：明确告知原始日志仍在，避免用户误以为丢了历史
    private var noMatchHint: some View {
        ContentUnavailableView {
            Label("没有匹配的日志", systemImage: "line.3.horizontal.decrease.circle")
        } description: {
            Text("换个关键词或级别再看看，已接收的日志不会丢失")
        } actions: {
            Button("清除筛选") {
                viewModel.clearFilters()
            }
            .buttonStyle(.bordered)
            .tint(Color.ocOrange)
        }
        .padding(.top, 60)
    }
}

// MARK: - 单条日志行

private struct LogLineRow: View {
    let line: WorkerTailViewModel.LogLine
    /// 长按复制整行（不用行尾按钮：日志控制台每行挂按钮会淹没正文）
    let onCopy: () -> Void
    /// 点击展开详情（详情里才允许框选行内片段）
    let onOpen: () -> Void

    var body: some View {
        // 用 Button 承载点击、contextMenu 挂在 Button 上：
        // 系统按钮自带长按/点击的手势仲裁，两者可稳定共存（自绘 onTapGesture + contextMenu 会互相吞手势）
        Button(action: onOpen) {
            rowContent
        }
        .buttonStyle(.plain)
        .contextMenu {
            Button("复制此行", systemImage: "doc.on.doc", action: onCopy)
        }
        .accessibilityHint(Text("轻点查看详情，长按复制整行"))
    }

    private var rowContent: some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            Text(line.timestamp, format: .dateTime.hour(.twoDigits(amPM: .omitted)).minute(.twoDigits).second(.twoDigits))
                .font(.caption2.monospaced())
                .foregroundStyle(.tertiary)

            Text(line.text)
                .font(.caption.monospaced())
                .foregroundStyle(LogLevelStyle.color(for: line.level))
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.vertical, 1)
        .contentShape(Rectangle())
    }
}

// MARK: - 级别配色

private enum LogLevelStyle {
    static func color(for level: String) -> Color {
        switch level {
        case "error", "exception": .red
        case "warn":               .orange
        case "event":              .secondary
        default:                   .primary
        }
    }
}

// MARK: - 日志详情（可框选）

/// 把一行日志按已有字段拆成分区；不改 VM 数据结构，仅在展示层解析 event / exception 的既定拼接格式。
private nonisolated struct LogLineDetail {
    let timestamp: Date
    let level: String
    let text: String
    /// 请求方法（event 行）
    var method: String?
    var url: String?
    var outcome: String?
    var cron: String?
    /// 异常名（exception 行）
    var exceptionName: String?

    init(_ line: WorkerTailViewModel.LogLine) {
        timestamp = line.timestamp
        level = line.level
        text = line.text

        if line.level == "event" {
            // 生成端格式："METHOD URL → outcome" 或 "cron EXPR → outcome"
            let parts = line.text.components(separatedBy: " → ")
            let head = parts.first ?? line.text
            if parts.count > 1 { outcome = parts.dropFirst().joined(separator: " → ") }
            if head.hasPrefix("cron ") {
                cron = String(head.dropFirst("cron ".count))
            } else {
                let tokens = head.split(separator: " ", maxSplits: 1, omittingEmptySubsequences: true)
                if tokens.count == 2 {
                    method = String(tokens[0])
                    url = String(tokens[1])
                }
            }
        } else if line.level == "exception" {
            let parts = line.text.components(separatedBy: ": ")
            if parts.count > 1 { exceptionName = parts[0] }
        }
    }
}

private struct LogLineDetailSheet: View {
    let line: WorkerTailViewModel.LogLine
    let onCopy: () -> Void

    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme

    private var detail: LogLineDetail { LogLineDetail(line) }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    metaSection
                    structuredSection
                    bodySection
                }
                .padding(16)
            }
            .background { SkyBackground() }
            .navigationTitle("日志详情")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("完成") { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("复制全部", systemImage: "doc.on.doc", action: onCopy)
                        .tint(Color.ocOrange)
                }
            }
        }
    }

    // MARK: 分区

    @ViewBuilder
    private var metaSection: some View {
        island {
            fieldRow(title: Text("级别"), value: detail.level, monospaced: true, tint: LogLevelStyle.color(for: detail.level))
            Divider().opacity(0.4)
            fieldRow(
                title: Text("时间"),
                value: detail.timestamp.formatted(date: .abbreviated, time: .standard),
                monospaced: true,
                tint: .secondary
            )
        }
    }

    @ViewBuilder
    private var structuredSection: some View {
        if detail.method != nil || detail.cron != nil || detail.exceptionName != nil {
            island {
                if let method = detail.method {
                    fieldRow(title: Text("请求方法"), value: method, monospaced: true, tint: .primary)
                }
                if let url = detail.url {
                    Divider().opacity(0.4)
                    fieldRow(title: Text("请求地址"), value: url, monospaced: true, tint: .primary)
                }
                if let cron = detail.cron {
                    fieldRow(title: Text("定时表达式"), value: cron, monospaced: true, tint: .primary)
                }
                if let outcome = detail.outcome {
                    Divider().opacity(0.4)
                    fieldRow(title: Text("结果"), value: outcome, monospaced: true, tint: .secondary)
                }
                if let name = detail.exceptionName {
                    fieldRow(title: Text("异常"), value: name, monospaced: true, tint: .red)
                }
            }
        }
    }

    @ViewBuilder
    private var bodySection: some View {
        island {
            VStack(alignment: .leading, spacing: 8) {
                Text("完整内容")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text(detail.text)
                    .font(.callout.monospaced())
                    .foregroundStyle(LogLevelStyle.color(for: detail.level))
                    .textSelection(.enabled)   // 详情态才开框选：列表行开了会吞掉长按手势
                    .frame(maxWidth: .infinity, alignment: .leading)
                    // 日志正文（URL / JSON）恒定 LTR，避免 RTL 语言下被镜像
                    .environment(\.layoutDirection, .leftToRight)
                Text("长按可选中其中的片段单独复制")
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
            }
        }
    }

    // MARK: 组件

    @ViewBuilder
    private func fieldRow(title: Text, value: String, monospaced: Bool, tint: Color) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 12) {
            title
                .font(.caption)
                .foregroundStyle(.secondary)
                .frame(width: 72, alignment: .leading)
            Text(value)
                .font(monospaced ? .footnote.monospaced() : .footnote)
                .foregroundStyle(tint)
                .textSelection(.enabled)
                .frame(maxWidth: .infinity, alignment: .leading)
                .environment(\.layoutDirection, .leftToRight)
        }
        .padding(.vertical, 4)
    }

    /// 玻璃岛：用 OCGlass 纯色近似，不使用真材质（backdrop blur 会掉帧）
    @ViewBuilder
    private func island<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            content()
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(OCGlass.fill(for: colorScheme), in: .rect(cornerRadius: 16))
    }
}
