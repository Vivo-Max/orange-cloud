//
//  PermissionGatedNavigationLink.swift
//  Orange Cloud
//
//  当用户未授权某 scope 时，将 NavigationLink 替换为锁定状态的按钮并弹出权限提示。
//  图标使用设计稿的 TintIcon（彩色圆底）。
//

import SwiftUI

/// 有 scope 则正常导航，无 scope 则显示锁图标并弹出说明。
struct PermissionGatedNavigationLink<Destination: View>: View {

    let label:         String
    let systemImage:   String
    let requiredScope: String
    var tint: Color = .ocOrange
    /// List 内由系统提供 chevron；卡片等自定义容器中置 true 手动绘制
    var showsChevron: Bool = false
    @ViewBuilder let destination: () -> Destination

    @Environment(AuthManager.self) private var auth
    @State private var showDenied = false

    private var rowLabel: some View {
        HStack(spacing: 12) {
            TintIcon(systemImage: systemImage, color: tint)
            Text(label)
                .foregroundStyle(.primary)
            Spacer()
        }
    }

    var body: some View {
        if auth.hasScope(requiredScope) {
            NavigationLink(destination: destination()) {
                HStack {
                    rowLabel
                    if showsChevron {
                        Image(systemName: "chevron.right")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(.tertiary)
                    }
                }
            }
        } else {
            Button { showDenied = true } label: {
                HStack {
                    rowLabel
                    Image(systemName: "lock.fill")
                        .foregroundStyle(.tertiary)
                        .font(.caption)
                        .accessibilityHidden(true)
                }
            }
            .foregroundStyle(.primary)
            .accessibilityHint("已锁定，需要额外授权")
            .alert("权限不足", isPresented: $showDenied) {
                if let sessionId = auth.currentSessionId {
                    Button("一键重授权") {
                        Task { await auth.reauthorize(sessionId: sessionId, additionalScopes: [requiredScope]) }
                    }
                }
                Button("好", role: .cancel) {}
            } message: {
                Text("当前授权未包含「\(label)」的访问权限（\(requiredScope)）。点「一键重授权」补齐，无需退出登录。")
            }
        }
    }
}

/// 值式（value-based）版权限门控导航行：有 scope 则 `NavigationLink(value:)`（目的地由宿主栈根的
/// `.navigationDestination` 解析），无 scope 则锁定态 + 重授权弹窗。
/// 用于「目的页自身还要继续 push」的入口（如 Workers→详情）：eager `NavigationLink(destination:)`
/// 急切构造的目的页内部再 push 会失灵/错乱，值式 + 栈根 navdest 才能单栈正常逐级 push。
struct PermissionGatedValueLink<V: Hashable>: View {

    let label:         String
    let systemImage:   String
    let requiredScope: String
    var tint: Color = .ocOrange
    /// List 内由系统提供 chevron；卡片等自定义容器中置 true 手动绘制
    var showsChevron: Bool = false
    let value:         V

    @Environment(AuthManager.self) private var auth
    @State private var showDenied = false

    private var rowLabel: some View {
        HStack(spacing: 12) {
            TintIcon(systemImage: systemImage, color: tint)
            Text(label).foregroundStyle(.primary)
            Spacer()
        }
    }

    var body: some View {
        if auth.hasScope(requiredScope) {
            NavigationLink(value: value) {
                HStack {
                    rowLabel
                    if showsChevron {
                        Image(systemName: "chevron.right")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(.tertiary)
                    }
                }
            }
        } else {
            Button { showDenied = true } label: {
                HStack {
                    rowLabel
                    Image(systemName: "lock.fill")
                        .foregroundStyle(.tertiary)
                        .font(.caption)
                        .accessibilityHidden(true)
                }
            }
            .foregroundStyle(.primary)
            .accessibilityHint("已锁定，需要额外授权")
            .alert("权限不足", isPresented: $showDenied) {
                if let sessionId = auth.currentSessionId {
                    Button("一键重授权") {
                        Task { await auth.reauthorize(sessionId: sessionId, additionalScopes: [requiredScope]) }
                    }
                }
                Button("好", role: .cancel) {}
            } message: {
                Text("当前授权未包含「\(label)」的访问权限（\(requiredScope)）。点「一键重授权」补齐，无需退出登录。")
            }
        }
    }
}
