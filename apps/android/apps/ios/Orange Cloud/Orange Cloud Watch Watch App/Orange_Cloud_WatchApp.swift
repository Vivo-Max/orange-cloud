//
//  Orange_Cloud_WatchApp.swift
//  Orange Cloud Watch Watch App
//
//  watchOS App 入口：建立 WatchConnectivity 桥并注入界面。
//

import SwiftUI

@main
struct Orange_Cloud_Watch_Watch_AppApp: App {

    @State private var bridge = WatchBridge()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(bridge)
                .tint(Color.ocOrange)
                .task { bridge.activate() }
        }
    }
}
