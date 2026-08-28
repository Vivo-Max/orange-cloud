//
//  Orange_CloudApp.swift
//  Orange Cloud
//
//  Created by 陳柘 on 2026/6/10.
//

import SwiftUI
import SwiftData
import TipKit
import CoreSpotlight
import ActivityKit

@main
struct Orange_CloudApp: App {

    @State private var authManager: AuthManager
    @UIApplicationDelegateAdaptor(PushAppDelegate.self) private var pushDelegate
    @Environment(\.scenePhase) private var scenePhase

    @AppStorage(AppAppearance.storageKey) private var appearanceRaw = AppAppearance.system.rawValue
    @AppStorage(AppMotion.storageKey) private var reduceAnimations = false

    let sharedModelContainer = CacheContainer.shared

    init() {
        // 最先安装崩溃捕获，让启动期任意一步崩溃都能被记录、随下次反馈带出。
        CrashReporter.install()
        CrashReporter.recordBreadcrumb("AppStart begin")
        // BGTask 处理器必须在 didFinishLaunching 返回前登记，且不依赖任何业务对象——
        // 越早越好，AuthManager 稍后用 setAuthManager 回填。
        BackgroundRefresh.register()
        #if DEBUG
        MockCloudflare.activateIfRequested()   // 诊断 mock：仅 ORANGE_MOCK=1 时生效
        #endif
        let manager = AuthManager()
        _authManager = State(initialValue: manager)
        CrashReporter.recordBreadcrumb("AppStart auth manager created")
        // 串行预热缓存库实体解析（iOS 17.x 冷启动首次并发 fetch 竞态，Sentry APPLE-IOS-Y）
        CacheContainer.warmUp()
        WhatsNewGate.wasLoggedInAtLaunch = manager.isLoggedIn
        // 参与度信号：只把「已登录启动」计入，用于稍后主动邀请评分（每版本至多一次）
        if manager.isLoggedIn { RatingPrompt.registerEngagedLaunch() }
        BackgroundRefresh.setAuthManager(manager)
        // iOS 26 连续后台任务（R2 大对象 copy/move 续传），须在启动时注册处理器
        if #available(iOS 26.0, *) {
            ContinuedTaskRunner.register()
        }
        WatchSessionManager.shared.start(authManager: manager)
        EntitlementStore.shared.start()
        // 体验者计划：仅当用户此前已同意才会真正拉起 Sentry（默认不初始化）。
        // 须在 CrashReporter.install() 之后，让 Sentry 链式保留我们的崩溃 handler。
        _ = TelemetryStore.shared
        Self.reapOrphanTailActivities()
        try? Tips.configure()
        AppLog.logLaunch(
            loggedIn: manager.isLoggedIn,
            sessionCount: manager.sessions.count
        )
        CrashReporter.recordBreadcrumb("AppStart launch completed")
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(authManager)
                .environment(EntitlementStore.shared)
                .tint(.ocOrange)   // 全局品牌橙（Cloudflare #F48120）
                .preferredColorScheme(AppAppearance(rawValue: appearanceRaw)?.colorScheme)
                // 「减少动画」：全局抹掉隐式与 withAnimation 过渡，让界面变化即时生效
                .transaction { txn in
                    if reduceAnimations {
                        txn.disablesAnimations = true
                        txn.animation = nil
                    }
                }
                .onContinueUserActivity(CSSearchableItemActionType) { activity in
                    handleSpotlightTap(activity)
                }
        }
        .modelContainer(sharedModelContainer)
        .onChange(of: scenePhase) {
            AppLog.app.info("scenePhase -> \(String(describing: scenePhase))")
            if scenePhase == .background {
                BackgroundRefresh.schedule()
            }
        }
    }

    /// 收尸：结束上次进程残留的 tail Live Activity。冷启动时没有任何 VM 持有引用，
    /// 屏上若还挂着卡片，必是崩溃 / 强杀遗留的孤儿——逐个 .immediate 结束。
    private static func reapOrphanTailActivities() {
        for activity in Activity<TailActivityAttributes>.activities {
            Task { await activity.end(nil, dismissalPolicy: .immediate) }
        }
    }

    /// Spotlight 搜索结果点击：跳到对应模块（Zone/DNS 都归属 Zones Tab）
    private func handleSpotlightTap(_ activity: NSUserActivity) {
        guard activity.userInfo?[CSSearchableItemActivityIdentifier] is String else { return }
        AppRouter.shared.pendingModule = .zones
    }
}
