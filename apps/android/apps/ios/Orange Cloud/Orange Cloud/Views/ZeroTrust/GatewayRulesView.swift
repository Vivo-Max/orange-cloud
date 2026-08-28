//
//  GatewayRulesView.swift
//  Orange Cloud
//
//  Zero Trust Gateway 策略 CRUD。account 级，读 teams.read / 写 teams.write。
//

import SwiftUI

struct GatewayRulesView: View {

    let session: SessionStore

    @Environment(AuthManager.self) private var auth
    @State private var vm: GatewayRulesViewModel?
    @State private var showCreate = false
    @State private var editTarget: GatewayRule?
    @State private var deleteTarget: GatewayRule?
    @State private var writeDenied = false

    private var canWrite: Bool { auth.hasScope("teams.write") }

    var body: some View {
        Group {
            if let vm {
                content(vm)
            } else {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .background { SkyBackground() }
        .navigationTitle("Gateway 策略")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if vm != nil {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("新建策略", systemImage: "plus") {
                        if canWrite { showCreate = true } else { writeDenied = true }
                    }
                }
            }
        }
        .sheet(isPresented: $showCreate) {
            if let vm { GatewayRuleEditorView(mode: .create, viewModel: vm) }
        }
        .sheet(item: $editTarget) { rule in
            if let vm { GatewayRuleEditorView(mode: .edit(rule), viewModel: vm) }
        }
        .alert("权限不足", isPresented: $writeDenied) {
            Button("好", role: .cancel) {}
        } message: {
            Text("当前授权未包含 Zero Trust 写权限（teams.write）。\n请在设置中退出登录后重新授权以启用此功能。")
        }
        .confirmationDialog(
            deleteTarget.map { String(localized: "删除策略「\($0.name ?? String(localized: "未命名策略"))」？") } ?? "",
            isPresented: Binding(get: { deleteTarget != nil }, set: { if !$0 { deleteTarget = nil } }),
            titleVisibility: .visible
        ) {
            Button("删除", role: .destructive) {
                if let rule = deleteTarget, let vm { Task { await vm.delete(rule) } }
            }
        } message: {
            Text("删除后该策略将立即停止生效，不可撤销。")
        }
        .task {
            await session.ensureAccounts()
            guard vm == nil else { return }
            let model = GatewayRulesViewModel(service: session.zeroTrustService, accountId: session.selectedAccount?.id)
            vm = model
            await model.load()
        }
    }

    @ViewBuilder
    private func content(_ vm: GatewayRulesViewModel) -> some View {
        if vm.isLoading && !vm.loaded {
            ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if vm.rules.isEmpty {
            ContentUnavailableView {
                Label("没有 Gateway 策略", systemImage: "shield.lefthalf.filled")
            } description: {
                Text(vm.error ?? String(localized: "该账号下还没有 Gateway（DNS / HTTP / 网络）策略。"))
            } actions: {
                if canWrite {
                    Button("新建策略") { showCreate = true }
                        .buttonStyle(.borderedProminent)
                        .tint(Color.ocOrangePressed)
                        .fontWeight(.bold)
                }
            }
        } else {
            List {
                Section {
                    ForEach(vm.rules) { rule in
                        Button {
                            if canWrite { editTarget = rule } else { writeDenied = true }
                        } label: {
                            row(rule)
                        }
                        .buttonStyle(.plain)
                        .swipeActions(edge: .trailing) {
                            if canWrite {
                                Button(role: .destructive) {
                                    deleteTarget = rule
                                } label: { Label("删除", systemImage: "trash") }
                                Button {
                                    Task { await vm.toggle(rule) }
                                } label: {
                                    Label(rule.isEnabled ? String(localized: "停用") : String(localized: "启用"),
                                          systemImage: rule.isEnabled ? "pause" : "play")
                                }
                                .tint(rule.isEnabled ? .gray : .ocOrange)
                            }
                        }
                    }
                } footer: {
                    Text("DNS / HTTP / 网络过滤策略，按优先级自上而下匹配。左滑可启停 / 删除，点按编辑。")
                }
                .glassRow()
            }
            .daybreakList()
            .refreshable { await vm.load() }
            .sensoryFeedback(.success, trigger: vm.didChange)
        }
    }

    private func row(_ rule: GatewayRule) -> some View {
        HStack(spacing: 12) {
            TintIcon(systemImage: "shield.lefthalf.filled", color: rule.isEnabled ? .ocOrange : .gray)
            VStack(alignment: .leading, spacing: 3) {
                Text(rule.name?.isEmpty == false ? rule.name! : String(localized: "未命名策略"))
                    .font(.callout).foregroundStyle(.primary).lineLimit(1)
                Text("\(rule.kindLabel) · \(rule.actionLabel)")
                    .font(.caption).foregroundStyle(.secondary).lineLimit(1)
            }
            Spacer(minLength: 8)
            if !rule.isEnabled {
                Text("已停用").font(.caption2).foregroundStyle(.tertiary)
            }
            Image(systemName: "chevron.right").font(.caption.weight(.semibold)).foregroundStyle(.tertiary)
        }
        .padding(.vertical, 2)
    }
}
