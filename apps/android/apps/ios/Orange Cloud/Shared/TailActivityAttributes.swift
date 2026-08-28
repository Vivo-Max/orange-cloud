//
//  TailActivityAttributes.swift
//  Orange Cloud（主 App 与 Widget Extension 共享）
//
//  Workers 实时日志的 Live Activity：主 App 发起/更新，Widget Extension 渲染。
//

import Foundation
import ActivityKit

nonisolated struct TailActivityAttributes: ActivityAttributes {

    nonisolated struct ContentState: Codable, Hashable {
        var eventCount:  Int
        var lastLine:    String
        var isConnected: Bool
    }

    var scriptName: String
}
