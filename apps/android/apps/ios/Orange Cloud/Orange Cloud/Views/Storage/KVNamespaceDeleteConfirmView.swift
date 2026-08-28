//
//  KVNamespaceDeleteConfirmView.swift
//  Orange Cloud
//
//  KV 命名空间删除二次确认（Sheet）：必须原样输入名称才能启用删除。
//  入口（StorageView 滑动删除）已按 workers-kv-storage.write 门控。
//  删除连同命名空间内全部键值，不可恢复。
//

import SwiftUI

struct KVNamespaceDeleteConfirmView: View {

    let namespace: KVNamespace
    let viewModel: KVNamespaceListViewModel
    let accountId: String

    @Environment(\.dismiss) private var dismiss
    @State private var typedName = ""
    @FocusState private var fieldFocused: Bool

    private var nameMatches: Bool {
        typedName.trimmingCharacters(in: .whitespacesAndNewlines) == namespace.title
    }

    private var canDelete: Bool {
        nameMatches && !accountId.isEmpty && !viewModel.isDeleting
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    VStack(spacing: 12) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .font(.system(size: 44))
                            .symbolRenderingMode(.hierarchical)
                            .foregroundStyle(.red)
                        Text("永久删除命名空间")
                            .font(.headline)
                        Text("此操作将永久删除命名空间 \(namespace.title) 及其全部键值，无法撤销，也无法恢复。")
                            .font(.callout)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .listRowBackground(Color.clear)
                }

                Section {
                    TextField(namespace.title, text: $typedName)
                        .autocorrectionDisabled()
                        .font(.callout)
                        .focused($fieldFocused)
                        .submitLabel(.done)
                        .onSubmit { Task { await performDelete() } }
                } header: {
                    Text("输入命名空间名称以确认")
                } footer: {
                    Text("请输入 \(namespace.title) 以启用删除。")
                }

                if let error = viewModel.error {
                    Section {
                        Text(error)
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                }

                Section {
                    Button(role: .destructive) {
                        Task { await performDelete() }
                    } label: {
                        HStack {
                            Spacer()
                            if viewModel.isDeleting {
                                ProgressView()
                            } else {
                                Label("永久删除", systemImage: "trash")
                                    .fontWeight(.semibold)
                            }
                            Spacer()
                        }
                    }
                    .disabled(!canDelete)
                }
            }
            .navigationTitle("删除命名空间")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
            }
            .onAppear { fieldFocused = true }
            .interactiveDismissDisabled(viewModel.isDeleting)
        }
    }

    private func performDelete() async {
        guard canDelete else { return }
        fieldFocused = false
        if await viewModel.delete(accountId: accountId, namespace: namespace) {
            dismiss()
        }
    }
}
