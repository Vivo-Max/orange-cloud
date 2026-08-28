//
//  OrangeCloudIntents.swift
//  Orange Cloud
//
//  App Intents：Siri / 快捷指令 / Spotlight 入口。
//  查询走 SwiftData 缓存（离线可用），打开模块通过 AppRouter 路由。
//

import Foundation
import AppIntents
import Observation

// MARK: - 路由（Intent → 主界面 Tab）

@Observable
@MainActor
final class AppRouter {
    static let shared = AppRouter()
    var pendingModule: AppModule?
    /// 置 true 时在根层以全屏 cover 弹出「免登录工具箱」。
    /// 入口统一：登录页次级按钮 / 设置入口 / 通知点按都设此标志，挂在 auth 闸门之上。
    var presentToolbox = false

    private init() {}
}

nonisolated enum AppModule: String, AppEnum {
    case dashboard, zones, workers, storage, settings

    static let typeDisplayRepresentation: TypeDisplayRepresentation = "模块"

    static let caseDisplayRepresentations: [AppModule: DisplayRepresentation] = [
        .dashboard: "概览",
        .zones:     "域名",
        .workers:   "Workers",
        .storage:   "存储",
        .settings:  "设置",
    ]
}

// MARK: - 查询域名状态

struct CheckZoneStatusIntent: AppIntent {

    nonisolated static let title: LocalizedStringResource = "查询域名状态"
    nonisolated static let description = IntentDescription("查看某个域名的状态与套餐（来自本地缓存，离线可用）")

    @Parameter(title: "域名")
    var zone: ZoneEntity

    nonisolated static var parameterSummary: some ParameterSummary {
        Summary("查询 \(\.$zone) 的状态")
    }

    func perform() async throws -> some IntentResult & ProvidesDialog {
        let statusText = zone.status == "active" ? String(localized: "正常运行") : zone.status
        return .result(dialog: "\(zone.name) 当前\(statusText)，套餐 \(zone.planName)。")
    }
}

// MARK: - 打开模块

struct OpenModuleIntent: AppIntent {

    nonisolated static let title: LocalizedStringResource = "打开 Orange Cloud"
    nonisolated static let description = IntentDescription("跳转到指定功能模块")
    nonisolated static let openAppWhenRun = true

    @Parameter(title: "模块", default: .dashboard)
    var module: AppModule

    nonisolated static var parameterSummary: some ParameterSummary {
        Summary("打开 \(\.$module)")
    }

    @MainActor
    func perform() async throws -> some IntentResult {
        AppRouter.shared.pendingModule = module
        return .result()
    }
}

// MARK: - Siri 短语注册

nonisolated struct OrangeCloudShortcuts: AppShortcutsProvider {

    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: CheckZoneStatusIntent(),
            phrases: [
                "用 \(.applicationName) 查域名",
                "查询 \(.applicationName) 域名状态",
            ],
            shortTitle: "域名状态",
            systemImageName: "globe"
        )
        AppShortcut(
            intent: OpenModuleIntent(),
            phrases: [
                "打开 \(.applicationName)",
            ],
            shortTitle: "打开模块",
            systemImageName: "square.grid.2x2"
        )
    }
}
