//
//  AppPreferences.swift
//  Orange Cloud
//
//  App 级显示偏好：外观（跟随系统 / 亮色 / 暗色）与界面语言。
//  外观经根视图 preferredColorScheme 即时生效；
//  语言写 AppleLanguages 覆盖（与系统「按 App 设定语言」同一机制），重新打开 App 生效。
//

import SwiftUI

/// 外观模式，存 UserDefaults（appAppearance）
nonisolated enum AppAppearance: String, CaseIterable, Identifiable, Sendable {

    case system
    case light
    case dark

    var id: String { rawValue }

    static let storageKey = "appAppearance"

    var label: String {
        switch self {
        case .system: String(localized: "跟随系统")
        case .light:  String(localized: "亮色")
        case .dark:   String(localized: "暗色")
        }
    }

    /// nil = 跟随系统
    var colorScheme: ColorScheme? {
        switch self {
        case .system: nil
        case .light:  .light
        case .dark:   .dark
        }
    }
}

/// 动效偏好，存 UserDefaults（reduceAppAnimations）。开启后由根视图统一禁用过渡/隐式动画，
/// 让页面切换与界面变化更跟手（独立于系统「减弱动态效果」，由用户在设置里手动控制）。
nonisolated enum AppMotion {
    static let storageKey = "reduceAppAnimations"
}

/// 界面语言，存 UserDefaults（appLanguage）。rawValue 即语言代码（system 除外）。
nonisolated enum AppLanguage: String, CaseIterable, Identifiable, Sendable {

    case system = "system"
    case zhHans = "zh-Hans"
    case en     = "en"
    case zhHant = "zh-Hant"
    case zhHK   = "zh-HK"
    case ja     = "ja"
    case ko     = "ko"
    case de     = "de"
    case fr     = "fr"
    case esMX   = "es-MX"
    case ptBR   = "pt-BR"
    case ptPT   = "pt-PT"
    case ar     = "ar"
    case tr     = "tr"

    var id: String { rawValue }

    static let storageKey = "appLanguage"

    /// 语言名按各自语言原文显示（不随界面语言翻译），仅「跟随系统」走本地化
    var label: String {
        switch self {
        case .system: String(localized: "跟随系统")
        case .zhHans: "简体中文"
        case .en:     "English"
        case .zhHant: "繁體中文（台灣）"
        case .zhHK:   "繁體中文（香港）"
        case .ja:     "日本語"
        case .ko:     "한국어"
        case .de:     "Deutsch"
        case .fr:     "Français"
        case .esMX:   "Español (México)"
        case .ptBR:   "Português (Brasil)"
        case .ptPT:   "Português (Portugal)"
        case .ar:     "العربية"
        case .tr:     "Türkçe"
        }
    }

    /// 写 / 清除 AppleLanguages 覆盖，重新打开 App 后生效
    func apply() {
        if self == .system {
            UserDefaults.standard.removeObject(forKey: "AppleLanguages")
        } else {
            UserDefaults.standard.set([rawValue], forKey: "AppleLanguages")
        }
    }
}
