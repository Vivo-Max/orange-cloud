//
//  WhatsNew.swift
//  Orange Cloud
//
//  版本更新后的「新功能」展示：内容按版本curated，启动后比对 lastSeen 决定是否弹。
//  ⚠️ 内容是单一数据源：改 packages/changelog/ios.json 后运行 `pnpm changelog:gen`，
//     会重新生成 WhatsNewReleases.generated.swift + WhatsNew.xcstrings（勿手改这两个文件）。
//

import Foundation

nonisolated struct WhatsNewItem: Identifiable, Sendable {
    let id = UUID()
    let icon:   String
    let title:  String
    let detail: String
}

nonisolated struct WhatsNewRelease: Sendable {
    let version: String
    let items:   [WhatsNewItem]
}

nonisolated enum WhatsNewContent {
    /// 内容由 packages/changelog 生成，见 WhatsNewReleases.generated.swift。
    static let releases: [WhatsNewRelease] = WhatsNewGenerated.releases
}

nonisolated enum WhatsNewStore {

    static var currentVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
    }

    /// 数值分段比较：a 是否比 b 新（"1.1.0" > "1.0.1"，"1.10" > "1.9"）
    static func isNewer(_ a: String, than b: String) -> Bool {
        a.compare(b, options: .numeric) == .orderedDescending
    }

    /// (seen, current] 区间内所有版本的新功能，新版在前
    static func items(after seen: String, upTo current: String) -> [WhatsNewItem] {
        WhatsNewContent.releases
            .filter { isNewer($0.version, than: seen) && !isNewer($0.version, than: current) }
            .sorted { isNewer($0.version, than: $1.version) }
            .flatMap(\.items)
    }
}

/// 启动时拍一张「是否已登录」的快照，用于区分老用户升级 vs 全新安装：
/// 老用户升级到首个带 What's New 的版本（lastSeen 为空但启动即已登录）要补看一次；
/// 全新安装首次登录后不打扰。由 App.init 在创建 AuthManager 后写入。
enum WhatsNewGate {
    static var wasLoggedInAtLaunch = false
    /// 本次启动弹了 What's New：体验者计划询问等其它一次性弹窗让路，下次启动再问
    static var presentedThisLaunch = false
}
