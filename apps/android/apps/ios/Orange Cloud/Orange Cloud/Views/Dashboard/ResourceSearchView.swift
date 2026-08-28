//
//  ResourceSearchView.swift
//  Orange Cloud
//
//  命令搜索：概览页工具栏放大镜弹出的 sheet，跨类型实时过滤当前账号的资源
//  （域名 / Workers / R2 / D1 / KV / Tunnel），点行跳转、点星标切换固定到首页。
//
//  **刻意不用 `.searchable`**：本工程已知 `.refreshable` + `.searchable` 组合下拉必报
//  「网络错误：已取消」（Zone/DNS/Worker/KV 四页修过），概览页是 refreshable 的，
//  因此搜索走 sheet + 普通 TextField。
//
//  **跳转不在 sheet 内 push**：选中只回调给概览页，由它先关 sheet、再由**栈根**的
//  `.navigationDestination(item:)` 承接（值式路由）。sheet 内 eager NavigationLink
//  指向「内部还要再导航」的目的页在 iOS 17 会卡死（见 DashboardView 注释）。
//

import SwiftUI

struct ResourceSearchView: View {

    /// 当前账号的全部可搜索资源（由概览页从缓存 + VM 清单拼好传入）
    let items: [DashboardResourceItem]
    let accountId: String
    /// 选中一条：概览页负责关闭 sheet 并在关闭后派发路由
    let onSelect: (DashboardResourceRoute) -> Void

    @Environment(\.dismiss) private var dismiss
    @FocusState private var queryFocused: Bool
    @State private var query = ""

    private let pinnedStore = PinnedResourceStore.shared

    /// 空查询显示前 30 条（已固定的排在最前），有查询最多 50 条
    private var results: [DashboardResourceItem] {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            let pins = pinnedStore.pins(for: accountId)
            let pinnedIds = Set(pins.map(\.id))
            let pinnedFirst = items.filter { pinnedIds.contains($0.id) }
                + items.filter { !pinnedIds.contains($0.id) }
            return Array(pinnedFirst.prefix(30))
        }
        return Array(items.filter { $0.matches(trimmed) }.prefix(50))
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 12) {
                searchField
                resultList
            }
            .padding(.top, 8)
            .background { SkyBackground() }
            .navigationTitle("搜索资源")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("完成") { dismiss() }
                }
            }
            .task {
                // sheet 完成呈现后再抢焦点，避免 iOS 17 上键盘与转场抢时序
                queryFocused = true
            }
        }
    }

    // MARK: - 输入框（普通 TextField，见文件头注释）

    private var searchField: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(.secondary)
            TextField("搜索域名、Workers、存储桶…", text: $query)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.search)
                .focused($queryFocused)
            if !query.isEmpty {
                Button {
                    query = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(.tertiary)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("清空")
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .glassIsland(cornerRadius: OCLayout.chipRadius)
        .padding(.horizontal, OCLayout.pagePadding)
    }

    // MARK: - 结果

    @ViewBuilder
    private var resultList: some View {
        if items.isEmpty {
            emptyState(
                icon: "square.stack.3d.up.slash",
                title: String(localized: "暂无可搜索的资源"),
                detail: String(localized: "下拉刷新概览页后，这里会列出当前账号的资源")
            )
        } else if results.isEmpty {
            emptyState(
                icon: "magnifyingglass",
                title: String(localized: "没有匹配的资源"),
                detail: String(localized: "换个关键词试试")
            )
        } else {
            List(results) { item in
                row(item)
                    .glassRow()
            }
            .listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
            .scrollDismissesKeyboard(.immediately)
        }
    }

    private func emptyState(icon: String, title: String, detail: String) -> some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundStyle(.secondary)
            Text(title)
                .font(.subheadline.weight(.medium))
            Text(detail)
                .font(.footnote)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func row(_ item: DashboardResourceItem) -> some View {
        HStack(spacing: 10) {
            Button {
                onSelect(item.route)
            } label: {
                HStack(spacing: 10) {
                    TintIcon(systemImage: item.type.symbolName, color: item.type.tint, size: 30)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(item.title)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(.primary)
                            .lineLimit(1)
                        Text("\(item.type.label) · \(item.subtitle)")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                    }
                    Spacer(minLength: 8)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            starButton(item)
        }
    }

    private func starButton(_ item: DashboardResourceItem) -> some View {
        let pinned = pinnedStore.isPinned(item.pin, accountId: accountId)
        return Button {
            pinnedStore.toggle(item.pin, accountId: accountId)
        } label: {
            Image(systemName: pinned ? "star.fill" : "star")
                .font(.subheadline)
                .foregroundStyle(pinned ? Color.ocOrangeText : Color.secondary)
                .frame(width: 32, height: 32)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(pinned ? String(localized: "取消固定") : String(localized: "固定到首页"))
    }

}

extension PinnedResourceType {
    /// 行内图标底色（与存储页 StorageRow 的配色口径一致；品牌橙只给域名 / R2）
    var tint: Color {
        switch self {
        case .zone:   .ocOrange
        case .worker: .purple
        case .r2:     .ocOrange
        case .d1:     .blue
        case .kv:     .green
        case .tunnel: .teal
        }
    }
}
