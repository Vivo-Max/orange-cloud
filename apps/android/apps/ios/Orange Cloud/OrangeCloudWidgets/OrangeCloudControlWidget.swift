//
//  OrangeCloudControlWidget.swift
//  OrangeCloudWidgets
//
//  控制中心按钮（iOS 18 ControlWidget）：一键打开 Orange Cloud。
//

import WidgetKit
import SwiftUI
import AppIntents

@available(iOS 18.0, *)
struct OrangeCloudControlWidget: ControlWidget {

    var body: some ControlWidgetConfiguration {
        StaticControlConfiguration(kind: "OrangeCloudControlWidget") {
            ControlWidgetButton(action: LaunchOrangeCloudIntent()) {
                Label("Orange Cloud", systemImage: "cloud.fill")
            }
        }
        .displayName("打开 Orange Cloud")
        .description("快速打开 Cloudflare 管理")
    }
}

/// 从控制中心启动主 App
nonisolated struct LaunchOrangeCloudIntent: AppIntent {
    static let title: LocalizedStringResource = "打开 Orange Cloud"
    static let openAppWhenRun = true

    func perform() async throws -> some IntentResult {
        .result()
    }
}
