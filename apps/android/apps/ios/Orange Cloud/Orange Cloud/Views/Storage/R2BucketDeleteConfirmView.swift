//
//  R2BucketDeleteConfirmView.swift
//  Orange Cloud
//
//  R2 存储桶删除二次确认（Sheet）：必须原样输入桶名称才能启用删除。
//  入口（StorageView 滑动删除）已按 workers-r2.write 门控。
//  注意：Cloudflare 要求桶内对象为空才能删除，否则服务端报错。
//

import SwiftUI

struct R2BucketDeleteConfirmView: View {

    let bucket: R2Bucket
    let viewModel: R2BucketListViewModel
    let accountId: String

    @Environment(\.dismiss) private var dismiss
    @State private var typedName = ""
    @FocusState private var fieldFocused: Bool

    private var nameMatches: Bool {
        typedName.trimmingCharacters(in: .whitespacesAndNewlines) == bucket.name
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
                        Text("永久删除存储桶")
                            .font(.headline)
                        Text("此操作将永久删除存储桶 \(bucket.name)，无法撤销。桶内必须为空，否则删除会失败。")
                            .font(.callout)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .listRowBackground(Color.clear)
                }

                Section {
                    TextField(bucket.name, text: $typedName)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .font(.callout.monospaced())
                        .focused($fieldFocused)
                        .submitLabel(.done)
                        .onSubmit { Task { await performDelete() } }
                } header: {
                    Text("输入存储桶名称以确认")
                } footer: {
                    Text("请输入 \(bucket.name) 以启用删除。")
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
            .navigationTitle("删除存储桶")
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
        if await viewModel.delete(accountId: accountId, bucket: bucket) {
            dismiss()
        }
    }
}
