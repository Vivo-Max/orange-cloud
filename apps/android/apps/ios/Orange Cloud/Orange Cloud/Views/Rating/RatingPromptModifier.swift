//
//  RatingPromptModifier.swift
//  Orange Cloud
//
//  登录后的会话根视图挂载：满足参与度与版本门控时，主动拉起系统评分邀请。
//  与 What's New / 体验者计划错峰（同一启动只出一个打扰），排在最后、优先级最低。
//

import SwiftUI
import StoreKit

struct RatingPromptModifier: ViewModifier {

    @Environment(\.requestReview) private var requestReview

    func body(content: Content) -> some View {
        content.task {
            guard RatingPrompt.shouldRequest else { return }
            // 让 What's New / 体验者计划先完成评估/呈现；主界面稳定后再邀请
            try? await Task.sleep(for: .seconds(4))
            guard !WhatsNewGate.presentedThisLaunch else { return }
            WhatsNewGate.presentedThisLaunch = true   // 占住本次启动的唯一打扰位
            RatingPrompt.markRequested()
            requestReview()
        }
    }
}

extension View {
    /// 应用内评分邀请门控（挂在登录后的会话根视图，排在其它一次性弹窗之后）
    func ratingPrompt() -> some View { modifier(RatingPromptModifier()) }
}
