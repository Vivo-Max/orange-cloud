//
//  R2CreateView.swift
//  Orange Cloud
//
//  R2 存储桶创建表单（Sheet）：名称 + 可选位置 + 存储类别。
//  入口（StorageView 的 + 按钮）已按 workers-r2.write 门控，此处只管表单提交。
//

import SwiftUI

struct R2CreateView: View {

    let viewModel: R2BucketListViewModel
    let accountId: String

    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var location: R2Location = .automatic
    @State private var storageClass: R2StorageClass = .standard
    @FocusState private var nameFocused: Bool

    private var trimmedName: String {
        name.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var canCreate: Bool {
        !trimmedName.isEmpty && !accountId.isEmpty && !viewModel.isCreating
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("存储桶名称", text: $name)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .font(.callout.monospaced())
                        .focused($nameFocused)
                        .submitLabel(.done)
                        .onSubmit { Task { await create() } }
                } header: {
                    Text("名称")
                } footer: {
                    Text("名称只能包含小写字母、数字和连字符，长度 3–63 个字符。")
                }

                Section {
                    Picker("位置", selection: $location) {
                        ForEach(R2Location.allCases) { option in
                            Text(option.label).tag(option)
                        }
                    }
                } footer: {
                    Text("位置决定数据存放区域，选择「自动」由 Cloudflare 就近分配。")
                }

                Section {
                    Picker("存储类别", selection: $storageClass) {
                        ForEach(R2StorageClass.allCases) { option in
                            Text(option.label).tag(option)
                        }
                    }
                } footer: {
                    Text("标准存储适合频繁访问；低频访问存储单价更低，但读取与最短存储期有额外计费。")
                }

                if let error = viewModel.error {
                    Section {
                        Text(error)
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                }
            }
            .navigationTitle("创建存储桶")
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
        if await viewModel.create(
            accountId: accountId,
            name: trimmedName,
            locationHint: location.hint,
            storageClass: storageClass.value
        ) {
            dismiss()
        }
    }
}

// MARK: - 位置选项（locationHint）

/// R2 桶放置提示。.automatic 不传 hint，由 Cloudflare 就近分配。
private enum R2Location: String, CaseIterable, Identifiable {
    case automatic, wnam, enam, weur, eeur, apac, oc

    var id: String { rawValue }

    var hint: String? { self == .automatic ? nil : rawValue }

    var label: LocalizedStringKey {
        switch self {
        case .automatic: "自动"
        case .wnam:      "北美西部"
        case .enam:      "北美东部"
        case .weur:      "欧洲西部"
        case .eeur:      "欧洲东部"
        case .apac:      "亚太地区"
        case .oc:        "大洋洲"
        }
    }
}

// MARK: - 存储类别（storage_class）

private enum R2StorageClass: String, CaseIterable, Identifiable {
    case standard, infrequentAccess

    var id: String { rawValue }

    /// API 期望的值（默认 Standard 时不传，省一次写入）
    var value: String? {
        switch self {
        case .standard:         nil
        case .infrequentAccess: "InfrequentAccess"
        }
    }

    var label: LocalizedStringKey {
        switch self {
        case .standard:         "标准存储"
        case .infrequentAccess: "低频访问存储"
        }
    }
}
