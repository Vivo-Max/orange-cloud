//
//  PublicHostnameFormView.swift
//  Orange Cloud
//
//  公共主机名（ingress 规则）新增 / 编辑表单（Sheet）。
//  hostname + 协议 + 本地目标 + 可选路径 → 整组回写隧道配置；新增时自动建代理 CNAME。
//

import SwiftUI

struct PublicHostnameFormView: View {

    let viewModel: TunnelDetailViewModel
    let editIndex: Int?
    let initialRule: IngressRule?

    @Environment(\.dismiss) private var dismiss
    @State private var hostname = ""
    @State private var kind: IngressServiceKind = .http
    @State private var target = "localhost:8000"
    @State private var rawService = ""
    @State private var path = ""

    private var isEditing: Bool { editIndex != nil }

    private var canSave: Bool {
        let hostOK = !hostname.trimmingCharacters(in: .whitespaces).isEmpty
        let svcOK = kind == .other
            ? !rawService.trimmingCharacters(in: .whitespaces).isEmpty
            : !target.trimmingCharacters(in: .whitespaces).isEmpty
        return hostOK && svcOK && !viewModel.isSaving
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("hostname，如 app.example.com", text: $hostname)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .font(.callout.monospaced())
                } header: {
                    Text("公共主机名")
                }

                Section {
                    Picker("协议", selection: $kind) {
                        ForEach(IngressServiceKind.allCases) { kind in
                            Text(kind.label).tag(kind)
                        }
                    }
                    if kind == .other {
                        TextField("完整服务地址，如 unix:/path.sock", text: $rawService)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .font(.callout.monospaced())
                    } else {
                        TextField(kind.targetPlaceholder, text: $target)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .font(.callout.monospaced())
                    }
                } header: {
                    Text("本地服务")
                } footer: {
                    Text("cloudflared 把命中该主机名的请求转发到这个本地地址。")
                }

                Section {
                    TextField("路径正则（可选），如 /api/.*", text: $path)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .font(.callout.monospaced())
                } header: {
                    Text("路径")
                }

                if isEditing {
                    Section {
                        Text("修改 hostname 后如需对外解析，请到 DNS 自行调整代理 CNAME。")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                }

                if let error = viewModel.error {
                    Section {
                        Text(error)
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                }
            }
            .navigationTitle(isEditing ? String(localized: "编辑公共主机名") : String(localized: "添加公共主机名"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button {
                        Task { await save() }
                    } label: {
                        if viewModel.isSaving {
                            ProgressView()
                        } else {
                            Text("保存").fontWeight(.semibold)
                        }
                    }
                    .disabled(!canSave)
                }
            }
            .onAppear(perform: populate)
            .interactiveDismissDisabled(viewModel.isSaving)
        }
    }

    private func populate() {
        guard let rule = initialRule else { return }
        hostname = rule.hostname ?? ""
        kind = rule.serviceKind
        if rule.serviceKind == .other {
            rawService = rule.service
        } else {
            target = rule.serviceTarget
        }
        path = rule.path ?? ""
    }

    private func save() async {
        viewModel.error = nil
        let trimmedHost = hostname.trimmingCharacters(in: .whitespaces)
        let service = kind == .other
            ? rawService.trimmingCharacters(in: .whitespaces)
            : kind.scheme + target.trimmingCharacters(in: .whitespaces)
        let trimmedPath = path.trimmingCharacters(in: .whitespaces)
        let rule = IngressRule(
            hostname: trimmedHost,
            service: service,
            path: trimmedPath.isEmpty ? nil : trimmedPath,
            originRequest: initialRule?.originRequest
        )
        if await viewModel.saveHostname(rule, at: editIndex) {
            dismiss()
        }
    }
}
