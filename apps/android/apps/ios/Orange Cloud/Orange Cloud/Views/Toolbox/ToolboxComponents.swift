//
//  ToolboxComponents.swift
//  Orange Cloud
//
//  工具箱各工具页共用的小组件：键值行、提示岛、输入岛。晨昏玻璃风格。
//

import SwiftUI

/// 键值结果行（值可长按选择，等宽便于读 IP / 哈希）
struct ToolKV: View {
    let key: LocalizedStringKey
    let value: String
    var mono: Bool = true

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Text(key)
                .foregroundStyle(.secondary)
            Spacer(minLength: 12)
            Text(value)
                .font(mono ? .callout.monospaced() : .callout)
                .multilineTextAlignment(.trailing)
                .textSelection(.enabled)
        }
        .padding(.horizontal, OCLayout.islandPadding)
        .padding(.vertical, 9)
    }
}

/// 一行键值的数据载体（供 ToolKVSection 的 ForEach 使用，规避 ViewBuilder 10 子视图上限）
struct ToolKVRow: Identifiable {
    let id = UUID()
    let key: LocalizedStringKey
    let value: String
    var mono: Bool = true
    init(_ key: LocalizedStringKey, _ value: String, mono: Bool = true) {
        self.key = key
        self.value = value
        self.mono = mono
    }
}

/// 标题 + 玻璃岛 + 若干键值行（行间细分隔线）
struct ToolKVSection: View {
    let title: LocalizedStringKey
    let rows: [ToolKVRow]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
                .padding(.horizontal, 4)
            VStack(spacing: 0) {
                ForEach(Array(rows.enumerated()), id: \.element.id) { index, row in
                    if index > 0 {
                        Divider().padding(.leading, OCLayout.islandPadding)
                    }
                    ToolKV(key: row.key, value: row.value, mono: row.mono)
                }
            }
            .glassIsland()
        }
    }
}

/// 提示岛（空态 / 错误态），玻璃底
struct ToolNotice: View {
    let systemImage: String
    let title: LocalizedStringKey
    var message: String? = nil
    var tint: Color = .secondary

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: systemImage)
                .font(.title3)
                .foregroundStyle(tint)
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .foregroundStyle(.primary)
                if let message {
                    Text(message)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(OCLayout.islandPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .glassIsland()
    }
}

/// 工具结果容器：标题 + 玻璃岛内容
struct ToolResultIsland<Content: View>: View {
    let title: LocalizedStringKey
    @ViewBuilder var content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
                .padding(.horizontal, 4)
            VStack(spacing: 0) {
                content
            }
            .glassIsland()
        }
    }
}

/// 工具页统一的日期格式化
nonisolated enum ToolFormat {
    static func date(_ date: Date?) -> String {
        guard let date else { return "—" }
        let f = DateFormatter()
        f.dateStyle = .medium
        f.timeStyle = .short
        return f.string(from: date)
    }
}
