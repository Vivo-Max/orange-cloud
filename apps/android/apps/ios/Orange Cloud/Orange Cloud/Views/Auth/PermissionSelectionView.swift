//
//  PermissionSelectionView.swift
//  Orange Cloud
//
//  OAuth 授权前的权限选择：按需勾选功能与读写级别，动态生成最小 scope。
//

import SwiftUI

struct PermissionSelectionView: View {

    /// 添加第二个身份时必须为 true（强制全新登录页，避免复用浏览器 Cookie）
    var freshLogin = false

    @Environment(AuthManager.self) private var auth
    @State private var viewModel = PermissionSelectionViewModel()
    @State private var showScopeDetail = false

    var body: some View {
        List {
            // 说明 Section
            Section {
                VStack(alignment: .leading, spacing: 8) {
                    Label("选择授权范围", systemImage: "lock.shield")
                        .font(.headline)
                    Text("只申请你需要的权限。授权后可在 Cloudflare Dashboard 随时撤销。")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
                .padding(.vertical, 4)
            }
            .glassRow()

            // 快捷预设：一键设定所有功能的读写级别
            Section("快捷设置") {
                HStack(spacing: 8) {
                    quickPresetButton("全部只读", systemImage: "eye") { viewModel.applyAllReadOnly() }
                    quickPresetButton("全部读写", systemImage: "square.and.pencil") { viewModel.applyAllReadWrite() }
                    quickPresetButton("仅必选", systemImage: "minus.circle") { viewModel.applyMinimal() }
                }
                .padding(.vertical, 2)
            }
            .glassRow()

            // 功能权限列表
            Section("功能模块") {
                ForEach($viewModel.permissions) { $permission in
                    FeaturePermissionRow(
                        permission: $permission,
                        onToggle: { viewModel.toggleFeature(id: permission.id) },
                        onToggleEdit: { viewModel.toggleEditPermission(id: permission.id) }
                    )
                }
            }
            .glassRow()

            // 当前 Scope 预览
            Section {
                Button {
                    withAnimation { showScopeDetail.toggle() }
                } label: {
                    HStack {
                        Label("已选 \(viewModel.selectedScopes.count) 个权限", systemImage: "list.bullet")
                            .font(.footnote)
                        Spacer()
                        Image(systemName: showScopeDetail ? "chevron.up" : "chevron.down")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
                .foregroundStyle(.primary)

                if showScopeDetail {
                    ForEach(viewModel.selectedScopes, id: \.self) { scope in
                        Text(scope)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .padding(.vertical, 1)
                    }
                }
            }
            .glassRow()
        }
        .daybreakList()
        .navigationTitle("授权设置")
        .navigationBarTitleDisplayMode(.inline)
        .safeAreaInset(edge: .bottom) {
            // 底部固定授权按钮
            VStack(spacing: 0) {
                Divider()
                Button {
                    Task {
                        await auth.login(scopeString: viewModel.scopeString, freshLogin: freshLogin)
                    }
                } label: {
                    HStack {
                        if auth.isLoading {
                            ProgressView().tint(.white)
                        }
                        Text("使用 Cloudflare 账号授权")
                            .fontWeight(.bold)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.ocOrangePressed)
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .disabled(auth.isLoading)
                .padding()
                .background(.regularMaterial)
            }
        }
        .alert("登录失败", isPresented: .init(
            get: { auth.errorMessage != nil },
            set: { if !$0 { auth.errorMessage = nil } }
        )) {
            Button("好", role: .cancel) {}
        } message: {
            Text(auth.errorMessage ?? "")
        }
    }

    /// 快捷预设小按钮（等宽铺满一行）
    private func quickPresetButton(_ title: LocalizedStringKey, systemImage: String, action: @escaping () -> Void) -> some View {
        Button {
            withAnimation(.smooth) { action() }
        } label: {
            VStack(spacing: 4) {
                Image(systemName: systemImage).font(.callout)
                Text(title).font(.caption.weight(.medium))
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .background(Color.ocOrange.opacity(0.12), in: RoundedRectangle(cornerRadius: 10))
            .foregroundStyle(Color.ocOrangeText)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - 单行权限行

struct FeaturePermissionRow: View {
    @Binding var permission: FeaturePermission
    let onToggle:     () -> Void
    let onToggleEdit: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Label {
                    VStack(alignment: .leading, spacing: 2) {
                        HStack {
                            Text(permission.title)
                                .font(.body)
                            if permission.isRequired {
                                Text("必选")
                                    .font(.caption2.weight(.semibold))
                                    .foregroundStyle(Color.ocOrangeText)
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2)
                                    .background(Color.ocOrange.opacity(0.16))
                                    .clipShape(Capsule())
                            }
                        }
                        Text(permission.description)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                } icon: {
                    Image(systemName: permission.icon)
                        .foregroundStyle(Color.ocOrange)
                        .frame(width: 24)
                }

                Spacer()

                Toggle("", isOn: Binding(
                    get: { permission.isEnabled },
                    set: { _ in onToggle() }
                ))
                .disabled(permission.isRequired)
                .labelsHidden()
                .accessibilityLabel(permission.title)
            }

            // 编辑权限切换（仅在功能开启且支持编辑时显示）
            if permission.isEnabled && permission.hasEditOption {
                Picker("权限级别", selection: Binding(
                    get: { permission.canEdit },
                    set: { _ in onToggleEdit() }
                )) {
                    Text("只读").tag(false)
                    Text("读写").tag(true)
                }
                .pickerStyle(.segmented)
                .padding(.leading, 34)
            }
        }
        .padding(.vertical, 2)
        .opacity(permission.isEnabled ? 1 : 0.4)
    }
}

#Preview {
    NavigationStack {
        PermissionSelectionView()
            .environment(AuthManager())
    }
}
