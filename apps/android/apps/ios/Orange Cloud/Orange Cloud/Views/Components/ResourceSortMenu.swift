//
//  ResourceSortMenu.swift
//  Orange Cloud
//
//  资源列表排序（Workers / Pages 等通用）：默认（名称）/ 创建日期 / 最近更新。
//  选择用 @AppStorage 持久化（ResourceSort 是 String RawRepresentable，可直接存）。
//

import SwiftUI

nonisolated enum ResourceSort: String, CaseIterable {
    case name       // 默认：名称字母序（列表原有顺序）
    case created    // 创建日期，新的在前
    case modified   // 最近更新，新的在前

    var label: String {
        switch self {
        case .name:     String(localized: "默认（名称）")
        case .created:  String(localized: "创建日期")
        case .modified: String(localized: "最近更新")
        }
    }

    /// 按可选 ISO8601 日期串排（nil 沉底）。名称序由调用方保持原顺序。
    func sorted<T>(_ items: [T], created: (T) -> String?, modified: (T) -> String?) -> [T] {
        switch self {
        case .name:     items
        case .created:  Self.sortByDate(items, key: created)
        case .modified: Self.sortByDate(items, key: modified)
        }
    }

    private static func sortByDate<T>(_ items: [T], key: (T) -> String?) -> [T] {
        items
            .map { ($0, WorkerScript.parseDate(key($0)) ?? .distantPast) }
            .sorted { $0.1 > $1.1 }
            .map(\.0)
    }
}

/// 工具栏排序菜单（当前选中项自动打勾）
struct ResourceSortMenu: View {
    @Binding var sort: ResourceSort

    var body: some View {
        Menu {
            Picker("排序", selection: $sort) {
                ForEach(ResourceSort.allCases, id: \.self) { option in
                    Text(option.label).tag(option)
                }
            }
        } label: {
            Label("排序", systemImage: "arrow.up.arrow.down")
        }
    }
}
