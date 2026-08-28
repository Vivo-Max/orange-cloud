//
//  PushAppDelegate.swift
//  Orange Cloud
//
//  仅为接住 APNs 注册回调与前台通知展示而引入的 UIApplicationDelegate。
//  Phase 1：前台到达的推送记进收件箱；后台到达留给 Phase 2 的 NSE。
//

import UIKit
import UserNotifications

final class PushAppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Task { await PushRegistrar.shared.handleToken(deviceToken) }
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        Task { @MainActor in PushRegistrar.shared.registrationFailed(error) }
    }

    // 前台到达：记进收件箱，并照常展示横幅
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        let content = notification.request.content
        // 前台兜底写收件箱（NSE 也会写，靠 sourceID = request.identifier 去重）
        PushInbox.append(PushInbox.message(
            from: content.userInfo,
            fallbackTitle: content.title,
            fallbackBody: content.body,
            sourceID: notification.request.identifier
        ))
        return [.banner, .sound, .badge, .list]
    }

    // 点击通知：打开免登录工具箱（推送中心）
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse
    ) async {
        await MainActor.run { AppRouter.shared.presentToolbox = true }
    }
}
