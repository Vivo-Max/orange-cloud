//
//  Theme.swift
//  Orange Cloud
//
//  设计系统色板（orange-cloud/project/_ds/tokens/colors.css）。
//  品牌主色是 Cloudflare 橙 #F48120，不是系统橙。
//

import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

nonisolated extension Color {

    /// Cloudflare 品牌橙 #F48120 — 装饰、天空插画、TintIcon 圆底、图表线、选中态
    static let ocOrange = Color(red: 0xF4 / 255, green: 0x81 / 255, blue: 0x20 / 255)

    /// 按压态 #D86F12 — 也用作主 CTA 填充（配白色粗体大字达 WCAG 大字 3:1）
    static let ocOrangePressed = Color(red: 0xD8 / 255, green: 0x6F / 255, blue: 0x12 / 255)

    /// Enterprise 金 #C99A1E
    static let ocGold = Color(red: 0xC9 / 255, green: 0x9A / 255, blue: 0x1E / 255)

    /// 浅色背景上的「橙色文字 / 可交互橙色图标」专用：
    /// 浅色模式加深到 #B5530A（≈5:1，达 WCAG AA 正文），深色模式回到品牌亮橙（深底上已达标）。
    /// 装饰性橙仍用 `.ocOrange`。watchOS 永远深色底，无 UIKit，直接用亮橙。
    static let ocOrangeText: Color = {
        #if canImport(UIKit) && !os(watchOS)
        return Color(uiColor: UIColor { trait in
            trait.userInterfaceStyle == .dark
                ? UIColor(red: 0xF4 / 255, green: 0x81 / 255, blue: 0x20 / 255, alpha: 1)
                : UIColor(red: 0xB5 / 255, green: 0x53 / 255, blue: 0x0A / 255, alpha: 1)
        })
        #else
        return ocOrange
        #endif
    }()
}

/// 域名字母头像的哈希色板（与设计稿 AV_PALETTE 一致）
nonisolated enum AvatarPalette {

    static let colors: [Color] = [
        Color(red: 0xE8 / 255, green: 0x74 / 255, blue: 0x3B / 255),
        Color(red: 0x3D / 255, green: 0x86 / 255, blue: 0xE0 / 255),
        Color(red: 0x1F / 255, green: 0x9D / 255, blue: 0x5B / 255),
        Color(red: 0x9B / 255, green: 0x59 / 255, blue: 0xC9 / 255),
        Color(red: 0xE0 / 255, green: 0x50 / 255, blue: 0x8C / 255),
        Color(red: 0xC9 / 255, green: 0x9A / 255, blue: 0x1E / 255),
        Color(red: 0x2B / 255, green: 0xAF / 255, blue: 0xA6 / 255),
        Color(red: 0x5B / 255, green: 0x6C / 255, blue: 0xE0 / 255),
        Color(red: 0xD8 / 255, green: 0x5C / 255, blue: 0x5C / 255),
        Color(red: 0x4F / 255, green: 0x7C / 255, blue: 0x9C / 255),
    ]

    /// 与设计稿一致的字符串哈希：h = h * 31 + char
    static func color(for text: String) -> Color {
        var hash: UInt32 = 0
        for unit in text.unicodeScalars {
            hash = hash &* 31 &+ UInt32(unit.value)
        }
        return colors[Int(hash % UInt32(colors.count))]
    }
}
