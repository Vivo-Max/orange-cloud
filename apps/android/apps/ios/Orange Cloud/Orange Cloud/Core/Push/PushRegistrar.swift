//
//  PushRegistrar.swift
//  Orange Cloud
//
//  推送注册编排：请求通知权限 + 注册远程通知；拿到 APNs token 后向中继注册换 device_key。
//  单例供 AppDelegate 回调，UI 观察其状态。
//

import Foundation
import UIKit
import Observation

@Observable
@MainActor
final class PushRegistrar {

    static let shared = PushRegistrar()

    enum Status: Equatable {
        case idle          // 未启用
        case registering   // 等 APNs token / 注册中
        case registered    // 已拿到 device_key
        case denied        // 通知权限被拒（仍可注册，但不弹横幅）
        case failed
    }

    private(set) var status: Status
    var error: String?

    var deviceKey: String? { PushConfig.deviceKey }
    var endpointURL: String? { PushConfig.endpointURL }

    private let service = PushService()

    private init() {
        status = PushConfig.deviceKey == nil ? .idle : .registered
    }

    /// 启用：请求权限 + 注册远程通知（token 异步到 AppDelegate → handleToken）
    func enable() async {
        error = nil
        status = .registering
        let granted = await AppNotifications.requestAuthorization()
        if !granted { status = .denied }   // 仍继续注册 token（端点可收，只是不弹横幅）
        UIApplication.shared.registerForRemoteNotifications()
    }

    /// AppDelegate 拿到 APNs device token 后调用
    func handleToken(_ tokenData: Data) async {
        let hex = tokenData.map { String(format: "%02x", $0) }.joined()
        do {
            let key = try await service.register(deviceToken: hex, existingKey: PushConfig.deviceKey)
            PushConfig.deviceKey = key
            if status != .denied { status = .registered }
        } catch {
            status = .failed
            self.error = error.localizedDescription
        }
    }

    func registrationFailed(_ error: Error) {
        status = .failed
        self.error = error.localizedDescription
    }
}
