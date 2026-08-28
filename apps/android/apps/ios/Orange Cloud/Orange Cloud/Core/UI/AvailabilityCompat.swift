//
//  AvailabilityCompat.swift
//  Orange Cloud
//
//  集中存放跨 iOS 版本的 SwiftUI 兼容封装：基线 iOS 17，对 iOS 18+ 专属 API
//  统一在此降级，避免在各视图里散落 #available 守卫。
//

import SwiftUI
import UIKit
import TipKit

extension ProcessInfo {
    /// 当前系统是否落在 iOS 17.0.x（17.0 / 17.0.1 / 17.0.2 / 17.0.3）。
    /// 这一窄段的 TipKit 把 popover 锚定到导航栏 bar button 时，会在
    /// `-[UINavigationBar layoutSubviews]` 阶段抛未捕获异常导致崩溃，Apple 自 17.1 起修复。
    /// 仅用于对这段版本做最小化 UI 降级，勿扩大到 17.1+。
    nonisolated static var isBuggyTipKitNavBar: Bool {
        let v = processInfo.operatingSystemVersion
        return v.majorVersion == 17 && v.minorVersion == 0
    }
}

extension View {
    /// `popoverTip` 的安全封装：iOS 17.0.x 上跳过（见 ``ProcessInfo/isBuggyTipKitNavBar``），
    /// 避免导航栏锚定的 TipKit popover 崩溃；17.1+ 与更高版本行为不变，正常展示气泡提示。
    /// 适用于挂在工具栏 bar button 上的提示；非导航栏场景同样安全（17.0.x 仅少展示一次提示）。
    @ViewBuilder
    func safePopoverTip<T: Tip>(_ tip: T) -> some View {
        if ProcessInfo.isBuggyTipKitNavBar {
            self
        } else {
            popoverTip(tip)
        }
    }
}

// MARK: - 统一动效信号

private struct AppReduceMotionKey: EnvironmentKey {
    static let defaultValue = false
}

extension EnvironmentValues {
    /// App 设置里「减少动画」开关的值（独立于系统辅助功能「减弱动态效果」）。在根视图注入，
    /// 让系统级转场（Zoom 导航转场）与自定义动画（玻璃岛浮现 / 骨架闪烁）都能跟着这个开关走——
    /// 否则开关只接了全局 .transaction，盖不住导航转场与只读系统设置的浮现动画，用户感觉「开了没用」。
    var appReduceMotion: Bool {
        get { self[AppReduceMotionKey.self] }
        set { self[AppReduceMotionKey.self] = newValue }
    }
}

extension View {
    /// 详情页：iOS 18+ 应用 Zoom 导航转场；iOS 17 或开启「减少动画」时原样返回（标准 push）。
    func zoomNavigationTransition<ID: Hashable>(sourceID: ID, in namespace: Namespace.ID) -> some View {
        modifier(ZoomNavigationTransition(sourceID: sourceID, namespace: namespace))
    }

    /// 源视图（列表行）：iOS 18+ 标记 Zoom 转场源；iOS 17 无操作。
    @ViewBuilder
    func zoomTransitionSource(id: some Hashable, in namespace: Namespace.ID) -> some View {
        if #available(iOS 18.0, *) {
            matchedTransitionSource(id: id, in: namespace)
        } else {
            self
        }
    }

    /// 刷新中持续动画：iOS 18+ 用 .rotate 旋转；iOS 17 回退 .pulse
    /// （.rotate 的“持续效果”conformance 自 iOS 18 起才有）。
    @ViewBuilder
    func loadingSpinSymbolEffect(isActive: Bool) -> some View {
        if #available(iOS 18.0, *) {
            symbolEffect(.rotate, isActive: isActive)
        } else {
            symbolEffect(.pulse, isActive: isActive)
        }
    }

    /// 出现时弹一下（一次性 bounce）：iOS 18+ 用 .nonRepeating 持续效果；
    /// iOS 17 静态显示（.bounce 的“持续效果”conformance 自 iOS 18 起才有）。
    @ViewBuilder
    func oneShotBounceSymbolEffect() -> some View {
        if #available(iOS 18.0, *) {
            symbolEffect(.bounce, options: .nonRepeating)
        } else {
            self
        }
    }
}

/// Zoom 导航转场（iOS 18+），开启「减少动画」时降级为标准 push。
/// 读环境注入的 appReduceMotion——系统「减弱动态效果」由系统自动让 .zoom 回退，这里只额外接 App 开关。
private struct ZoomNavigationTransition<ID: Hashable>: ViewModifier {
    @Environment(\.appReduceMotion) private var appReduceMotion
    let sourceID: ID
    let namespace: Namespace.ID

    @ViewBuilder
    func body(content: Content) -> some View {
        if #available(iOS 18.0, *), !appReduceMotion {
            content.navigationTransition(.zoom(sourceID: sourceID, in: namespace))
        } else {
            content
        }
    }
}

extension Color {
    /// Color.mix(with:by:) 的兼容封装：iOS 18+ 用系统实现；iOS 17 回退 UIColor 的 RGB 线性插值。
    nonisolated func mixed(with other: Color, by amount: Double) -> Color {
        if #available(iOS 18.0, *) {
            return mix(with: other, by: amount)
        }
        let t = max(0, min(1, amount))
        var ar: CGFloat = 0, ag: CGFloat = 0, ab: CGFloat = 0, aa: CGFloat = 0
        var br: CGFloat = 0, bg: CGFloat = 0, bb: CGFloat = 0, ba: CGFloat = 0
        UIColor(self).getRed(&ar, green: &ag, blue: &ab, alpha: &aa)
        UIColor(other).getRed(&br, green: &bg, blue: &bb, alpha: &ba)
        return Color(
            .sRGB,
            red:   Double(ar + (br - ar) * t),
            green: Double(ag + (bg - ag) * t),
            blue:  Double(ab + (bb - ab) * t),
            opacity: Double(aa + (ba - aa) * t)
        )
    }
}
