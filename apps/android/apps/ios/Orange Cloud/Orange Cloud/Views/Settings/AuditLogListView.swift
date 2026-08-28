//
//  AuditLogListView.swift
//  Orange Cloud
//
//  账号审计日志（最近 30 天）。只读、账号级，复用 account-settings.read。
//

import SwiftUI

struct AuditLogListView: View {

    @State private var historyTarget: IdentifiedAuditEntry?

    let session: SessionStore

    @State private var vm: AuditLogViewModel?

    var body: some View {
        Group {
            if let vm {
                content(vm)
            } else {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .background { SkyBackground() }
        .navigationTitle("审计日志")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await session.ensureAccounts()
            guard vm == nil, let accountId = session.selectedAccount?.id else { return }
            let model = AuditLogViewModel(service: session.auditLogService, accountId: accountId)
            vm = model
            await model.load()
        }
    }

    @ViewBuilder
    private func content(_ vm: AuditLogViewModel) -> some View {
        if vm.isLoading && vm.entries.isEmpty {
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if vm.entries.isEmpty {
            ContentUnavailableView {
                Label("暂无审计记录", systemImage: "clock.arrow.circlepath")
            } description: {
                Text(vm.error ?? String(localized: "最近 30 天没有可显示的账号操作记录。"))
            }
        } else {
            List {
                Section {
                    ForEach(vm.entries) { item in
                        // 点行看该资源的完整变更序列（v2 的 history 端点）
                        Button {
                            historyTarget = item
                            Task { await vm.loadHistory(for: item.entry) }
                        } label: {
                            AuditLogRow(entry: item.entry)
                        }
                        .buttonStyle(.plain)
                    }
                } footer: {
                    Text("仅显示当前账号最近 30 天的操作记录。")
                }
                .glassRow()

                if vm.canLoadMore {
                    HStack {
                        Spacer()
                        ProgressView()
                        Spacer()
                    }
                    .listRowBackground(Color.clear)
                    .onAppear {
                        Task { await vm.loadMore() }
                    }
                }
            }
            .daybreakList()
            .refreshable { await vm.load() }
            .sheet(item: $historyTarget) { item in
                NavigationStack {
                    AuditHistoryView(source: item.entry, vm: vm)
                }
                .presentationDetents([.medium, .large])
            }
        }
    }
}

// MARK: - 单条记录

private struct AuditLogRow: View {

    let entry: AuditLogEntry

    private var title: String {
        entry.action?.description?.nilIfEmpty
            ?? entry.action?.type?.nilIfEmpty
            ?? String(localized: "未知操作")
    }

    private var subtitle: String {
        let who = entry.actor?.email?.nilIfEmpty
            ?? entry.actor?.type?.nilIfEmpty
            ?? String(localized: "系统")
        if let product = entry.resource?.product?.nilIfEmpty {
            return "\(product) · \(who)"
        }
        return who
    }

    private var relativeTime: String {
        guard let date = entry.timestamp else { return entry.action?.time ?? "" }
        return date.formatted(.relative(presentation: .named))
    }

    var body: some View {
        HStack(spacing: 12) {
            statusIcon
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.callout)
                    .lineLimit(2)
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            Spacer(minLength: 8)
            Text(relativeTime)
                .font(.caption2)
                .foregroundStyle(.tertiary)
        }
        .padding(.vertical, 2)
    }

    @ViewBuilder
    private var statusIcon: some View {
        switch entry.succeeded {
        case .some(true):
            TintIcon(systemImage: "checkmark", color: .green)
        case .some(false):
            TintIcon(systemImage: "xmark", color: .red)
        case .none:
            TintIcon(systemImage: "circle.dotted", color: .gray)
        }
    }
}

private extension String {
    /// 去空白后为空则视为无值
    var nilIfEmpty: String? {
        let t = trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? nil : t
    }
}

// MARK: - 资源变更历史

private struct AuditHistoryView: View {

    let source: AuditLogEntry
    let vm: AuditLogViewModel
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        List {
            Section {
                if vm.isLoadingHistory {
                    ProgressView().frame(maxWidth: .infinity)
                } else if vm.history.isEmpty {
                    Text(vm.historyStatus == "unavailable"
                         ? String(localized: "这条记录没有携带足够信息来定位资源，因此查不到它的变更历史。")
                         : String(localized: "没有查到这个资源的其它变更。"))
                        .font(.footnote).foregroundStyle(.secondary)
                } else {
                    ForEach(vm.history) { item in
                        AuditLogRow(entry: item.entry)
                    }
                }
            } header: {
                Text("变更历史")
            } footer: {
                // 识别质量必须如实说明——approximate 时结果可能混入无关条目
                if vm.historyStatus == "approximate" {
                    Text("这条记录没有资源地址，只能按其它字段近似匹配，列表里可能混入无关变更。")
                } else if !vm.history.isEmpty {
                    Text("同一资源在所选时间窗内的全部变更，最新在上。")
                }
            }
            .glassRow()
        }
        .daybreakList()
        .navigationTitle(source.resource?.type ?? String(localized: "变更历史"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("完成") { dismiss() }
            }
        }
    }
}

