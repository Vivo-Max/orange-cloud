//
//  AccessAppsView.swift
//  Orange Cloud
//
//  Zero Trust Access 应用 CRUD（自托管）。account 级，读 access.read / 写 access.write。
//

import SwiftUI

struct AccessAppsView: View {

    let session: SessionStore

    @Environment(AuthManager.self) private var auth
    @State private var vm: AccessAppsViewModel?
    @State private var showCreate = false
    @State private var editTarget: AccessApp?
    @State private var deleteTarget: AccessApp?
    @State private var writeDenied = false

    private var canWrite: Bool { auth.hasScope("access.write") }

    var body: some View {
        Group {
            if let vm {
                content(vm)
            } else {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .background { SkyBackground() }
        .navigationTitle("Access 应用")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if vm != nil {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("新建应用", systemImage: "plus") {
                        if canWrite { showCreate = true } else { writeDenied = true }
                    }
                }
            }
        }
        .sheet(isPresented: $showCreate) {
            if let vm {
                AccessAppEditorView(mode: .create, viewModel: vm) {}
            }
        }
        .sheet(item: $editTarget) { app in
            if let vm {
                AccessAppEditorView(mode: .edit(appId: app.id), viewModel: vm) {}
            }
        }
        .alert("权限不足", isPresented: $writeDenied) {
            Button("好", role: .cancel) {}
        } message: {
            Text("当前授权未包含 Access 写权限（access.write）。\n请在设置中退出登录后重新授权以启用此功能。")
        }
        .confirmationDialog(
            deleteTarget.map { String(localized: "删除应用「\($0.name ?? $0.publicHostnames.first ?? "")」？") } ?? "",
            isPresented: Binding(get: { deleteTarget != nil }, set: { if !$0 { deleteTarget = nil } }),
            titleVisibility: .visible
        ) {
            Button("删除", role: .destructive) {
                if let app = deleteTarget, let vm { Task { await vm.delete(app) } }
            }
        } message: {
            Text("将移除该 Access 应用（其引用的可复用策略会保留）。")
        }
        .task {
            await session.ensureAccounts()
            guard vm == nil else { return }
            let model = AccessAppsViewModel(service: session.zeroTrustService, accountId: session.selectedAccount?.id)
            vm = model
            await model.load()
        }
    }

    @ViewBuilder
    private func content(_ vm: AccessAppsViewModel) -> some View {
        if vm.isLoading && !vm.loaded {
            ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if vm.apps.isEmpty {
            ContentUnavailableView {
                Label("没有 Access 应用", systemImage: "lock.shield")
            } description: {
                Text(vm.error ?? String(localized: "该账号下还没有受 Access 保护的应用。"))
            } actions: {
                if canWrite {
                    Button("新建应用") { showCreate = true }
                        .buttonStyle(.borderedProminent)
                        .tint(Color.ocOrangePressed)
                        .fontWeight(.bold)
                }
            }
        } else {
            List {
                Section {
                    ForEach(vm.apps) { app in
                        Button {
                            if canWrite { editTarget = app } else { writeDenied = true }
                        } label: {
                            row(app)
                        }
                        .buttonStyle(.plain)
                        .swipeActions(edge: .trailing) {
                            if canWrite {
                                Button(role: .destructive) { deleteTarget = app } label: {
                                    Label("删除", systemImage: "trash")
                                }
                            }
                        }
                    }
                } footer: {
                    Text("受 Cloudflare Access 保护的应用。点按编辑、左滑删除。")
                }
                .glassRow()
            }
            .daybreakList()
            .refreshable { await vm.load() }
            .sensoryFeedback(.success, trigger: vm.didChange)
        }
    }

    private func row(_ app: AccessApp) -> some View {
        HStack(spacing: 12) {
            TintIcon(systemImage: "lock.shield", color: .ocOrange)
            VStack(alignment: .leading, spacing: 3) {
                Text(app.name?.isEmpty == false ? app.name! : (app.publicHostnames.first ?? "—"))
                    .font(.callout).foregroundStyle(.primary).lineLimit(1)
                if let first = app.publicHostnames.first {
                    // 多主机名时主机名后缀「+N」，提示这个应用绑了不止一个公共主机名
                    let extra = app.publicHostnames.count - 1
                    Text(extra > 0 ? "\(first) +\(extra)" : first)
                        .font(.caption).foregroundStyle(.secondary).lineLimit(1)
                }
            }
            Spacer(minLength: 8)
            Text(app.typeLabel)
                .font(.caption2).foregroundStyle(.secondary)
                .padding(.horizontal, 8).padding(.vertical, 3)
                .background(Color.secondary.opacity(0.12), in: Capsule())
            Image(systemName: "chevron.right").font(.caption.weight(.semibold)).foregroundStyle(.tertiary)
        }
        .padding(.vertical, 2)
    }
}
