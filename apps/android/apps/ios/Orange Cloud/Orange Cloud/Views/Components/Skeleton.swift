//
//  Skeleton.swift
//  Orange Cloud
//
//  骨架屏体系：加载中用「内容形状」的占位块整体呼吸，代替整页转圈。
//  这里只放通用积木（块 / 行 / 列表 / 卡片），页面专属的骨架
//  （分析图表、D1 网格等）在各自 View 里用这些积木拼装。
//

import SwiftUI

// MARK: - 呼吸脉冲

private struct SkeletonPulse: ViewModifier {

    @Environment(\.appReduceMotion) private var appReduceMotion
    @Environment(\.accessibilityReduceMotion) private var systemReduceMotion
    @State private var dimmed = false

    /// App 开关 ∨ 系统「减弱动态效果」，任一开启即静止
    private var reduceMotion: Bool { appReduceMotion || systemReduceMotion }

    func body(content: Content) -> some View {
        content
            .opacity(dimmed ? 0.45 : 1)
            .onAppear {
                guard !reduceMotion else { return }
                withAnimation(.easeInOut(duration: 0.85).repeatForever(autoreverses: true)) {
                    dimmed = true
                }
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text("加载中"))
    }
}

extension View {
    /// 骨架最外层调用一次，整组占位块同步呼吸（开启「减弱动态效果」时静止）
    func skeletonPulse() -> some View {
        modifier(SkeletonPulse())
    }
}

// MARK: - 基础块

/// 条状占位（文本行）
struct SkeletonBlock: View {

    var width: CGFloat? = nil
    var height: CGFloat = 12
    var cornerRadius: CGFloat = 5

    var body: some View {
        RoundedRectangle(cornerRadius: cornerRadius)
            .fill(.quaternary)
            .frame(width: width, height: height)
    }
}

/// 行首图标占位的形状
nonisolated enum SkeletonIcon {
    case none
    case circle(CGFloat)
    case rounded(width: CGFloat, height: CGFloat)
}

// MARK: - 通用行

/// 图标 + 双行文本（+ 可选行尾短条）的列表行骨架，
/// 对应 StorageRow / WorkerRow / TunnelRow / DNSRecordRow 等形状。
struct SkeletonRow: View {

    var icon: SkeletonIcon = .rounded(width: 32, height: 32)
    var titleWidth: CGFloat = 150
    var subtitleWidth: CGFloat? = 210
    var trailingWidth: CGFloat? = nil

    var body: some View {
        HStack(spacing: 12) {
            switch icon {
            case .none:
                EmptyView()
            case .circle(let size):
                Circle()
                    .fill(.quaternary)
                    .frame(width: size, height: size)
            case .rounded(let width, let height):
                RoundedRectangle(cornerRadius: 8)
                    .fill(.quaternary)
                    .frame(width: width, height: height)
            }
            VStack(alignment: .leading, spacing: 6) {
                SkeletonBlock(width: titleWidth, height: 13)
                if let subtitleWidth {
                    SkeletonBlock(width: subtitleWidth, height: 10)
                }
            }
            Spacer()
            if let trailingWidth {
                SkeletonBlock(width: trailingWidth, height: 10)
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - List 形态整页骨架

/// List 页面的整页骨架（玻璃行）。行宽按行号错落，避免呆板。
struct SkeletonList: View {

    var rows: Int = 8
    var icon: SkeletonIcon = .rounded(width: 32, height: 32)
    var showsSubtitle = true
    var trailing = false

    var body: some View {
        List(0..<rows, id: \.self) { index in
            SkeletonRow(
                icon: icon,
                titleWidth: 120 + CGFloat((index * 37) % 70),
                subtitleWidth: showsSubtitle ? 150 + CGFloat((index * 53) % 90) : nil,
                trailingWidth: trailing ? 36 : nil
            )
            .glassRow()
        }
        .scrollContentBackground(.hidden)
        .scrollDisabled(true)
        .skeletonPulse()
    }
}

// MARK: - 玻璃岛内多行骨架

/// ScrollView 页面里嵌在玻璃岛中的多行骨架（D1 表入口、Dashboard 域名卡等）
struct SkeletonIslandRows: View {

    var rows: Int = 4
    var icon: SkeletonIcon = .rounded(width: 32, height: 32)
    var showsSubtitle = true

    var body: some View {
        VStack(spacing: 0) {
            ForEach(0..<rows, id: \.self) { index in
                SkeletonRow(
                    icon: icon,
                    titleWidth: 120 + CGFloat((index * 37) % 70),
                    subtitleWidth: showsSubtitle ? 150 + CGFloat((index * 53) % 90) : nil
                )
                .padding(.horizontal, OCLayout.islandPadding)
                .padding(.vertical, 7)
                if index < rows - 1 {
                    Divider()
                        .padding(.leading, 56)
                }
            }
        }
        .glassIsland(cornerRadius: OCLayout.chipRadius)
        .skeletonPulse()
    }
}

// MARK: - 卡片列表骨架（域名列表）

/// 每行一张玻璃卡的整页骨架（ZoneListView iPhone 形态）
struct SkeletonCardList: View {

    var cards: Int = 6

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: OCLayout.islandGap) {
                SkeletonBlock(width: 150, height: 11)
                    .padding(.horizontal, 4)
                ForEach(0..<cards, id: \.self) { index in
                    HStack(spacing: 12) {
                        Circle()
                            .fill(.quaternary)
                            .frame(width: 36, height: 36)
                        VStack(alignment: .leading, spacing: 6) {
                            SkeletonBlock(width: 130 + CGFloat((index * 41) % 60), height: 13)
                            SkeletonBlock(width: 90, height: 10)
                        }
                        Spacer()
                        Circle()
                            .fill(.quaternary)
                            .frame(width: 8, height: 8)
                    }
                    .padding(OCLayout.islandPadding)
                    .glassIsland()
                }
            }
            .padding(OCLayout.pagePadding)
        }
        .scrollDisabled(true)
        .skeletonPulse()
    }
}

#Preview("骨架") {
    VStack(spacing: 20) {
        SkeletonIslandRows(rows: 3)
        SkeletonRow(icon: .circle(36), trailingWidth: 36)
            .padding(OCLayout.islandPadding)
            .glassIsland()
            .skeletonPulse()
    }
    .padding()
    .background { SkyBackground() }
}
