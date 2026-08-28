//
//  RatingPrompt.swift
//  Orange Cloud
//
//  应用内评分邀请的参与度门控与节流（系统 SKStoreReviewController 自身另有年度限频，
//  可能静默不弹）。策略：只把「已登录启动」计为参与度信号；达到阈值后，每个版本最多
//  主动邀请一次。设置「关于」页仍保留用户可手动触发的评分入口，二者互不影响。
//

import Foundation

nonisolated enum RatingPrompt {

    private static let engagedLaunchesKey   = "ratingEngagedLaunches"
    private static let lastPromptVersionKey = "ratingLastPromptVersion"

    /// 触发主动邀请所需的最小「已登录启动」次数
    private static let minEngagedLaunches = 3

    /// App 启动且此次为已登录态时 +1（在 App.init 调用）。
    static func registerEngagedLaunch() {
        let d = UserDefaults.standard
        d.set(d.integer(forKey: engagedLaunchesKey) + 1, forKey: engagedLaunchesKey)
    }

    /// 是否满足主动邀请评分：达到最小启动次数，且当前版本尚未邀请过。
    static var shouldRequest: Bool {
        let d = UserDefaults.standard
        guard d.integer(forKey: engagedLaunchesKey) >= minEngagedLaunches else { return false }
        return d.string(forKey: lastPromptVersionKey) != WhatsNewStore.currentVersion
    }

    /// 记录已在当前版本邀请过（每个版本最多主动邀请一次）。
    static func markRequested() {
        UserDefaults.standard.set(WhatsNewStore.currentVersion, forKey: lastPromptVersionKey)
    }
}
