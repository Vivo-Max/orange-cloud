//
//  PagesCreateView.swift
//  Orange Cloud
//
//  Pages 项目创建表单（Sheet）：名称 + 生产分支。
//  仅创建一个 Direct Upload 空项目——手机端无法上传构建产物或连接 Git 仓库，
//  创建后需用 Wrangler 或 Dashboard 完成首次部署。
//  入口（PagesProjectListView 的 + 按钮）已按 page.write 门控。
//

import SwiftUI

struct PagesCreateView: View {

    let viewModel: PagesProjectListViewModel
    let accountId: String

    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var productionBranch = "main"
    @FocusState private var nameFocused: Bool

    private var trimmedName: String {
        name.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var trimmedBranch: String {
        productionBranch.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var canCreate: Bool {
        !trimmedName.isEmpty && !trimmedBranch.isEmpty && !accountId.isEmpty && !viewModel.isCreating
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("项目名称", text: $name)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .font(.callout.monospaced())
                        .focused($nameFocused)
                        .submitLabel(.next)
                } header: {
                    Text("名称")
                } footer: {
                    Text("名称只能包含小写字母、数字和连字符。")
                }

                Section {
                    TextField("生产分支", text: $productionBranch)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .font(.callout.monospaced())
                        .submitLabel(.done)
                        .onSubmit { Task { await create() } }
                } header: {
                    Text("生产分支")
                }

                Section {
                    Label("将创建一个空的「直接上传」项目。创建后需使用 Wrangler 或 Cloudflare Dashboard 上传文件以完成首次部署。", systemImage: "info.circle")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }

                if let error = viewModel.error {
                    Section {
                        Text(error)
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                }
            }
            .navigationTitle("创建项目")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button {
                        Task { await create() }
                    } label: {
                        if viewModel.isCreating {
                            ProgressView()
                        } else {
                            Text("创建").fontWeight(.semibold)
                        }
                    }
                    .disabled(!canCreate)
                }
            }
            .onAppear { nameFocused = true }
            .interactiveDismissDisabled(viewModel.isCreating)
        }
    }

    private func create() async {
        guard canCreate else { return }
        nameFocused = false
        if await viewModel.create(accountId: accountId, name: trimmedName, productionBranch: trimmedBranch) {
            dismiss()
        }
    }
}
