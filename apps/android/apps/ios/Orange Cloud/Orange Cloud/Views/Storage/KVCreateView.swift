//
//  KVCreateView.swift
//  Orange Cloud
//
//  KV 命名空间创建表单（Sheet）：仅名称。
//  入口（StorageView 的 + 按钮）已按 workers-kv-storage.write 门控，此处只管表单提交。
//

import SwiftUI

struct KVCreateView: View {

    let viewModel: KVNamespaceListViewModel
    let accountId: String

    @Environment(\.dismiss) private var dismiss
    @State private var title = ""
    @FocusState private var titleFocused: Bool

    private var trimmedTitle: String {
        title.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var canCreate: Bool {
        !trimmedTitle.isEmpty && !accountId.isEmpty && !viewModel.isCreating
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("命名空间名称", text: $title)
                        .autocorrectionDisabled()
                        .font(.callout)
                        .focused($titleFocused)
                        .submitLabel(.done)
                        .onSubmit { Task { await create() } }
                } header: {
                    Text("名称")
                } footer: {
                    Text("为命名空间起一个便于识别的名字。")
                }

                if let error = viewModel.error {
                    Section {
                        Text(error)
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                }
            }
            .navigationTitle("创建命名空间")
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
            .onAppear { titleFocused = true }
            .interactiveDismissDisabled(viewModel.isCreating)
        }
    }

    private func create() async {
        guard canCreate else { return }
        titleFocused = false
        if await viewModel.create(accountId: accountId, title: trimmedTitle) {
            dismiss()
        }
    }
}
