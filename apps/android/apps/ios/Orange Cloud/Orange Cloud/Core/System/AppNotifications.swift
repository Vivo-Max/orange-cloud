//
//  AppNotifications.swift
//  Orange Cloud
//
//  本地通知（方案 A）：BGAppRefreshTask 给后台时间时检测变化并发本地通知。
//  时机由 iOS 调度（依用户使用习惯，延迟数分钟至数小时），是无服务器下的尽力而为。
//

import Foundation
import UserNotifications
import SwiftData

@MainActor
enum AppNotifications {

    static let masterKey = "notificationsEnabled"

    // MARK: - 授权

    static func requestAuthorization() async -> Bool {
        (try? await UNUserNotificationCenter.current()
            .requestAuthorization(options: [.alert, .sound, .badge])) ?? false
    }

    static func authorizationStatus() async -> UNAuthorizationStatus {
        await UNUserNotificationCenter.current().notificationSettings().authorizationStatus
    }

    // MARK: - 后台检测（由 BackgroundRefresh 调用）

    static func runBackgroundChecks(authManager: AuthManager) async {
        let defaults = UserDefaults.standard
        guard defaults.bool(forKey: masterKey),
              await authorizationStatus() == .authorized,
              authManager.isLoggedIn else { return }

        let client = CFAPIClient(authManager: authManager)

        if defaults.bool(forKey: "notifyZoneStatus") {
            await checkZoneStatusChanges(zoneService: ZoneService(client: client))
        }
        if defaults.bool(forKey: "notifyBuildFailures"),
           authManager.hasScope("workers-ci.read") {
            await checkBuildFailures(
                buildService: WorkerBuildService(client: client),
                accountService: AccountService(client: client)
            )
        }
        if defaults.bool(forKey: "notifyWorkerErrors"),
           authManager.hasScope("account-analytics.read") {
            await checkWorkerErrors(
                analyticsService: AnalyticsService(client: client),
                accountService: AccountService(client: client)
            )
        }
    }

    /// Zone 状态：与 SwiftData 缓存对比，变化即通知并回写缓存
    private static func checkZoneStatusChanges(zoneService: ZoneService) async {
        let context = ModelContext(CacheContainer.shared)
        guard let cached = SafeCache.fetch(FetchDescriptor<CachedZone>(), context: context),
              !cached.isEmpty else { return }

        var changes: [(name: String, from: String, to: String)] = []
        // 按缓存中的账户分组拉取（上限 5 个账户，控制后台时间预算）
        let accountIds = Array(Set(cached.map(\.accountId)).prefix(5))
        for accountId in accountIds {
            guard let zones = try? await zoneService.listZones(accountId: accountId) else { continue }
            let byId = Dictionary(uniqueKeysWithValues: zones.map { ($0.id, $0) })
            for entry in cached where entry.accountId == accountId {
                // 比对展示态：暂停与 status 正交（暂停时 status 仍是 active），
                // 在别处暂停/恢复了域名也要能推送到
                if let fresh = byId[entry.id] {
                    let freshStatus = (fresh.paused ?? false) ? "paused" : fresh.status
                    if freshStatus != entry.displayStatus {
                        changes.append((entry.name, entry.displayStatus, freshStatus))
                        entry.update(from: fresh)
                    }
                }
            }
        }
        SafeCache.perform("Zone 状态回写") { try context.save() }

        guard !changes.isEmpty else { return }
        if changes.count == 1, let change = changes.first {
            notify(
                title: String(localized: "域名状态变更"),
                body: String(localized: "\(change.name) 状态从 \(change.from) 变为 \(change.to)"),
                id: "zone-status-\(change.name)"
            )
        } else {
            notify(
                title: String(localized: "域名状态变更"),
                body: String(localized: "\(changes.count) 个域名状态发生变化：\(changes.map(\.name).formatted())"),
                id: "zone-status-multi"
            )
        }
    }

    /// Worker 错误：过去 1 小时错误数 > 0 时通知（至少间隔 55 分钟，避免轰炸）
    private static func checkWorkerErrors(
        analyticsService: AnalyticsService,
        accountService: AccountService
    ) async {
        let defaults = UserDefaults.standard
        let lastNotifyKey = "lastWorkerErrorNotify"
        if let last = defaults.object(forKey: lastNotifyKey) as? Date,
           Date().timeIntervalSince(last) < 55 * 60 {
            return
        }
        guard let account = try? await accountService.listAccounts().first,
              let errors = try? await analyticsService.workersErrorsLastHour(accountId: account.id),
              errors > 0 else { return }

        defaults.set(Date(), forKey: lastNotifyKey)
        notify(
            title: String(localized: "Workers 错误"),
            body: String(localized: "过去 1 小时共 \(errors.formatted()) 次调用错误，点击查看详情"),
            id: "worker-errors"
        )
    }

    /// 构建失败：Cloudflare **没有** Workers Builds 的告警类型（69 个 alert_type 里无对应项），
    /// 所以走不了服务端 webhook，只能在后台刷新窗口里自己比对。
    /// 已通知过的 build_uuid 落盘去重，避免同一次失败反复提醒。
    private static func checkBuildFailures(
        buildService: WorkerBuildService,
        accountService: AccountService
    ) async {
        let defaults = UserDefaults.standard
        // 用户在 Worker 详情页勾选要盯的脚本；没勾就不做任何请求
        let watched = defaults.stringArray(forKey: watchedBuildScriptsKey) ?? []
        guard !watched.isEmpty,
              let account = try? await accountService.listAccounts().first else { return }

        var notified = Set(defaults.stringArray(forKey: notifiedBuildsKey) ?? [])
        var failures: [(script: String, uuid: String)] = []

        // 后台时间预算有限，最多盯 5 个脚本
        for script in watched.prefix(5) {
            guard let builds = try? await buildService.builds(accountId: account.id, scriptId: script),
                  let latest = builds.first else { continue }
            guard latest.displayState == .failed, !notified.contains(latest.buildUuid) else { continue }
            failures.append((script, latest.buildUuid))
            notified.insert(latest.buildUuid)
        }

        guard !failures.isEmpty else { return }
        // 去重集合只保留最近 200 条，避免无限膨胀
        defaults.set(Array(notified.suffix(200)), forKey: notifiedBuildsKey)

        if failures.count == 1, let failure = failures.first {
            notify(
                title: String(localized: "构建失败"),
                body: String(localized: "\(failure.script) 的最新一次构建失败了"),
                id: "build-failed-\(failure.uuid)"
            )
        } else {
            notify(
                title: String(localized: "构建失败"),
                body: String(localized: "\(failures.count) 个 Worker 的最新构建失败了：\(failures.map(\.script).formatted())"),
                id: "build-failed-multi"
            )
        }
    }

    // MARK: - 盯构建的脚本清单（Worker 详情页勾选）

    static let watchedBuildScriptsKey = "watchedBuildScripts"
    private static let notifiedBuildsKey = "notifiedBuildUuids"

    static func isWatchingBuilds(_ scriptName: String) -> Bool {
        (UserDefaults.standard.stringArray(forKey: watchedBuildScriptsKey) ?? []).contains(scriptName)
    }

    static func setWatchingBuilds(_ scriptName: String, _ on: Bool) {
        var list = UserDefaults.standard.stringArray(forKey: watchedBuildScriptsKey) ?? []
        if on {
            guard !list.contains(scriptName) else { return }
            list.append(scriptName)
        } else {
            list.removeAll { $0 == scriptName }
        }
        UserDefaults.standard.set(list, forKey: watchedBuildScriptsKey)
    }

    // MARK: - 发送

    private static func notify(title: String, body: String, id: String) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        let request = UNNotificationRequest(identifier: id, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request)
    }
}
