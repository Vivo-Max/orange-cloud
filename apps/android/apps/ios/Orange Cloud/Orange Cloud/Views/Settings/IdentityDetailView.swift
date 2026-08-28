//
//  IdentityDetailView.swift
//  Orange Cloud
//
//  单个登录身份（Cloudflare 账号）详情：授权状态 + 设为当前 + 退出登录。
//  退出只移除此身份；所有身份退出后由 ContentView 自动回到登录页。
//

import SwiftUI

struct IdentityDetailView: View {

    let identity: AuthSessionMeta

    @Environment(AuthManager.self) private var auth
    @Environment(\.dismiss) private var dismiss

    @State private var showSignOutConfirm = false
    @State private var isSigningOut = false

    private var isCurrent: Bool {
        auth.currentSessionId == identity.id
    }

    /// 此身份的 scope → 功能模块（中文名 + 读写级别）
    private var grantedFeatures: [(title: String, note: String)] {
        FeaturePermission.allFeatures.compactMap { feature in
            let readGranted = feature.readScopes.contains { identity.scopes.contains($0) }
            let editGranted = feature.editScopes.contains { identity.scopes.contains($0) }
            guard readGranted || editGranted else { return nil }
            return (feature.title, editGranted ? String(localized: "读写") : String(localized: "只读"))
        }
    }

    /// 此身份完全未授予（读写都没有）的功能 → 可一键补齐
    private var upgradeableFeatures: [FeaturePermission] {
        FeaturePermission.allFeatures.filter { feature in
            let hasRead = feature.readScopes.contains { identity.scopes.contains($0) }
            let hasEdit = feature.editScopes.contains { identity.scopes.contains($0) }
            return !hasRead && !hasEdit
        }
    }

    /// 补齐 upgradeableFeatures 需要请求的 scope 全集
    private var upgradeScopes: [String] {
        var set = Set<String>()
        for feature in upgradeableFeatures {
            feature.readScopes.forEach { set.insert($0) }
            feature.editScopes.forEach { set.insert($0) }
        }
        return Array(set)
    }

    var body: some View {
        List {
            // ── 身份信息 ──
            Section {
                VStack(spacing: 10) {
                    Text(String(identity.label.first ?? "C").uppercased())
                        .font(.system(size: 28, weight: .bold, design: .rounded))
                        .foregroundStyle(.white)
                        .frame(width: 64, height: 64)
                        .background(
                            LinearGradient(
                                colors: [Color(red: 1, green: 0.65, blue: 0.31), .ocOrangePressed],
                                startPoint: .topLeading, endPoint: .bottomTrailing
                            ),
                            in: Circle()
                        )
                        .accessibilityHidden(true)
                    Text(identity.label)
                        .font(.title3.bold())
                    Label("OAuth 2.0", systemImage: "checkmark.seal.fill")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.green)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
                .listRowBackground(Color.clear)
            }

            // ── 当前身份 ──
            Section {
                if isCurrent {
                    Label("当前使用中的账号", systemImage: "checkmark.circle.fill")
                        .foregroundStyle(.green)
                } else {
                    Button {
                        auth.switchSession(identity.id)
                    } label: {
                        Label("切换到此账号", systemImage: "arrow.right.circle")
                    }
                }
            } footer: {
                Text("Zones、Workers、存储等页面的数据来自当前账号。")
            }
            .glassRow()

            // ── 已授权权限 ──
            Section {
                if grantedFeatures.isEmpty {
                    Text("无权限信息")
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(grantedFeatures, id: \.title) { feature in
                        HStack(spacing: 12) {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundStyle(.green)
                            Text(feature.title)
                            Spacer()
                            Text(feature.note)
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            } header: {
                Text("已授权权限")
            } footer: {
                Text("权限在登录时授予。如需开放更多功能可重新授权补齐；收回权限请在 Cloudflare Dashboard → Authorized Applications 中撤销。")
            }
            .glassRow()

            // ── 升级授权（补齐未授予的功能，原地重授权同一账号）──
            if !upgradeableFeatures.isEmpty {
                Section {
                    ForEach(upgradeableFeatures) { feature in
                        HStack(spacing: 12) {
                            Image(systemName: "lock")
                                .foregroundStyle(.tertiary)
                            Text(feature.title)
                            Spacer()
                            Text("未授权")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                    }
                    ReauthorizeButton(
                        sessionId: identity.id,
                        scopes: upgradeScopes,
                        title: String(localized: "一键补齐以上权限")
                    )
                    .foregroundStyle(Color.ocOrangeText)
                } header: {
                    Text("升级授权")
                } footer: {
                    Text("一键补齐会跳转 Cloudflare 重新授权当前账号，无需退出登录；你可在授权页自行勾选要开放的权限。")
                }
                .glassRow()
            }

            // ── 退出登录 ──
            Section {
                Button(role: .destructive) {
                    showSignOutConfirm = true
                } label: {
                    HStack(spacing: 12) {
                        TintIcon(systemImage: "rectangle.portrait.and.arrow.right", color: .red)
                        Text("退出登录")
                        if isSigningOut {
                            Spacer()
                            ProgressView()
                        }
                    }
                }
                .disabled(isSigningOut)
            } footer: {
                Text("仅退出此账号并撤销其授权，其他已登录账号不受影响。")
            }
            .glassRow()
        }
        .daybreakList()
        .navigationTitle(identity.label)
        .navigationBarTitleDisplayMode(.inline)
        .confirmationDialog("退出此账号？", isPresented: $showSignOutConfirm, titleVisibility: .visible) {
            Button("退出 \(identity.label)", role: .destructive) {
                Task { await signOut() }
            }
        } message: {
            Text(auth.sessions.count <= 1
                 ? String(localized: "这是最后一个账号，退出后将返回登录页。")
                 : String(localized: "此账号的 Token 将被撤销并从 App 移除。"))
        }
    }

    private func signOut() async {
        isSigningOut = true
        await auth.logout(sessionId: identity.id)
        isSigningOut = false
        // 还有其他身份时回设置页；全部退出由 ContentView 切到登录页
        if auth.isLoggedIn {
            dismiss()
        }
    }
}
