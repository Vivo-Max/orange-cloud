//
//  ReauthorizeButton.swift
//  Orange Cloud
//
//  「一键重授权」：对指定身份补授一组 scope（原地升级，不新建账号、不退出登录）。
//  缺 scope 的功能入口与整页锁定态、账号详情页共用。
//

import SwiftUI

struct ReauthorizeButton: View {

    /// 要重新授权的身份
    let sessionId: UUID
    /// 要补齐的 scope（会与该身份已授权的合并后一起请求）
    let scopes: [String]
    var title: String = String(localized: "一键重授权")

    @Environment(AuthManager.self) private var auth
    @State private var isWorking = false
    @State private var errorText: String?

    var body: some View {
        Button {
            isWorking = true
            Task {
                await auth.reauthorize(sessionId: sessionId, additionalScopes: scopes)
                isWorking = false
                // 失败/串号时 reauthorize 会写 errorMessage；就地取出后清空，避免污染登录流程
                if let msg = auth.errorMessage {
                    errorText = msg
                    auth.errorMessage = nil
                }
            }
        } label: {
            HStack(spacing: 8) {
                if isWorking {
                    ProgressView()
                } else {
                    Image(systemName: "lock.open")
                }
                Text(title)
            }
        }
        .disabled(isWorking)
        .alert(String(localized: "重新授权未完成"), isPresented: .init(
            get: { errorText != nil },
            set: { if !$0 { errorText = nil } }
        )) {
            Button("好", role: .cancel) {}
        } message: {
            Text(errorText ?? "")
        }
    }
}
