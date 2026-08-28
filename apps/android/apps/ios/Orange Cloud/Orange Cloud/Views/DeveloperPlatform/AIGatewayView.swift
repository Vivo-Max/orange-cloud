//
//  AIGatewayView.swift
//  Orange Cloud
//
//  Cloudflare AI Gateway 列表 + 新建 / 删除。account 级，读 aig.read / 写 aig.write。
//

import SwiftUI

struct AIGatewayView: View {

    let session: SessionStore

    @Environment(AuthManager.self) private var auth
    @State private var vm: AIGatewayViewModel?
    @State private var showCreate = false
    @State private var deleteTarget: AIGateway?
    @State private var writeDenied = false

    private var canWrite: Bool { auth.hasScope("aig.write") }

    var body: some View {
        Group {
            if let vm { content(vm) } else { ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity) }
        }
        .background { SkyBackground() }
        .navigationTitle("AI Gateway")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if vm != nil {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("新建网关", systemImage: "plus") {
                        if canWrite { showCreate = true } else { writeDenied = true }
                    }
                }
            }
        }
        .sheet(isPresented: $showCreate) {
            if let vm { AIGatewayCreateView(viewModel: vm) }
        }
        .alert("权限不足", isPresented: $writeDenied) {
            Button("好", role: .cancel) {}
        } message: {
            Text("当前授权未包含 AI Gateway 写权限（aig.write）。\n请在设置中退出登录后重新授权以启用此功能。")
        }
        .confirmationDialog(
            deleteTarget.map { String(localized: "删除网关「\($0.id)」？") } ?? "",
            isPresented: Binding(get: { deleteTarget != nil }, set: { if !$0 { deleteTarget = nil } }),
            titleVisibility: .visible
        ) {
            Button("删除", role: .destructive) {
                if let g = deleteTarget, let vm { Task { await vm.delete(g) } }
            }
        } message: {
            Text("删除后该网关的端点与日志将不可用，不可撤销。")
        }
        .task {
            await session.ensureAccounts()
            guard vm == nil else { return }
            let model = AIGatewayViewModel(service: session.aiGatewayService, accountId: session.selectedAccount?.id)
            vm = model
            await model.load()
        }
    }

    @ViewBuilder
    private func content(_ vm: AIGatewayViewModel) -> some View {
        if vm.isLoading && !vm.loaded {
            ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if vm.gateways.isEmpty {
            ContentUnavailableView {
                Label("没有 AI Gateway", systemImage: "brain.head.profile")
            } description: {
                Text(vm.error ?? String(localized: "该账号下还没有 AI Gateway。"))
            } actions: {
                if canWrite {
                    Button("新建网关") { showCreate = true }
                        .buttonStyle(.borderedProminent).tint(Color.ocOrangePressed).fontWeight(.bold)
                }
            }
        } else {
            List {
                Section {
                    ForEach(vm.gateways) { gateway in
                        VStack(alignment: .leading, spacing: 3) {
                            Text(gateway.id).font(.callout.weight(.semibold).monospaced()).lineLimit(1)
                            Text(subtitle(gateway)).font(.caption).foregroundStyle(.secondary).lineLimit(1)
                        }
                        .padding(.vertical, 2)
                        .swipeActions(edge: .trailing) {
                            if canWrite {
                                Button(role: .destructive) { deleteTarget = gateway } label: {
                                    Label("删除", systemImage: "trash")
                                }
                            }
                        }
                    }
                } footer: {
                    Text("AI Gateway 为 LLM 调用提供缓存、限速与日志。")
                }
                .glassRow()
            }
            .daybreakList()
            .refreshable { await vm.load() }
            .sensoryFeedback(.success, trigger: vm.didChange)
        }
    }

    private func subtitle(_ g: AIGateway) -> String {
        var parts: [String] = []
        if (g.collectLogs ?? false) { parts.append(String(localized: "日志开")) }
        if let ttl = g.cacheTtl, ttl > 0 { parts.append(String(localized: "缓存 \(ttl)s")) }
        if let lim = g.rateLimitingLimit, lim > 0 { parts.append(String(localized: "限速 \(lim)")) }
        return parts.isEmpty ? String(localized: "默认配置") : parts.joined(separator: " · ")
    }
}

private struct AIGatewayCreateView: View {
    let viewModel: AIGatewayViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var gatewayId = ""
    @State private var collectLogs = true
    @State private var cacheTtl = 0
    @State private var rateLimitingLimit = 0
    @State private var rateLimitingInterval = 60

    private var trimmedId: String { gatewayId.trimmingCharacters(in: .whitespacesAndNewlines) }
    private var canSave: Bool { !trimmedId.isEmpty && !viewModel.isSaving }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("网关 ID", text: $gatewayId)
                        .textInputAutocapitalization(.never).autocorrectionDisabled()
                        .font(.callout.monospaced())
                } header: {
                    Text("标识")
                } footer: {
                    Text("小写字母 / 数字 / 连字符，创建后不可更改。")
                }
                Section("日志与缓存") {
                    Toggle("收集日志", isOn: $collectLogs)
                    Stepper("缓存 TTL：\(cacheTtl) 秒", value: $cacheTtl, in: 0...86400, step: 60)
                }
                Section {
                    Stepper("限速上限：\(rateLimitingLimit == 0 ? String(localized: "不限") : "\(rateLimitingLimit)")",
                            value: $rateLimitingLimit, in: 0...100000, step: 10)
                    Stepper("限速窗口：\(rateLimitingInterval) 秒", value: $rateLimitingInterval, in: 1...3600, step: 10)
                } header: {
                    Text("限速")
                } footer: {
                    Text("上限为 0 表示不限速。")
                }
                if let error = viewModel.error {
                    Section { Text(error).font(.footnote).foregroundStyle(.red) }
                }
            }
            .navigationTitle("新建网关")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("取消") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button {
                        Task { await save() }
                    } label: {
                        if viewModel.isSaving { ProgressView() } else { Text("创建").fontWeight(.semibold) }
                    }
                    .disabled(!canSave)
                }
            }
            .interactiveDismissDisabled(viewModel.isSaving)
            .onAppear { viewModel.error = nil }
        }
    }

    private func save() async {
        let body = AIGatewayCreate(
            id: trimmedId,
            cacheInvalidateOnUpdate: false,
            cacheTtl: cacheTtl,
            collectLogs: collectLogs,
            rateLimitingInterval: rateLimitingInterval,
            rateLimitingLimit: rateLimitingLimit
        )
        if await viewModel.create(body) { dismiss() }
    }
}
