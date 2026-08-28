//
//  AppTips.swift
//  Orange Cloud
//
//  TipKit 上下文引导：首次使用关键功能时的提示。
//  Tips.configure() 在 App 入口调用。
//

import Foundation
import TipKit

/// Zone 列表：下拉刷新
nonisolated struct ZoneRefreshTip: Tip {
    var title: Text {
        Text("下拉刷新")
    }
    var message: Text? {
        Text("列表展示的是本地缓存，下拉可从 Cloudflare 拉取最新数据")
    }
    var image: Image? {
        Image(systemName: "arrow.clockwise")
    }
}

/// DNS 列表：滑动操作
nonisolated struct DNSSwipeTip: Tip {
    var title: Text {
        Text("滑动管理记录")
    }
    var message: Text? {
        Text("在记录上向左滑动可以编辑或删除")
    }
    var image: Image? {
        Image(systemName: "hand.draw")
    }
}

/// 实时日志：暂停
nonisolated struct TailPauseTip: Tip {
    var title: Text {
        Text("日志滚动太快？")
    }
    var message: Text? {
        Text("点击暂停可冻结画面，连接保持不断开")
    }
    var image: Image? {
        Image(systemName: "pause.circle")
    }
}

/// Dashboard：账号切换
nonisolated struct AccountSwitchTip: Tip {
    var title: Text {
        Text("多账号切换")
    }
    var message: Text? {
        Text("点击账号卡片可在多个 Cloudflare 账号间快速切换")
    }
    var image: Image? {
        Image(systemName: "person.2")
    }

    /// 只有多账号用户才显示
    @Parameter static var hasMultipleAccounts: Bool = false

    var rules: [Rule] {
        #Rule(Self.$hasMultipleAccounts) { $0 == true }
    }
}
